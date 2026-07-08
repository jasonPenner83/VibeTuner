package com.jpenner.vibetuner.ui.screens.lineup

import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayLineupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    private fun channel(id: String, vararg programs: Program) =
        Channel(
            id = id,
            name = id,
            abbreviation = "AB",
            description = "",
            number = "100",
            category = Category.DEFAULT,
            programs = programs.toList(),
        )

    @Test
    fun `load resolves the channel and tags slots against the guide clock`() {
        val ch = channel(
            "a",
            program("p1", 18 * 60, 60),
            program("p2", 19 * 60, 120),
            program("p3", 21 * 60, 30),
        )
        val vm = DayLineupViewModel(
            loadChannels = { listOf(channel("other"), ch) },
            nowMinutes = { 19 * 60 + 17 },
            logError = {},
        )
        vm.load("a")
        val s = vm.state.value
        assertFalse(s.isLoading)
        assertEquals("a", s.channel?.id)
        assertEquals(
            listOf(SlotStatus.Aired, SlotStatus.OnNow, SlotStatus.Upcoming),
            s.slots.map { it.status },
        )
    }

    @Test
    fun `load with an unknown id lands in the empty state`() {
        val vm = DayLineupViewModel(
            loadChannels = { listOf(channel("a")) },
            nowMinutes = { 0 },
            logError = {},
        )
        vm.load("nope")
        assertNull(vm.state.value.channel)
        assertTrue(vm.state.value.slots.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `loader failure logs and lands in the empty state`() {
        var logged: String? = null
        val vm = DayLineupViewModel(
            loadChannels = { error("boom") },
            nowMinutes = { 0 },
            logError = { logged = it },
        )
        vm.load("a")
        assertNull(vm.state.value.channel)
        assertFalse(vm.state.value.isLoading)
        assertTrue(logged.orEmpty().contains("boom"))
    }

    @Test
    fun `retag flips a slot from OnNow to Aired when the clock passes its end`() {
        var now = 19 * 60 + 30
        val vm = DayLineupViewModel(
            loadChannels = { listOf(channel("a", program("p1", 19 * 60, 60))) },
            nowMinutes = { now },
            logError = {},
        )
        vm.load("a")
        assertEquals(SlotStatus.OnNow, vm.state.value.slots.single().status)
        now = 20 * 60 + 1
        vm.retag()
        assertEquals(SlotStatus.Aired, vm.state.value.slots.single().status)
    }

    @Test
    fun `loading a second channel replaces the previous slots`() {
        val a = channel("a", program("pa", 8 * 60, 60))
        val b = channel("b", program("pb", 9 * 60, 60))
        val vm = DayLineupViewModel(
            loadChannels = { listOf(a, b) },
            nowMinutes = { 10 * 60 },
            logError = {},
        )
        vm.load("a")
        vm.load("b")
        assertEquals("b", vm.state.value.channel?.id)
        assertEquals("pb", vm.state.value.slots.single().program.id)
    }
}
