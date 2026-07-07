package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/** Paints a 1dp hairline along the top edge of the element. */
fun Modifier.drawTopDivider(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}

/** A focusable back affordance ("←") used in settings sub-screen headers. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BackChip(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        modifier = modifier.size(46.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
    ) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("←", fontSize = 22.sp, color = AerialColors.Txt2) } }
}

/** `< label >` cycling stepper for D-pad input. Optional [dotColor] renders a leading category dot. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StepperField(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepArrow("‹", onPrev)
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(AerialColors.Bg)
                .border(1.dp, AerialColors.Line, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (dotColor != null) Box(Modifier.size(9.dp).clip(CircleShape).background(dotColor))
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 16.sp), color = AerialColors.Txt)
        }
        StepArrow("›", onNext)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StepArrow(glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text(glyph, fontSize = 20.sp, color = AerialColors.Txt2) } }
}

/** Two-segment Random / Marathon control. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ModeSegmented(marathon: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Segment("Random", selected = !marathon, onClick = { onSelect(false) }, modifier = Modifier.weight(1f))
        Segment("Marathon", selected = marathon, onClick = { onSelect(true) }, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AerialColors.Accent else AerialColors.Surface,
            contentColor = if (selected) AerialColors.AccentInk else AerialColors.Txt2,
            focusedContainerColor = if (selected) AerialColors.Accent else AerialColors.Raised,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text(label, style = AerialTypography.titleMedium.copy(fontSize = 15.sp)) } }
}

/** ON/OFF toggle button (label + display-only AerialSwitch); flips [on] on click. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ToggleField(on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onToggle(!on) },
        modifier = modifier.height(44.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) {
        Row(Modifier.padding(horizontal = 14.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (on) "ON" else "OFF", style = AerialTypography.labelSmall,
                color = if (on) AerialColors.Success else AerialColors.Txt3)
            AerialSwitch(on)
        }
    }
}

/** An outlined action button that can be disabled (dims + ignores clicks). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OutlineAction(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier.height(46.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface,
            contentColor = if (enabled) AerialColors.Txt else AerialColors.Txt,
            focusedContainerColor = AerialColors.Raised),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(10.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) {
        Box(Modifier.padding(horizontal = 18.dp).fillMaxHeight(), Alignment.Center) {
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 15.sp))
        }
    }
}

/** Small "OFFICIAL" pill for first-party add-ons. */
@Composable
fun OfficialTag(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).background(AerialColors.Accent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text("OFFICIAL", style = AerialTypography.labelSmall.copy(fontSize = 10.sp), color = AerialColors.Accent2) }
}

/** Content-type pill ("Movie" / "Series"). */
@Composable
fun TypePill(type: String, modifier: Modifier = Modifier) {
    val label = if (type == "series") "Series" else "Movie"
    Box(
        modifier.clip(RoundedCornerShape(6.dp)).background(AerialColors.Raised)
            .border(1.dp, AerialColors.Line, RoundedCornerShape(6.dp)).padding(horizontal = 9.dp, vertical = 3.dp),
    ) { Text(label, style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt2) }
}

/** Square initials chip used inside the SOURCE card. */
@Composable
fun AddonChip(abbrev: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(AerialColors.Raised),
        Alignment.Center,
    ) { Text(abbrev, style = AerialTypography.labelSmall.copy(fontSize = 12.sp), color = AerialColors.Txt2) }
}

/** Add-on logo tile: Coil image when a URL is present, else the abbreviation on a raised tile. */
@Composable
fun AddonLogo(abbrev: String, logoUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(14.dp)).background(AerialColors.Raised),
        Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
        } else {
            Text(abbrev, style = AerialTypography.headlineMedium.copy(fontSize = 20.sp), color = AerialColors.Txt2)
        }
    }
}
