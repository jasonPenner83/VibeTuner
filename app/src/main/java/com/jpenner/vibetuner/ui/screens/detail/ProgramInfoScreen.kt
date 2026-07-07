package com.jpenner.vibetuner.ui.screens.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jpenner.vibetuner.ui.components.AerialButton
import com.jpenner.vibetuner.ui.components.IconChipButton
import com.jpenner.vibetuner.ui.components.LiveBadge
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.Dimens
import com.jpenner.vibetuner.ui.screens.guide.formatTime
import com.jpenner.vibetuner.ui.screens.guide.timeRange
import com.jpenner.vibetuner.ui.theme.AerialTypography

@Composable
fun ProgramInfoScreen(
    programId: String,
    onBack: () -> Unit,
    onWatch: (channelId: String) -> Unit,
    viewModel: ProgramInfoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val watchFocus = remember { FocusRequester() }
    LaunchedEffect(programId) { viewModel.load(programId) }
    // Watch Live takes focus once the program resolves.
    LaunchedEffect(state.program) { if (state.program != null) watchFocus.requestFocus() }

    val program = state.program
    val channel = state.channel

    AerialCanvas {
        val infinite = rememberInfiniteTransition(label = "tune")
        val breathe by infinite.animateFloat(
            0.45f, 0.9f,
            infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "breathe",
        )
        val drift by infinite.animateFloat(
            1.02f, 1.1f,
            infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "drift",
        )
        val sweep by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "sweep",
        )
        val pulse by infinite.animateFloat(
            0.5f, 1f,
            infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "pulse",
        )

    Box(
        Modifier.fillMaxSize().background(AerialColors.Bg).verticalScroll(rememberScrollState()),
    ) {
        // Key-art backdrop behind everything (660dp tall), with scrims.
        // ── LAYER 1: fanart backdrop (drifts slowly), quieted by scrims ──
        Box(Modifier.fillMaxSize().background(AerialColors.Surface))
        val art = program?.backdropUrl ?: program?.posterUrl
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(drift),
            )
        }

        // Back affordance, top-left.
        BackChip(onBack, Modifier.padding(start = Dimens.SafeArea, top = 40.dp))

        Column(Modifier.padding(start = Dimens.SafeArea, top = 250.dp, end = Dimens.SafeArea)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (program?.isLive == true) LiveBadge()
                Text("CH " + (channel?.number ?: "") + "  \u00B7  " + (channel?.name ?: ""),
                    style = AerialTypography.labelSmall, color = AerialColors.Txt2)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                program?.title.orEmpty(),
                style = AerialTypography.displayLarge.copy(fontSize = 78.sp),
                modifier = Modifier.widthIn(max = 1100.dp),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = AerialColors.Txt
            )
            Spacer(Modifier.height(18.dp))

            // meta row: rating · time · duration · category · ★ rating
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                program?.let { RatingChip(it.rating) }
                Text(program?.let { timeRange(it) }.orEmpty(), style = AerialTypography.labelSmall, color = AerialColors.Accent)
                Text((program?.durationMinutes ?: 0).toString() + " min", color = AerialColors.Txt2, fontSize = 19.sp)
                Text(channel?.category?.label.orEmpty(), color = AerialColors.Txt2, fontSize = 19.sp)
                Text("\u2605 4.6", color = AerialColors.Warn, fontSize = 19.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                program?.description.orEmpty(),
                style = AerialTypography.bodyLarge.copy(fontSize = 23.sp),
                color = AerialColors.Txt2, modifier = Modifier.widthIn(max = 980.dp),
            )
            Spacer(Modifier.height(30.dp))

            // actions — Watch Live is pre-focused
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AerialButton("Watch Live", { channel?.let { onWatch(it.id) } },
                    Modifier.focusRequester(watchFocus), icon = Icons.Default.PlayArrow, filled = true)
                IconChipButton(if (state.inMyList) Icons.Default.Check else Icons.Default.Add, viewModel::toggleMyList)
                IconChipButton(Icons.Default.Share, onClick = { /* share */ })
            }
            Spacer(Modifier.height(48.dp))

            // metadata strip
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                MetaColumn("CAST", state.cast)
                MetaColumn("UP NEXT", listOfNotNull(
                    state.upNext?.title, state.upNext?.let { formatTime(it.startMinutes) }))
                MetaColumn("AUDIO", state.audioLines)
            }
            Spacer(Modifier.height(56.dp))
        }
    }
    }
}

/* ───────── detail-local helpers ───────── */

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackChip(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        modifier = modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface.copy(alpha = 0.6f),
            focusedContainerColor = AerialColors.Raised, contentColor = AerialColors.Txt2),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(11.dp))),
    ) {
        Row(Modifier.padding(horizontal = 16.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("\u2190", fontSize = 17.sp)
            Text("Back to Guide", style = AerialTypography.titleMedium.copy(fontSize = 16.sp))
        }
    }
}

@Composable
private fun RatingChip(label: String) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AerialColors.Surface)
        .padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(label, style = AerialTypography.labelSmall, color = AerialColors.Txt2)
    }
}

/** A mono uppercase label over its value lines. */
@Composable
private fun MetaColumn(label: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AerialTypography.labelSmall.copy(fontSize = 13.sp,
            letterSpacing = 1.4.sp), color = AerialColors.Txt3)
        lines.forEach { Text(it, color = AerialColors.Txt, fontSize = 18.sp) }
    }
}