package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.SettingControl
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for user-editable preferences.
 *
 * The defaults live in [SettingsViewModel]; this store only carries the rows
 * the user has actually changed. Keys match [SettingItem.key] (e.g. "resolution",
 * "autoplay", "dialogue").
 */
interface SettingsRepository {

    /**
     * Cold stream of saved overrides, keyed by row. Emits the current snapshot
     * immediately on collection and again whenever a value is saved, so the
     * ViewModel can hydrate on launch and stay in sync across screens.
     */
    fun overrides(): Flow<Map<String, SettingControl>>

    /** Persist a single edited row. Overwrites any prior value for [key]. */
    suspend fun save(key: String, control: SettingControl)

    /** Remove every override, returning all rows to their defaults. */
    suspend fun reset()
}