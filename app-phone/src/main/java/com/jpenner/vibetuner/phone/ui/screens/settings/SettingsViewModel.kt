package com.jpenner.vibetuner.phone.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.sync.SyncManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jpenner.vibetuner.ui.screens.settings.SyncSettingsState

/**
 * Phase 1's "small settings page" is sync-only — a single-purpose ViewModel
 * around [SyncManager], not the TV app's larger multi-section SettingsViewModel
 * (display/parental rows aren't part of this MVP). The sync row copy
 * itself (syncRows/SyncSettingsState in :core) is shared with the TV app.
 */
class SettingsViewModel(private val syncManager: SyncManager) : ViewModel() {

    private val _signInGoogle = Channel<Unit>(Channel.BUFFERED)
    val signInGoogle = _signInGoogle.receiveAsFlow()

    val state: StateFlow<SyncSettingsState> = combine(
        syncManager.auth?.account ?: MutableStateFlow(null),
        syncManager.lastSyncMs,
        syncManager.syncError,
    ) { account, lastSync, error ->
        SyncSettingsState(
            configured = syncManager.isConfigured,
            email = account?.email,
            lastSyncMs = lastSync,
            error = error,
        )
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        SyncSettingsState(configured = syncManager.isConfigured, email = null, lastSyncMs = null, error = null),
    )

    fun onActivate(key: String) {
        when (key) {
            "sync_signin" -> viewModelScope.launch { _signInGoogle.send(Unit) }
            "sync_now" -> viewModelScope.launch { syncManager.syncNow() }
            "sync_signout" -> viewModelScope.launch { syncManager.signOut() }
        }
    }
}
