package com.jpenner.vibetuner.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * Channel logo chip. Loads [Channel.logoUrl] via Coil when present,
 * otherwise falls back to the 2-letter abbreviation on a surface chip.
 * Used by ChannelRowHeader (62dp) and ChannelSwitcherCard (54dp).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelLogo(channel: Channel, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(if (size >= 60.dp) 12.dp else 11.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(AerialColors.Surface)
            .borderChip(shape),                  // 1dp Line border (see ext below)
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logoUrl != null) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                channel.abbreviation,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.24f).sp,
                color = AerialColors.Txt2,
            )
        }
    }
}

/** Small "LIVE" pill used on content thumbnails. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(AerialColors.Live)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** App wordmark — gradient "A" mark + "Aerial". Reused in the top bar,
 *  sign-in screen and the guide/home headers. */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(AerialColors.Accent, AerialColors.Accent2))),
            contentAlignment = Alignment.Center,
        ) {
            Text("VT", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = AerialColors.Txt)
        }
        Text("VibeTunner", style = AerialTypography.titleMedium.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold, color = AerialColors.Txt)
    }
}

/** Round 42dp icon-chip button (top-bar search, and anywhere a single
 *  focusable icon action is needed). [icon] selects the glyph. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IconChipButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt2, focusedContentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = CircleShape),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = CircleShape)),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 12.dp)),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** Convenience wrapper used by the top bar. */
@Composable
fun SearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) =
    IconChipButton(Icons.Default.Search, onClick, modifier)

/** Settings toggle. Display-only — the parent SettingsRow owns D-pad focus. */
@Composable
fun AerialSwitch(on: Boolean, modifier: Modifier = Modifier) {
    val knobX by animateDpAsState(if (on) 25.dp else 3.dp, label = "knob")
    Box(
        modifier
            .size(width = 52.dp, height = 28.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) AerialColors.Accent else AerialColors.Line),
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .offset(x = knobX)
                .size(22.dp)
                .background(Color.White, CircleShape),
        )
    }
}

/** Settings slider track (fraction 0f..1f). Display-only; SettingsRow
 *  handles D-pad left/right to change the value. */
@Composable
fun AerialSlider(fraction: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
        val knobX = (maxWidth - 20.dp) * fraction.coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AerialColors.Line))
        Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp)).background(AerialColors.Accent))
        Box(Modifier.offset(x = knobX).size(20.dp).background(Color.White, CircleShape))   // knob
    }
}

// ---- player transport controls (shared by PlaybackControls) ----

/** The seek bar: track + filled progress + playhead, with the live-edge
 *  marker pinned right. The whole bar is one focusable Surface; the screen
 *  maps D-pad left/right to onSeek while it holds focus. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScrubBar(progress: Float, atLiveEdge: Boolean, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onSeek(progress) },
        modifier = modifier.height(20.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        BoxWithConstraints(contentAlignment = Alignment.CenterStart) {
            val knobX = (maxWidth - 20.dp) * progress.coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                .background(AerialColors.Txt.copy(alpha = 0.16f)))
            Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(RoundedCornerShape(4.dp))
                .background(AerialColors.Accent))
            Box(Modifier.align(Alignment.CenterEnd).size(11.dp).background(AerialColors.Live, CircleShape))
            Box(Modifier.offset(x = knobX).size(20.dp).background(Color.White, CircleShape))   // playhead
        }
    }
}

/** 60dp square transport button (restart, +30s, channel up/down). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ControlButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface.copy(alpha = 0.7f),
            focusedContainerColor = AerialColors.Raised, contentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(14.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 12.dp)),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

/** 56dp pill action (Subtitles / Audio / Guide). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PillButton(label: String, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface.copy(alpha = 0.7f),
            focusedContainerColor = AerialColors.Raised, contentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(13.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(13.dp))),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 22.dp), Alignment.Center) {
            Text(label, style = AerialTypography.titleMedium, fontSize = 18.sp)
        }
    }
}

// 1dp hairline border helper kept local so chips stay one-liners.
private fun Modifier.borderChip(shape: RoundedCornerShape) =
    this.then(Modifier.border(1.dp, AerialColors.Line, shape))