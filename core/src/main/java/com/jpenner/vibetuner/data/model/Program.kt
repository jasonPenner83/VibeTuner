package com.jpenner.vibetuner.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Program(
    val id: String,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val startMinutes: Int,        // minutes since midnight, e.g. 1140 == 19:00
    val imageUrl: String,
    val mediaType: String,        // "Movie" / "Episode" / "TV Show" — resolver metadata, NOT a genre
    val rating: String,
    val cast: List<String> = emptyList(),
    val displayTimeSlot: String,
    val durationMinutes: Int,
    val imdbId: String = "",
    val posterUrl: String? = null,   // ➔ Ensure these exist or add them
    val backdropUrl: String? = null,
    val episodeTitle: String? = null,   // 🟢 NEW: "Encounter at Farpoint"
    val originalAirDate: String? = null,
    val isLive: Boolean = false
) {
    val endMinutes: Int get() = startMinutes + durationMinutes

    /** True when [nowMinutes] falls inside this program's window. */
    fun isAiringAt(nowMinutes: Int): Boolean =
        nowMinutes in startMinutes until endMinutes

    /** 0f..1f progress through the program at [nowMinutes]. */
    fun progressAt(nowMinutes: Int): Float =
        ((nowMinutes - startMinutes).toFloat() / durationMinutes).coerceIn(0f, 1f)
}
