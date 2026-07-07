package com.jpenner.vibetuner.data.repository

import android.content.Context
import android.util.Log
import com.jpenner.vibetuner.data.model.Profile
import java.io.File

/**
 * Supplies the viewer profiles shown on the "Who's watching?" picker and
 * remembers which one is active. Profiles live in the shared [ProfileStore]
 * (vibetuner_profiles.json); only the active selection is persisted here.
 */
class ProfileRepository(private val context: Context) {

    private val activeFile = File(context.filesDir, "vibetuner_active_profile.txt")

    fun profiles(): List<Profile> = ProfileStore.get(context).profilesNow()

    /** Persist [id] as the active profile. */
    fun setActiveProfile(id: String) {
        runCatching { activeFile.writeText(id) }
            .onFailure { Log.e("VibeTuner Storage", "🔥 Active profile write error: ${it.message}") }
    }

    /** The last-selected profile id, or null if none chosen yet. */
    fun activeProfileId(): String? =
        if (activeFile.exists()) runCatching { activeFile.readText() }.getOrNull() else null
}
