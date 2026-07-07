package com.jpenner.vibetuner.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val HIDE_DELAY_MS = 4_000L

/**
 * Holds the player chrome state. Seeded directly with the [Channel]/[Program]
 * the caller already tuned (see [open]); it does not look channels up itself.
 * In-player zapping is deferred — the switcher routes back out through the
 * existing resolve flow — so no repository dependency is needed here.
 */
class PlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")
    private var hideJob: kotlinx.coroutines.Job? = null
    private var tickerStarted = false

    /** Seed the chrome from the channel/program that was tuned. */
    fun open(channel: Channel?, program: Program?) {
        val now = nowMinutes()
        _state.update {
            it.copy(
                channel = channel,
                program = program ?: channel?.nowPlaying(now),
                clock = clock(),
                chromeFocused = false,
                switcherOpen = false,
            )
        }
        if (!tickerStarted) {
            tickerStarted = true
            startProgressTicker()
        }
        showControls()           // visible on entry, then auto-hides
    }

    /** Any D-pad activity calls this: reveal chrome and restart the timer. */
    fun showControls() {
        _state.update { it.copy(controlsVisible = true) }
        hideJob?.cancel()
        hideJob = viewModelScope.launch {
            delay(HIDE_DELAY_MS)
            // never auto-hide while an overlay holds focus
            if (!_state.value.switcherOpen && !_state.value.chromeFocused) {
                _state.update { it.copy(controlsVisible = false) }
            }
        }
    }

    /** Mirrors the real player's playWhenReady — fed by the Player.Listener. */
    fun setPlaying(playing: Boolean) = _state.update { it.copy(isPlaying = playing) }

    /** Short-press Enter at baseline: opens the standard overlay with focus, no auto-hide. */
    fun openChrome() {
        hideJob?.cancel()
        _state.update { it.copy(controlsVisible = true, chromeFocused = true) }
    }

    /** Back while the standard overlay is focused: returns to baseline. */
    fun closeChrome() = _state.update { it.copy(controlsVisible = false, chromeFocused = false) }

    /** Long-press Enter at baseline: opens only the channel switcher, never the
     *  standard overlay — the two would otherwise render on top of each other. */
    fun openSwitcher() = _state.update { it.copy(switcherOpen = true) }
    fun closeSwitcher() = _state.update { it.copy(switcherOpen = false, controlsVisible = false) }

    /** Player events flow back into the chrome state. */
    fun setBuffering(buffering: Boolean) = _state.update { it.copy(isBuffering = buffering) }
    fun setError(message: String?) = _state.update { it.copy(error = message) }

    private fun startProgressTicker() = viewModelScope.launch {
        while (true) {
            val now = nowMinutes()
            _state.update { s ->
                val p = s.channel?.nowPlaying(now) ?: s.program
                s.copy(program = p, clock = clock(),
                    progress = p?.progressAt(now) ?: s.progress)
            }
            delay(1_000)
        }
    }

    private fun clock() = LocalTime.now().format(clockFormat)
    private fun nowMinutes() = LocalTime.now().let { it.hour * 60 + it.minute }
}
