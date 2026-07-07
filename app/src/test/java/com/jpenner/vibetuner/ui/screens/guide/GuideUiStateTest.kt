package com.jpenner.vibetuner.ui.screens.guide

import com.jpenner.vibetuner.data.model.CatalogSource
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.ChannelType
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideUiStateTest {

    private fun program(id: String, startMinutes: Int, durationMinutes: Int) =
        Program(
            id = id,
            title = id,
            description = "",
            startTimeMillis = 0L,
            endTimeMillis = 0L,
            startMinutes = startMinutes,
            imageUrl = "",
            mediaType = "Movie",
            rating = "",
            displayTimeSlot = "",
            durationMinutes = durationMinutes,
        )

    private fun channel(vararg programs: Program) =
        Channel(
            id = "ch1",
            name = "Channel One",
            abbreviation = "C1",
            description = "",
            number = "1",
            category = Category.DEFAULT,
            programs = programs.toList(),
        )

    @Test
    fun focusedProgram_is_the_now_playing_program_not_the_first() {
        // Schedule: 18:00 first show, 19:00 the show airing "now" (nowMinutes = 19:10).
        val first = program("first", startMinutes = 18 * 60, durationMinutes = 60)
        val now = program("now", startMinutes = 19 * 60, durationMinutes = 60)
        val state = GuideUiState(
            channels = listOf(channel(first, now)),
            nowMinutes = 19 * 60 + 10,
            isLoading = false,
        )

        assertEquals("now", state.focusedProgram?.id)
    }

    private fun catalogSource(type: String) = CatalogSource(
        addonId = "addon1",
        addonName = "Addon",
        addonAbbrev = "AD",
        type = type,
        catalogId = "cat1",
        catalogName = "Catalog",
    )

    private fun channel(
        id: String,
        category: Category,
        source: CatalogSource? = null,
        vararg programs: Program,
    ) = Channel(
        id = id,
        name = id,
        abbreviation = id.take(2),
        description = "",
        number = "1",
        category = category,
        programs = programs.toList(),
        source = source,
        sourceType = if (source != null) SourceType.STREMIO_CATALOG else SourceType.GENRE,
    )

    @Test
    fun `visibleChannels applies both type and genre filters`() {
        val movie = channel("movie", Category.Movies, catalogSource("movie"))
        val series = channel("series", Category.Series, catalogSource("series"))
        val sports = channel("sports", Category.Sports)
        val state = GuideUiState(channels = listOf(movie, series, sports))

        assertEquals(listOf(movie, series, sports), state.visibleChannels)
        assertEquals(listOf(movie), state.withTypeFilter(ChannelType.MOVIES).visibleChannels)
        assertEquals(listOf(sports), state.withGenreFilter(Category.Sports).visibleChannels)
    }

    @Test
    fun `availableTypes lists only types present, in enum order`() {
        val movie = channel("movie", Category.Movies, catalogSource("movie"))
        val sports = channel("sports", Category.Sports)
        val state = GuideUiState(channels = listOf(movie, sports))

        assertEquals(listOf(ChannelType.MOVIES, ChannelType.LIVE_OTHER), state.availableTypes)
    }

    @Test
    fun `availableGenres narrows to the current type filter`() {
        val movie = channel("movie", Category.Movies, catalogSource("movie"))
        val series = channel("series", Category.Action, catalogSource("series"))
        val state = GuideUiState(channels = listOf(movie, series))

        assertEquals(listOf(Category.Movies), state.withTypeFilter(ChannelType.MOVIES).availableGenres)
        assertEquals(listOf(Category.Movies, Category.Action), state.availableGenres)
    }

    @Test
    fun `withTypeFilter resets genre when it is no longer available`() {
        val movie = channel("movie", Category.Movies, catalogSource("movie"))
        val series = channel("series", Category.Action, catalogSource("series"))
        val state = GuideUiState(channels = listOf(movie, series)).withGenreFilter(Category.Action)

        val afterTypeChange = state.withTypeFilter(ChannelType.MOVIES)

        assertEquals(null, afterTypeChange.genreFilter)
        assertEquals(listOf(movie), afterTypeChange.visibleChannels)
    }

    @Test
    fun `withTypeFilter keeps genre when it is still available`() {
        val movie = channel("movie", Category.Movies, catalogSource("movie"))
        val movie2 = channel("movie2", Category.Movies, catalogSource("movie"))
        val state = GuideUiState(channels = listOf(movie, movie2)).withGenreFilter(Category.Movies)

        val afterTypeChange = state.withTypeFilter(ChannelType.MOVIES)

        assertEquals(Category.Movies, afterTypeChange.genreFilter)
    }
}
