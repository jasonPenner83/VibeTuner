package com.jpenner.vibetuner.ui.screens.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chrome's isPlaying must mirror the real player (via setPlaying from the
 * Player.Listener), not a cosmetic toggle that can drift from actual playback.
 */
class PlayerViewModelPlayingStateTest {

    @Test
    fun `setPlaying updates chrome state from real player events`() {
        val vm = PlayerViewModel()
        assertTrue(vm.state.value.isPlaying)

        vm.setPlaying(false)
        assertFalse(vm.state.value.isPlaying)

        vm.setPlaying(true)
        assertTrue(vm.state.value.isPlaying)
    }
}
