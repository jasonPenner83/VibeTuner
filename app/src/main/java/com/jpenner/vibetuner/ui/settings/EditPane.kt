package com.jpenner.vibetuner.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.CatalogSource
import com.jpenner.vibetuner.ui.components.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/* The EDIT pane — read-only SOURCE card above editable field rows. The Marathon-limit row is
 * only composed while the channel is in Marathon mode. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EditPane(
    edit: ChannelEdit,
    onRename: (String) -> Unit,
    onCycleCategory: (Int) -> Unit,
    onSetMode: (Boolean) -> Unit,
    onCycleLimit: (Int) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleSubChannel: (String, String, Boolean) -> Unit,
    onSetAllSubChannels: (Boolean) -> Unit,
    onMove: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = AerialColors.Surface),
        border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("EDIT · CH ${edit.number}", style = AerialTypography.labelSmall, color = AerialColors.Accent)
            Text(edit.name, style = AerialTypography.headlineMedium.copy(fontSize = 25.sp), color = AerialColors.Txt)

            edit.source?.let { SourceCard(it) }

            var nameInput by remember(edit.sourceKey) { mutableStateOf(edit.name) }
            FieldRow("Name") {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    onDone = { if (it != edit.name && it.isNotBlank()) onRename(it) else if (it.isBlank()) nameInput = edit.name },
                )
            }
            FieldRow("Category") {
                StepperField(label = edit.categoryLabel, dotColor = edit.categoryColor,
                    onPrev = { onCycleCategory(-1) }, onNext = { onCycleCategory(+1) })
            }
            FieldRow("Mode") { ModeSegmented(marathon = edit.marathon, onSelect = onSetMode) }
            if (edit.marathon) {
                FieldRow("Marathon limit", limitSub(edit.marathonLimit)) {
                    StepperField(label = edit.marathonLimitLabel, onPrev = { onCycleLimit(-1) }, onNext = { onCycleLimit(+1) })
                }
            }
            if (edit.subChannels.isNotEmpty()) {
                val multiExtra = edit.subChannels.map { it.extraName }.distinct().size > 1
                val allSelected = edit.subChannels.all { it.selected }
                FieldRow("Sub-channels", "Toggle an option to add it to the lineup as its own channel") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubChannelRow(
                            label = if (allSelected) "Deselect all" else "Select all",
                            selected = allSelected,
                            onToggle = { onSetAllSubChannels(!allSelected) },
                        )
                        androidx.compose.foundation.lazy.LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(edit.subChannels.size, key = { i -> edit.subChannels[i].let { "${it.extraName}=${it.option}" } }) { i ->
                                val t = edit.subChannels[i]
                                SubChannelRow(
                                    label = if (multiExtra) "${t.extraName} · ${t.option}" else t.option,
                                    selected = t.selected,
                                    onToggle = { onToggleSubChannel(t.extraName, t.option, !t.selected) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().drawTopDivider(AerialColors.Line).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enabled", style = AerialTypography.titleMedium.copy(fontSize = 17.sp), color = AerialColors.Txt)
                    ToggleField(on = edit.enabled, onToggle = onToggleEnabled)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineAction("↑ Move Up", enabled = edit.canMoveUp, onClick = { onMove(true) })
                    OutlineAction("↓ Move Down", enabled = edit.canMoveDown, onClick = { onMove(false) })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubChannelRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(10.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = AerialTypography.titleMedium.copy(fontSize = 15.sp),
                color = if (selected) AerialColors.Txt else AerialColors.Txt2)
            AerialSwitch(selected)
        }
    }
}

@Composable
private fun FieldRow(label: String, sub: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AerialTypography.labelSmall.copy(fontSize = 12.sp), color = AerialColors.Txt2)
        content()
        if (sub != null) Text(sub, style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt3)
    }
}

/* Read-only provenance from the catalog's manifest.json entry. Keeps two "Popular" channels legible. */
@Composable
private fun SourceCard(s: CatalogSource) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(AerialColors.Bg)
            .border(1.dp, AerialColors.Line, RoundedCornerShape(13.dp)).padding(horizontal = 18.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text("SOURCE · manifest.json", style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt3)
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            SourceCell("ADD-ON") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AddonChip(s.addonAbbrev); Text(s.addonName, fontSize = 15.sp, color = AerialColors.Txt)
                }
            }
            SourceCell("CONTENT TYPE") { TypePill(s.type) }
            SourceCell("CATALOG") { Text("${s.catalogLabel}  · ${s.catalogId}", fontSize = 15.sp, color = AerialColors.Txt) }
            SourceCell("LIBRARY") { Text(s.libraryLabel, fontSize = 15.sp, color = AerialColors.Txt) }
        }
        HorizontalDivider(color = AerialColors.Line)
        Text(s.sourceKey, style = AerialTypography.labelSmall.copy(fontSize = 11.sp), color = AerialColors.Txt3, maxLines = 1)
    }
}

@Composable
private fun SourceCell(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = AerialTypography.labelSmall.copy(fontSize = 10.sp), color = AerialColors.Txt3)
        content()
    }
}

private fun limitSub(n: Int?) = when (n) {
    null -> "Binge each show fully, then the next"
    else -> "$n episodes per show, then rotate · resumes where it left off"
}
