package com.jpenner.vibetuner.ui.screens.startup

/** Everything the startup loading screen renders. Progress is a real 0f..1f fraction of the warm,
 *  spanning every profile being preloaded (not just one). */
data class StartupUiState(
    val progress: Float = 0f,
    val stageIndex: Int = 0,
    val done: Boolean = false,
    val profileName: String? = null,
    val profileIndex: Int = 0,
    val profileTotal: Int = 0,
)

/** A loading-screen stage: mono status label + witty headline. */
data class StartupStage(val label: String, val headline: String)

/**
 * The honest stage set. Labels + witty movie-reference copy from the design, minus the design's
 * "SYNCING RECORDINGS" stage (no recordings feature exists). Single source of truth for stage count.
 */
val STARTUP_STAGES: List<StartupStage> = listOf(
    StartupStage("CONNECTING", "Phoning home for the listings…"),
    StartupStage("GETTING CHANNELS", "Rounding up the usual channels."),
    StartupStage("BUILDING SCHEDULES", "You're gonna need a bigger guide."),
    StartupStage("TUNING SIGNAL", "Tuning the antenna to 88 mph."),
    StartupStage("ALMOST READY", "Here's looking at you, guide."),
)

/** Maps a 0f..1f progress fraction to a valid index into [STARTUP_STAGES]. */
fun stageFor(progress: Float): Int =
    (progress * STARTUP_STAGES.size).toInt().coerceIn(0, STARTUP_STAGES.lastIndex)
