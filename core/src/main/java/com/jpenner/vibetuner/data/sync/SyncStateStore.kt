package com.jpenner.vibetuner.data.sync

import android.util.Log
import org.json.JSONObject
import java.io.File

/** Per-doc sync bookkeeping. [lastSeen] is the server updated_at we last applied/pushed. */
data class DocState(
    val dirty: Boolean = false,
    val deleted: Boolean = false,
    val lastSeen: String? = null,
)

/**
 * Tracks which sync documents have unpushed local edits and the last server
 * timestamp seen per doc, persisted to vibetuner_sync_state.json as
 * { "<profileId>:<kind>": { "dirty": bool, "deleted": bool, "lastSeen": str? } }.
 * Same load-once/per-file JSON pattern as the other stores. Thread-safe via
 * synchronized — callers are the debounced sync scope plus store write hooks.
 */
class SyncStateStore(private val file: File) {

    private val lock = Any()
    private var states: MutableMap<String, DocState>? = null

    fun get(profileId: String, kind: String): DocState = synchronized(lock) {
        loadLocked()["$profileId:$kind"] ?: DocState()
    }

    fun all(): Map<String, DocState> = synchronized(lock) { loadLocked().toMap() }

    fun markDirty(profileId: String, kind: String) = update("$profileId:$kind") {
        it.copy(dirty = true)
    }

    /** A deleted profile is a dirty tombstone on its 'profile' doc. */
    fun markDeleted(profileId: String) = update("$profileId:profile") {
        it.copy(dirty = true, deleted = true)
    }

    fun clearDirty(profileId: String, kind: String, lastSeen: String) = update("$profileId:$kind") {
        it.copy(dirty = false, lastSeen = lastSeen)
    }

    fun setLastSeen(profileId: String, kind: String, lastSeen: String) = update("$profileId:$kind") {
        it.copy(lastSeen = lastSeen)
    }

    fun remove(profileId: String) = synchronized(lock) {
        val map = loadLocked()
        map.keys.removeAll { it.substringBeforeLast(':') == profileId }
        persistLocked(map)
    }

    fun clearAll() = synchronized(lock) {
        states = mutableMapOf()
        file.delete()
        Unit
    }

    private fun update(key: String, transform: (DocState) -> DocState) = synchronized(lock) {
        val map = loadLocked()
        map[key] = transform(map[key] ?: DocState())
        persistLocked(map)
    }

    private fun loadLocked(): MutableMap<String, DocState> {
        states?.let { return it }
        val map = mutableMapOf<String, DocState>()
        runCatching {
            if (file.exists()) {
                val root = JSONObject(file.readText())
                root.keys().forEach { key ->
                    val o = root.getJSONObject(key)
                    map[key] = DocState(
                        dirty = o.optBoolean("dirty", false),
                        deleted = o.optBoolean("deleted", false),
                        lastSeen = if (o.isNull("lastSeen")) null else o.optString("lastSeen").ifBlank { null },
                    )
                }
            }
        }.onFailure { Log.e("VibeTuner Sync", "🔥 state read error: ${it.message}") }
        states = map
        return map
    }

    private fun persistLocked(map: Map<String, DocState>) {
        runCatching {
            val root = JSONObject()
            map.forEach { (key, st) ->
                root.put(key, JSONObject().apply {
                    put("dirty", st.dirty)
                    put("deleted", st.deleted)
                    put("lastSeen", st.lastSeen ?: JSONObject.NULL)
                })
            }
            file.writeText(root.toString())
        }.onFailure { Log.e("VibeTuner Sync", "🔥 state write error: ${it.message}") }
    }
}
