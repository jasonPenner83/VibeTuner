package com.jpenner.vibetuner.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.repository.ChannelRepository
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import com.jpenner.vibetuner.data.sync.SyncManager
import com.jpenner.vibetuner.phone.ui.screens.guide.GuideScreen
import com.jpenner.vibetuner.phone.ui.screens.player.PlayerScreen
import com.jpenner.vibetuner.phone.ui.screens.player.ResolvingScreen
import com.jpenner.vibetuner.phone.ui.screens.settings.SettingsScreen
import com.jpenner.vibetuner.phone.ui.screens.settings.SettingsViewModel
import com.jpenner.vibetuner.phone.ui.screens.signin.ProfilePickerScreen
import com.jpenner.vibetuner.phone.ui.theme.VibeTunerPhoneTheme
import com.jpenner.vibetuner.ui.screens.guide.GuideViewModel
import com.jpenner.vibetuner.ui.screens.signin.ProfileViewModel
import kotlinx.coroutines.launch

/** Phase 1 MVP screens: pick a profile, browse the guide, watch, and a small
 *  settings page (sync only — see SettingsViewModel in ui/screens/settings). */
private enum class PhoneScreen { PROFILE_PICKER, GUIDE, RESOLVING, PLAYER, SETTINGS }

// The schedule is assembled in Central time (see ChannelRepository / GuideViewModel),
// so "now" for program lookups must read the same zone or it won't line up.
private fun currentGuideMinutes(): Int {
    val now = java.time.LocalTime.now(java.time.ZoneId.of("America/Chicago"))
    return now.hour * 60 + now.minute
}

class MainActivity : ComponentActivity() {

    private val channelRepository by lazy { ChannelRepository(this) }
    private val profileRepository by lazy { ProfileRepository(this) }
    private val profileStore by lazy { ProfileStore.get(this) }
    private val syncManager by lazy { SyncManager.get(this) }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val idToken = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)?.idToken
        }.onFailure {
            android.util.Log.e("VibeTuner Sync", "🔥 Google sign-in failed: $it")
        }.getOrNull()
        if (idToken != null) {
            lifecycleScope.launch { syncManager.signInWithGoogleIdToken(idToken) }
        } else {
            android.util.Log.e("VibeTuner Sync", "🔥 Google sign-in returned no ID token")
        }
    }

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInLauncher.launch(GoogleSignIn.getClient(this, gso).signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VibeTunerPhoneTheme {
                var currentScreen by remember { mutableStateOf(PhoneScreen.PROFILE_PICKER) }
                var tunedChannel by remember { mutableStateOf<Channel?>(null) }
                var tunedProgram by remember { mutableStateOf<Program?>(null) }
                var playerChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
                var resolvedUrl by remember { mutableStateOf<String?>(null) }

                // Program.isLive is static content metadata (e.g. "this is a live
                // broadcast"), not "airing right now" — picking programs.firstOrNull
                // { it.isLive } can tune to a program unrelated to what the Guide
                // row actually showed. nowPlaying(nowMinutes) is the same lookup the
                // Guide list itself uses, so the player always matches what was tapped.
                fun beginWatch(ch: Channel?, channels: List<Channel>) {
                    tunedChannel = ch
                    tunedProgram = ch?.nowPlaying(currentGuideMinutes())
                    playerChannels = channels
                    resolvedUrl = null
                    currentScreen = PhoneScreen.RESOLVING
                }

                // Every screen keeps the status bar clear except the player,
                // which wants the video edge-to-edge behind it.
                val rootModifier = if (currentScreen == PhoneScreen.PLAYER) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxSize().statusBarsPadding()
                }
                Box(rootModifier) {
                    when (currentScreen) {
                        PhoneScreen.PROFILE_PICKER -> {
                            val profileVm: ProfileViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        ProfileViewModel(profileRepository, profileStore, syncPull = { syncManager.pullAllQuietly() })
                                    }
                                }
                            )
                            ProfilePickerScreen(
                                onProfileChosen = { currentScreen = PhoneScreen.GUIDE },
                                onAddProfile = { /* profile management: not in Phase 1 */ },
                                onManage = { /* profile management: not in Phase 1 */ },
                                viewModel = profileVm,
                            )
                        }

                        PhoneScreen.GUIDE -> {
                            val guideVm: GuideViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { GuideViewModel(channelRepository) }
                                }
                            )
                            GuideScreen(
                                onWatch = { channelId ->
                                    val ch = guideVm.state.value.channels.find { it.id == channelId }
                                    beginWatch(ch, guideVm.state.value.channels)
                                },
                                onOpenSettings = { currentScreen = PhoneScreen.SETTINGS },
                                onOpenProfile = { currentScreen = PhoneScreen.PROFILE_PICKER },
                                viewModel = guideVm,
                            )
                        }

                        PhoneScreen.SETTINGS -> {
                            val settingsVm: SettingsViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { SettingsViewModel(syncManager) }
                                }
                            )
                            SettingsScreen(
                                onBack = { currentScreen = PhoneScreen.GUIDE },
                                onSignInGoogle = { launchGoogleSignIn() },
                                viewModel = settingsVm,
                            )
                        }

                        PhoneScreen.RESOLVING -> {
                            ResolvingScreen(
                                channel = tunedChannel,
                                program = tunedProgram,
                                onResolved = { url ->
                                    resolvedUrl = url
                                    currentScreen = PhoneScreen.PLAYER
                                },
                            )
                        }

                        PhoneScreen.PLAYER -> {
                            PlayerScreen(
                                channel = tunedChannel,
                                program = tunedProgram,
                                streamUrl = resolvedUrl,
                                channels = playerChannels,
                                onExit = { currentScreen = PhoneScreen.GUIDE },
                                onZap = { channelId ->
                                    val ch = playerChannels.find { it.id == channelId }
                                    beginWatch(ch, playerChannels)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
