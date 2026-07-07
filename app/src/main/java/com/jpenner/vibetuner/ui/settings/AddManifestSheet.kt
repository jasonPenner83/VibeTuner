package com.jpenner.vibetuner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.data.api.ManifestResult
import com.jpenner.vibetuner.ui.components.AerialButton
import com.jpenner.vibetuner.ui.components.TextField
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * The paste-a-manifest sheet. The URL box is auto-focused; OK/Enter opens the TV IME; live
 * validation (debounced in the ViewModel) renders under it. "Add add-on" is enabled only on Ok.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddManifestSheet(
    state: AddSheetState,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {   // scrim
        Surface(
            modifier = Modifier.width(760.dp),
            shape = RoundedCornerShape(22.dp),
            colors = SurfaceDefaults.colors(containerColor = AerialColors.Raised),
            border = Border(androidx.compose.foundation.BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(22.dp)),
        ) {
            Column(Modifier.padding(horizontal = 36.dp, vertical = 34.dp)) {
                Text("ADD ADD-ON", style = AerialTypography.labelSmall, color = AerialColors.Accent)
                Spacer(Modifier.height(8.dp))
                Text("Paste a manifest URL", style = AerialTypography.headlineMedium.copy(fontSize = 27.sp), color = AerialColors.Txt)
                Spacer(Modifier.height(8.dp))
                Text("The Stremio add-on manifest ends in /manifest.json. We'll fetch it, then mirror its catalogs into channels.",
                    color = AerialColors.Txt2, fontSize = 15.sp)
                Spacer(Modifier.height(22.dp))

                TextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
                    placeholder = "https://…/manifest.json",
                )
                Spacer(Modifier.height(12.dp))
                ValidationLine(state.result)

                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AerialButton("Cancel", onDismiss, Modifier.weight(1f))
                    AerialButton("Add add-on", onConfirm, Modifier.weight(1f), filled = true, enabled = state.canAdd)
                }
            }
        }
    }
}

@Composable
private fun ValidationLine(result: ManifestResult?) {
    when (result) {
        null -> Spacer(Modifier.height(20.dp))
        is ManifestResult.Loading -> HintRow(AerialColors.Txt3, "Checking manifest…")
        is ManifestResult.Ok -> HintRow(AerialColors.Success, "✓ Valid manifest · ${result.name} · ${result.catalogCount} catalogs")
        is ManifestResult.Invalid -> HintRow(AerialColors.Live, "! ${result.reason}")
    }
}

@Composable
private fun HintRow(accent: Color, text: String) {
    Text(text, style = AerialTypography.labelSmall.copy(fontSize = 13.sp), color = accent)
}
