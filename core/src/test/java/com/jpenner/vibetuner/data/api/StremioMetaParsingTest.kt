package com.jpenner.vibetuner.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioMetaParsingTest {
    private val body = """
        { "meta": { "id": "tt1", "type": "series", "name": "Show", "runtime": "50 min",
          "poster": "p.jpg", "background": "b.jpg",
          "videos": [
            { "id": "tt1:1:1", "title": "Pilot", "season": 1, "episode": 1, "released": "2010-01-01T00:00:00.000Z", "overview": "o1" },
            { "id": "tt1:1:2", "title": "Two",   "season": 1, "episode": 2, "released": "2010-01-08T00:00:00.000Z" },
            { "id": "tt1:0:1", "title": "Special","season": 0, "episode": 1 }
          ] } }
    """.trimIndent()

    @Test fun maps_videos_to_episode_items_skipping_specials() {
        val eps = parseSeriesEpisodes(body, "tt1")
        assertEquals(2, eps.size) // season 0 skipped
        val e1 = eps[0]
        assertEquals("Show S01E01", e1.title)
        assertEquals("Pilot", e1.episodeTitle)
        assertEquals("Episode", e1.mediaType)
        assertEquals("tt1", e1.imdbId)
        assertEquals(1, e1.season)
        assertEquals(1, e1.episodeNumber)
        assertEquals("2010-01-01", e1.originalAirDate)
        assertEquals(50f, e1.durationMinutes, 0.001f)
    }

    @Test fun sorts_by_season_then_episode() {
        val eps = parseSeriesEpisodes(body, "tt1")
        assertTrue(eps.map { it.episodeNumber } == listOf(1, 2))
    }

    @Test fun returns_empty_on_garbage() {
        assertEquals(emptyList<Any>(), parseSeriesEpisodes("not json", "tt1"))
    }
}
