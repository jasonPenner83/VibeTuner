package com.jpenner.vibetuner.ui.screens.lineup

import com.jpenner.vibetuner.data.model.Program
import org.junit.Assert.assertEquals
import org.junit.Test

class DayLineupUiStateTest {

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

    private fun slots(vararg slots: LineupSlot) = DayLineupUiState(slots = slots.toList())

    @Test
    fun `toSlot tags aired at the exact end minute and onNow at the exact start`() {
        val p = program("p", startMinutes = 19 * 60, durationMinutes = 60)
        assertEquals(SlotStatus.Upcoming, p.toSlot(19 * 60 - 1).status)
        assertEquals(SlotStatus.OnNow, p.toSlot(19 * 60).status)
        assertEquals(SlotStatus.OnNow, p.toSlot(20 * 60 - 1).status)
        assertEquals(SlotStatus.Aired, p.toSlot(20 * 60).status)
    }

    @Test
    fun `onNow slot carries progress and minutes left`() {
        val slot = program("p", startMinutes = 19 * 60, durationMinutes = 120).toSlot(19 * 60 + 30)
        assertEquals(0.25f, slot.progress, 0.001f)
        assertEquals(90, slot.minutesLeft)
    }

    @Test
    fun `midnight-spanning program tags Upcoming after the day frame rolls (accepted behavior)`() {
        // Pins spec-accepted behavior, not a bug: status math is same-day-frame only,
        // so a program that ends after midnight (endMinutes > 1440) reads as Upcoming
        // once `now` has rolled over to the next day's minute frame, even though the
        // real-world program is still airing.
        val p = program("late", startMinutes = 23 * 60 + 30, durationMinutes = 90)
        assertEquals(SlotStatus.OnNow, p.toSlot(23 * 60 + 45).status)
        assertEquals(SlotStatus.Upcoming, p.toSlot(30).status)
    }

    @Test
    fun `aired and upcoming slots have zero progress`() {
        val p = program("p", startMinutes = 19 * 60, durationMinutes = 60)
        assertEquals(0f, p.toSlot(21 * 60).progress, 0f)
        assertEquals(0f, p.toSlot(18 * 60).progress, 0f)
        assertEquals(0, p.toSlot(21 * 60).minutesLeft)
        assertEquals(0, p.toSlot(18 * 60).minutesLeft)
    }

    @Test
    fun `focusIndex prefers the live slot`() {
        val state = slots(
            program("a", 18 * 60, 60).toSlot(19 * 60 + 10),
            program("b", 19 * 60, 60).toSlot(19 * 60 + 10),
            program("c", 20 * 60, 60).toSlot(19 * 60 + 10),
        )
        assertEquals(1, state.focusIndex)
    }

    @Test
    fun `focusIndex falls back to the first upcoming slot during a schedule gap`() {
        // Gap: nothing airing at 19:30.
        val state = slots(
            program("a", 18 * 60, 60).toSlot(19 * 60 + 30),
            program("b", 20 * 60, 60).toSlot(19 * 60 + 30),
        )
        assertEquals(1, state.focusIndex)
    }

    @Test
    fun `focusIndex is zero when everything has aired`() {
        val state = slots(
            program("a", 6 * 60, 60).toSlot(23 * 60),
            program("b", 7 * 60, 60).toSlot(23 * 60),
        )
        assertEquals(0, state.focusIndex)
    }

    @Test
    fun `earlierLabel counts aired programs from the first start time`() {
        val state = slots(
            program("a", 6 * 60, 60).toSlot(19 * 60),
            program("b", 7 * 60, 60).toSlot(19 * 60),
            program("c", 19 * 60, 60).toSlot(19 * 60),
        )
        assertEquals("2 earlier programs from 6:00 AM", state.earlierLabel)
    }

    @Test
    fun `earlierLabel uses the singular for one aired program`() {
        val state = slots(
            program("a", 6 * 60, 60).toSlot(8 * 60),
            program("b", 8 * 60, 60).toSlot(8 * 60),
        )
        assertEquals("1 earlier program from 6:00 AM", state.earlierLabel)
    }

    @Test
    fun `earlierLabel is empty when nothing has aired`() {
        val state = slots(program("a", 20 * 60, 60).toSlot(19 * 60))
        assertEquals("", state.earlierLabel)
    }

    @Test
    fun `durationLabel formats hours and minutes`() {
        assertEquals("1h 30m", durationLabel(90))
        assertEquals("2h", durationLabel(120))
        assertEquals("45m", durationLabel(45))
        assertEquals("0m", durationLabel(0))
    }
}
