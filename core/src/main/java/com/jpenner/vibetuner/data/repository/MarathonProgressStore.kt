package com.jpenner.vibetuner.data.repository

import org.json.JSONObject
import java.io.File

/**
 * Persisted marathon progress, keyed profile -> channel: the epoch day last scheduled
 * plus each show's next-episode pointer at the start and at the end of that day.
 * Rebuilding the same day reuses startPointers (identical schedule on reload); a later
 * day resumes from endPointers, so days the channel never aired don't advance shows.
 */
class MarathonProgressStore(private val file: File) {

    private val root: JSONObject by lazy {
        if (file.exists()) runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
        else JSONObject()
    }

    @Synchronized
    fun startPointersFor(profileId: String, channelId: String, epochDay: Long): Map<String, Int> {
        val entry = root.optJSONObject(profileId)?.optJSONObject(channelId) ?: return emptyMap()
        val sameDay = entry.optLong("epochDay", Long.MIN_VALUE) == epochDay
        val pointers = entry.optJSONObject(if (sameDay) "startPointers" else "endPointers")
            ?: return emptyMap()
        return pointers.keys().asSequence().associateWith { pointers.getInt(it) }
    }

    @Synchronized
    fun save(
        profileId: String,
        channelId: String,
        epochDay: Long,
        startPointers: Map<String, Int>,
        endPointers: Map<String, Int>,
    ) {
        val profile = root.optJSONObject(profileId) ?: JSONObject().also { root.put(profileId, it) }
        profile.put(channelId, JSONObject().apply {
            put("epochDay", epochDay)
            put("startPointers", JSONObject(startPointers as Map<*, *>))
            put("endPointers", JSONObject(endPointers as Map<*, *>))
        })
        runCatching { file.writeText(root.toString()) }
    }
}
