package com.jpenner.vibetuner.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.components.BackChip
import com.jpenner.vibetuner.ui.components.EmptyLineupScreen
import com.jpenner.vibetuner.ui.components.OutlineAction
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelManagerScreen(
    onBack: () -> Unit,
    onOpenAddons: () -> Unit,
    viewModel: ChannelManagerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val editFocus = remember { FocusRequester() }
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.reload() }   // re-entering picks up addon/seeding changes

    AerialCanvas {
    Column(
        Modifier.fillMaxSize().background(AerialColors.Bg)
            .padding(start = Dimens.SafeArea, end = Dimens.SafeArea, top = Dimens.SafeArea),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BackChip(onBack)
            Text("Channel Manager", style = AerialTypography.headlineMedium.copy(fontSize = 40.sp), color = AerialColors.Txt)
            Spacer(Modifier.weight(1f))
            if (!state.isEmpty) {
                val rebuild = state.rebuild
                OutlineAction(
                    label = when (rebuild) {
                        is RebuildState.Running -> "Rebuilding… ${rebuild.done}/${rebuild.total}"
                        is RebuildState.Done -> "Rebuilt · ${rebuild.channels} channels"
                        RebuildState.Idle -> "Rebuild Guide"
                    },
                    enabled = rebuild !is RebuildState.Running,
                    onClick = viewModel::rebuildGuide,
                )
            }
            if (!state.isEmpty) Text("${state.rows.size} channels",
                style = AerialTypography.labelSmall, color = AerialColors.Txt3)
        }
        Spacer(Modifier.height(28.dp))

        if (state.isEmpty) {
            EmptyLineupScreen(onOpenAddons = onOpenAddons, modifier = Modifier.fillMaxSize())
            return@Column
        }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            LineupList(
                rows = state.rows,
                selectedKey = state.selectedKey,
                onSelect = viewModel::select,
                onExitRight = { editFocus.requestFocus() },
                modifier = Modifier.width(430.dp).fillMaxHeight(),
            )
            state.editing?.let { edit ->
                EditPane(
                    edit = edit,
                    onRename = { viewModel.rename(edit.sourceKey, it) },
                    onCycleCategory = { step -> viewModel.cycleCategory(edit.sourceKey, edit.categoryLabel, step) },
                    onSetMode = { viewModel.setMode(edit.sourceKey, it) },
                    onCycleLimit = { step -> viewModel.cycleMarathonLimit(edit.sourceKey, edit.marathonLimit, step) },
                    onToggleEnabled = { viewModel.toggleEnabled(edit.sourceKey, it) },
                    onToggleSubChannel = { extra, option, sel -> viewModel.toggleSubChannel(edit.sourceKey, extra, option, sel) },
                    onSetAllSubChannels = { sel -> viewModel.setAllSubChannels(edit.sourceKey, sel) },
                    onMove = { up -> viewModel.move(edit.sourceKey, up) },
                    modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(editFocus),
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LineupList(
    rows: List<LineupRow>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onExitRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("LINEUP", style = AerialTypography.labelSmall, color = AerialColors.Txt3,
            modifier = Modifier.padding(start = 4.dp, bottom = 11.dp))
        LazyColumn(Modifier.focusRestorer(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.sourceKey }) { row ->
                LineupItem(
                    row = row,
                    selected = row.sourceKey == selectedKey,
                    onClick = { onSelect(row.sourceKey) },
                    modifier = Modifier.onKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { onExitRight(); true } else false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LineupItem(row: LineupRow, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().alpha(if (row.enabled) 1f else 0.55f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AerialColors.Raised else AerialColors.Bg,
            focusedContainerColor = AerialColors.Raised, contentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(row.number, Modifier.width(34.dp), style = AerialTypography.labelSmall.copy(fontSize = 15.sp), color = AerialColors.Txt3)
            Box(Modifier.size(9.dp).clip(CircleShape).background(row.categoryColor))
            Column(Modifier.weight(1f)) {
                Text(row.name, style = AerialTypography.titleMedium.copy(fontSize = 17.sp), color = AerialColors.Txt, maxLines = 1)
                Text(row.sourceSub, style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt3, maxLines = 1)
            }
            Text(if (row.enabled) "ON" else "OFF", style = AerialTypography.labelSmall.copy(fontSize = 12.sp),
                color = if (row.enabled) AerialColors.Success else AerialColors.Txt3)
        }
    }
}
