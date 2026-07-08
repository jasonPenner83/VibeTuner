package com.jpenner.vibetuner

import android.os.Bundle
import android.view.KeyEvent
import com.jpenner.vibetuner.ui.LongPressTracker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.ui.screens.player.PlayerScreen
import com.jpenner.vibetuner.ui.components.TransitionLoadingScreen
import com.jpenner.vibetuner.data.repository.ChannelRepository
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.settings.DataStoreSettingsRepository
import com.jpenner.vibetuner.data.model.PlaybackTarget
import com.jpenner.vibetuner.data.model.Profile
import com.jpenner.vibetuner.ui.components.TopTab
import kotlinx.coroutines.launch
import com.jpenner.vibetuner.ui.screens.guide.GuideScreen
import com.jpenner.vibetuner.ui.screens.guide.GuideViewModel
import com.jpenner.vibetuner.ui.screens.home.HomeScreen
import com.jpenner.vibetuner.ui.screens.home.HomeViewModel
import com.jpenner.vibetuner.ui.screens.detail.ProgramInfoScreen
import com.jpenner.vibetuner.ui.screens.detail.ProgramInfoViewModel
import com.jpenner.vibetuner.ui.screens.lineup.DayLineupScreen
import com.jpenner.vibetuner.ui.screens.lineup.DayLineupViewModel
import com.jpenner.vibetuner.ui.screens.settings.SettingsScreen
import com.jpenner.vibetuner.ui.screens.settings.SettingsViewModel
import com.jpenner.vibetuner.ui.screens.signin.ProfilePickerScreen
import com.jpenner.vibetuner.ui.screens.signin.ProfileViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jpenner.vibetuner.ui.screens.startup.AppStartupLoadingScreen
import com.jpenner.vibetuner.ui.screens.startup.GuideWarmer
import com.jpenner.vibetuner.ui.screens.startup.StartupViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.lifecycleScope
import com.jpenner.vibetuner.ui.screens.guide.currentGuideMinutes

enum class AppScreen {
    GUIDE,
    PLAYER, // the watch flow: transition overlay + player buffering behind it
    SETTINGS_ADDONS,
    CHANNEL_MANAGER,
    PROFILE_MANAGER,
    // Aerial-redesign screens (phased): reachable from the Guide's Home tab.
    PROFILE_PICKER,
    HOME,
    PROGRAM_INFO,
    DAY_LINEUP,
    SETTINGS,
}

class MainActivity : ComponentActivity() {

