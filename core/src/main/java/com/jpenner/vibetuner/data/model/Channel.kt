package com.jpenner.vibetuner.data.model

import androidx.compose.runtime.Immutable



@Immutable
data class Channel(
    val id: String,
    val name: String,
    val abbreviation: String,
    val description: String,
    val logoUrl: String? = null,
    val number: String,
    val category: Category,
    val sortingRule: String = "RANDOM",
    val marathonLimit: Int? = null,
    val orderIndex: Int = 0,
    val programs: List<Program> = emptyList(),
    val sourceType: SourceType = SourceType.GENRE,
    val sourceValue: String = "", // For STREMIO_CATALOG: "<catalogType>/<catalogId>"; for GENRE: genre name
    val sourceKey: String = "",    // Stable mirror identity for auto-imported channels (e.g. "stremio:<addonId>:<type>:<catalogId>")
    val enabled: Boolean = true,
    val autoImported: Boolean = false,
    val source: CatalogSource? = null
) {
    fun nowPlaying(nowMinutes: Int): Program? =
        programs.firstOrNull { it.isAiringAt(nowMinutes) }

    fun nextUp(nowMinutes: Int): Program? =
        programs.firstOrNull { it.startMinutes > nowMinutes }
}
enum class SourceType {
    GENRE,           // Sourced from a streaming genre provider block (legacy / manual channels)
    STREMIO_CATALOG  // Sourced from a Stremio addon catalog; sourceValue = "<type>/<catalogId>"
}