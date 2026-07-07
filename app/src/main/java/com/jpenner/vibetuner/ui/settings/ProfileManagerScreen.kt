package com.jpenner.vibetuner.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.ProfileType
import com.jpenner.vibetuner.ui.components.AerialConfirmDialog
import com.jpenner.vibetuner.ui.components.BackChip
import com.jpenner.vibetuner.ui.components.PinEntryDialog
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

/**
 * Two-pane profile manager, mirroring ChannelManagerScreen's rail/pane focus
 * model: a focusable PROFILES list beside the EDIT pane. A PinPrompt in state
 * lays the PinEntryDialog over the whole screen (unlock or set/change).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileManagerScreen(
    onBack: () -> Unit,
    viewModel: ProfileManagerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val editFocus = remember { FocusRequester() }
    BackHandler { onBack() }

    AerialCanvas {
    Box(Modifier.fillMaxSize().background(AerialColors.Bg)) {
        Column(
            Modifier.fillMaxSize()
                .padding(start = Dimens.SafeArea, end = Dimens.SafeArea, top = Dimens.SafeArea),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BackChip(onBack)
                Text("Manage Profiles", style = AerialTypography.headlineMedium.copy(fontSize = 40.sp), color = AerialColors.Txt)
                Spacer(Modifier.weight(1f))
                Text("${state.rows.size} profiles · ${state.restrictedCount} restricted",
                    style = AerialTypography.labelSmall, color = AerialColors.Txt3)
            }
            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                ProfileList(
                    rows = state.rows,
                    selectedId = state.selectedId,
                    onSelect = viewModel::select,
                    onNew = viewModel::createProfile,
                    onExitRight = { editFocus.requestFocus() },
                    modifier = Modifier.width(400.dp).fillMaxHeight(),
                )
                state.editing?.let { edit ->
                    ProfileEditPane(
                        edit = edit,
                        onRename = { viewModel.rename(edit.id, it) },
                        onSetType = { viewModel.applyType(edit.id, it) },
                        onStepRating = { step -> viewModel.stepRating(edit.id, edit.maxRating, step) },
                        onToggleType = { viewModel.toggleType(edit.id, it) },
                        onAdultContent = { viewModel.setAdultContent(edit.id, it) },
                        onRequirePin = { viewModel.setRequirePin(edit.id, it) },
                        onChangePin = { viewModel.requestSetPin(edit.id) },
                        onDelete = { if (state.rows.size > 1) viewModel.requestDelete() },
                        modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(editFocus),
                    )
                }
            }
        }

        // PIN overlay — unlock or set/change, copy driven by the prompt mode.
        state.prompt?.let { prompt ->
            val (title, sub) = when (prompt) {
                is PinPrompt.Unlock -> "Enter PIN" to "“${prompt.name}” is protected · 4-digit PIN"
                is PinPrompt.SetPin ->
                    if (prompt.confirming) "Confirm PIN" to "Re-enter to confirm"
                    else "Set a PIN" to "Choose a 4-digit PIN for “${prompt.name}”"
            }
            val row = state.rows.firstOrNull { it.id == prompt.profileId } ?: return@let
            PinEntryDialog(
                title = if (state.pinError) "Try again" else title,
                subtitle = if (state.pinError) "That PIN didn’t match" else sub,
                monogram = row.initial,
                gradient = row.gradient,
                error = state.pinError,
                onComplete = viewModel::submitPin,
                onDismiss = viewModel::dismissPrompt,
            )
        }

        if (state.confirmingDelete) state.editing?.let { edit ->
            AerialConfirmDialog(
                title = "Delete “${edit.name}”?",
                message = "This removes the profile and its restrictions. Watch history and add-ons for other profiles are unaffected.",
                confirmLabel = "Delete profile",
                onConfirm = { viewModel.confirmDelete(edit.id) },
                onDismiss = viewModel::dismissDelete,
            )
        }
    }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileList(
    rows: List<ProfileRow>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onExitRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("PROFILES", style = AerialTypography.labelSmall, color = AerialColors.Txt3,
            modifier = Modifier.padding(start = 4.dp, bottom = 11.dp))
        LazyColumn(Modifier.focusRestorer(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.id }) { row ->
                ProfileListItem(
                    row = row,
                    selected = row.id == selectedId,
                    onClick = { onSelect(row.id) },
                    modifier = Modifier.onKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { onExitRight(); true } else false
                    },
                )
            }
            item { NewProfileItem(onNew) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileListItem(row: ProfileRow, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AerialColors.Raised else AerialColors.Bg,
            focusedContainerColor = AerialColors.Raised, contentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(row.gradient)),
                Alignment.Center,
            ) { Text(row.initial, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AerialColors.Txt) }
            Column(Modifier.weight(1f)) {
                Text(row.name, style = AerialTypography.titleMedium.copy(fontSize = 17.sp), color = AerialColors.Txt, maxLines = 1)
                Text(row.summary, style = AerialTypography.labelSmall.copy(fontSize = 12.sp),
                    color = row.summaryColor(), maxLines = 1)
            }
            if (row.hasPin) Text("🔒", fontSize = 14.sp)
        }
    }
}

/** Restriction summaries take the profile-type tint (kid amber, teen blue). */
private fun ProfileRow.summaryColor() = when {
    !restricted -> AerialColors.Txt3
    type == ProfileType.KID -> AerialColors.Warn
    else -> AerialColors.Accent2
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NewProfileItem(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Surface,
            contentColor = AerialColors.Txt2),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(12.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                    .background(AerialColors.Bg)
                    .border(1.dp, AerialColors.Line, RoundedCornerShape(11.dp)),
                Alignment.Center,
            ) { Text("+", fontSize = 24.sp, color = AerialColors.Txt3) }
            Text("New Profile", style = AerialTypography.titleMedium.copy(fontSize = 16.sp))
        }
    }
}
