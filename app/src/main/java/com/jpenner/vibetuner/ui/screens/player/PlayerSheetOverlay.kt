package com.jpenner.vibetuner.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * Right-docked player side panel: audio/subtitle track pickers and the
 * program info panel, one at a time (see [PlayerSheet]). D-pad focus is
 * requested onto the selected row (or the info body) via [focusRequester];
 * Back is handled by PlayerScreen's onPreviewKeyEvent, not here.
 */
@Composable
fun PlayerSheetOverlay(
    sheet: PlayerSheet,
    channel: Channel?,
    program: Program?,
    audioOptions: List<TrackOption>,
    subtitleOptions: List<TrackOption>,
    onSelect: (TrackOption) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxHeight()) {
        // Soft scrim easing the panel onto the video.
        Box(
            Modifier.width(80.dp).fillMaxHeight().background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, AerialColors.Bg.copy(alpha = 0.92f)),
                ),
            ),
        )
        Column(
            Modifier
                .width(400.dp)
                .fillMaxHeight()
                .background(AerialColors.Bg.copy(alpha = 0.92f))
                .padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (sheet) {
                PlayerSheet.Audio -> TrackList("AUDIO", audioOptions, onSelect, focusRequester)
                PlayerSheet.Subtitles -> TrackList("SUBTITLES", subtitleOptions, onSelect, focusRequester)
                PlayerSheet.Info -> InfoPanel(channel, program, focusRequester)
                PlayerSheet.Schedule -> Unit // full-screen overlay, hosted by PlayerScreen
            }
        }
    }
}

@Composable
private fun TrackList(
    title: String,
    options: List<TrackOption>,
    onSelect: (TrackOption) -> Unit,
    focusRequester: FocusRequester,
) {
    Text(title, color = AerialColors.Txt2, fontSize = 12.sp, fontWeight = FontWeight.Black)
    if (title == "SUBTITLES" && options.size <= 1) {
        Text("No subtitles available", color = AerialColors.Txt3, fontSize = 14.sp)
    }
    if (title == "AUDIO" && options.isEmpty()) {
        Text("No audio tracks available", color = AerialColors.Txt3, fontSize = 14.sp)
    }
    val focusIndex = options.indexOfFirst { it.selected }.coerceAtLeast(0)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(options, key = { _, opt -> opt.id }) { index, option ->
            OptionRow(
                option = option,
                onClick = { onSelect(option) },
                modifier = if (index == focusIndex) Modifier.focusRequester(focusRequester) else Modifier,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OptionRow(option: TrackOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface.copy(alpha = 0.7f),
            focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt2,
            focusedContentColor = AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(option.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (option.selected) {
                Icon(
                    Icons.Default.Check, contentDescription = "Selected",
                    tint = AerialColors.Accent, modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoPanel(channel: Channel?, program: Program?, focusRequester: FocusRequester) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .focusRequester(focusRequester)
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("INFO", color = AerialColors.Txt2, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(
            program?.title ?: "No programme information",
            style = AerialTypography.titleMedium, fontSize = 26.sp, color = AerialColors.Txt,
        )
        program?.episodeTitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = AerialColors.Txt2, fontSize = 17.sp)
        }
        channel?.let {
            Text("CH ${it.number} · ${it.name}", color = AerialColors.Txt2, fontSize = 14.sp)
        }
        program?.let { p ->
            val meta = listOf(p.displayTimeSlot, p.rating, p.mediaType)
                .filter { it.isNotBlank() }
                .joinToString("  ·  ")
            if (meta.isNotBlank()) Text(meta, color = AerialColors.Txt3, fontSize = 13.sp)
            if (p.description.isNotBlank()) {
                Text(p.description, color = AerialColors.Txt2, fontSize = 15.sp, lineHeight = 22.sp)
            }
            if (p.cast.isNotEmpty()) {
                Text("Cast: " + p.cast.joinToString(", "), color = AerialColors.Txt3, fontSize = 13.sp)
            }
        }
    }
}
