package com.jpenner.vibetuner.data.repository

import android.content.Context
import android.util.Log
import com.jpenner.vibetuner.data.api.ManifestResult
import com.jpenner.vibetuner.data.api.manifestResultOf
import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import com.jpenner.vibetuner.data.model.stremio.StremioManifest
import com.jpenner.vibetuner.data.model.stremio.addonFromJson
import com.jpenner.vibetuner.data.model.stremio.parseManifest
import com.jpenner.vibetuner.data.model.stremio.pruneSelections
import com.jpenner.vibetuner.data.model.stremio.seedRequiredSelections
import com.jpenner.vibetuner.data.model.stremio.selectionKey
import com.jpenner.vibetuner.data.model.stremio.toJson
import com.jpenner.vibetuner.data.model.stremio.withAllOptions
import com.jpenner.vibetuner.data.sync.SyncHooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Per-profile store of installed Stremio addons. Each profile gets its own file
 * (`vibetuner_addons_<profileId>.json`) holding the addon list with cached manifests,
 * so catalogs can be listed without re-fetching. New profiles are seeded with Cinemeta
 * so the guide is never empty on first run.
 */
class AddonRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun fileFor(profileId: String): File =
        File(context.filesDir, "vibetuner_addons_${profileId.ifBlank { "default" }}.json")

    // ── Read ───────────────────────────────────────────────────────────────────

    /** The addons installed for [profileId] (empty if none / unreadable). */
    fun getAddons(profileId: String): List<StremioAddon> {
        val file = fileFor(profileId)
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::addonFromJson) }
        }.getOrElse {
            Log.e("VibeTuner Addons", "🔥 Read error: ${it.message}")
            emptyList()
        }
    }

    /** Base URLs of the profile's enabled addons, in priority order (for stream resolution). */
    fun enabledBaseUrls(profileId: String): List<String> =
        getAddons(profileId).filter { it.enabled }.map { it.baseUrl }

    /** Enabled channel specs (base catalogs + selected option sub-channels) — the source of truth for which channels exist. */
    fun enabledCatalogs(profileId: String, allowAdult: Boolean = false): List<CatalogChannelSpec> =
        expandCatalogSpecs(getAddons(profileId), allowAdult)

    // ── Write ────────────────────────────────────────────────────────────────────

    private fun save(profileId: String, addons: List<StremioAddon>) {
        runCatching {
            val arr = JSONArray()
            addons.forEach { arr.put(it.toJson()) }
            fileFor(profileId).writeText(arr.toString())
        }.onFailure { Log.e("VibeTuner Addons", "🔥 Write error: ${it.message}") }
        SyncHooks.notifyChanged(profileId, "addons")
    }

    /**
     * Fetch, validate, and install the addon at [url] for [profileId]. De-duplicates by
     * manifest id (a re-add refreshes the cached manifest). Returns the installed addon,
     * or a failure whose message explains why the manifest was rejected.
     */
    suspend fun addByUrl(profileId: String, url: String): Result<StremioAddon> = withContext(Dispatchers.IO) {
        val manifestUrl = normalizeManifestUrl(url)
        val manifest = fetchManifest(manifestUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Couldn't load a valid manifest from that URL."))

        val existing = getAddons(profileId).firstOrNull { it.id == manifest.id }
        val addon = StremioAddon(
            manifestUrl = manifestUrl,
            manifest = manifest,
            enabled = existing?.enabled ?: true,
            selections = pruneSelections(existing?.selections.orEmpty(), manifest),
        ).seedRequiredSelections()
        val current = getAddons(profileId).filterNot { it.id == manifest.id }
        save(profileId, current + addon)
        Result.success(addon)
    }

    /** Fetch + parse a manifest URL WITHOUT installing it — drives the paste sheet's live validation. */
    suspend fun validate(url: String): ManifestResult = withContext(Dispatchers.IO) {
        val manifestUrl = normalizeManifestUrl(url)
        manifestResultOf(manifestUrl, fetchManifest(manifestUrl))
    }

    fun setEnabled(profileId: String, addonId: String, enabled: Boolean) {
        save(profileId, getAddons(profileId).map {
            if (it.id == addonId) it.copy(enabled = enabled) else it
        })
    }

    fun remove(profileId: String, addonId: String) {
        save(profileId, getAddons(profileId).filterNot { it.id == addonId })
    }

    /** Toggle one extra option's sub-channel selection for a catalog (spec §3). */
    fun setOptionSelected(
        profileId: String, addonId: String, catalogType: String, catalogId: String,
        extraName: String, option: String, selected: Boolean,
    ) {
        save(profileId, getAddons(profileId).map { addon ->
            if (addon.id != addonId) addon else {
                val key = selectionKey(catalogType, catalogId, extraName)
                val next = if (selected) (addon.selections[key].orEmpty() + option).distinct()
                           else addon.selections[key].orEmpty() - option
                addon.copy(selections = (addon.selections + (key to next)).filterValues { it.isNotEmpty() })
            }
        })
    }

    /** Select or clear every option of one catalog at once (sub-channel Select all). */
    fun setAllOptionsSelected(
        profileId: String, addonId: String, catalogType: String, catalogId: String, selected: Boolean,
    ) {
        save(profileId, getAddons(profileId).map { addon ->
            if (addon.id != addonId) addon else {
                addon.manifest.catalogs
                    .firstOrNull { it.type == catalogType && it.id == catalogId }
                    ?.let { addon.withAllOptions(it, selected) } ?: addon
            }
        })
    }

    /** Best-effort re-fetch of every installed manifest; selections are pruned against
     *  the fresh manifest, then unselected required extras are seeded (spec 2026-07-04). */
    suspend fun refreshManifests(profileId: String): Unit = withContext(Dispatchers.IO) {
        val addons = getAddons(profileId)
        if (addons.isEmpty()) return@withContext
        save(profileId, addons.map { addon ->
            fetchManifest(addon.manifestUrl)?.let { fresh ->
                addon.copy(manifest = fresh, selections = pruneSelections(addon.selections, fresh))
                    .seedRequiredSelections()
            } ?: addon
        })
    }

    /** Seed Cinemeta the first time a profile is used, so the guide isn't empty. */
    suspend fun ensureSeeded(profileId: String): Unit = withContext(Dispatchers.IO) {
        if (fileFor(profileId).exists()) return@withContext
        val fetched = fetchManifest(CINEMETA_URL)
        val manifest = fetched ?: CINEMETA_FALLBACK
        save(profileId, listOf(StremioAddon(CINEMETA_URL, manifest, enabled = true)))
        Log.d("VibeTuner Addons", "🌱 Seeded Cinemeta for '$profileId' (live=${fetched != null})")
    }

    // ── Sync (no hook notifications: these apply REMOTE state) ────────────────────

    /** Raw addons-file JSON for the sync payload, or null when the profile has none. */
    fun exportJson(profileId: String): String? =
        fileFor(profileId).takeIf { it.exists() }?.readText()

    /** Overwrite the profile's addon file from a sync payload. Never notifies SyncHooks. */
    fun importJson(profileId: String, json: String) {
        runCatching { fileFor(profileId).writeText(JSONArray(json).toString()) }
            .onFailure { Log.e("VibeTuner Addons", "🔥 import error: ${it.message}") }
    }

    /** Remove the profile's addon file (profile tombstone applied). */
    fun deleteProfileData(profileId: String) {
        fileFor(profileId).delete()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun fetchManifest(manifestUrl: String): StremioManifest? = runCatching {
        client.newCall(Request.Builder().url(manifestUrl).build()).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) null else parseManifest(body)
        }
    }.getOrNull()

    private fun normalizeManifestUrl(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        return if (trimmed.endsWith("/manifest.json")) trimmed else "$trimmed/manifest.json"
    }

    private companion object {
        const val CINEMETA_URL = "https://v3-cinemeta.strem.io/manifest.json"

        /** Offline fallback if Cinemeta can't be reached during first-run seeding. */
        val CINEMETA_FALLBACK = StremioManifest(
            id = "com.linvo.cinemeta",
            name = "Cinemeta",
            version = "3.0.0",
            description = "The official addon for movie and series catalogs",
            types = listOf("movie", "series"),
            resources = listOf("catalog", "meta"),
            catalogs = listOf(
                StremioCatalog("movie", "top", "Popular Movies"),
                StremioCatalog("series", "top", "Popular Series")
            )
        )
    }
}
