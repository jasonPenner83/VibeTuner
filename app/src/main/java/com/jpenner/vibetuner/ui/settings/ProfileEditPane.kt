package com.jpenner.vibetuner.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.ProfileType
import com.jpenner.vibetuner.data.model.Rating
import com.jpenner.vibetuner.ui.components.OutlineAction
import com.jpenner.vibetuner.ui.components.StepperField
import com.jpenner.vibetuner.ui.components.TextField
import com.jpenner.vibetuner.ui.components.ToggleField
import com.jpenner.vibetuner.ui.components.drawTopDivider
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * The EDIT pane. A read-only RESTRICTIONS summary card sits above the editable
 * rows: Profile type (segmented preset), Maximum rating (‹ value › stepper),
 * Allowed content types (toggle chips), Adult content (Adult profiles only),
 * and the PIN row (Require PIN switch + Set/Change PIN). Name uses the TV IME. Delete is a danger action.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileEditPane(
    edit: ProfileEdit,
    onRename: (String) -> Unit,
    onSetType: (ProfileType) -> Unit,
    onStepRating: (Int) -> Unit,
    onToggleType: (String) -> Unit,
    onAdultContent: (Boolean) -> Unit,
    onRequirePin: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = AerialColors.Surface),
        border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
            // header: avatar + name + type badge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(edit.gradient)),
                    Alignment.Center,
                ) { Text(edit.initial, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = AerialColors.Txt) }
                Column {
                    Text(edit.name, style = AerialTypography.headlineMedium.copy(fontSize = 24.sp), color = AerialColors.Txt)
                    Text("EDIT · profile id ${edit.id}", style = AerialTypography.labelSmall, color = AerialColors.Txt3)
                }
                Spacer(Modifier.weight(1f))
                TypeBadge(edit.type)
            }
            Spacer(Modifier.height(14.dp))

            RestrictionsCard(edit)
            Spacer(Modifier.height(10.dp))

            var nameInput by remember(edit.id) { mutableStateOf(edit.name) }
            PaneRow("Name") {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.width(250.dp),
                    onDone = { if (it != edit.name && it.isNotBlank()) onRename(it) else if (it.isBlank()) nameInput = edit.name },
                )
            }
            PaneRow("Profile type") { TypeSegmented(selected = edit.type, onSelect = onSetType) }
            PaneRow("Maximum rating") {
                StepperField(
                    label = edit.maxRating.label,
                    onPrev = { onStepRating(-1) }, onNext = { onStepRating(+1) },
                    modifier = Modifier.width(240.dp),
                )
            }
            Column(Modifier.fillMaxWidth().drawTopDivider(AerialColors.Line).padding(vertical = 9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allowed content types", style = AerialTypography.titleMedium.copy(fontSize = 16.sp), color = AerialColors.Txt)
                    Text(
                        if (edit.allowedTypes.isEmpty()) "All on" else "${edit.allowedCount} of ${edit.totalTypes} on",
                        style = AerialTypography.labelSmall,
                        color = if (edit.allowedTypes.isEmpty()) AerialColors.Success else AerialColors.Txt3,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    edit.availableTypes.forEach { type ->
                        TypeChip(
                            label = typeLabel(type),
                            // empty allow-list means every type is on
                            selected = edit.allowedTypes.isEmpty() || type in edit.allowedTypes,
                            onClick = { onToggleType(type) },
                        )
                    }
                }
            }

            // Adult-profile-only opt-in; Teen/Kid never see the row (spec: remember-but-suppress).
            if (edit.type == ProfileType.ADULT) {
                PaneRow("Adult content") { ToggleField(on = edit.adultContent, onToggle = onAdultContent) }
            }

            Spacer(Modifier.weight(1f))
            // PIN + delete
            Row(Modifier.fillMaxWidth().drawTopDivider(AerialColors.Line).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Require PIN", style = AerialTypography.titleMedium.copy(fontSize = 16.sp), color = AerialColors.Txt)
                    ToggleField(on = edit.requirePin, onToggle = onRequirePin)
                    OutlineAction(if (edit.hasPin) "Change PIN" else "Set PIN", enabled = true, onClick = onChangePin)
                }
                DangerAction("Delete profile", onClick = onDelete)
            }
        }
    }
}

/** Label-left / control-right editable row with a top hairline. */
@Composable
private fun PaneRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().drawTopDivider(AerialColors.Line).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AerialTypography.titleMedium.copy(fontSize = 16.sp), color = AerialColors.Txt)
        control()
    }
}

