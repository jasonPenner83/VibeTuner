package com.jpenner.vibetuner.ui.screens.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale

/** Sentinel id for the subtitles-off entry. */
const val TRACK_OFF_ID = "off"

/**
 * One selectable audio/subtitle track as the picker UIs render it.
 * [id] is "<groupIndex>:<trackIndex>" over the FULL [Tracks.getGroups] list,
 * so [selectTrack] can find the same group again — or [TRACK_OFF_ID].
 */
data class TrackOption(
    val id: String,
    val label: String,
    val selected: Boolean,
)

fun audioOptions(tracks: Tracks): List<TrackOption> =
    trackOptions(tracks, C.TRACK_TYPE_AUDIO)

/** Subtitle tracks prefixed with an Off entry; Off is selected while no text track is on. */
fun subtitleOptions(tracks: Tracks): List<TrackOption> {
    val options = trackOptions(tracks, C.TRACK_TYPE_TEXT)
    val anySelected = options.any { it.selected }
    return listOf(TrackOption(TRACK_OFF_ID, "Off", !anySelected)) + options
}

/**
 * Applies [option] to [player]: the Off entry disables text rendering,
 * anything else re-enables the type and pins a [TrackSelectionOverride].
 */
fun selectTrack(player: Player, trackType: Int, option: TrackOption) {
    val builder = player.trackSelectionParameters.buildUpon()
    if (option.id == TRACK_OFF_ID) {
        player.trackSelectionParameters =
            builder.setTrackTypeDisabled(trackType, true).build()
        return
    }
    val parts = option.id.split(":")
    if (parts.size != 2) return
    val groupIndex = parts[0].toIntOrNull() ?: return
    val trackIndex = parts[1].toIntOrNull() ?: return
    val group = player.currentTracks.groups.getOrNull(groupIndex) ?: return
    player.trackSelectionParameters = builder
        .setTrackTypeDisabled(trackType, false)
        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
        .build()
}

private fun trackOptions(tracks: Tracks, type: Int): List<TrackOption> {
    val options = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != type) return@forEachIndexed
        for (trackIndex in 0 until group.length) {
            if (!group.isTrackSupported(trackIndex)) continue
            val format = group.getTrackFormat(trackIndex)
            options += TrackOption(
                id = "$groupIndex:$trackIndex",
                label = trackLabel(format, type, options.size),
                selected = group.isTrackSelected(trackIndex),
            )
        }
    }
    return options
}

private fun trackLabel(format: Format, type: Int, index: Int): String {
    val base = format.label
        ?: format.language
            ?.let { Locale.forLanguageTag(it).displayLanguage }
            ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
            ?.replaceFirstChar { it.uppercase() }
        ?: "Track ${index + 1}"
    if (type != C.TRACK_TYPE_AUDIO) return base
    val layout = when (format.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> null
    }
    return if (layout != null) "$base · $layout" else base
}
