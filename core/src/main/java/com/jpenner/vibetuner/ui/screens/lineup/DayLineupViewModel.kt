package com.jpenner.vibetuner.ui.screens.lineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.screens.guide.currentGuideMinutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * One channel's full-day timeline. Takes a loader rather than ChannelRepository
 * directly (same pattern as StartupViewModel/GuideWarmer) so the clock/status
 * logic stays unit-testable; hosts pass `{ channelRepository.loadGuide() }`.
 * Status math reads the guide clock (Central — see [currentGuideMinutes]);
 * the displayed clock string is local wall time, like the player's.
 */
class DayLineupViewModel(
    private val loadChannels: suspend () -> List<Channel>,
    private val nowMinutes: () -> Int = ::currentGuideMinutes,
    private val logError: (String) -> Unit = { android.util.Log.e("VibeTuner Lineup", it) },
) : ViewModel() {

    private val _state = MutableStateFlow(DayLineupUiState())
    val state: StateFlow<DayLineupUiState> = _state.asStateFlow()

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormat = DateTimeFormatter.ofPattern("EEE · MMM d")
    private var tickerStarted = false

    /** Called on every entry (screen or player overlay): the VM is
     *  activity-scoped, so each open must re-resolve the channel. */
    fun load(channelId: String) {
        _state.update { it.copy(isLoading = true, channel = null, slots = emptyList()) }
        viewModelScope.launch {
            val channels = try {
                loadChannels()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logError("🔥 Day lineup load failed: ${t.message}")
                emptyList()
            }
            val channel = channels.find { it.id == channelId }
            val now = nowMinutes()
            _state.update {
                it.copy(
                    channel = channel,
                    slots = channel?.programs.orEmpty().map { p -> p.toSlot(now) },
                    dateLabel = LocalDate.now().format(dateFormat).uppercase(),
                    clock = LocalTime.now().format(clockFormat),
                    isLoading = false,
                )
            }
        }
        if (!tickerStarted) {
            tickerStarted = true
            startClock()
        }
    }

    /** Re-tag every slot so status + the OnNow fill track the clock. */
    internal fun retag(now: Int = nowMinutes()) = _state.update { s ->
        s.copy(
            clock = LocalTime.now().format(clockFormat),
            slots = s.slots.map { it.program.toSlot(now) },
        )
    }

    private fun startClock() = viewModelScope.launch {
        while (true) {
            delay(30_000)
            retag()
        }
    }
}