    private val channelRepository by lazy { ChannelRepository(this) }
    private val profileRepository by lazy { ProfileRepository(this) }
    private val profileStore by lazy { com.jpenner.vibetuner.data.repository.ProfileStore.get(this) }
    private val addonRepository by lazy { com.jpenner.vibetuner.data.repository.AddonRepository(this) }
    private val settingsRepository by lazy { DataStoreSettingsRepository(this) }
    private val syncManager by lazy { com.jpenner.vibetuner.data.sync.SyncManager.get(this) }
    private val backLongPress by lazy { LongPressTracker(lifecycleScope, thresholdMs = 500L) }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val idToken = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)?.idToken
        }.onFailure {
            // Surface the status code: 10 = DEVELOPER_ERROR (client id/SHA-1 mismatch).
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

    // One activity-scoped lineup VM serves both hosts (Guide full screen and the
    // player's Schedule overlay); DayLineupContent reloads it on every entry.
    @Composable
    private fun rememberDayLineupViewModel(): DayLineupViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DayLineupViewModel(loadChannels = { channelRepository.loadGuide() }) }
        },
    )

    // Long-press back (exit) vs short press (navigate back) needs raw key
    // down/up timing, which OnBackPressedDispatcher callbacks don't expose.
    // Safe while enableOnBackInvokedCallback stays off in the manifest.
    @Suppress("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event?.repeatCount == 0) backLongPress.onDown(onLongPress = { finish() })
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("GestureBackNavigation")
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backLongPress.onUp()) onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.PROFILE_PICKER) }
            var tunedChannel by remember { mutableStateOf<Channel?>(null) }
            var tunedProgram by remember { mutableStateOf<Program?>(null) }

            val troubleshootLogs = remember { mutableStateListOf<String>() }
            var resolvedUrl by remember { mutableStateOf<String?>(null) }
            // Watch-flow overlay state: the player's first-frame signal and whether the
            // channel-transition overlay has lifted.
            var playerReady by remember { mutableStateOf(false) }
            var tuneOverlayDone by remember { mutableStateOf(false) }

            // The program whose detail screen is open (PROGRAM_INFO route arg).
            var infoProgramId by remember { mutableStateOf<String?>(null) }

            // The channel whose full-day lineup is open (DAY_LINEUP route arg).
            var lineupChannelId by remember { mutableStateOf<String?>(null) }

            // Fully-populated guide channels (with programs), captured at watch time
            // so the in-player switcher can render real now-playing rows.
            var playerChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
            val scope = rememberCoroutineScope()

            // Enter the watch flow for the already-set tunedChannel/tunedProgram: reset the
            // resolve/buffer state so the transition overlay re-runs from scratch.
            fun beginWatch() {
                resolvedUrl = null
                playerReady = false
                tuneOverlayDone = false
                currentScreen = AppScreen.PLAYER
            }

            // Resolve a channel id against the live guide and route into the watch flow.
            // Used by the aerial Home + ProgramInfo screens, which don't hold the
            // GuideViewModel's populated channel list.
            fun tuneToChannel(channelId: String) {
                scope.launch {
                    val channels = channelRepository.loadGuide()
                    val ch = channels.find { it.id == channelId }
                    tunedChannel = ch
                    tunedProgram = ch?.nowPlaying(currentGuideMinutes())
                    ch?.let { channelRepository.setTunedChannel(it.id) }
                    playerChannels = channels
                    beginWatch()
                }
            }

            val startupVm: StartupViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        StartupViewModel(
                            GuideWarmer { onProgress ->
                                syncManager.pullAllQuietly()
                                val profiles = profileStore.profilesNow()
                                val profileTotal = profiles.size.coerceAtLeast(1)
                                profiles.forEachIndexed { index, profile ->
                                    try {
                                        channelRepository.loadGuideForProfile(profile.id) { done, total ->
                                            onProgress(index, profileTotal, profile.name, done, total)
                                        }
                                    } catch (c: kotlinx.coroutines.CancellationException) {
                                        throw c
                                    } catch (t: Throwable) {
                                        android.util.Log.e(
                                            "VibeTuner Guide",
                                            "🔥 Startup guide preload failed for ${profile.name}: ${t.message}",
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            )
            val startupState by startupVm.state.collectAsState()

            if (!startupState.done) {
                AppStartupLoadingScreen(state = startupState)
            } else {
                    when (currentScreen) {
                        AppScreen.GUIDE -> {
                            val guideVm: GuideViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { GuideViewModel(channelRepository) }
                                }
                            )
                            GuideScreen(
                                onWatch = { channelId ->
                                    val ch = guideVm.state.value.channels.find { it.id == channelId }
                                    tunedChannel = ch
                                    tunedProgram = ch?.nowPlaying(currentGuideMinutes())
                                    ch?.let { channelRepository.setTunedChannel(it.id) }
                                    playerChannels = guideVm.state.value.channels
                                    beginWatch()
                                },
                                onOpenInfo = { programId ->
                                    // No ProgramInfo screen yet — clicking a cell tunes/plays it.
                                    val ch = guideVm.state.value.channels
                                        .find { c -> c.programs.any { it.id == programId } }
                                    tunedChannel = ch
                                    tunedProgram = ch?.programs?.firstOrNull { it.id == programId }
                                    ch?.let { channelRepository.setTunedChannel(it.id) }
                                    playerChannels = guideVm.state.value.channels
                                    beginWatch()
                                },
                                onOpenLineup = { channelId ->
                                    lineupChannelId = channelId
                                    currentScreen = AppScreen.DAY_LINEUP
                                },
                                onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                                onOpenProfile = { currentScreen = AppScreen.PROFILE_PICKER },
                                onOpenAddons = { currentScreen = AppScreen.SETTINGS_ADDONS },
                                onNavigate = { tab ->
                                    // Home tab enters the aerial flow (pick profile -> Home).
                                    if (tab == TopTab.Home) currentScreen = AppScreen.PROFILE_PICKER
                                    // On Demand / Recordings not built yet.
                                },
                                viewModel = guideVm,
                            )
                        }

                        AppScreen.PROFILE_PICKER -> {
                            val profileVm: ProfileViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { ProfileViewModel(profileRepository, profileStore, syncPull = { syncManager.pullAllQuietly() }) }
                                }
                            )
                            ProfilePickerScreen(
                                onProfileChosen = { _: Profile -> currentScreen = AppScreen.HOME },
                                onAddProfile = { currentScreen = AppScreen.PROFILE_MANAGER },
                                onManage = { currentScreen = AppScreen.PROFILE_MANAGER },
                                viewModel = profileVm,
                            )
                        }

                        AppScreen.PROFILE_MANAGER -> {
                            val managerVm: com.jpenner.vibetuner.ui.settings.ProfileManagerViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        com.jpenner.vibetuner.ui.settings.ProfileManagerViewModel(profileStore, addonRepository)
                                    }
                                }
                            )
                            com.jpenner.vibetuner.ui.settings.ProfileManagerScreen(
                                onBack = { currentScreen = AppScreen.PROFILE_PICKER },
                                viewModel = managerVm,
                            )
                        }

                        AppScreen.HOME -> {
                            val homeVm: HomeViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { HomeViewModel(channelRepository) }
                                }
                            )
                            HomeScreen(
                                onNavigate = { tab ->
                                    if (tab == TopTab.Guide) currentScreen = AppScreen.GUIDE
                                    // On Demand / Recordings not built yet; Home stays put.
                                },
                                onOpenTarget = { target ->
                                    when (target) {
                                        is PlaybackTarget.WatchChannel -> tuneToChannel(target.channelId)
                                        is PlaybackTarget.ProgramDetail -> {
                                            infoProgramId = target.programId
                                            currentScreen = AppScreen.PROGRAM_INFO
                                        }
                                    }
                                },
                                onSettings = { currentScreen = AppScreen.SETTINGS },
                                onProfile = { currentScreen = AppScreen.PROFILE_PICKER },
                                onOpenAddons = { currentScreen = AppScreen.SETTINGS_ADDONS },
                                viewModel = homeVm,
                            )
                        }

                        AppScreen.PROGRAM_INFO -> {
                            val infoVm: ProgramInfoViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { ProgramInfoViewModel(channelRepository) }
                                }
                            )
                            ProgramInfoScreen(
                                programId = infoProgramId.orEmpty(),
                                onBack = { currentScreen = AppScreen.HOME },
                                onWatch = { channelId -> tuneToChannel(channelId) },
                                viewModel = infoVm,
                            )
                        }

                        AppScreen.DAY_LINEUP -> {
                            DayLineupScreen(
                                channelId = lineupChannelId.orEmpty(),
                                onBack = { currentScreen = AppScreen.GUIDE },
                                viewModel = rememberDayLineupViewModel(),
                            )
                        }

                        AppScreen.SETTINGS -> {
                            val settingsVm: SettingsViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        SettingsViewModel(
                                            settingsRepository,
                                            syncManager,
                                        )
                                    }
                                }
                            )
                            SettingsScreen(
                                onBack = { currentScreen = AppScreen.HOME },
                                onOpenAddons = { currentScreen = AppScreen.SETTINGS_ADDONS },
                                onOpenChannels = { currentScreen = AppScreen.CHANNEL_MANAGER },
                                onSignInGoogle = { launchGoogleSignIn() },
                                viewModel = settingsVm,
                            )
                        }

                        AppScreen.SETTINGS_ADDONS -> {
                            val addonsVm: com.jpenner.vibetuner.ui.settings.AddonsViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        com.jpenner.vibetuner.ui.settings.AddonsViewModel(
                                            addonRepository,
                                            profileRepository,
                                            profileStore,
                                        )
                                    }
                                }
                            )
                            com.jpenner.vibetuner.ui.settings.AddonsScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS },
                                viewModel = addonsVm,
                            )
                        }

                        AppScreen.CHANNEL_MANAGER -> {
                            val channelVm: com.jpenner.vibetuner.ui.settings.ChannelManagerViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { com.jpenner.vibetuner.ui.settings.ChannelManagerViewModel(channelRepository) }
                                }
                            )
                            com.jpenner.vibetuner.ui.settings.ChannelManagerScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS },
                                onOpenAddons = { currentScreen = AppScreen.SETTINGS_ADDONS },
                                viewModel = channelVm,
                            )
                        }

                        // The watch flow: the player buffers behind the channel-transition
                        // overlay, which lifts on the first buffered frame (see beginWatch()).
                        AppScreen.PLAYER -> {
                            Box(Modifier.fillMaxSize()) {
                                // Only mount the player once we have a URL to resolve against
                                // (or the overlay has given up) — never during the resolve phase.
                                if (resolvedUrl != null || tuneOverlayDone) {
                                    var playerFavourite by remember(tunedChannel?.id) {
                                        mutableStateOf(tunedChannel?.let { channelRepository.isFavourite(it.id) } == true)
                                    }
                                    PlayerScreen(
                                        channel = tunedChannel,
                                        program = tunedProgram,
                                        streamUrl = resolvedUrl,
                                        channels = playerChannels,
                                        onExit = { currentScreen = AppScreen.GUIDE },
                                        onOpenGuide = { currentScreen = AppScreen.GUIDE },
                                        // In-player zapping re-enters the same resolve/buffer flow.
                                        onZap = { channelId ->
                                            val ch = playerChannels.find { it.id == channelId }
                                            tunedChannel = ch
                                            tunedProgram = ch?.nowPlaying(currentGuideMinutes())
                                            ch?.let { channelRepository.setTunedChannel(it.id) }
                                            beginWatch()
                                        },
                                        onFirstFrameReady = { playerReady = true },
                                        interactive = tuneOverlayDone,
                                        isFavourite = playerFavourite,
                                        onToggleFavourite = {
                                            tunedChannel?.let {
                                                channelRepository.toggleFavourite(it.id)
                                                playerFavourite = !playerFavourite
                                            }
                                        },
                                        lineupViewModel = rememberDayLineupViewModel(),
                                    )
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !tuneOverlayDone,
                                    enter = androidx.compose.animation.fadeIn(),
                                    exit = androidx.compose.animation.fadeOut(),
                                ) {
                                    TransitionLoadingScreen(
                                        channel = tunedChannel,
                                        program = tunedProgram,
                                        troubleshootLogs = troubleshootLogs,
                                        ready = playerReady,
                                        onResolved = { resolvedUrl = it },
                                        onFinished = { tuneOverlayDone = true },
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }

}