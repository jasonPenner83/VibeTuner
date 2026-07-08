package com.jpenner.vibetuner.phone.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.player.TrackOption

/** Radio-list bottom sheet for audio/subtitle track selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackPickerSheet(
    title: String,
    options: List<TrackOption>,
    onSelect: (TrackOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PhoneColors.Raised) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title, style = MaterialTheme.typography.titleMedium, color = PhoneColors.Txt,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (title == "Subtitles" && options.size <= 1) {
                Text("No subtitles available", color = PhoneColors.Txt2, fontSize = 13.sp)
            }
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option.selected, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = PhoneColors.Accent),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(option.label, color = PhoneColors.Txt)
                }
            }
        }
    }
}

/** Program-details bottom sheet (touch counterpart of the TV info panel). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramInfoSheet(channel: Channel?, program: Program?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PhoneColors.Raised) {
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                program?.title ?: "No programme information",
                style = MaterialTheme.typography.titleLarge, color = PhoneColors.Txt,
            )
            program?.episodeTitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = PhoneColors.Txt2, fontSize = 15.sp)
            }
            channel?.let {
                Text("CH ${it.number} · ${it.name}", color = PhoneColors.Txt2, fontSize = 13.sp)
            }
            program?.let { p ->
                val meta = listOf(p.displayTimeSlot, p.rating, p.mediaType)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  ")
                if (meta.isNotBlank()) Text(meta, color = PhoneColors.Txt3, fontSize = 12.sp)
                if (p.description.isNotBlank()) {
                    Text(p.description, color = PhoneColors.Txt2, fontSize = 14.sp, lineHeight = 20.sp)
                }
                if (p.cast.isNotEmpty()) {
                    Text("Cast: " + p.cast.joinToString(", "), color = PhoneColors.Txt3, fontSize = 12.sp)
                }
            }
        }
    }
}
