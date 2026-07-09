package com.jpenner.vibetuner.ui.screens.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.ChannelType
import com.jpenner.vibetuner.data.repository.ChannelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GuideViewModel(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GuideUiState())
    val state: StateFlow<GuideUiState> = _state.asStateFlow()

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")

    // The schedule is assembled in Central time (see ChannelRepository), so the
    // clock + now-line must read the same zone or they won't line up with the grid.
    private val guideZone = ZoneId.of("America/Chicago")

    /** Which profile the held guide belongs to — a switch must rebuild it. */
    private var loadedProfileId: String? = null

    init {
        reload()
        startClock()
    }

    /** Also called on screen entry: the VM outlives navigation, so a profile switch
     *  (or a Channel Manager edit) must re-derive the guide. Same-profile re-entry
     *  is nearly free — an unchanged lineup is served from the repository cache. */
    fun reload() {
        val profileId = repository.activeProfileId()
        if (profileId != loadedProfileId) {
            // Different viewer: blank the old grid and show the loading state.
            _state.update { it.copy(isLoading = true, channels = emptyList(), focused = FocusedCell()) }
        }
        viewModelScope.launch {
            val channels = repository.loadGuide()
            loadedProfileId = profileId
            _state.update {
                it.copy(
                    channels = channels,
                    // start the grid 30 min in the past so "now" sits just inside
                    gridStartMinutes = nowMinutes() - 30,
                    isLoading = false,
                    focusChannelId = repository.tunedChannelId(),
                    favouriteChannelIds = repository.favouriteChannelIds(),
                )
            }
        }
    }

    /** Re-reads the wall clock every 30s to advance the now-line. */
    private fun startClock() = viewModelScope.launch {
        while (true) {
            val now = LocalTime.now(guideZone)
            _state.update {
                it.copy(nowMinutes = now.hour * 60 + now.minute, clock = now.format(clockFormat))
            }
            delay(30_000)
        }
    }

    fun onProgramFocused(channel: Int, program: Int) =
        _state.update { it.copy(focused = FocusedCell(channel, program)) }

    fun onTypeSelected(type: ChannelType?) =
        _state.update { it.withTypeFilter(type) }

    fun onGenreSelected(category: Category?) =
        _state.update { it.withGenreFilter(category) }

    private fun nowMinutes(): Int = LocalTime.now(guideZone).let { it.hour * 60 + it.minute }

    fun toggleFavourite(channelId: String) {
        repository.toggleFavourite(channelId)
        // Re-read so the row star and menu label recompose instantly.
        _state.update { it.copy(favouriteChannelIds = repository.favouriteChannelIds()) }
    }
}