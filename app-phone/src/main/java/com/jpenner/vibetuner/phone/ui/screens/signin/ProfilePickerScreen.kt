package com.jpenner.vibetuner.phone.ui.screens.signin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.phone.ui.components.PhoneCard
import com.jpenner.vibetuner.phone.ui.components.PhonePinEntryDialog
import com.jpenner.vibetuner.phone.ui.components.PhoneProfileTile
import com.jpenner.vibetuner.phone.ui.theme.PhoneColors
import com.jpenner.vibetuner.ui.screens.signin.ProfileViewModel

@Composable
fun ProfilePickerScreen(
    onProfileChosen: (Profile) -> Unit,
    onAddProfile: () -> Unit,
    onManage: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshFromSync() }

    // Mirrors the TV picker: this is the mandatory startup gate, nothing to go
    // "back" to — swallow Back here instead of letting it finish the Activity.
    BackHandler(enabled = true) {}

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(PhoneColors.Surface, PhoneColors.Bg), radius = 900f),
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Who’s watching?",
                style = MaterialTheme.typography.displayLarge,
                color = PhoneColors.Txt,
            )
            Spacer(Modifier.height(28.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.profiles) { profile ->
                    PhoneProfileTile(
                        profile = profile,
                        onClick = { if (viewModel.choose(profile)) onProfileChosen(profile) },
                    )
                }
                item { AddProfileTile(onAddProfile) }
            }

            ManagePill(onManage)
        }

        state.unlocking?.let { locked ->
            PhonePinEntryDialog(
                title = if (state.pinError) "Try again" else "Enter PIN",
                subtitle = if (state.pinError) "That PIN didn’t match"
                           else "“${locked.name}” is protected · 4-digit PIN",
                monogram = locked.initial,
                gradient = locked.gradient,
                error = state.pinError,
                onComplete = { pin -> if (viewModel.submitPin(pin)) onProfileChosen(locked) },
                onDismiss = viewModel::dismissUnlock,
            )
        }
    }
}

@Composable
private fun AddProfileTile(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PhoneCard(onClick = onClick, modifier = Modifier.height(112.dp), shape = RoundedCornerShape(20.dp)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Add profile", tint = PhoneColors.Txt3)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Add Profile", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = PhoneColors.Txt2)
    }
}

@Composable
private fun ManagePill(onClick: () -> Unit) {
    PhoneCard(onClick = onClick, shape = RoundedCornerShape(24.dp)) {
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.height(18.dp), tint = PhoneColors.Txt2)
                Text("Manage Profiles", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = PhoneColors.Txt2)
            }
        }
    }
}
