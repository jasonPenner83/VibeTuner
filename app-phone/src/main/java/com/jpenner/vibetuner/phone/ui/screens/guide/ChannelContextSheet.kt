package com.jpenner.vibetuner.phone.ui.screens.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors

/** One tappable action in the channel context sheet — replaces the TV app's
 *  ChannelMenuItem/ContextSideSheet (ui/components/ContextSideSheet.kt in :app). */
data class ChannelMenuItem(val label: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelContextSheet(
    channel: Channel,
    items: List<ChannelMenuItem>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = PhoneColors.Raised,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(channel.name, style = MaterialTheme.typography.titleMedium, color = PhoneColors.Txt)
            Text(channel.category.label, style = MaterialTheme.typography.labelSmall, color = channel.category.color)
            Spacer(Modifier.height(12.dp))
            items.forEach { item ->
                PhoneCard(
                    onClick = { item.onClick(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(
                        item.label,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        color = PhoneColors.Txt,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
