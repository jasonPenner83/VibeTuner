package com.jpenner.vibetuner.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerViewModelSheetTest {

    @Test
    fun `openSheet shows controls and records the sheet`() {
        val vm = PlayerViewModel()
        vm.openSheet(PlayerSheet.Audio)
        assertEquals(PlayerSheet.Audio, vm.state.value.sheet)
        assertTrue(vm.state.value.controlsVisible)
    }

    @Test
    fun `closeSheet clears the sheet`() {
        val vm = PlayerViewModel()
        vm.openSheet(PlayerSheet.Subtitles)
        vm.closeSheet()
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun `open resets the sheet on channel change`() {
        val vm = PlayerViewModel()
        vm.openSheet(PlayerSheet.Info)
        vm.open(channel = null, program = null)
        assertNull(vm.state.value.sheet)
    }

    @Test
    fun `setTrackOptions stores both lists`() {
        val vm = PlayerViewModel()
        val audio = listOf(TrackOption("1:0", "English · Stereo", true))
        val subs = listOf(TrackOption(TRACK_OFF_ID, "Off", true))
        vm.setTrackOptions(audio, subs)
        assertEquals(audio, vm.state.value.audioOptions)
        assertEquals(subs, vm.state.value.subtitleOptions)
    }
}
