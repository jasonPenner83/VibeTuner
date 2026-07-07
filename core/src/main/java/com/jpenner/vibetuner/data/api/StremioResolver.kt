package com.jpenner.vibetuner.data.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.repository.AddonRepository
import com.jpenner.vibetuner.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

class StremioResolver(context: Context) {
    private val addonRepository = AddonRepository(context)
    private val profileRepository = ProfileRepository(context)

    private val isEmulator = Build.FINGERPRINT.contains("generic")
            || Build.FINGERPRINT.contains("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun resolveStreamUrl(
        program: Program,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val profileId = profileRepository.activeProfileId() ?: "default"
        val addonUrls = addonRepository.enabledBaseUrls(profileId)

        onStatusUpdate?.invoke("🚀 Starting Resolution for: ${program.title}")
        Log.d("VibeTuner Resolver", "🚀 Starting Resolution for: ${program.title} [IMDb: ${program.imdbId}]")

        if (addonUrls.isEmpty()) {
            onStatusUpdate?.invoke("❌ No addons configured.")
            Log.e("VibeTuner Resolver", "❌ No addons configured in storage.")
            return@withContext null
        }

        if (program.imdbId.isBlank()) {
            onStatusUpdate?.invoke("❌ Aborted: Missing IMDb ID.")
            Log.e("VibeTuner Resolver", "❌ Aborted: Missing IMDb ID.")
            return@withContext null
        }

        val match = Regex("S(\\d+)E(\\d+)").find(program.title)
        val season = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val episode = match?.groupValues?.get(2)?.toIntOrNull() ?: 1

        for (addonBaseUrl in addonUrls) {
            if (!isActive) {
                Log.d("VibeTuner Resolver", "⏹ Resolution cancelled.")
                return@withContext null
            }

            val typePath = if (program.mediaType == "Episode" || program.mediaType == "TV Show") "series" else "movie"
            val idPath = if (typePath == "series") "${program.imdbId}:$season:$episode" else program.imdbId
            val endpoint = "$addonBaseUrl/stream/$typePath/$idPath.json"

            onStatusUpdate?.invoke("📡 Hitting Addon: $addonBaseUrl")
            Log.d("VibeTuner Resolver", "📡 Hitting Addon: $addonBaseUrl")

            val responseBody = executeCancellableRequest(endpoint) ?: continue

            try {
                val bodyJson = JSONObject(responseBody)
                val streamsArray = bodyJson.optJSONArray("streams")

                if (streamsArray == null || streamsArray.length() == 0) {
                    Log.w("VibeTuner Resolver", "⚪ No streams found at this node.")
                    continue
                }

                val streamsList = mutableListOf<JSONObject>()
                for (i in 0 until streamsArray.length()) {
                    streamsList.add(streamsArray.getJSONObject(i))
                }

                // Sort streams based on layout compatibility rules
                val sortedStreams = streamsList.sortedByDescending { stream ->
                    val title = stream.optString("title", "").lowercase()
                    val name = stream.optString("name", "").lowercase()
                    val combined = "$title $name"

                    var score = 0

                    // Slightly penalize web rips to let physical formats naturally float higher
                    if (combined.contains("web-dl") || combined.contains("webrip")) score -= 20
                    if (combined.contains("1080p")) score += 50
                    if (combined.contains("720p")) score += 30
                    if (combined.contains("2160p") || combined.contains("4k")) {
                        if (isEmulator) score -= 100
                        else score += 20
                    }

                    if (combined.contains("h264") || combined.contains("x264") || combined.contains("avc")) score += if (isEmulator) 100 else 80
                    if (combined.contains("h265") || combined.contains("x265") || combined.contains("hevc")) score += if (isEmulator) -50 else 100
                    if (combined.contains("av1")) score -= 200
                    if (combined.contains("remux")) score -= 10

                    score
                }

                // 🟢 NEW TWO-PASS BUFFER POOLS
                val highRiskUrlPool = mutableListOf<String>()

                // 🎬 PASS 1: Select only 100% clean, non-streaming formats
                for (stream in sortedStreams) {
                    val targetUrl = stream.optString("url", "")
                    if (targetUrl.isNotBlank()) {
                        val title = stream.optString("title", "")
                        val name = stream.optString("name", "")
                        val combined = "$title $name".lowercase()

                        val isHighRiskKeyword = combined.contains("web-dl") ||
                                combined.contains("webrip") ||
                                combined.contains("web-rip") ||
                                combined.contains("amzn") ||
                                combined.contains("nf") ||
                                combined.contains("cr") ||
                                combined.contains("dsnp") ||
                                combined.contains("yts") ||
                                combined.contains("rarbg")

                        if (isHighRiskKeyword) {
                            highRiskUrlPool.add(targetUrl) // Shelve it for Pass 2 if needed
                            continue
                        }

                        onStatusUpdate?.invoke("🎯 Pass 1 Match: ${title.take(25)}...")
                        Log.d("VibeTuner Resolver", "🎯 Pass 1 Match Found: $targetUrl")
                        return@withContext targetUrl
                    }
                }

                // 📺 PASS 2: Executed if no physical discs exist (Standard TV Show behavior)
                if (highRiskUrlPool.isNotEmpty()) {
                    // 🟢 THE FIX: Skip the poisoned Index 0 link entirely and grab the alternative Index 1 upload hash
                    val saferFallbackUrl = if (highRiskUrlPool.size > 1) highRiskUrlPool[1] else highRiskUrlPool[0]

                    Log.w("VibeTuner Resolver", "🚨 No disc formats found. Running Pass 2 rotation fallback (Skipping Index 0).")
                    onStatusUpdate?.invoke("🎯 Pass 2 Backup Stream Active...")
                    return@withContext saferFallbackUrl
                }

                Log.w("VibeTuner Resolver", "⚠️ No direct URLs found in this manifest.")
            } catch (e: Exception) {
                Log.e("VibeTuner Resolver", "🔥 Parse error: ${e.message}")
            }
        }

        onStatusUpdate?.invoke("🏁 Chain exhausted. No links found.")
        Log.d("VibeTuner Resolver", "🏁 Chain exhausted. No links found.")
        return@withContext null
    }

    private suspend fun executeCancellableRequest(url: String): String? = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    Log.e("VibeTuner Resolver", "🌐 Network failure: ${e.message}")
                    continuation.resume(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        continuation.resume(body)
                    } else {
                        Log.e("VibeTuner Resolver", "🌐 Server error: ${it.code}")
                        continuation.resume(null)
                    }
                }
            }
        })
    }
}