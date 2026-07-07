package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.jpenner.vibetuner.ui.theme.AerialColors

/** Shown wherever the derived lineup is empty (no add-ons / no enabled catalogs). */
@Composable
fun EmptyLineupScreen(
    onOpenAddons: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(AerialColors.Bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 560.dp).padding(32.dp),
        ) {
            Text("No channels yet", color = AerialColors.Txt, fontSize = 28.sp)
            Text(
                "Channels come from Stremio add-on catalogs. Open Settings → Add-Ons and paste a " +
                    "manifest URL (for example Cinemeta) to fill your guide.",
                color = AerialColors.Txt3,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            if (onOpenAddons != null) {
                Button(onClick = onOpenAddons, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Open Add-Ons")
                }
            }
        }
    }
}
