package com.jpenner.vibetuner.ui.screens.settings

import com.jpenner.vibetuner.data.model.SettingControl
import com.jpenner.vibetuner.data.model.SettingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Snapshot of sync/account state used to build the Settings rows. */
data class SyncSettingsState(
    val configured: Boolean,
    val email: String?,
    val lastSyncMs: Long?,
    val error: String?,
)

/** The rows shown in the Settings "Sync" pane for the given state. */
fun syncRows(state: SyncSettingsState): List<SettingItem> = when {
    !state.configured -> listOf(
        SettingItem(
            key = "sync_status",
            label = "Sync",
            sub = "Not configured — add supabase.* keys to local.properties",
            control = SettingControl.Info("Unavailable"),
        ),
    )
    state.email == null -> listOf(
        SettingItem(
            key = "sync_status",
            label = "Account",
            sub = "Not signed in — profiles stay on this device",
            control = SettingControl.Info("Offline"),
        ),
        SettingItem(
            key = "sync_signin",
            label = "Sign in with Google",
            sub = "Sync profiles, add-ons and channels across devices",
            control = SettingControl.Value(""),
        ),
    )
    else -> listOf(
        SettingItem(
            key = "sync_status",
            label = "Account",
            sub = "Signed in as ${state.email}",
            control = SettingControl.Info("Connected"),
        ),
        SettingItem(
            key = "sync_last",
            label = "Last synced",
            sub = state.error?.let { "Sync failed: $it" }
                ?: state.lastSyncMs?.let { "At ${timeFmt.format(Date(it))}" }
                ?: "Not yet synced this session",
            control = SettingControl.Info(if (state.error != null) "Error" else "Auto"),
        ),
        SettingItem(
            key = "sync_now",
            label = "Sync now",
            sub = "Pull the latest and push local changes",
            control = SettingControl.Value(""),
        ),
        SettingItem(
            key = "sync_signout",
            label = "Sign out",
            sub = "Stop syncing on this device (local data stays)",
            control = SettingControl.Value("", danger = true),
        ),
    )
}

private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
