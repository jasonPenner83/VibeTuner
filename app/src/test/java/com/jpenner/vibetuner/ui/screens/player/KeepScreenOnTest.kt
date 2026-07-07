package com.jpenner.vibetuner.ui.screens.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenOnTest {

    @Test
    fun `playing holds the screen awake`() {
        assertTrue(isActivelyPlaying(playWhenReady = true, playbackState = Player.STATE_READY))
    }

    @Test
    fun `buffering mid-playback holds the screen awake`() {
        assertTrue(isActivelyPlaying(playWhenReady = true, playbackState = Player.STATE_BUFFERING))
    }

    @Test
    fun `paused releases the screen`() {
        assertFalse(isActivelyPlaying(playWhenReady = false, playbackState = Player.STATE_READY))
        assertFalse(isActivelyPlaying(playWhenReady = false, playbackState = Player.STATE_BUFFERING))
    }

    @Test
    fun `idle and ended release the screen even if playWhenReady`() {
        assertFalse(isActivelyPlaying(playWhenReady = true, playbackState = Player.STATE_IDLE))
        assertFalse(isActivelyPlaying(playWhenReady = true, playbackState = Player.STATE_ENDED))
    }
}
