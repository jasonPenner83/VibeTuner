package com.jpenner.vibetuner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.api.ManifestResult
import com.jpenner.vibetuner.data.repository.AddonRepository
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddonsViewModel(
    private val addonRepository: AddonRepository,
    private val profileRepository: ProfileRepository,
    private val profileStore: ProfileStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AddonsUiState())
    val state: StateFlow<AddonsUiState> = _state.asStateFlow()

    /** Resolved fresh per call — the VM outlives navigation, so a frozen id would
     *  keep showing the previous profile's addons after a switch. */
    private fun profileId(): String = profileRepository.activeProfileId() ?: "default"

    /** Profiles whose manifests were already re-fetched this session. */
    private val refreshedProfiles = mutableSetOf<String>()

    init { refresh() }

    fun refresh() {
        val id = profileId()
        publish(id)
        // Best-effort manifest re-fetch (once per profile per session) so pre-options
        // installs pick up extra.options[] and required extras get seeded (spec §5).
        if (refreshedProfiles.add(id)) viewModelScope.launch {
            addonRepository.refreshManifests(id)
            publish(id)
        }
    }

    private fun publish(id: String) = _state.update {
        it.copy(
            addons = addonRepository.getAddons(id),
            allowAdult = profileStore.byId(id)?.allowsAdult == true,
        )
    }

    fun setEnabled(addonId: String, enabled: Boolean) {
        addonRepository.setEnabled(profileId(), addonId, enabled); refresh()
    }

    fun remove(addonId: String) { addonRepository.remove(profileId(), addonId); refresh() }

    fun openSheet() = _state.update { it.copy(sheet = AddSheetState()) }
    fun closeSheet() { validateJob?.cancel(); _state.update { it.copy(sheet = null) } }

    private var validateJob: Job? = null

    /** Debounced validation as the user types / pastes. */
    fun onUrlChange(url: String) {
        _state.update { it.copy(sheet = AddSheetState(url = url, result = if (url.isBlank()) null else ManifestResult.Loading)) }
        validateJob?.cancel()
        if (url.isBlank()) return
        validateJob = viewModelScope.launch {
            delay(400)
            val result = addonRepository.validate(url)
            _state.update { s -> if (s.sheet?.url == url) s.copy(sheet = s.sheet.copy(result = result)) else s }
        }
    }

    fun confirmAdd() = viewModelScope.launch {
        val url = _state.value.sheet?.takeIf { it.canAdd }?.url ?: return@launch
        addonRepository.addByUrl(profileId(), url)
        closeSheet(); refresh()
    }
}
