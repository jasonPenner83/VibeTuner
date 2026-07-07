package com.jpenner.vibetuner.data.model

import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program

/**
 * A presentation projection for any tile shown in a home rail.
 *
 * The home screen mixes Programs ("On Now") and Channels ("Live Channels")
 * into rails of identical-looking cards. Mapping each
 * domain type into ContentItem keeps ContentCard / ContentRail dumb and
 * source-agnostic — they never branch on what the tile actually is.
 *
 * Lives in ui/model (not domain) because it's shaped for the view,
 * not the business domain.
 */
data class ContentItem(
    val id: String,
    val title: String,
    val subtitle: String,        // e.g. "CH 2 · Meridian News"
    val thumbnailUrl: String?,   // Coil; null shows the placeholder surface
    val isLive: Boolean = false,
    val badge: String? = null,   // small corner tag: "LIVE", "REC", category…
    val target: PlaybackTarget,  // what OK/Enter does
)

/** Where activating a tile sends the user. */
sealed interface PlaybackTarget {
    data class WatchChannel(val channelId: String) : PlaybackTarget
    data class ProgramDetail(val programId: String) : PlaybackTarget
}

// ---- mappers: domain -> presentation ----

fun Program.toContentItem(channel: Channel) = ContentItem(
    id = id,
    title = title,
    subtitle = "CH " + channel.number + " · " + channel.name,
    // Landscape card, so prefer the wide art; fall back to poster, then the
    // repository's resolved imageUrl. Stay null when there's genuinely no art
    // so ContentCard shows its placeholder surface.
    thumbnailUrl = backdropUrl ?: posterUrl ?: imageUrl.ifBlank { null },
    isLive = isLive,
    badge = if (isLive) "LIVE" else channel.category.label,
    target = PlaybackTarget.ProgramDetail(id),
)

fun Channel.toContentItem(nowMinutes: Int) = ContentItem(
    id = id,
    title = name,
    subtitle = nowPlaying(nowMinutes)?.title ?: category.label,
    thumbnailUrl = logoUrl,
    isLive = true,
    badge = "CH " + number,
    target = PlaybackTarget.WatchChannel(id),
)