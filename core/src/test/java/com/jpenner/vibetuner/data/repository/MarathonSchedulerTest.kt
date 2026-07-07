package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MarathonSchedulerTest {
    private fun ep(show: String, s: Int, e: Int) =
        RawMediaItem("$show S0${s}E0${e}", "", 30f, "Episode", show, season = s, episodeNumber = e)

    @Test fun groups_by_show_preserving_first_seen_order_and_sorts_episodes() {
        val pool = listOf(ep("B", 1, 2), ep("A", 1, 2), ep("A", 1, 1), ep("B", 1, 1))
        val groups = groupEpisodesByShow(pool)
        assertEquals(listOf("B", "A"), groups.map { it.first().imdbId })
        assertEquals(listOf(1, 2), groups[0].map { it.episodeNumber }) // B sorted
        assertEquals(listOf(1, 2), groups[1].map { it.episodeNumber }) // A sorted
    }

    @Test fun limit_null_binges_each_show_fully_then_next() {
        val perShow = listOf(
            listOf(ep("A", 1, 1), ep("A", 1, 2)),
            listOf(ep("B", 1, 1), ep("B", 1, 2)),
        )
        val seq = buildMarathonSequence(perShow, null)
        assertEquals(listOf("A S01E01", "A S01E02", "B S01E01", "B S01E02"), seq.map { it.title })
    }

    @Test fun limit_n_round_robins_resuming_each_show() {
        val perShow = listOf(
            listOf(ep("A", 1, 1), ep("A", 1, 2), ep("A", 1, 3)),
            listOf(ep("B", 1, 1), ep("B", 1, 2)),
        )
        val seq = buildMarathonSequence(perShow, 2)
        // 2 of A, 2 of B, then remaining 1 of A
        assertEquals(
            listOf("A S01E01", "A S01E02", "B S01E01", "B S01E02", "A S01E03"),
            seq.map { it.title })
    }

    @Test fun rotate_moves_start_to_front() {
        assertEquals(listOf(3, 4, 1, 2), rotateToStart(listOf(1, 2, 3, 4), 2))
        assertEquals(listOf(1, 2), rotateToStart(listOf(1, 2), 0))
    }

    @Test fun start_index_is_stable_per_channel_and_advances_with_days() {
        val seq = (1..4).map { ep("A", 1, it) } // 4 eps x 30min = 120 min total
        val day0 = marathonStartIndex("chan-x", seq, epochMinute = 0)
        val sameDay = marathonStartIndex("chan-x", seq, epochMinute = 0)
        assertEquals(day0, sameDay) // deterministic
        val laterByHalfTotal = marathonStartIndex("chan-x", seq, epochMinute = 60)
        assertEquals(((day0 + 2) % 4), laterByHalfTotal) // +60min = +2 items of 30min
    }
}
