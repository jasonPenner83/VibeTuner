package com.jpenner.vibetuner.data.cache

import com.jpenner.vibetuner.data.model.RawMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Daily disk cache for harvested pools, held in memory after a single parse. The whole
 * cache persists in one JSON file:
 *   { "<channelId>": { "epochDay": <Long>, "harvestedAtMs": <Long>, "items": [ {...}, ... ] } }
 *
 * Read semantics: null means "no usable entry, caller must fetch"; a non-null list
 * (possibly empty) is authoritative. Empty pools are cached too (negative caching) so a
 * failing catalog isn't re-fetched on every launch — but only for [EMPTY_RETRY_TTL_MS],
 * so a transient outage self-heals. Non-empty entries are honored for the whole epochDay.
 *
 * Writes only mutate memory; call [flush] once after a batch of harvests to persist.
 * Serializing the whole multi-MB file per channel write stalls the populate loop for
 * seconds, so persistence must stay out of the per-channel path. Flush also prunes
 * entries from previous days to keep the file (and its one-time parse) small.
 */
class HarvestCache(
    private val file: File,
    private val clock: () -> Long = { LocalDate.now().toEpochDay() },
    private val nowMs: () -> Long = System::currentTimeMillis
) {

    private class Entry(val epochDay: Long, val harvestedAtMs: Long, val items: List<RawMediaItem>)

    private val mutex = Mutex()
    private var entries: MutableMap<String, Entry>? = null
    private var dirty = false

    suspend fun read(channelId: String): List<RawMediaItem>? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry = loadLocked()[channelId] ?: return@withLock null
            if (entry.epochDay != clock()) return@withLock null
            if (entry.items.isEmpty() && nowMs() - entry.harvestedAtMs >= EMPTY_RETRY_TTL_MS) {
                return@withLock null
            }
            entry.items
        }
    }

    suspend fun write(channelId: String, pool: List<RawMediaItem>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val map = loadLocked()
            map[channelId] = Entry(epochDay = clock(), harvestedAtMs = nowMs(), items = pool)
            dirty = true
        }
    }

    /** Persist pending writes (pruning previous days' entries). No-op when nothing changed. */
    suspend fun flush() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!dirty) return@withLock
            val map = loadLocked()
            val today = clock()
            map.entries.removeAll { it.value.epochDay != today }
            persistLocked(map)
            dirty = false
        }
    }

    /** Drop every cached pool — positive and negative — and delete the backing file
     *  (manual guide rebuild). The next reads all miss and re-fetch. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            entries = mutableMapOf()
            dirty = false
            file.delete()
        }
    }

    /** Parse the file into memory exactly once; afterwards all reads/writes go through [entries]. */
    private fun loadLocked(): MutableMap<String, Entry> {
        entries?.let { return it }
        val map = mutableMapOf<String, Entry>()
        try {
            if (file.exists()) {
                val root = JSONObject(file.readText())
                for (channelId in root.keys()) {
                    val entry = root.optJSONObject(channelId) ?: continue
                    val items = entry.optJSONArray("items") ?: continue
                    map[channelId] = Entry(
                        epochDay = entry.optLong("epochDay", -1L),
                        harvestedAtMs = entry.optLong("harvestedAtMs", 0L),
                        items = rawItemsFromJson(items)
                    )
                }
            }
        } catch (e: Exception) {
            // best-effort cache; a corrupt file just means an empty cache
        }
        entries = map
        return map
    }

    private fun persistLocked(map: Map<String, Entry>) {
        try {
            val root = JSONObject()
            map.forEach { (channelId, entry) ->
                root.put(
                    channelId,
                    JSONObject()
                        .put("epochDay", entry.epochDay)
                        .put("harvestedAtMs", entry.harvestedAtMs)
                        .put("items", rawItemsToJson(entry.items))
                )
            }
            file.writeText(root.toString())
        } catch (e: Exception) {
            // best-effort cache; ignore write failures
        }
    }

    companion object {
        /** How long a cached empty pool suppresses re-fetching before we try the addon again. */
        const val EMPTY_RETRY_TTL_MS: Long = 30 * 60 * 1000L
    }
}

/** JSON codec for a harvested pool — used by the cache file and by harvest-pool sync payloads. */
fun rawItemsToJson(pool: List<RawMediaItem>): JSONArray {
    val arr = JSONArray()
    pool.forEach { item ->
        arr.put(
            JSONObject()
                .put("title", item.title)
                .put("description", item.description)
                .put("durationMinutes", item.durationMinutes.toDouble())
                .put("mediaType", item.mediaType)
                .put("imdbId", item.imdbId)
                .put("posterUrl", item.posterUrl ?: JSONObject.NULL)
                .put("backdropUrl", item.backdropUrl ?: JSONObject.NULL)
                .put("episodeTitle", item.episodeTitle ?: JSONObject.NULL)
                .put("originalAirDate", item.originalAirDate ?: JSONObject.NULL)
                .put("season", item.season ?: JSONObject.NULL)
                .put("episodeNumber", item.episodeNumber ?: JSONObject.NULL)
        )
    }
    return arr
}

fun rawItemsFromJson(arr: JSONArray): List<RawMediaItem> {
    val out = mutableListOf<RawMediaItem>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        out.add(
            RawMediaItem(
                title = o.optString("title", ""),
                description = o.optString("description", ""),
                durationMinutes = o.optDouble("durationMinutes", 45.0).toFloat(),
                mediaType = o.optString("mediaType", "Movie"),
                imdbId = o.optString("imdbId", ""),
                posterUrl = o.optStringOrNull("posterUrl"),
                backdropUrl = o.optStringOrNull("backdropUrl"),
                episodeTitle = o.optStringOrNull("episodeTitle"),
                originalAirDate = o.optStringOrNull("originalAirDate"),
                season = if (o.isNull("season")) null else o.optInt("season"),
                episodeNumber = if (o.isNull("episodeNumber")) null else o.optInt("episodeNumber")
            )
        )
    }
    return out
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key, "").ifBlank { null }
