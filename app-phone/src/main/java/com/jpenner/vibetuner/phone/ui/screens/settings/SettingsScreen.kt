package com.jpenner.vibetuner.phone.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.data.model.SettingControl
import com.jpenner.vibetuner.data.model.SettingItem
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.settings.syncRows

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignInGoogle: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.signInGoogle.collect { onSignInGoogle() } }

    Box(Modifier.fillMaxSize().background(PhoneColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PhoneColors.Txt)
                }
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = PhoneColors.Txt)
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(syncRows(state), key = { it.key }) { row ->
                    SettingRow(row, onClick = { viewModel.onActivate(row.key) })
                }
            }
        }
    }
}

@Composable
private fun SettingRow(item: SettingItem, onClick: () -> Unit) {
    val clickable = item.control is SettingControl.Value
    PhoneCard(onClick = { if (clickable) onClick() }, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = PhoneColors.Txt)
                Text(
                    item.sub, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                    color = PhoneColors.Txt2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            when (val control = item.control) {
                is SettingControl.Info -> Text(control.text, color = PhoneColors.Txt3, fontSize = 12.sp)
                is SettingControl.Value -> if (control.text.isNotEmpty()) {
                    Text(control.text, color = if (control.danger) PhoneColors.Live else PhoneColors.Accent, fontSize = 12.sp)
                } else if (clickable) {
                    Text(
                        "›", color = if (control.danger) PhoneColors.Live else PhoneColors.Txt2,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                else -> Unit
            }
        }
    }
}
