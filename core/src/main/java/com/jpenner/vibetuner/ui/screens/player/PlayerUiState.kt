package com.jpenner.vibetuner.ui.screens.player

import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program

/** Which overlay sheet (TV side panel / phone bottom sheet) is open. */
enum class PlayerSheet { Audio, Subtitles, Info }

/** Immutable snapshot of everything the player chrome renders. */
data class PlayerUiState(
    val channel: Channel? = null,
    val program: Program? = null,        // what's airing now on this channel
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,            // 0f..1f within the live window
    val behindLiveLabel: String = "LIVE",// "-42:18" when time-shifted
    val atLiveEdge: Boolean = true,
    val clock: String = "",
    val controlsVisible: Boolean = true, // chrome shown / faded
    val chromeFocused: Boolean = false,  // standard overlay explicitly opened+focused via Enter
    val switcherOpen: Boolean = false,   // mini-guide overlay
    val sheet: PlayerSheet? = null,      // audio/subtitle picker or info panel
    val audioOptions: List<TrackOption> = emptyList(),
    val subtitleOptions: List<TrackOption> = emptyList(),
    val error: String? = null,
)