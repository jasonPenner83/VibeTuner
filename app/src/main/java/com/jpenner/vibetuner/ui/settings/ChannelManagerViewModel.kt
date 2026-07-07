package com.jpenner.vibetuner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val MARATHON_LADDER = listOf<Int?>(null, 1, 2, 3, 4, 5)

class ChannelManagerViewModel(
    private val repo: ChannelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelManagerUiState())
    val state: StateFlow<ChannelManagerUiState> = _state.asStateFlow()

    /** sourceKey -> cached title count (filled asynchronously; drives the SOURCE card LIBRARY figure). */
    @Volatile
    private var counts: Map<String, Int> = emptyMap()
    private var countsJob: Job? = null

    init { reload() }

    /** Re-read the lineup from the repository. Public so the screen can call it on
     *  entry — the VM outlives navigation, and addon/seeding changes made elsewhere
     *  (Add-Ons refresh, profile edits) would otherwise stay invisible all session. */
    fun reload() {
        val channels = repo.channelLineup()
        _state.update { s ->
            val key = s.selectedKey?.takeIf { k -> channels.any { it.sourceKey == k } }
                ?: channels.firstOrNull()?.sourceKey
            buildState(channels, key)
        }
        loadCounts(channels)
    }

    /** Read cached item counts off the main thread, then re-emit state with them merged in. */
    private fun loadCounts(channels: List<Channel>) {
        countsJob?.cancel()
        countsJob = viewModelScope.launch(Dispatchers.IO) {
            counts = channels.mapNotNull { ch ->
                repo.cachedItemCount(ch.sourceKey, ch.sortingRule)?.let { ch.sourceKey to it }
            }.toMap()
            _state.update { s -> buildState(channels, s.selectedKey) }
        }
    }

    private fun buildState(channels: List<Channel>, selectedKey: String?): ChannelManagerUiState =
        ChannelManagerUiState(
            rows = channels.map { it.toRow() },
            selectedKey = selectedKey,
            editing = channels.firstOrNull { it.sourceKey == selectedKey }?.toEdit(channels),
            rebuild = _state.value.rebuild,
        )

    fun select(sourceKey: String) = _state.update { it.copy(editing = editFor(sourceKey), selectedKey = sourceKey) }

    private fun editFor(sourceKey: String): ChannelEdit? {
        val channels = repo.channelLineup()
        return channels.firstOrNull { it.sourceKey == sourceKey }?.toEdit(channels)
    }

    fun rename(sourceKey: String, name: String) { repo.renameChannel(sourceKey, name); reload() }

    fun cycleCategory(sourceKey: String, currentLabel: String, step: Int) {
        val genres = Category.GENRES
        val idx = genres.indexOfFirst { it.label == currentLabel }.let { if (it < 0) 0 else it }
        val next = genres[(idx + step).mod(genres.size)]
        repo.setChannelCategory(sourceKey, next.label); reload()
    }

    fun setMode(sourceKey: String, marathon: Boolean) {
        repo.setChannelMode(sourceKey, if (marathon) "CHRONOLOGICAL" else "RANDOM"); reload()
    }

    /** None -> 1 -> 2 -> 3 -> 4 -> 5 -> None (wraps). step is +1 / -1. */
    fun cycleMarathonLimit(sourceKey: String, current: Int?, step: Int) {
        val next = MARATHON_LADDER[(MARATHON_LADDER.indexOf(current) + step).mod(MARATHON_LADDER.size)]
        repo.setChannelMarathonLimit(sourceKey, next); reload()
    }

    fun toggleEnabled(sourceKey: String, enabled: Boolean) { repo.setChannelEnabled(sourceKey, enabled); reload() }

    /** Drop the day's caches and repopulate the guide, reporting progress in the header. */
    fun rebuildGuide() {
        if (_state.value.rebuild is RebuildState.Running) return
        viewModelScope.launch {
            _state.update { it.copy(rebuild = RebuildState.Running(0, 0)) }
            try {
                val populated = repo.rebuildGuide { done, total ->
                    _state.update { it.copy(rebuild = RebuildState.Running(done, total)) }
                }
                _state.update { it.copy(rebuild = RebuildState.Done(populated.size)) }
                reload()   // fresh harvest may change the SOURCE card item counts
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(rebuild = RebuildState.Idle) }   // failed: let the user retry
            }
        }
    }

    fun move(sourceKey: String, up: Boolean) { repo.moveChannel(sourceKey, up); reload() }

    /** Toggle one extra option's sub-channel; the lineup rebuild adds/removes its row immediately. */
    fun toggleSubChannel(sourceKey: String, extraName: String, option: String, selected: Boolean) {
        repo.setSubChannelOption(sourceKey, extraName, option, selected)
        reload()
    }

    /** Select or clear every sub-channel option at once; the lineup rebuild applies it immediately. */
    fun setAllSubChannels(sourceKey: String, selected: Boolean) {
        repo.setAllSubChannelOptions(sourceKey, selected)
        reload()
    }

    // ── mappers ──
    private fun Channel.toRow() = LineupRow(
        sourceKey = sourceKey,
        number = number,
        name = name,
        sourceSub = source?.let { "${it.addonName} · ${it.typeLabel}" } ?: description,
        categoryColor = category.color,
        enabled = enabled,
    )

    private fun Channel.toEdit(all: List<Channel>) = ChannelEdit(
        sourceKey = sourceKey,
        number = number,
        name = name,
        source = source?.copy(itemCount = counts[sourceKey]),
        categoryLabel = category.label,
        categoryColor = category.color,
        marathon = sortingRule.equals("CHRONOLOGICAL", ignoreCase = true),
        marathonLimit = marathonLimit,
        enabled = enabled,
        canMoveUp = all.firstOrNull()?.sourceKey != sourceKey,
        canMoveDown = all.lastOrNull()?.sourceKey != sourceKey,
        subChannels = repo.subChannelToggles(sourceKey),
    )
}
