package com.jpenner.vibetuner.data.api

import android.util.Log
import com.jpenner.vibetuner.data.model.RawMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches items for a single Stremio catalog and normalizes them into [RawMediaItem]s
 * for the EPG pipeline.
 *
 * Catalog route: `{baseUrl}/catalog/{type}/{catalogId}.json`, paginated with `/skip={n}`.
 * Response shape: `{ "metas": [ { id, type, name, poster, ... }, ... ] }`.
 */
class StremioCatalogDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch up to [limit] items for the catalog, following `skip` pagination in steps
     * of [PAGE] until the addon stops returning items or the cap is reached.
     */
    suspend fun fetchCatalog(
        baseUrl: String,
        type: String,
        catalogId: String,
        extra: Pair<String, String>? = null,
        limit: Int = 60
    ): List<RawMediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<RawMediaItem>()
        var skip = 0
        while (out.size < limit) {
            val page = fetchPage(baseUrl, type, catalogId, extra, skip)
            if (page.isEmpty()) break
            out.addAll(page)
            if (page.size < PAGE) break
            skip += PAGE
        }
        out.take(limit)
    }

    private fun fetchPage(baseUrl: String, type: String, catalogId: String, extra: Pair<String, String>?, skip: Int): List<RawMediaItem> {
        val url = baseUrl + catalogPath(type, catalogId, extra, skip)
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    Log.w("VibeTuner Catalog", "⚪ ${response.code} for $url")
                    return emptyList()
                }
                parseMetas(body, type)
            }
        } catch (e: Exception) {
            Log.e("VibeTuner Catalog", "🔥 Fetch error for $url: ${e.message}")
            emptyList()
        }
    }

    private fun parseMetas(body: String, catalogType: String): List<RawMediaItem> {
        val metas = JSONObject(body).optJSONArray("metas") ?: return emptyList()
        val out = mutableListOf<RawMediaItem>()
        for (i in 0 until metas.length()) {
            val meta = metas.optJSONObject(i) ?: continue
            val id = meta.optString("id", "").trim()
            val name = meta.optString("name", "").trim()
            if (id.isBlank() || name.isBlank()) continue

            val type = meta.optString("type", catalogType)
            val isSeries = type.equals("series", ignoreCase = true) ||
                    type.equals("tv", ignoreCase = true) ||
                    type.equals("channel", ignoreCase = true)
            val poster = meta.optString("poster", "").ifBlank { null }
            val background = meta.optString("background", "").ifBlank { null }

            out.add(
                RawMediaItem(
                    title = name,
                    description = meta.optString("description", ""),
                    durationMinutes = runtimeMinutes(meta.optString("runtime", ""), isSeries),
                    mediaType = if (isSeries) "TV Show" else "Movie",
                    imdbId = id,
                    posterUrl = poster,
                    backdropUrl = background ?: poster,
                    originalAirDate = meta.optString("releaseInfo", "").ifBlank { null }
                )
            )
        }
        return out
    }

    /** Parse a runtime string like "148 min" / "1h 22min"; fall back to a per-type default. */
    private fun runtimeMinutes(runtime: String, isSeries: Boolean): Float {
        val default = if (isSeries) 45f else 115f
        if (runtime.isBlank()) return default
        val hours = Regex("(\\d+)\\s*h").find(runtime)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = Regex("(\\d+)\\s*m").find(runtime)?.groupValues?.get(1)?.toIntOrNull()
            ?: runtime.filter { it.isDigit() }.toIntOrNull()?.takeIf { hours == 0 }
            ?: 0
        val total = hours * 60 + mins
        return if (total > 0) total.toFloat() else default
    }

    private companion object {
        const val PAGE = 100
    }
}

/** Stremio catalog route: /catalog/{type}/{id}[/{extraArgs}].json — extra args URL-encoded, &-joined. */
internal fun catalogPath(type: String, catalogId: String, extra: Pair<String, String>?, skip: Int): String {
    val args = buildList {
        extra?.let { (k, v) -> add("$k=${java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")}") }
        if (skip > 0) add("skip=$skip")
    }
    val suffix = if (args.isEmpty()) "" else "/" + args.joinToString("&")
    return "/catalog/$type/$catalogId$suffix.json"
}
