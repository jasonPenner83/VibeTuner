package com.jpenner.vibetuner.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.tv.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.ui.components.drawTopDivider
import com.jpenner.vibetuner.data.model.SettingControl
import com.jpenner.vibetuner.data.model.SettingItem
import com.jpenner.vibetuner.ui.components.SettingsRow
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAddons: () -> Unit,
    onOpenChannels: () -> Unit,
    onSignInGoogle: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val paneFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.openAddons.collect { onOpenAddons() }
    }
    LaunchedEffect(Unit) {
        viewModel.openChannels.collect { onOpenChannels() }
    }
    LaunchedEffect(Unit) {
        viewModel.signInGoogle.collect { onSignInGoogle() }
    }

    AerialCanvas {
    Column(
        Modifier
            .fillMaxSize()
            .background(AerialColors.Bg)
            .padding(start = Dimens.SafeArea, end = Dimens.SafeArea, top = Dimens.SafeArea),
    ) {
        // ── header ──
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(46.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(12.dp))),
            ) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("\u2190", fontSize = 22.sp, color = AerialColors.Txt2) } }
            Text("Settings", style = AerialTypography.headlineMedium.copy(fontSize = 40.sp), color = AerialColors.Txt)
        }

        Spacer(Modifier.height(34.dp))

        // ── two panes ──
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            SectionRail(
                sections = state.sections,
                selectedId = state.selectedId,
                onSelect = viewModel::select,
                // Right from the rail jumps focus into the pane
                onExitRight = { paneFocus.requestFocus() },
                modifier = Modifier.width(440.dp).fillMaxHeight(),
            )
            SettingsPane(
                title = state.selected?.title.orEmpty(),
                rows = state.rows,
                onActivate = viewModel::activate,
                onAdjust = viewModel::adjust,
                modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(paneFocus),
            )
        }
    }
    }
}

/* ───────────────────────── SectionRail ─────────────────────────
 * Focusable list of sections. Focus follows D-pad; OK selects.
 * focusRestorer() keeps the last item focused when returning. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionRail(
    sections: List<SettingsSection>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onExitRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.focusRestorer(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(sections, key = { it.id }) { section ->
            SectionRailItem(
                section = section,
                active = section.id == selectedId,
                onClick = { onSelect(section.id) },
                modifier = Modifier.onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { onExitRight(); true } else false
                },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionRailItem(
    section: SettingsSection,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(
            // selected rests on raised even when unfocused; focus deepens it
            containerColor = if (active) AerialColors.Raised else AerialColors.Bg,
            focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt,
            focusedContentColor = AerialColors.Txt
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(13.dp))),
    ) {
        Box {
            // 4dp accent marker on the active section
            if (active) Box(
                Modifier.align(Alignment.CenterStart).padding(vertical = 14.dp)
                    .width(4.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(AerialColors.Accent),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(section.title, style = AerialTypography.titleMedium.copy(fontSize = 21.sp))
                Text(section.summary, style = AerialTypography.labelSmall, color = AerialColors.Txt3)
            }
        }
    }
}

/* ───────────────────────── SettingsPane ────────────────────────
 * The surface card of rows for the selected section. Each row is the
 * shared SettingsRow; slider rows map D-pad left/right to onAdjust. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsPane(
    title: String,
    rows: List<SettingItem>,
    onActivate: (String) -> Unit,
    onAdjust: (String, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = AerialColors.Surface),
        border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(20.dp)),
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 40.dp).focusRestorer(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Text(
                    title.uppercase(),
                    style = AerialTypography.labelSmall.copy(fontSize = 15.sp),
                    color = AerialColors.Txt,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
            }
            items(rows, key = { it.key }) { row ->
                val rowMod = if (row.control is SettingControl.Slider) {
                    Modifier.onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (e.key) {
                            Key.DirectionLeft  -> { onAdjust(row.key, -0.05f); true }
                            Key.DirectionRight -> { onAdjust(row.key, +0.05f); true }
                            else -> false
                        }
                    }
                } else Modifier
                SettingsRow(
                    label = row.label,
                    sub = row.sub,
                    control = row.control,
                    onClick = { onActivate(row.key) },
                    modifier = rowMod
                        // hairline divider above each row
                        .drawTopDivider(AerialColors.Line),
                )
            }
        }
    }
}

