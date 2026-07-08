package com.jpenner.vibetuner.data.api

import android.util.Log
import com.jpenner.vibetuner.data.model.RawMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Parse a Stremio `/meta/series/{id}.json` body into episode items, skipping season 0. */
fun parseSeriesEpisodes(body: String, seriesId: String): List<RawMediaItem> = runCatching {
    val meta = JSONObject(body).optJSONObject("meta") ?: return emptyList()
    val showName = meta.optString("name", "Series")
    val runtime = runtimeMinutes(meta.optString("runtime", ""))
    val poster = meta.optString("poster", "").ifBlank { null }
    val background = meta.optString("background", "").ifBlank { null }
    val videos = meta.optJSONArray("videos") ?: return emptyList()

    val out = ArrayList<RawMediaItem>()
    for (i in 0 until videos.length()) {
        val v = videos.optJSONObject(i) ?: continue
        val season = v.optInt("season", -1)
        val episode = v.optInt("episode", -1)
        if (season <= 0 || episode <= 0) continue // skip specials / invalid
        out.add(
            RawMediaItem(
                title = String.format(Locale.ROOT, "%s S%02dE%02d", showName, season, episode),
                description = v.optString("overview", ""),
                durationMinutes = runtime,
                mediaType = "Episode",
                imdbId = seriesId,
                posterUrl = poster,
                backdropUrl = background ?: poster,
                episodeTitle = v.optString("title", "Episode $episode"),
                originalAirDate = v.optString("released", "").take(10).ifBlank { null },
                season = season,
                episodeNumber = episode,
            )
        )
    }
    out.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNumber ?: 0 }))
}.getOrElse { emptyList() }

private fun runtimeMinutes(runtime: String): Float {
    if (runtime.isBlank()) return 45f
    val n = runtime.filter { it.isDigit() }.toIntOrNull() ?: return 45f
    return if (n > 0) n.toFloat() else 45f
}

/** Parse a `/meta` body's `meta.runtime` into minutes; null when absent or unparseable. */
fun parseMetaRuntimeMinutes(body: String): Float? = runCatching {
    JSONObject(body).optJSONObject("meta")?.optString("runtime", "")?.let { parseRuntimeString(it) }
}.getOrNull()

/** Fetches a series' episode list from an addon's `/meta` resource. */
class StremioMetaDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSeriesEpisodes(baseUrl: String, seriesId: String): List<RawMediaItem> =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/meta/series/$seriesId.json"
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrBlank()) {
                        Log.w("VibeTuner Meta", "⚪ ${response.code} for $url")
                        emptyList()
                    } else parseSeriesEpisodes(body, seriesId)
                }
            } catch (e: Exception) {
                Log.e("VibeTuner Meta", "🔥 meta error for $url: ${e.message}")
                emptyList()
            }
        }

    /** Fetch an item's real runtime from its `/meta` resource; null when the addon has none. */
    suspend fun fetchRuntimeMinutes(baseUrl: String, type: String, id: String): Float? =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/meta/$type/$id.json"
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrBlank()) null
                    else parseMetaRuntimeMinutes(body)
                }
            } catch (e: Exception) {
                Log.e("VibeTuner Meta", "🔥 runtime error for $url: ${e.message}")
                null
            }
        }
}
