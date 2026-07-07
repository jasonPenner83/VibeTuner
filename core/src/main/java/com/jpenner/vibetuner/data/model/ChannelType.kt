package com.jpenner.vibetuner.data.model

import com.jpenner.vibetuner.data.model.stremio.isAdultType

/**
 * Coarse channel type for Guide filtering: derived from the Stremio catalog's
 * manifest-declared `type`, not persisted. [ADULT] only ever appears for
 * channels sourced from an adult catalog — which only reach [Channel] at all
 * when the active profile's Profile.allowsAdult is true (adult catalogs are
 * excluded earlier, in CatalogExpansion), so no separate profile check is
 * needed anywhere this enum is consumed.
 */
enum class ChannelType(val label: String) {
    MOVIES("Movies"),
    TV_SHOWS("TV Shows"),
    ADULT("Adult"),
    LIVE_OTHER("Live/Other"),
}

val Channel.type: ChannelType get() {
    val rawType = source?.type?.trim()?.lowercase() ?: return ChannelType.LIVE_OTHER
    return when {
        isAdultType(rawType) -> ChannelType.ADULT
        rawType == "movie" -> ChannelType.MOVIES
        rawType == "series" -> ChannelType.TV_SHOWS
        else -> ChannelType.LIVE_OTHER
    }
}
