package com.jpenner.vibetuner.ui.screens.detail

import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program

/** Immutable snapshot for the detail screen. */
data class ProgramInfoUiState(
    val program: Program? = null,
    val channel: Channel? = null,
    val upNext: Program? = null,        // resolved from the channel schedule
    val inMyList: Boolean = false,
    val isLoading: Boolean = true,
) {
    // Pre-formatted metadata columns, so the composable just lays them out.
    val cast: List<String> get() = program?.cast.orEmpty()
    val audioLines: List<String> get() = listOf("5.1 Surround \u00B7 Stereo", "Subtitles available")
}