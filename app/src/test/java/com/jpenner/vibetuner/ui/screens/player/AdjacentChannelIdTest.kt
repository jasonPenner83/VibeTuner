package com.jpenner.vibetuner.ui.screens.player

import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdjacentChannelIdTest {

    private fun channel(id: String) = Channel(
        id = id,
        name = id,
        abbreviation = id,
        description = "",
        number = id,
        category = Category.DEFAULT,
    )

    private val channels = listOf(channel("a"), channel("b"), channel("c"))

    @Test
    fun `moves to the next channel`() {
        assertEquals("b", adjacentChannelId(channels, "a", delta = 1))
    }

    @Test
    fun `moves to the previous channel`() {
        assertEquals("a", adjacentChannelId(channels, "b", delta = -1))
    }

    @Test
    fun `wraps forward past the last channel`() {
        assertEquals("a", adjacentChannelId(channels, "c", delta = 1))
    }

    @Test
    fun `wraps backward past the first channel`() {
        assertEquals("c", adjacentChannelId(channels, "a", delta = -1))
    }

    @Test
    fun `returns null when the current channel isn't in the list`() {
        assertNull(adjacentChannelId(channels, "z", delta = 1))
    }

    @Test
    fun `returns null for an empty list`() {
        assertNull(adjacentChannelId(emptyList(), "a", delta = 1))
    }

    @Test
    fun `single channel list wraps to itself`() {
        assertEquals("a", adjacentChannelId(listOf(channel("a")), "a", delta = 1))
    }
}
