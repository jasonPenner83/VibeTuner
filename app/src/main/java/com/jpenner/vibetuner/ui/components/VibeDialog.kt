package com.jpenner.vibetuner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

// Confirmation dialog — destructive action is pre-focused via FocusRequester.
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AerialConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { confirmFocus.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {   // scrim 72%
        Surface(
            // Trap D-pad focus inside the dialog — the scrim doesn't block traversal,
            // so without this the focus engine walks out to the screen behind.
            modifier = Modifier.width(680.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            shape = RoundedCornerShape(24.dp),
            colors = SurfaceDefaults.colors(AerialColors.Raised),
            border = Border(BorderStroke(1.dp, AerialColors.Line)),
        ) {
            Column(Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DangerBadge()                              // 74dp tinted circle
                Spacer(Modifier.height(26.dp))
                Text(title, style = AerialTypography.headlineMedium.copy(fontSize = 36.sp),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(message, color = AerialColors.Txt2, textAlign = TextAlign.Center)
                Spacer(Modifier.height(38.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AerialButton("Cancel", onDismiss, Modifier.weight(1f))
                    AerialButton(confirmLabel, onConfirm,
                        Modifier.weight(1f).focusRequester(confirmFocus), filled = true)
                }
            }
        }
    }
}

// ---- dialog-local helpers ----

/** 74dp circular danger glyph at the top of the confirm dialog. */
@Composable
private fun DangerBadge() {
    Box(
        Modifier.size(74.dp).clip(CircleShape).background(AerialColors.Live.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = AerialColors.Live, fontSize = 30.sp)
    }
}

/** Round status glyph — "!" for errors, check for success. */
@Composable
private fun StatusIcon(kind: StatusKind, accent: Color) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (kind == StatusKind.Error) "!" else "✓", color = accent,
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// Non-blocking status messaging — error banner (top) & success toast (corner).
// A 5dp accent bar leads the row; [kind] picks the color.
@Composable
fun StatusBanner(title: String, message: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val accent = if (kind == StatusKind.Error) AerialColors.Live else AerialColors.Success
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AerialColors.Raised)
            .border(1.dp, AerialColors.Line, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(58.dp).background(accent))   // leading accent bar
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusIcon(kind, accent)
            Column {
                Text(title, style = AerialTypography.titleMedium)
                Text(message, color = AerialColors.Txt2, fontSize = 16.sp)
            }
        }
    }
}

enum class StatusKind { Error, Success }