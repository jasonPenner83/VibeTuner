package com.jpenner.vibetuner.ui.screens.lineup

import androidx.compose.runtime.Immutable
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.screens.guide.formatTime

/** How a slot sits relative to the guide clock — drives dimming + the live fill. */
enum class SlotStatus { Aired, OnNow, Upcoming }

/** A program in the day, tagged with its status and (for OnNow) its progress. */
@Immutable
data class LineupSlot(
    val program: Program,
    val status: SlotStatus,
    val progress: Float = 0f, // 0f..1f, only meaningful when OnNow
    /** Whole minutes left in an OnNow program (exact int math); 0 otherwise. */
    val minutesLeft: Int = 0,
)

/** Tag [this] against the guide clock (minutes since midnight, Central). */
fun Program.toSlot(nowMinutes: Int): LineupSlot {
    val status = when {
        endMinutes <= nowMinutes -> SlotStatus.Aired
        isAiringAt(nowMinutes) -> SlotStatus.OnNow
        else -> SlotStatus.Upcoming
    }
    val progress = if (status == SlotStatus.OnNow) progressAt(nowMinutes) else 0f
    val minutesLeft = if (status == SlotStatus.OnNow) (endMinutes - nowMinutes).coerceAtLeast(0) else 0
    return LineupSlot(this, status, progress, minutesLeft)
}

/** "1h 30m" / "2h" / "45m". */
fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

/** Everything the Day Lineup renders, in one immutable snapshot. */
@Immutable
data class DayLineupUiState(
    val channel: Channel? = null,
    val dateLabel: String = "",   // "TUE · JUL 8"
    val clock: String = "",       // "7:17 PM"
    val slots: List<LineupSlot> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Index the list opens scrolled/focused to: the live slot, else the next one, else the top. */
    val focusIndex: Int
        get() = slots.indexOfFirst { it.status == SlotStatus.OnNow }
            .takeIf { it >= 0 }
            ?: slots.indexOfFirst { it.status == SlotStatus.Upcoming }.coerceAtLeast(0)

    /** "6 earlier programs from 6:00 AM", or "" when nothing has aired yet. */
    val earlierLabel: String
        get() {
            val aired = slots.filter { it.status == SlotStatus.Aired }
            if (aired.isEmpty()) return ""
            val noun = if (aired.size == 1) "program" else "programs"
            return "${aired.size} earlier $noun from ${formatTime(aired.first().program.startMinutes)}"
        }
}
