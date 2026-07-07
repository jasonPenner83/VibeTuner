package com.jpenner.vibetuner.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.repository.SettingsRepository
import com.jpenner.vibetuner.data.model.SettingControl
import com.jpenner.vibetuner.data.model.SettingItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val syncManager: com.jpenner.vibetuner.data.sync.SyncManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _openAddons = Channel<Unit>(Channel.BUFFERED)
    val openAddons = _openAddons.receiveAsFlow()

    private val _openChannels = Channel<Unit>(Channel.BUFFERED)
    val openChannels = _openChannels.receiveAsFlow()

    private val _signInGoogle = Channel<Unit>(Channel.BUFFERED)
    val signInGoogle = _signInGoogle.receiveAsFlow()

    // section id -> its rows. Single source of truth the pane reads from.
    private val rowsBySection: MutableMap<String, List<SettingItem>> = defaultRows()

    init {
        _state.update {
            it.copy(sections = sections, rows = rowsBySection.getValue(it.selectedId))
        }
        // hydrate persisted values, then refresh the visible pane
        viewModelScope.launch {
            repository.overrides().collect { saved ->
                applyOverrides(saved)
                refresh()
            }
        }
        syncManager?.let { sm ->
            viewModelScope.launch {
                combine(
                    sm.auth?.account ?: MutableStateFlow(null),
                    sm.lastSyncMs,
                    sm.syncError,
                ) { account, last, err ->
                    SyncSettingsState(sm.isConfigured, account?.email, last, err)
                }.collect { rowsBySection["sync"] = syncRows(it); refresh() }
            }
        } ?: run { rowsBySection["sync"] = syncRows(SyncSettingsState(false, null, null, null)) }
    }

    fun select(id: String) = _state.update {
        it.copy(selectedId = id, rows = rowsBySection.getValue(id))
    }

    /** OK / Enter on a row. Toggles flip; value rows advance to the next option. */
    fun activate(key: String) {
        when (key) {
            "addons_manage"    -> { viewModelScope.launch { _openAddons.send(Unit) }; return }
            "channels_manage"  -> { viewModelScope.launch { _openChannels.send(Unit) }; return }
            "sync_signin"  -> { viewModelScope.launch { _signInGoogle.send(Unit) }; return }
            "sync_now"     -> { viewModelScope.launch { syncManager?.syncNow() }; return }
            "sync_signout" -> { viewModelScope.launch { syncManager?.signOut() }; return }
        }
        val section = _state.value.selectedId
        rowsBySection[section] = rowsBySection.getValue(section).map { row ->
            if (row.key != key) row else when (val c = row.control) {
                is SettingControl.Toggle -> row.copy(control = c.copy(on = !c.on))
                is SettingControl.Value  -> row.copy(control = c.copy(text = nextOption(key, c.text)))
                else -> row
            }
        }
        persist(section, key); refresh()
    }

    /** D-pad left/right on a focused slider row. [delta] is +/-0.05f per press. */
    fun adjust(key: String, delta: Float) {
        val section = _state.value.selectedId
        rowsBySection[section] = rowsBySection.getValue(section).map { row ->
            val c = row.control
            if (row.key == key && c is SettingControl.Slider)
                row.copy(control = c.copy(fraction = (c.fraction + delta).coerceIn(0f, 1f)))
            else row
        }
        persist(section, key); refresh()
    }

    private fun refresh() = _state.update {
        it.copy(rows = rowsBySection.getValue(it.selectedId))
    }

    private fun persist(section: String, key: String) = viewModelScope.launch {
        val control = rowsBySection.getValue(section).first { it.key == key }.control
        repository.save(key, control)
    }

    private fun applyOverrides(saved: Map<String, SettingControl>) {
        rowsBySection.replaceAll { _, rows ->
            rows.map { r -> saved[r.key]?.let { r.copy(control = it) } ?: r }
        }
    }

    private fun nextOption(key: String, current: String): String {
        val opts = options[key] ?: return current
        return opts[(opts.indexOf(current) + 1).mod(opts.size)]
    }

    private companion object {
        val sections = listOf(
            SettingsSection("sync",    "Sync",               "Profiles across devices"),
            SettingsSection("account",  "Account",            "Jordan C."),
            SettingsSection("display",  "Display & Sound",    "4K HDR · Atmos"),
            SettingsSection("network",  "Network",            "Wi-Fi · Studio 5G"),
            SettingsSection("playback", "Playback",           "Autoplay on"),
            SettingsSection("parental", "Parental Controls",  "Locked · TV-14"),
            SettingsSection("addons",    "Add-Ons",            "Add/Manage Manifests"),
            SettingsSection("channels", "Channels",           "Rename, sort, Marathon/Random"),
            SettingsSection("about",    "About",              "Aerial OS 14.2"),
        )

        // selectable options for Value rows, cycled by activate()
        val options = mapOf(
            "resolution" to listOf("4K · Dolby Vision", "4K · HDR10", "1080p · SDR"),
            "audioOut"   to listOf("HDMI eARC", "Optical", "TV Speakers"),
            "surround"   to listOf("Dolby Atmos", "5.1 Surround", "Stereo"),
            "maxRating"  to listOf("TV-14", "TV-PG", "TV-G", "TV-MA"),
            "streamQ"    to listOf("Auto · up to 4K", "High · 1080p", "Data Saver · 720p"),
            "audioLang"  to listOf("English", "Spanish", "French"),
            "subs"       to listOf("Off", "English", "English SDH"),
        )

        fun defaultRows(): MutableMap<String, List<SettingItem>> = mutableMapOf(
            "sync" to emptyList(),
            "account" to listOf(
                SettingItem("profile",    "Profile",        "Manage your profile and avatar",      SettingControl.Value("Jordan C.")),
                SettingItem("plan",       "Subscription",   "Plan, billing and devices",           SettingControl.Value("Premium · 4K")),
                SettingItem("recs",       "Personalized recommendations", "Use viewing history to suggest content", SettingControl.Toggle(true)),
                SettingItem("signout",    "Sign out of all devices",      "You will need to sign in again",         SettingControl.Value("", danger = true)),
            ),
            "display" to listOf(
                SettingItem("resolution", "Resolution",            "Output resolution and range",        SettingControl.Value("4K · Dolby Vision")),
                SettingItem("frameMatch", "Match content frame rate", "Switch refresh rate to match source", SettingControl.Toggle(true)),
                SettingItem("audioOut",   "Audio output",          "Connected sound system",             SettingControl.Value("HDMI eARC")),
                SettingItem("surround",   "Surround sound",        "Spatial audio format",               SettingControl.Value("Dolby Atmos")),
                SettingItem("dialogue",   "Dialogue boost",        "Lift speech over background audio",  SettingControl.Slider(0.40f)),
                SettingItem("reduceMotion","Reduce motion",        "Minimise UI animation and parallax", SettingControl.Toggle(false)),
            ),
            "network" to listOf(
                SettingItem("connection", "Connection",       "Wireless network",                    SettingControl.Value("Studio 5G")),
                SettingItem("signal",     "Signal strength",  "Current quality",                     SettingControl.Info("Excellent")),
                SettingItem("streamQ",    "Streaming quality","Maximum bitrate over this network",   SettingControl.Value("Auto · up to 4K")),
                SettingItem("dataSaver",  "Data saver",       "Lower quality to reduce bandwidth",   SettingControl.Toggle(false)),
            ),
            "playback" to listOf(
                SettingItem("autoplay",   "Autoplay next episode", "Continue automatically when one ends", SettingControl.Toggle(true)),
                SettingItem("audioLang",  "Default audio language","Preferred track when available",       SettingControl.Value("English")),
                SettingItem("subs",       "Subtitles & captions",  "Default subtitle behaviour",           SettingControl.Value("Off")),
                SettingItem("skip",       "Skip recaps & intros",  "Jump straight into the action",        SettingControl.Toggle(true)),
                SettingItem("buffer",     "Live buffer length",    "Seconds held for instant rewind",      SettingControl.Slider(0.60f)),
            ),
            "parental" to listOf(
                SettingItem("lock",       "Content lock",      "Require PIN for restricted content",  SettingControl.Toggle(true)),
                SettingItem("maxRating",  "Maximum rating",    "Highest rating allowed without PIN",   SettingControl.Value("TV-14")),
                SettingItem("pin",        "Change PIN",        "4-digit access code",                 SettingControl.Value("••••")),
                SettingItem("approval",   "Purchase approval", "Require PIN to buy or rent",          SettingControl.Toggle(true)),
            ),
            "addons" to listOf(
                SettingItem("addons_manage", "Manage Add-Ons", "Add or remove Stremio catalog sources for this profile", SettingControl.Value(""))
            ),
            "channels" to listOf(
                SettingItem("channels_manage", "Manage Channels", "Customize your catalog channels", SettingControl.Value(""))
            ),
            "about" to listOf(
                SettingItem("version",    "Software version",  "Current build",                       SettingControl.Info("Aerial OS 14.2")),
                SettingItem("device",     "Device",            "Registered name",                     SettingControl.Info("Living Room TV")),
                SettingItem("storage",    "Storage",           "Available for recordings",            SettingControl.Info("38.2 GB free")),
                SettingItem("update",     "Check for updates", "Last checked today",                  SettingControl.Value("")),
                SettingItem("legal",      "Legal & licences",  "Terms, privacy and open source",      SettingControl.Value("")),
            ),
        )
    }
}
