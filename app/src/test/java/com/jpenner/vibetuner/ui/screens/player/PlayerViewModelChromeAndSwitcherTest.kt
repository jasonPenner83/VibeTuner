package com.jpenner.vibetuner.ui.screens.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerViewModelChromeAndSwitcherTest {

    @Test
    fun `openChrome shows controls and marks chrome focused`() {
        val vm = PlayerViewModel()
        vm.openChrome()
        assertTrue(vm.state.value.controlsVisible)
        assertTrue(vm.state.value.chromeFocused)
    }

    @Test
    fun `closeChrome returns to baseline`() {
        val vm = PlayerViewModel()
        vm.openChrome()
        vm.closeChrome()
        assertFalse(vm.state.value.controlsVisible)
        assertFalse(vm.state.value.chromeFocused)
    }

    @Test
    fun `openSwitcher does not show the standard overlay`() {
        val vm = PlayerViewModel()
        vm.closeChrome() // baseline: controlsVisible false
        vm.openSwitcher()
        assertTrue(vm.state.value.switcherOpen)
        assertFalse(vm.state.value.controlsVisible)
    }

    @Test
    fun `closeSwitcher returns to baseline`() {
        val vm = PlayerViewModel()
        vm.openSwitcher()
        vm.closeSwitcher()
        assertFalse(vm.state.value.switcherOpen)
        assertFalse(vm.state.value.controlsVisible)
    }

    @Test
    fun `open resets chromeFocused to baseline`() {
        val vm = PlayerViewModel()
        vm.openChrome()
        vm.open(channel = null, program = null)
        assertFalse(vm.state.value.chromeFocused)
    }

    @Test
    fun `open resets switcherOpen to baseline`() {
        val vm = PlayerViewModel()
        vm.openSwitcher()
        vm.open(channel = null, program = null)
        assertFalse(vm.state.value.switcherOpen)
    }
}
