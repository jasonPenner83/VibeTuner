package com.jpenner.vibetuner.data.sync

import android.util.Log
import com.jpenner.vibetuner.data.repository.AddonRepository
import com.jpenner.vibetuner.data.repository.ChannelOverrideStore
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

/** One sync_docs row as returned by PostgREST. */
@Serializable
data class SyncDocRow(
    @SerialName("profile_id") val profileId: String,
    val kind: String,
    val payload: JsonElement,
    val deleted: Boolean = false,
    @SerialName("updated_at") val updatedAt: String,
)

/** The shape we push: updated_at omitted — the server trigger stamps it. */
@Serializable
data class SyncDocPush(
    @SerialName("profile_id") val profileId: String,
    val kind: String,
    val payload: JsonElement,
    val deleted: Boolean = false,
)

private const val TAG = "VibeTuner Sync"
private const val TABLE = "sync_docs"
val DOC_KINDS = listOf("profile", "addons", "overrides")

/**
 * The only class that reads/writes sync_docs. Last-push-wins with client-side
 * dirty protection (see decideDocAction). All store writes here go through the
 * stores' import APIs, which never notify SyncHooks — a pull can't echo.
 */
class SyncEngine(
    private val client: SupabaseClient,
    private val stateStore: SyncStateStore,
    private val profileStore: ProfileStore,
    private val addonRepository: AddonRepository,
    private val overrideStore: ChannelOverrideStore,
    private val profileRepository: ProfileRepository,
) {

    /** Fetch every remote doc, apply/skip per merge rules, reconcile never-pushed locals, push. */
    suspend fun pullAll() {
        val rows = client.from(TABLE).select().decodeList<SyncDocRow>()
        val remoteKeys = rows.map { "${it.profileId}:${it.kind}" }.toSet()

        // Profiles first so addon/override imports land on profiles that exist.
        val ordered = rows.sortedBy { DOC_KINDS.indexOf(it.kind) }
        val tombstoned = rows.filter { it.kind == "profile" && it.deleted }.map { it.profileId }.toSet()
        for (row in ordered) {
            if (row.profileId in tombstoned && row.kind != "profile") continue
            val state = stateStore.get(row.profileId, row.kind)
            when (decideDocAction(row.updatedAt, row.deleted, state, localDocExists(row.profileId, row.kind))) {
                SyncAction.ApplyRemote -> {
                    applyRemote(row)
                    stateStore.setLastSeen(row.profileId, row.kind, row.updatedAt)
                }
                SyncAction.ApplyTombstone -> {
                    applyTombstone(row.profileId)
                    stateStore.setLastSeen(row.profileId, row.kind, row.updatedAt)
                }
                SyncAction.PushLocal -> Unit          // handled by pushDirty below
                SyncAction.Skip -> Unit
            }
        }

        // Local docs the cloud has never seen (first sign-in seeding). Deliberate
        // trade-off: a device is assumed to stay in one household, so signing in
        // as a DIFFERENT account unions this device's existing local profiles
        // into that account's cloud. Revisit if a build is ever shared between
        // households (guard: persist the user id and skip seeding on change).
        localDocKeys().forEach { (profileId, kind) ->
            if ("$profileId:$kind" !in remoteKeys) stateStore.markDirty(profileId, kind)
        }

        pushDirty()
        Log.i(TAG, "✅ pull complete: ${rows.size} remote docs")
    }

    /** Upsert every dirty doc; the returned server timestamps become lastSeen. */
    suspend fun pushDirty() {
        val dirty = stateStore.all().filterValues { it.dirty }
        if (dirty.isEmpty()) return

        val pushes = dirty.mapNotNull { (key, state) ->
            val profileId = key.substringBeforeLast(':')
            val kind = key.substringAfterLast(':')
            if (state.deleted) {
                SyncDocPush(profileId, "profile", JsonObject(emptyMap()), deleted = true)
            } else {
                exportPayload(profileId, kind)?.let { SyncDocPush(profileId, kind, it) }
            }
        }
        if (pushes.isEmpty()) return

        val returned = client.from(TABLE)
            .upsert(pushes) { select() }
            .decodeList<SyncDocRow>()
        returned.forEach {
            if (it.deleted) stateStore.remove(it.profileId)
            else stateStore.clearDirty(it.profileId, it.kind, it.updatedAt)
        }
        Log.i(TAG, "✅ pushed ${returned.size} docs")
    }

    // ── local doc inventory ────────────────────────────────────────────────────

    private fun localDocExists(profileId: String, kind: String): Boolean = when (kind) {
        "profile" -> profileStore.byId(profileId) != null
        "addons" -> addonRepository.exportJson(profileId) != null
        "overrides" -> overrideStore.exportJson(profileId) != null
        else -> false
    }

    private fun localDocKeys(): List<Pair<String, String>> =
        profileStore.profilesNow().flatMap { p ->
            DOC_KINDS.filter { localDocExists(p.id, it) }.map { p.id to it }
        }

    private fun exportPayload(profileId: String, kind: String): JsonElement? {
        val raw = when (kind) {
            "profile" -> profileStore.exportProfile(profileId)?.toString()
            "addons" -> addonRepository.exportJson(profileId)
            "overrides" -> overrideStore.exportJson(profileId)
            else -> null
        } ?: return null
        return runCatching { Json.parseToJsonElement(raw) }
            .onFailure { Log.e(TAG, "🔥 export parse error ($profileId/$kind): ${it.message}") }
            .getOrNull()
    }

    // ── applying remote state ──────────────────────────────────────────────────

    private fun applyRemote(row: SyncDocRow) {
        val raw = row.payload.toString()
        when (row.kind) {
            "profile" -> runCatching { profileStore.importProfile(JSONObject(raw)) }
                .onFailure { Log.e(TAG, "🔥 profile import error: ${it.message}") }
            "addons" -> addonRepository.importJson(row.profileId, raw)
            "overrides" -> overrideStore.importJson(row.profileId, raw)
        }
    }

    /** Delete the profile and its per-profile files; repoint the active profile if needed. */
    private fun applyTombstone(profileId: String) {
        profileStore.removeFromSync(profileId)
        // Guard may have kept it (last profile) — if so, leave its files alone.
        if (profileStore.byId(profileId) != null) return
        addonRepository.deleteProfileData(profileId)
        overrideStore.deleteProfileData(profileId)
        stateStore.remove(profileId)
        if (profileRepository.activeProfileId() == profileId) {
            profileStore.profilesNow().firstOrNull()?.let { profileRepository.setActiveProfile(it.id) }
        }
        Log.i(TAG, "🪦 applied tombstone for $profileId")
    }
}