/* Read-only lock summary — mirrors the SOURCE card language from the Channel
 * Manager: max rating badge, allowed-type count, PIN state. */
@Composable
private fun RestrictionsCard(edit: ProfileEdit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(AerialColors.Bg)
            .border(1.dp, AerialColors.Line, RoundedCornerShape(13.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text("RESTRICTIONS", style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt3)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            SummaryCell("MAX RATING") { RatingBadge(edit.maxRating) }
            SummaryCell("ALLOWED TYPES") {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${edit.allowedCount}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AerialColors.Txt)
                    Text("of ${edit.totalTypes}", fontSize = 13.sp, color = AerialColors.Txt3)
                }
            }
            SummaryCell("PIN") {
                Text(
                    when {
                        edit.requirePin -> "🔒 Required"
                        edit.hasPin -> "Set"
                        else -> "Off"
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = if (edit.requirePin) AerialColors.Success else AerialColors.Txt3,
                )
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AerialTypography.labelSmall.copy(fontSize = 10.sp), color = AerialColors.Txt3)
        content()
    }
}

@Composable
private fun RatingBadge(rating: Rating) {
    val locked = rating != Rating.TVMA
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (locked) AerialColors.Warn.copy(alpha = 0.14f) else AerialColors.Raised)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(rating.label, style = AerialTypography.labelSmall.copy(fontSize = 12.sp),
            color = if (locked) AerialColors.Warn else AerialColors.Txt2)
    }
}

@Composable
private fun TypeBadge(type: ProfileType) {
    val (fg, bg) = when (type) {
        ProfileType.KID -> AerialColors.Warn to AerialColors.Warn.copy(alpha = 0.14f)
        ProfileType.TEEN -> AerialColors.Accent2 to AerialColors.Accent.copy(alpha = 0.14f)
        ProfileType.ADULT -> AerialColors.Txt2 to AerialColors.Bg
    }
    Box(
        Modifier.clip(RoundedCornerShape(7.dp)).background(bg)
            .border(1.dp, if (type == ProfileType.ADULT) AerialColors.Line else bg, RoundedCornerShape(7.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) { Text(type.name, style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = fg) }
}

/** Three-segment Adult / Teen / Kid preset control. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TypeSegmented(selected: ProfileType, onSelect: (ProfileType) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(AerialColors.Bg)
            .border(1.dp, AerialColors.Line, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProfileType.entries.forEach { type ->
            val on = type == selected
            Surface(
                onClick = { onSelect(type) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (on) AerialColors.Accent else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (on) AerialColors.AccentInk else AerialColors.Txt2,
                    focusedContainerColor = if (on) AerialColors.Accent else AerialColors.Raised,
                    focusedContentColor = if (on) AerialColors.AccentInk else AerialColors.Txt,
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(9.dp))),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 7.dp), Alignment.Center) {
                    Text(type.label, style = AerialTypography.titleMedium.copy(fontSize = 14.sp))
                }
            }
        }
    }
}

/** Toggle chip for one manifest content type. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AerialColors.Accent else AerialColors.Bg,
            contentColor = if (selected) AerialColors.AccentInk else AerialColors.Txt3,
            focusedContainerColor = if (selected) AerialColors.Accent else AerialColors.Raised,
            focusedContentColor = if (selected) AerialColors.AccentInk else AerialColors.Txt,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) AerialColors.Accent else AerialColors.Line),
                shape = RoundedCornerShape(8.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(8.dp))),
    ) {
        Box(Modifier.padding(horizontal = 11.dp, vertical = 5.dp), Alignment.Center) {
            Text(label, style = AerialTypography.labelSmall.copy(fontSize = 12.sp), fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Outlined danger action (Live-tinted) for destructive operations. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DangerAction(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(46.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Live.copy(alpha = 0.08f),
            contentColor = AerialColors.Live,
            focusedContainerColor = AerialColors.Live.copy(alpha = 0.2f),
            focusedContentColor = AerialColors.Live,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Live.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Live), shape = RoundedCornerShape(10.dp))),
    ) {
        Box(Modifier.padding(horizontal = 18.dp).fillMaxHeight(), Alignment.Center) {
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 15.sp))
        }
    }
}

/** "movie" -> "Movie", "tv" -> "TV". */
private fun typeLabel(type: String) = when (type.lowercase()) {
    "tv" -> "TV"
    else -> type.replaceFirstChar { it.uppercase() }
}
