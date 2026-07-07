package com.jpenner.vibetuner.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

/**
 * A 4-digit PIN pad shown over a scrim. Buffers digits locally and calls
 * [onComplete] with the 4-char string when full (or OK); the caller decides
 * whether it verifies, and flips [error] to tint the dots on a miss.
 *
 * Reused for both unlock (verify against pinHash) and set/change (entered
 * twice) — the caller drives the copy via [title] / [subtitle].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PinEntryDialog(
    title: String,
    subtitle: String,
    monogram: String,
    gradient: List<Color>,
    error: Boolean,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit,
    onForgot: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    val five = remember { FocusRequester() }
    LaunchedEffect(Unit) { five.requestFocus() }
    BackHandler { onDismiss() }

    // submit clears the local buffer so a wrong PIN starts fresh
    fun submit() { if (pin.length == 4) { val entered = pin; pin = ""; onComplete(entered) } }
    fun push(d: Char) { if (pin.length < 4) pin += d; if (pin.length == 4) submit() }
    fun back() { if (pin.isNotEmpty()) pin = pin.dropLast(1) }

    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {   // 72% scrim
        Surface(
            // Trap D-pad focus inside the keypad — the scrim doesn't block traversal,
            // so without this the focus engine walks out to the screen behind.
            modifier = Modifier.width(430.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            shape = RoundedCornerShape(24.dp),
            colors = SurfaceDefaults.colors(AerialColors.Raised),
            border = Border(BorderStroke(1.dp, AerialColors.Line)),
        ) {
            Column(
                Modifier.padding(horizontal = 38.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(gradient)),
                    Alignment.Center,
                ) { Text(monogram, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AerialColors.Txt) }
                Spacer(Modifier.height(16.dp))
                Text(title, style = AerialTypography.headlineMedium.copy(fontSize = 24.sp), color = AerialColors.Txt)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = if (error) AerialColors.Live else AerialColors.Txt2,
                    fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                PinDots(filled = pin.length, error = error)
                Spacer(Modifier.height(28.dp))

                // 3-col keypad: 1..9, ⌫ 0 OK. "5" takes initial focus.
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "back", "0", "ok")
                keys.chunked(3).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowKeys.forEach { key ->
                            when (key) {
                                "back" -> GhostKey("⌫", onClick = ::back)
                                "ok" -> GhostKey("OK", enabled = pin.length == 4, onClick = ::submit)
                                else -> DigitKey(
                                    key,
                                    modifier = if (key == "5") Modifier.focusRequester(five) else Modifier,
                                    onClick = { push(key.first()) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (onForgot != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Forgot PIN?", color = AerialColors.Txt3, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PinDots(filled: Int, error: Boolean) {
    val active = if (error) AerialColors.Live else AerialColors.Accent
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            Box(
                Modifier.size(17.dp).clip(CircleShape)
                    .background(if (i < filled) active else Color.Transparent)
                    .border(2.dp, if (i <= filled) active else AerialColors.Line, CircleShape),
            )
        }
    }
}

/** 60dp digit key — accent focus ring from Surface indication. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DigitKey(d: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(width = 68.dp, height = 60.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(14.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(AerialColors.Glow, elevation = 10.dp)),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(d, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GhostKey(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(width = 68.dp, height = 60.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent, focusedContainerColor = AerialColors.Surface,
            contentColor = if (enabled) AerialColors.Txt2 else AerialColors.Txt3),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(14.dp))),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(label, fontSize = if (label == "OK") 15.sp else 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
