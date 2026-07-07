package com.jpenner.vibetuner.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProgramInfoViewModel(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgramInfoUiState())
    val state: StateFlow<ProgramInfoUiState> = _state.asStateFlow()

    /** Called by the screen with the route's programId. */
    fun load(programId: String) = viewModelScope.launch {
        val channel = repository.channelForProgram(programId)
        val program = channel?.programs?.firstOrNull { it.id == programId }
        val upNext = program?.let { p ->
            channel.programs.firstOrNull { it.startMinutes >= p.endMinutes }
        }
        _state.update {
            it.copy(
                program = program,
                channel = channel,
                upNext = upNext,
                inMyList = program?.let { p -> repository.inMyList(p.id) } ?: false,
                isLoading = false,
            )
        }
    }

    fun toggleMyList() {
        val p = _state.value.program ?: return
        _state.update { it.copy(inMyList = !it.inMyList) }
        viewModelScope.launch { repository.setInMyList(p.id, _state.value.inMyList) }
    }
}