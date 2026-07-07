package com.jpenner.vibetuner.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.repository.ChannelRepository
import com.jpenner.vibetuner.data.model.toContentItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a")

    /** Which profile the held rails belong to — a switch must rebuild them. */
    private var loadedProfileId: String? = null

    init {
        reload()
        startClock()
    }

    /** Also called on screen entry: the VM outlives navigation, so a profile switch
     *  must rebuild the hero + rails for the new viewer. Same-profile re-entry is
     *  nearly free — an unchanged lineup is served from the repository cache. */
    fun reload() {
        val profileId = repository.activeProfileId()
        if (profileId != loadedProfileId) {
            // Different viewer: blank the old rails and show the loading state.
            _state.update { it.copy(isLoading = true, featuredChannel = null, featuredProgram = null, rails = emptyList()) }
        }
        load(profileId)
    }

    private fun load(profileId: String) = viewModelScope.launch {
        val channels = repository.loadGuide()
        loadedProfileId = profileId
        val now = nowMinutes()

        // The hero is the first channel currently airing live content.
        val heroChannel = channels.firstOrNull { it.nowPlaying(now)?.isLive == true }
            ?: channels.firstOrNull()

        // Map each domain source into the shared ContentItem so the rails stay dumb.
        val onNow = channels.mapNotNull { ch ->
            ch.nowPlaying(now)?.toContentItem(ch)
        }
        val liveChannels = channels.map { it.toContentItem(now) }

        _state.update {
            it.copy(
                featuredChannel = heroChannel,
                featuredProgram = heroChannel?.nowPlaying(now),
                rails = listOfNotNull(
                    Rail("on-now", "On Now", onNow).takeIf { r -> r.items.isNotEmpty() },
//                    Rail("live", "Live Channels", liveChannels),
                ),
                isLoading = false,
            )
        }
    }

    private fun startClock() = viewModelScope.launch {
        while (true) {
            _state.update { it.copy(clock = LocalTime.now().format(clockFormat)) }
            delay(30_000)
        }
    }

    private fun nowMinutes() = LocalTime.now().let { it.hour * 60 + it.minute }
}