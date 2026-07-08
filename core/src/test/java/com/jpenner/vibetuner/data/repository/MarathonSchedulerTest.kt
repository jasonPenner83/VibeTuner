package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarathonSchedulerTest {
    private fun ep(show: String, s: Int, e: Int, mins: Float = 30f) =
        RawMediaItem("$show S0${s}E0${e}", "", mins, "Episode", show, season = s, episodeNumber = e)

    private fun movie(n: Int, mins: Float = 30f) =
        RawMediaItem("Movie $n", "", mins, "Movie", "tt$n")

    private fun shows(count: Int, epsEach: Int): List<List<RawMediaItem>> =
        (1..count).map { s -> (1..epsEach).map { e -> ep("show$s", 1, e) } }

    @Test fun groups_by_show_preserving_first_seen_order_and_sorts_episodes() {
        val pool = listOf(ep("B", 1, 2), ep("A", 1, 2), ep("A", 1, 1), ep("B", 1, 1))
        val groups = groupEpisodesByShow(pool)
        assertEquals(listOf("B", "A"), groups.map { it.first().imdbId })
        assertEquals(listOf(1, 2), groups[0].map { it.episodeNumber }) // B sorted
        assertEquals(listOf(1, 2), groups[1].map { it.episodeNumber }) // A sorted
    }

    @Test fun same_day_and_channel_builds_identical_schedule() {
        val perShow = shows(6, 8)
        val a = buildDaySchedule("chan-x", 20_640L, perShow, 3, emptyMap())
        val b = buildDaySchedule("chan-x", 20_640L, perShow, 3, emptyMap())
        assertEquals(a.items.map { it.title }, b.items.map { it.title })
        assertEquals(a.endPointers, b.endPointers)
    }

    @Test fun different_days_and_channels_build_different_orders() {
        val perShow = shows(6, 8)
        val day0 = buildDaySchedule("chan-x", 20_640L, perShow, 3, emptyMap())
        val day1 = buildDaySchedule("chan-x", 20_641L, perShow, 3, emptyMap())
        val other = buildDaySchedule("chan-y", 20_640L, perShow, 3, emptyMap())
        assertNotEquals(day0.items.map { it.title }, day1.items.map { it.title })
        assertNotEquals(day0.items.map { it.title }, other.items.map { it.title })
    }

    @Test fun schedule_covers_at_least_the_whole_day() {
        val day = buildDaySchedule("c", 1L, shows(3, 5), 2, emptyMap())
        assertTrue(day.items.sumOf { it.durationMinutes.toDouble() } >= 1440.0)
    }

    @Test fun limit_n_plays_blocks_of_n_consecutive_episodes_per_show() {
        val day = buildDaySchedule("c", 1L, shows(4, 10), 3, emptyMap())
        // Walk the first full round: 4 blocks of 3, each block one show, episodes ascending.
        val firstRound = day.items.take(12)
        val blockShows = firstRound.chunked(3).map { block ->
            assertEquals(1, block.map { it.imdbId }.distinct().size)
            assertEquals(
                block.map { it.episodeNumber!! },
                block.map { it.episodeNumber!! }.sorted())
            block.first().imdbId
        }
        // Every show plays exactly once before any repeats.
        assertEquals(4, blockShows.distinct().size)
    }

    @Test fun limit_null_binges_a_whole_show_per_block() {
        val day = buildDaySchedule("c", 1L, shows(3, 6), null, emptyMap())
        val firstShow = day.items.first().imdbId
        val firstBlock = day.items.takeWhile { it.imdbId == firstShow }
        assertEquals((1..6).toList(), firstBlock.map { it.episodeNumber })
    }

    @Test fun blocks_resume_from_start_pointers() {
        val perShow = listOf((1..8).map { ep("A", 1, it) })
        val day = buildDaySchedule("c", 1L, perShow, 2, mapOf("A" to 3))
        assertEquals(listOf(4, 5), day.items.take(2).map { it.episodeNumber })
    }

    @Test fun end_pointers_advance_by_episodes_consumed_wrapping_past_the_end() {
        // One 3-episode show of 600-min episodes: day needs 3 episodes (1800 >= 1440),
        // so the pointer wraps 0 -> 3 mod 3 -> 0... use pointer 1: consumes idx 1,2,0 -> ends at 1?
        // Use 2 episodes consumed: 720-min episodes, day = 2 eps.
        val perShow = listOf((1..3).map { ep("A", 1, it, mins = 720f) })
        val day = buildDaySchedule("c", 1L, perShow, 1, mapOf("A" to 2))
        assertEquals(listOf(3, 1), day.items.take(2).map { it.episodeNumber }) // wraps 2 -> 0
        assertEquals(1, day.endPointers["A"])
    }

    @Test fun block_is_capped_at_show_length_so_episodes_never_repeat_within_a_block() {
        val perShow = listOf(
            (1..2).map { ep("A", 1, it) },
            (1..9).map { ep("B", 1, it) },
        )
        val day = buildDaySchedule("c", 1L, perShow, 5, emptyMap())
        val aRun = day.items.takeWhile { it.imdbId == "A" }.ifEmpty {
            day.items.dropWhile { it.imdbId == "B" }.takeWhile { it.imdbId == "A" }
        }
        assertEquals(listOf(1, 2), aRun.map { it.episodeNumber })
    }

    @Test fun movies_never_repeat_until_the_whole_pool_has_aired() {
        val perShow = (1..30).map { listOf(movie(it)) }
        val day = buildDaySchedule("c", 1L, perShow, 1, emptyMap())
        val firstPass = day.items.take(30).map { it.imdbId }
        assertEquals(30, firstPass.distinct().size)
    }

    @Test fun consecutive_rounds_never_repeat_the_same_show_back_to_back() {
        val perShow = shows(3, 2) // tiny pool -> many rounds needed for 1440 min
        val day = buildDaySchedule("c", 1L, perShow, 2, emptyMap())
        day.items.map { it.imdbId }.chunked(2).zipWithNext().forEach { (prev, next) ->
            assertNotEquals(prev.last(), next.first())
        }
    }

    @Test fun empty_pool_returns_empty_schedule() {
        val day = buildDaySchedule("c", 1L, emptyList(), 2, emptyMap())
        assertTrue(day.items.isEmpty())
        assertTrue(day.endPointers.isEmpty())
    }
}
