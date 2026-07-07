package com.jpenner.vibetuner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelTypeTest {

    private fun channel(source: CatalogSource?) = Channel(
        id = "ch1",
        name = "Channel One",
        abbreviation = "C1",
        description = "",
        number = "1",
        category = Category.DEFAULT,
        source = source,
        sourceType = if (source != null) SourceType.STREMIO_CATALOG else SourceType.GENRE,
    )

    private fun source(type: String) = CatalogSource(
        addonId = "addon1",
        addonName = "Addon",
        addonAbbrev = "AD",
        type = type,
        catalogId = "cat1",
        catalogName = "Catalog",
    )

    @Test
    fun `movie catalog channel maps to MOVIES`() {
        assertEquals(ChannelType.MOVIES, channel(source("movie")).type)
    }

    @Test
    fun `series catalog channel maps to TV_SHOWS`() {
        assertEquals(ChannelType.TV_SHOWS, channel(source("series")).type)
    }

    @Test
    fun `xxx catalog channel maps to ADULT`() {
        assertEquals(ChannelType.ADULT, channel(source("xxx")).type)
    }

    @Test
    fun `porn catalog channel maps to ADULT`() {
        assertEquals(ChannelType.ADULT, channel(source("porn")).type)
    }

    @Test
    fun `channel with no catalog source maps to LIVE_OTHER`() {
        assertEquals(ChannelType.LIVE_OTHER, channel(null).type)
    }

    @Test
    fun `catalog channel with unrecognized type maps to LIVE_OTHER`() {
        assertEquals(ChannelType.LIVE_OTHER, channel(source("channel")).type)
    }

    @Test
    fun `movie type is case-insensitive`() {
        assertEquals(ChannelType.MOVIES, channel(source("Movie")).type)
        assertEquals(ChannelType.MOVIES, channel(source("MOVIE")).type)
    }

    @Test
    fun `series type is case-insensitive`() {
        assertEquals(ChannelType.TV_SHOWS, channel(source("Series")).type)
        assertEquals(ChannelType.TV_SHOWS, channel(source("SERIES")).type)
    }

    @Test
    fun `adult types are case-insensitive`() {
        assertEquals(ChannelType.ADULT, channel(source("XXX")).type)
        assertEquals(ChannelType.ADULT, channel(source("Porn")).type)
        assertEquals(ChannelType.ADULT, channel(source("ADULT")).type)
        assertEquals(ChannelType.ADULT, channel(source("Erotic")).type)
    }

    @Test
    fun `type strings with whitespace are trimmed`() {
        assertEquals(ChannelType.MOVIES, channel(source("  movie  ")).type)
        assertEquals(ChannelType.ADULT, channel(source("  xxx  ")).type)
    }
}
