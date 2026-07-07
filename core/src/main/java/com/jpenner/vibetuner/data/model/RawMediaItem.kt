package com.jpenner.vibetuner.data.model

/**
 * Normalized media item (movie or single episode) harvested from a Stremio addon
 * catalog, consumed by ChannelRepository's scheduling/assembly path. Source-neutral:
 * the EPG pipeline downstream never knows or cares which provider produced it.
 */
data class RawMediaItem(
    val title: String,
    val description: String,
    val durationMinutes: Float,
    val mediaType: String,
    val imdbId: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val episodeTitle: String? = null,
    val originalAirDate: String? = null,
    val season: Int? = null,
    val episodeNumber: Int? = null
)
