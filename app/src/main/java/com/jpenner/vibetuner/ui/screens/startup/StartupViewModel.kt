package com.jpenner.vibetuner.ui.screens.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Safety cap so a slow/hanging addon can never freeze startup. Normal launches finish well under this. */
const val STARTUP_TIMEOUT_MS: Long = 20_000L

/** Seam so the preload can be driven by a fake in tests and by ChannelRepository in the app.
 *  [onProgress] reports (profileIndex, profileTotal, profileName, done, total) — [done]/[total] are
 *  the current profile's channel-populate progress; [profileIndex]/[profileTotal] place that profile
 *  within the full preload-all-profiles sweep. */
fun interface GuideWarmer {
    suspend fun warm(onProgress: (profileIndex: Int, profileTotal: Int, profileName: String, done: Int, total: Int) -> Unit)
}

/**
 * Runs the guide warm and maps real populate progress to [StartupUiState], emitting each update.
 * Pure and dispatcher-free so it is testable with runBlocking. Wrapped in withTimeoutOrNull so the
 * screen always resolves to done, even if [warmer] hangs — the per-channel disk cache is warm by
 * then, so the remainder fills in fast on first screen entry.
 */
suspend fun runStartupPreload(
    warmer: GuideWarmer,
    timeoutMs: Long,
    emit: (StartupUiState) -> Unit,
) {
    emit(StartupUiState(progress = 0f, stageIndex = 0, done = false))
    try {
        withTimeoutOrNull(timeoutMs) {
            warmer.warm { profileIndex, profileTotal, profileName, done, total ->
                val perProfile = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
                val overall = if (profileTotal <= 0) perProfile
                    else ((profileIndex + perProfile) / profileTotal).coerceIn(0f, 1f)
                emit(
                    StartupUiState(
                        progress = overall,
                        stageIndex = stageFor(overall),
                        done = false,
                        profileName = profileName,
                        profileIndex = profileIndex,
                        profileTotal = profileTotal,
                    )
                )
            }
        }
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        // Startup must never hang or crash on a preload failure. The individual
        // network/disk callees already log their own errors; fall through to the
        // terminal emit so the app proceeds and screens repopulate from cache later.
    } finally {
        emit(StartupUiState(progress = 1f, stageIndex = STARTUP_STAGES.lastIndex, done = true))
    }
}

/** Thin ViewModel: kicks off the preload on construction and exposes its state. */
class StartupViewModel(
    private val warmer: GuideWarmer,
    private val timeoutMs: Long = STARTUP_TIMEOUT_MS,
) : ViewModel() {

    private val _state = MutableStateFlow(StartupUiState())
    val state: StateFlow<StartupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runStartupPreload(warmer, timeoutMs) { _state.value = it }
        }
    }
}
