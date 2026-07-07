package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodicSortTest {

    private fun ep(title: String, season: Int?, number: Int?) =
        RawMediaItem(title, "", 45f, "Episode", "tt1", season = season, episodeNumber = number)

    @Test
    fun sorts_by_real_season_and_episode_fields() {
        val unsorted = listOf(
            ep("Show S02E01", 2, 1),
            ep("Show S01E02", 1, 2),
            ep("Show S01E01", 1, 1)
        )
        val sorted = sortEpisodicChronologically(unsorted)
        assertEquals(listOf("Show S01E01", "Show S01E02", "Show S02E01"), sorted.map { it.title })
    }

    @Test
    fun falls_back_to_title_regex_when_fields_absent() {
        val unsorted = listOf(
            ep("Show S01E03", null, null),
            ep("Show S01E01", null, null),
            ep("Show S01E02", null, null)
        )
        val sorted = sortEpisodicChronologically(unsorted)
        assertEquals(listOf("Show S01E01", "Show S01E02", "Show S01E03"), sorted.map { it.title })
    }

    @Test
    fun groups_by_show_before_ordering_episodes() {
        val unsorted = listOf(
            ep("Beta S01E01", 1, 1),
            ep("Alpha S01E02", 1, 2),
            ep("Alpha S01E01", 1, 1)
        )
        val sorted = sortEpisodicChronologically(unsorted)
        assertEquals(
            listOf("Alpha S01E01", "Alpha S01E02", "Beta S01E01"),
            sorted.map { it.title }
        )
    }
}
