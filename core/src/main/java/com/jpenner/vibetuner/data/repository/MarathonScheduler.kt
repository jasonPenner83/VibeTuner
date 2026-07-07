package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import java.util.Random

/**
 * The RANDOM-mode daily order: seeded by the CST epoch day, so every device
 * that starts from the same harvested pool produces the same schedule.
 */
fun dailySeedShuffle(pool: List<RawMediaItem>, epochDay: Long): List<RawMediaItem> =
    pool.shuffled(Random(epochDay))

/** Group a flat episode pool by show (imdbId), preserving first-seen order; sort each show's episodes. */
fun groupEpisodesByShow(pool: List<RawMediaItem>): List<List<RawMediaItem>> {
    val byShow = LinkedHashMap<String, MutableList<RawMediaItem>>()
    pool.forEach { byShow.getOrPut(it.imdbId) { mutableListOf() }.add(it) }
    return byShow.values.map { grp ->
        grp.sortedWith(compareBy({ it.season ?: 0 }, { it.episodeNumber ?: 0 }))
    }
}

/**
 * Order episodes for a marathon channel.
 * - limit == null (None): binge each show fully, then the next show.
 * - limit == N: N episodes per show, round-robin across shows, resuming each show where it left off.
 */
fun buildMarathonSequence(perShow: List<List<RawMediaItem>>, limit: Int?): List<RawMediaItem> {
    if (limit == null || limit <= 0) return perShow.flatten()
    val queues = perShow.map { ArrayDeque(it) }
    val out = ArrayList<RawMediaItem>()
    while (queues.any { it.isNotEmpty() }) {
        for (q in queues) repeat(limit) { if (q.isNotEmpty()) out.add(q.removeFirst()) }
    }
    return out
}

/**
 * Where today's midnight lands in the endless looped [sequence]: a per-channel random phase
 * (stable, from the channel id) plus minutes elapsed since epoch, mod the sequence's total runtime.
 * Result is the item index at that minute position, so channels differ and each day carries on.
 */
fun marathonStartIndex(channelId: String, sequence: List<RawMediaItem>, epochMinute: Long): Int {
    if (sequence.isEmpty()) return 0
    val total = sequence.sumOf { it.durationMinutes.toDouble() }.toLong().coerceAtLeast(1L)
    val phase = Math.floorMod(channelId.hashCode().toLong(), total)
    val pos = Math.floorMod(phase + epochMinute, total)
    var acc = 0.0
    for (i in sequence.indices) {
        acc += sequence[i].durationMinutes
        if (pos < acc) return i
    }
    return 0
}

/** Rotate [list] so element at [startIndex] becomes first (wraps). Empty list returns itself. */
fun <T> rotateToStart(list: List<T>, startIndex: Int): List<T> {
    if (list.isEmpty()) return list
    val s = ((startIndex % list.size) + list.size) % list.size
    return list.subList(s, list.size) + list.subList(0, s)
}
