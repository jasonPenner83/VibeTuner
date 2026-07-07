package com.jpenner.vibetuner.phone.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors

/**
 * Touch counterpart of the TV app's PinEntryDialog (ui/components/PinEntryDialog.kt
 * in :app): same 4-digit keypad and copy, tap instead of D-pad focus/OK.
 */
@Composable
fun PhonePinEntryDialog(
    title: String,
    subtitle: String,
    monogram: String,
    gradient: List<Color>,
    error: Boolean,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    BackHandler { onDismiss() }

    fun submit() { if (pin.length == 4) { val entered = pin; pin = ""; onComplete(entered) } }
    fun push(d: Char) { if (pin.length < 4) pin += d; if (pin.length == 4) submit() }
    fun back() { if (pin.isNotEmpty()) pin = pin.dropLast(1) }

    Box(Modifier.fillMaxSize().background(Color(0xB8060709)), Alignment.Center) {
        Card(
            modifier = Modifier.width(320.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PhoneColors.Raised),
            border = BorderStroke(1.dp, PhoneColors.Line),
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradient)),
                    Alignment.Center,
                ) { Text(monogram, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PhoneColors.Txt) }
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp), color = PhoneColors.Txt)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = if (error) PhoneColors.Live else PhoneColors.Txt2,
                    fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                PinDots(filled = pin.length, error = error)
                Spacer(Modifier.height(24.dp))

                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "back", "0", "ok")
                keys.chunked(3).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowKeys.forEach { key ->
                            when (key) {
                                "back" -> GhostKey("⌫", onClick = ::back)
                                "ok" -> GhostKey("OK", enabled = pin.length == 4, onClick = ::submit)
                                else -> DigitKey(key, onClick = { push(key.first()) })
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PinDots(filled: Int, error: Boolean) {
    val active = if (error) PhoneColors.Live else PhoneColors.Accent
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { i ->
            Box(
                Modifier.size(15.dp).clip(CircleShape)
                    .background(if (i < filled) active else Color.Transparent)
                    .border(2.dp, if (i <= filled) active else PhoneColors.Line, CircleShape),
            )
        }
    }
}

@Composable
private fun DigitKey(d: String, onClick: () -> Unit) {
    PhoneCard(onClick = onClick, modifier = Modifier.size(width = 64.dp, height = 56.dp), shape = RoundedCornerShape(14.dp)) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(d, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PhoneColors.Txt)
        }
    }
}

@Composable
private fun GhostKey(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    PhoneCard(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(width = 64.dp, height = 56.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                label, fontSize = if (label == "OK") 14.sp else 18.sp, fontWeight = FontWeight.SemiBold,
                color = if (enabled) PhoneColors.Txt2 else PhoneColors.Txt3,
            )
        }
    }
}
