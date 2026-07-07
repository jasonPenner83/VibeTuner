package com.jpenner.vibetuner.ui.screens.signin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.ui.components.Logo
import com.jpenner.vibetuner.ui.components.PinEntryDialog
import com.jpenner.vibetuner.ui.components.ProfileTile
import com.jpenner.vibetuner.ui.theme.AerialCanvas
import com.jpenner.vibetuner.ui.theme.AerialColors
import com.jpenner.vibetuner.ui.theme.AerialTypography

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onProfileChosen: (Profile) -> Unit,
    onAddProfile: () -> Unit,
    onManage: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { viewModel.refreshFromSync() }
    LaunchedEffect(state.profiles) { if (state.profiles.isNotEmpty()) firstTile.requestFocus() }

    // The mandatory startup gate has nowhere to go "back" to. Swallow a short Back
    // press here instead of letting it fall through to the Activity default (which
    // would finish the app) — a 500ms+ hold still exits, via MainActivity's own
    // long-press handling, which calls finish() directly and bypasses this callback.
    BackHandler(enabled = true) {}

    AerialCanvas {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(AerialColors.Surface, AerialColors.Bg),
                radius = 1400f,
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Logo()
            Spacer(Modifier.height(30.dp))
            Text("Who\u2019s watching?",
                style = AerialTypography.displayLarge.copy(fontSize = 56.sp), color = AerialColors.Txt)
            Spacer(Modifier.height(50.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.Top) {
                state.profiles.forEachIndexed { index, profile ->
                    ProfileTile(
                        profile = profile,
                        // PIN-locked profiles open the gate; open ones route straight to Home.
                        onClick = { if (viewModel.choose(profile)) onProfileChosen(profile) },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
                AddProfileTile(onAddProfile)
            }

            Spacer(Modifier.height(48.dp))
            ManagePill(onManage)
        }

        // PIN gate over the picker for restricted profiles.
        state.unlocking?.let { locked ->
            PinEntryDialog(
                title = if (state.pinError) "Try again" else "Enter PIN",
                subtitle = if (state.pinError) "That PIN didn\u2019t match"
                           else "\u201c${locked.name}\u201d is protected \u00b7 4-digit PIN",
                monogram = locked.initial,
                gradient = locked.gradient,
                error = state.pinError,
                onComplete = { pin -> if (viewModel.submitPin(pin)) onProfileChosen(locked) },
                onDismiss = viewModel::dismissUnlock,
            )
        }
    }
    }
}

/* ───────── signin-local helpers ───────── */

/** Dashed “+” tile that mirrors ProfileTile’s footprint. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileTile(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(168.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AerialColors.Bg, focusedContainerColor = AerialColors.Surface),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(2.dp, AerialColors.Line), shape = RoundedCornerShape(24.dp)),
                focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(24.dp))),
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("+", fontSize = 50.sp, color = AerialColors.Txt3)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Add Profile", style = AerialTypography.titleMedium.copy(fontSize = 24.sp),
            color = AerialColors.Txt2)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ManagePill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AerialColors.Surface, focusedContainerColor = AerialColors.Raised,
            contentColor = AerialColors.Txt2),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, AerialColors.Line), shape = RoundedCornerShape(24.dp)),
            focusedBorder = Border(BorderStroke(2.dp, AerialColors.Accent), shape = RoundedCornerShape(24.dp))),
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Manage Profiles", style = AerialTypography.titleMedium.copy(fontSize = 18.sp))
        }
    }
}