package com.jpenner.vibetuner.data.cache

import com.jpenner.vibetuner.data.model.RawMediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RawItemsJsonTest {

    private val items = listOf(
        RawMediaItem(
            title = "The Movie", description = "A film", durationMinutes = 92.5f,
            mediaType = "Movie", imdbId = "tt0000001",
            posterUrl = "https://img/p.jpg", backdropUrl = null,
            episodeTitle = null, originalAirDate = null,
            season = null, episodeNumber = null,
        ),
        RawMediaItem(
            title = "The Show", description = "An episode", durationMinutes = 44f,
            mediaType = "TV Show", imdbId = "tt0000002",
            posterUrl = null, backdropUrl = "https://img/b.jpg",
            episodeTitle = "Pilot", originalAirDate = "2001-01-01",
            season = 1, episodeNumber = 1,
        ),
    )

    @Test
    fun `round trip preserves items and order`() {
        assertEquals(items, rawItemsFromJson(rawItemsToJson(items)))
    }

    @Test
    fun `empty pool round trips`() {
        assertEquals(emptyList<RawMediaItem>(), rawItemsFromJson(rawItemsToJson(emptyList())))
    }
}
