package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.RawMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyShuffleTest {

    private fun item(n: Int) = RawMediaItem(
        title = "Item $n", description = "", durationMinutes = 30f,
        mediaType = "Movie", imdbId = "tt$n",
        posterUrl = null, backdropUrl = null, episodeTitle = null,
        originalAirDate = null, season = null, episodeNumber = null,
    )

    private val pool = (1..50).map(::item)

    @Test
    fun `same pool and day shuffle identically — the shared-schedule invariant`() {
        assertEquals(dailySeedShuffle(pool, 20_640L), dailySeedShuffle(pool, 20_640L))
    }

    @Test
    fun `different days shuffle differently`() {
        assertNotEquals(dailySeedShuffle(pool, 20_640L), dailySeedShuffle(pool, 20_641L))
    }
}
