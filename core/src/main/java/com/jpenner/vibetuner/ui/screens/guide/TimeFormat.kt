package com.jpenner.vibetuner.ui.screens.guide

import com.jpenner.vibetuner.data.model.Program
import java.time.LocalTime
import java.time.ZoneId

/** Minutes-since-midnight -> "7:00 PM". */
fun formatTime(minutes: Int): String {
    val h24 = (minutes / 60) % 24
    val m = minutes % 60
    val period = if (h24 < 12) "AM" else "PM"
    val h12 = ((h24 + 11) % 12) + 1
    return "%d:%02d %s".format(h12, m, period)
}

/** A program's window -> "7:00 \u2013 9:00 PM". */
fun timeRange(p: Program): String =
    formatTime(p.startMinutes) + " \u2013 " + formatTime(p.endMinutes)

/** The zone the schedule is assembled in (see ChannelRepository): all "now" math
 *  against Program.startMinutes/endMinutes must read this zone or it won't line up. */
val GuideZone: ZoneId = ZoneId.of("America/Chicago")

/** Minutes since midnight in [GuideZone] \u2014 the shared "now" for schedule lookups. */
fun currentGuideMinutes(): Int =
    LocalTime.now(GuideZone).let { it.hour * 60 + it.minute }