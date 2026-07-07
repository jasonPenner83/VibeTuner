package com.jpenner.vibetuner.data.repository

import android.content.Context
import android.util.Log
import com.jpenner.vibetuner.data.model.ChannelOverride
import com.jpenner.vibetuner.data.model.channelOverrideFromJson
import com.jpenner.vibetuner.data.model.toJson
import com.jpenner.vibetuner.data.sync.SyncHooks
import org.json.JSONObject
import java.io.File

/**
 * Per-profile store of user channel customizations, keyed by channel sourceKey. Survives catalog
 * re-syncs and addon disable/re-enable (overrides for absent catalogs are simply retained).
 * File: vibetuner_channel_overrides_<profileId>.json = { "<sourceKey>": { override }, ... }.
 */
class ChannelOverrideStore(private val context: Context) {

    private fun fileFor(profileId: String): File =
        File(context.filesDir, "vibetuner_channel_overrides_${profileId.ifBlank { "default" }}.json")

    fun getAll(profileId: String): Map<String, ChannelOverride> {
        val file = fileFor(profileId)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val root = JSONObject(file.readText())
            buildMap {
                root.keys().forEach { key ->
                    root.optJSONObject(key)?.let { put(key, channelOverrideFromJson(it)) }
                }
            }
        }.getOrElse {
            Log.e("VibeTuner Overrides", "🔥 read error: ${it.message}")
            emptyMap()
        }
    }

    fun save(profileId: String, overrides: Map<String, ChannelOverride>) {
        runCatching {
            val root = JSONObject()
            overrides.forEach { (key, ov) -> root.put(key, ov.toJson()) }
            fileFor(profileId).writeText(root.toString())
        }.onFailure { Log.e("VibeTuner Overrides", "🔥 write error: ${it.message}") }
        SyncHooks.notifyChanged(profileId, "overrides")
    }

    fun upsert(profileId: String, sourceKey: String, transform: (ChannelOverride) -> ChannelOverride) {
        val all = getAll(profileId).toMutableMap()
        all[sourceKey] = transform(all[sourceKey] ?: ChannelOverride())
        save(profileId, all)
    }

    // ── Sync (no hook notifications: these apply REMOTE state) ────────────────────

    fun exportJson(profileId: String): String? =
        fileFor(profileId).takeIf { it.exists() }?.readText()

    fun importJson(profileId: String, json: String) {
        runCatching { fileFor(profileId).writeText(JSONObject(json).toString()) }
            .onFailure { Log.e("VibeTuner Overrides", "🔥 import error: ${it.message}") }
    }

    fun deleteProfileData(profileId: String) {
        fileFor(profileId).delete()
    }
}
