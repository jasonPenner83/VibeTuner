package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import java.util.Random

/** Group a flat episode pool by show (imdbId), preserving first-seen order; sort each show's episodes. */
fun groupEpisodesByShow(pool: List<RawMediaItem>): List<List<RawMediaItem>> {
    val byShow = LinkedHashMap<String, MutableList<RawMediaItem>>()
    pool.forEach { byShow.getOrPut(it.imdbId) { mutableListOf() }.add(it) }
    return byShow.values.map { grp ->
        grp.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNumber ?: 0 }))
    }
}

/** One channel-day of programming plus each show's next-episode index after midnight. */
data class DaySchedule(val items: List<RawMediaItem>, val endPointers: Map<String, Int>)

private const val DAY_MINUTES = 24 * 60

/**
 * Build a full day (midnight to midnight) of random show blocks:
 * shows play in a seeded-random order, [limit] episodes per block (null = the whole
 * show), each show resuming from its [startPointers] index and wrapping past its last
 * episode. No show repeats until every show has played a block (a "round"), and a new
 * round never opens with the show that just closed the previous one. Seeded by
 * [epochDay] + [channelId] so every reload and device builds the same schedule.
 */
fun buildDaySchedule(
    channelId: String,
    epochDay: Long,
    perShow: List<List<RawMediaItem>>,
    limit: Int?,
    startPointers: Map<String, Int>,
): DaySchedule {
    val shows = perShow.filter { it.isNotEmpty() }
    if (shows.isEmpty()) return DaySchedule(emptyList(), emptyMap())

    val rnd = Random(epochDay * 31 + channelId.hashCode())
    val pointers = HashMap<String, Int>()
    shows.forEach { s ->
        val id = s.first().imdbId
        pointers[id] = Math.floorMod(startPointers[id] ?: 0, s.size)
    }

    val out = ArrayList<RawMediaItem>()
    var minutes = 0.0
    var lastShowId: String? = null
    while (minutes < DAY_MINUTES) {
        var order = shows.shuffled(rnd)
        if (shows.size > 1 && order.first().first().imdbId == lastShowId) {
            order = order.drop(1) + order.take(1)
        }
        val roundStartMinutes = minutes
        for (show in order) {
            if (minutes >= DAY_MINUTES) break
            val id = show.first().imdbId
            val blockSize = if (limit == null || limit <= 0) show.size else minOf(limit, show.size)
            var consumed = 0
            while (consumed < blockSize && minutes < DAY_MINUTES) {
                val ep = show[pointers[id]!!]
                out.add(ep)
                minutes += ep.durationMinutes
                pointers[id] = (pointers[id]!! + 1) % show.size
                consumed++
            }
            if (consumed > 0) lastShowId = id
        }
        // All durations zero/negative would spin forever; a silent-progress round ends the day.
        if (minutes <= roundStartMinutes) break
    }
    return DaySchedule(out, pointers)
}
