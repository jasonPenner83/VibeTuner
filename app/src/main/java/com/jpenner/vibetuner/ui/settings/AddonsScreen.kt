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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.abbreviation
import com.jpenner.vibetuner.data.model.stremio.host
import com.jpenner.vibetuner.data.model.stremio.official
import com.jpenner.vibetuner.ui.components.*
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography
import com.jpenner.vibetuner.ui.theme.Dimens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddonsScreen(
    onBack: () -> Unit,
    viewModel: AddonsViewModel,
) {
    val state by viewModel.state.collectAsState()
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.refresh() }   // re-entering the screen picks up profile/unlock changes

    AerialCanvas {
    Column(
        Modifier.fillMaxSize().background(AerialColors.Bg)
            .padding(start = Dimens.SafeArea, end = Dimens.SafeArea, top = Dimens.SafeArea),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BackChip(onBack)
            Text("Add-Ons", style = AerialTypography.headlineMedium.copy(fontSize = 40.sp), color = AerialColors.Txt)
            Spacer(Modifier.weight(1f))
            Text("${state.enabledCount} enabled · ${state.catalogCount} catalogs",
                style = AerialTypography.labelSmall, color = AerialColors.Txt3)
        }
        Spacer(Modifier.height(28.dp))

        LazyColumn(
            Modifier.fillMaxWidth().widthIn(max = 880.dp).align(Alignment.CenterHorizontally).focusRestorer(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = Dimens.SafeArea),
        ) {
            items(state.addons, key = { it.id }) { addon ->
                AddonRow(
                    addon = addon,
                    allowAdult = state.allowAdult,
                    onToggle = { viewModel.setEnabled(addon.id, !addon.enabled) },
                    onRemove = { viewModel.remove(addon.id) },
                )
            }
            item { AddManifestRow(onClick = viewModel::openSheet) }
        }
    }

    state.sheet?.let { sheet ->
        AddManifestSheet(
            state = sheet,
            onUrlChange = viewModel::onUrlChange,
            onConfirm = viewModel::confirmAdd,
            onDismiss = viewModel::closeSheet,
        )
    }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddonRow(addon: StremioAddon, allowAdult: Boolean, onToggle: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth().alpha(if (addon.enabled) 1f else 0.55f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.008f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(16.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AddonLogo(addon.abbreviation, addon.manifest.logo, size = 54.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(addon.manifest.name, style = AerialTypography.titleMedium.copy(fontSize = 20.sp), color = AerialColors.Txt, maxLines = 1)
                    if (addon.official) OfficialTag()
                    if (addon.manifest.adultBlocked) BlockedTag(unlocked = allowAdult)
                }
                Text(subtitleFor(addon, allowAdult), style = AerialTypography.labelSmall.copy(fontSize = 13.sp), color = AerialColors.Txt3, maxLines = 1)
            }
            AerialSwitch(addon.enabled)
            OutlineAction("Remove", enabled = true, onClick = onRemove)
        }
    }
}

private fun subtitleFor(a: StremioAddon, allowAdult: Boolean): String {
    val catalogs = when {
        a.manifest.adultBlocked && !allowAdult -> "no channels (blocked)"
        a.enabled -> "${a.manifest.catalogs.size} catalogs"
        else -> "disabled"
    }
    return "${a.manifest.version} · $catalogs · ${a.host}"
}

/* Adult addons are hard-blocked from the lineup (spec §4) unless the active
 * profile's adult unlock is on; the tag flags the content either way. */
@Composable
private fun BlockedTag(unlocked: Boolean) {
    Text(
        if (unlocked) "ADULT" else "BLOCKED · ADULT",
        style = AerialTypography.labelSmall.copy(fontSize = 10.sp),
        color = AerialColors.Live,
        modifier = Modifier
            .border(BorderStroke(1.dp, AerialColors.Live), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddManifestRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Surface),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.008f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, AerialColors.Line), shape = RoundedCornerShape(16.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(16.dp))),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 17.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("+  Add manifest URL", style = AerialTypography.titleMedium.copy(fontSize = 17.sp), color = AerialColors.Txt2)
        }
    }
}
