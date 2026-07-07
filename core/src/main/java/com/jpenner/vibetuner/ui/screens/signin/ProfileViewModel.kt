package com.jpenner.vibetuner.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilePickerUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    /** Profile whose PIN gate is currently open, if any. */
    val unlocking: Profile? = null,
    val pinError: Boolean = false,
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val store: ProfileStore,
    private val syncPull: suspend () -> Unit = {},
) : ViewModel() {

    /** Reload-on-entry: pull remote profile edits every time the picker opens. */
    fun refreshFromSync() {
        viewModelScope.launch { syncPull() }
    }

    private val unlocking = MutableStateFlow<Profile?>(null)
    private val pinError = MutableStateFlow(false)

    val state: StateFlow<ProfilePickerUiState> =
        combine(store.profiles(), unlocking, pinError) { profiles, unlock, err ->
            ProfilePickerUiState(profiles = profiles, isLoading = false, unlocking = unlock, pinError = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilePickerUiState())

    /**
     * Route straight through for open profiles (persist active + return true);
     * PIN-locked profiles open the gate instead and return false.
     */
    fun choose(profile: Profile): Boolean {
        if (profile.hasPin && profile.requirePin) {
            unlocking.value = profile
            return false
        }
        select(profile)
        return true
    }

    /** Persist the chosen profile as active; the screen then routes to Home. */
    fun select(profile: Profile) {
        repository.setActiveProfile(profile.id)
    }

    /** Verify the gate attempt. True = unlocked (active persisted, gate closed). */
    fun submitPin(pin: String): Boolean {
        val profile = unlocking.value ?: return false
        return if (store.verifyPin(profile, pin)) {
            select(profile)
            dismissUnlock()
            true
        } else {
            pinError.value = true
            false
        }
    }

    fun dismissUnlock() {
        unlocking.value = null
        pinError.value = false
    }
}
