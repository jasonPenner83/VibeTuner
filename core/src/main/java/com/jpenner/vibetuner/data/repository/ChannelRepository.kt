package com.jpenner.vibetuner.data.repository

import android.content.Context
import android.util.Log
import com.jpenner.vibetuner.data.model.Channel
import com.jpenner.vibetuner.data.model.Program
import com.jpenner.vibetuner.data.model.SourceType
import com.jpenner.vibetuner.data.model.RawMediaItem
import com.jpenner.vibetuner.data.model.Category
import com.jpenner.vibetuner.data.model.stremio.StremioAddon
import com.jpenner.vibetuner.data.model.stremio.StremioCatalog
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class ChannelRepository(private val context: Context) {

    private companion object {
        const val TAG = "VibeTuner Guide"
    }

    /** All schedules are anchored to CST so every channel runs 00:00→23:59 Central regardless of device timezone. */
    private val scheduleZone: ZoneId = ZoneId.of("America/Chicago")

    private fun scheduleEpochDay(): Long = LocalDate.now(scheduleZone).toEpochDay()

    private val addonRepository = AddonRepository(context)
    private val catalogSource = com.jpenner.vibetuner.data.api.StremioCatalogDataSource()
    private val profileRepository = ProfileRepository(context)
    private val profileStore = ProfileStore.get(context)
    private val harvestCache = com.jpenner.vibetuner.data.cache.HarvestCache(
        File(context.filesDir, "vibetuner_harvest_cache.json"),
        // Anchor cache expiry to the schedule day so it rolls over with the guide, not the device timezone.
        clock = { LocalDate.now(scheduleZone).toEpochDay() }
    )
    private val overrideStore = ChannelOverrideStore(context)
    private val metaSource = com.jpenner.vibetuner.data.api.StremioMetaDataSource()
    private val marathonProgressStore =
        MarathonProgressStore(File(context.filesDir, "vibetuner_marathon_progress.json"))
    private val syncManager by lazy { com.jpenner.vibetuner.data.sync.SyncManager.get(context) }

    /** In-memory hold of the populated guide so Home/Guide/tune share one build per day+lineup. */
    private val guideCache = GuideCache()

    /** Serializes guide builds so a manual rebuild can never interleave with a
     *  normal load — the loser of the race would otherwise publish stale data
     *  under the same day+profile+signature cache key. */
    private val guideBuildMutex = Mutex()

    /** The profile whose addons + channels we read/write. Resolved fresh so a profile
     *  switch is picked up. Public so screen VMs can detect a switch and reload. */
    fun activeProfileId(): String = profileRepository.activeProfileId() ?: "default"

    /** Last channel each profile tuned to, in-memory only — lets the Guide
     *  refocus it on return from Player. Never persisted; lost on process death. */
    private val tunedChannelByProfile = mutableMapOf<String, String>()

    /** Records [channelId] as the active profile's current channel. Call this
     *  everywhere the app enters the watch flow (Guide/Home/ProgramInfo "watch",
     *  in-player zap/switch). */
    fun setTunedChannel(channelId: String) {
        tunedChannelByProfile[activeProfileId()] = channelId
    }

    /** The active profile's last-tuned channel id, or null if none this session. */
    fun tunedChannelId(): String? = tunedChannelByProfile[activeProfileId()]

    /** All channels for [profileId] (incl. disabled) — the Channel Manager reads this for the active profile. */
    fun channelLineup(profileId: String = activeProfileId()): List<Channel> {
        val allowAdult = profileStore.byId(profileId)?.allowsAdult == true
        return buildLineup(addonRepository.enabledCatalogs(profileId, allowAdult), overrideStore.getAll(profileId))
    }

    fun toggleFavourite(channelId: String) {
        profileStore.toggleFavourite(activeProfileId(), channelId)
    }

    /** Whether [channelId] is in the active profile's favourites. */
    fun isFavourite(channelId: String): Boolean =
        profileStore.byId(activeProfileId())?.favouriteChannelIds?.contains(channelId) == true

    /**
     * One-shot guide load for the active profile: seed addons -> derive enabled lineup -> populate
     * live timelines. Caches the fully-populated result in memory (keyed by day + profile + lineup
     * signature) so repeat callers (Home, Guide, tune) get it instantly. [onProgress] reports populate
     * progress (done, total) for the startup loading screen; the cache-hit path reports 100% immediately.
     */
    suspend fun loadGuide(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<Channel> =
        guideBuildMutex.withLock { loadGuideLocked(activeProfileId(), onProgress) }

    /** Same one-shot build as [loadGuide], but for an explicit [profileId] rather than whichever
     *  profile is currently active — used by startup preload to warm every profile's guide without
     *  touching the persisted active-profile file. */
    suspend fun loadGuideForProfile(
        profileId: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Channel> =
        guideBuildMutex.withLock { loadGuideLocked(profileId, onProgress) }

    /** Troubleshooting: drop the day's caches (including negative entries) and
     *  rebuild the guide from the network. Holds the build mutex across the
     *  clear so an in-flight load can't republish pre-clear data. */
    suspend fun rebuildGuide(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<Channel> =
        guideBuildMutex.withLock {
            syncManager.deletePoolsForDay(activeProfileId(), scheduleEpochDay())
            harvestCache.clear()
            guideCache.clear()
            loadGuideLocked(activeProfileId(), onProgress)
        }

    private suspend fun loadGuideLocked(
        profileId: String,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<Channel> {
        val startedAtMs = System.currentTimeMillis()
        addonRepository.ensureSeeded(profileId)
        val enabled = channelLineup(profileId).filter { it.enabled }
        val key = guideCacheKey(LocalDate.now(scheduleZone).toEpochDay(), profileId, lineupSignature(enabled))
        guideCache.get(key)?.let {
            onProgress(enabled.size, enabled.size)
            Log.i(TAG, "⚡ Guide cache HIT: ${enabled.size} channels in ${System.currentTimeMillis() - startedAtMs}ms")
            return it
        }
        // Shared schedules: seed today's pools from Supabase before harvesting, so
        // this device regenerates the same guide the first builder produced.
        syncManager.pullPoolsQuietly(profileId, scheduleEpochDay()) { sourceKey, pool ->
            harvestCache.write(sourceKey, pool)
        }
        val populated = populateLiveTimelineForChannels(enabled, onProgress)
        guideCache.put(key, populated)
        Log.i(TAG, "✅ Guide populated: ${enabled.size} channels in ${System.currentTimeMillis() - startedAtMs}ms")
        return populated
    }

    /** Lineup metadata only (no populate) — seeds addons first. */
    suspend fun loadChannelMetadata(): List<Channel> {
        addonRepository.ensureSeeded(activeProfileId())
        return channelLineup()
    }

    /** The channel currently scheduling [programId] in the live guide, or null if none. */
    suspend fun channelForProgram(programId: String): Channel? =
        loadGuide().firstOrNull { ch -> ch.programs.any { it.id == programId } }

    /**
     * Title count for a catalog from today's HarvestCache, or null if it hasn't been harvested yet
     * (the guide populates the cache on load). Feeds the Channel Manager SOURCE card's LIBRARY figure.
     */
    suspend fun cachedItemCount(sourceKey: String, sortingRule: String): Int? {
        val cacheKey = "$sourceKey:${sortingRule.uppercase()}"
        return harvestCache.read(cacheKey)?.size
    }

    // ── Channel Manager edits (write overrides keyed by sourceKey) ──────────────
    fun renameChannel(sourceKey: String, name: String) =
        overrideStore.upsert(activeProfileId(), sourceKey) { it.copy(name = name.ifBlank { null }) }

    fun setChannelCategory(sourceKey: String, label: String) =
        overrideStore.upsert(activeProfileId(), sourceKey) { it.copy(category = label) }

    fun setChannelMode(sourceKey: String, mode: String) =
        overrideStore.upsert(activeProfileId(), sourceKey) { it.copy(mode = mode) }

    fun setChannelMarathonLimit(sourceKey: String, limit: Int?) =
        overrideStore.upsert(activeProfileId(), sourceKey) { it.copy(marathonLimit = limit) }

    fun setChannelEnabled(sourceKey: String, enabled: Boolean) =
        overrideStore.upsert(activeProfileId(), sourceKey) { it.copy(enabled = enabled) }

    // ── Sub-channel (extra option) toggles ───────────────────────────────────────

    /** The drill-down rows for a catalog channel: every extra option with its selection state. */
    fun subChannelToggles(sourceKey: String): List<SubChannelToggle> {
        val src = channelLineup().firstOrNull { it.sourceKey == sourceKey }?.source ?: return emptyList()
        val addon = addonRepository.getAddons(activeProfileId()).firstOrNull { it.id == src.addonId }
            ?: return emptyList()
        val catalog = addon.manifest.catalogs.firstOrNull { it.type == src.type && it.id == src.catalogId }
            ?: return emptyList()
        // Option channels of a normal catalog don't drill (their base row is the drill
        // point) — but required-extra catalogs have no base row (spec §3), so their
        // option channels carry the catalog's drill-down instead.
        if (src.option != null && !catalog.requiresExtra) return emptyList()
        return optionToggles(addon, catalog)
    }

    fun setSubChannelOption(sourceKey: String, extraName: String, option: String, selected: Boolean) {
        val src = channelLineup().firstOrNull { it.sourceKey == sourceKey }?.source ?: return
        addonRepository.setOptionSelected(
            activeProfileId(), src.addonId, src.type, src.catalogId, extraName, option, selected)
    }

    fun setAllSubChannelOptions(sourceKey: String, selected: Boolean) {
        val src = channelLineup().firstOrNull { it.sourceKey == sourceKey }?.source ?: return
        addonRepository.setAllOptionsSelected(activeProfileId(), src.addonId, src.type, src.catalogId, selected)
    }

    /** Move a channel up/down and lock explicit orderIndex for every channel. */
    fun moveChannel(sourceKey: String, up: Boolean) {
        val profileId = activeProfileId()
        val order = channelLineup().map { it.sourceKey }
        val newOrder = reorderedSourceKeys(order, sourceKey, up)
        if (newOrder == order) return
        val all = overrideStore.getAll(profileId).toMutableMap()
        newOrder.forEachIndexed { idx, sk ->
            all[sk] = (all[sk] ?: com.jpenner.vibetuner.data.model.ChannelOverride()).copy(orderIndex = idx)
        }
        overrideStore.save(profileId, all)
    }

    // ── My List ──────────────────────────────────────────────────────────────
    private val myListFile = File(context.filesDir, "vibetuner_mylist.json")
    private val myListIds: MutableSet<String> by lazy { readMyList() }

    /** True when [programId] is on the user's My List. */
    fun inMyList(programId: String): Boolean = programId in myListIds

    /** Add or remove [programId] from My List and persist the change. */
    suspend fun setInMyList(programId: String, inList: Boolean): Unit = withContext(Dispatchers.IO) {
        if (inList) myListIds.add(programId) else myListIds.remove(programId)
        runCatching { myListFile.writeText(JSONArray(myListIds.toList()).toString()) }
            .onFailure { Log.e("VibeTuner Storage", "🔥 My List write error: ${it.message}") }
    }

    private fun readMyList(): MutableSet<String> {
        if (!myListFile.exists()) return mutableSetOf()
        return runCatching {
            val arr = JSONArray(myListFile.readText())
            (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
        }.getOrDefault(mutableSetOf())
    }

    suspend fun populateLiveTimelineForChannels(
        channels: List<Channel>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Channel> {
        val newlyHarvested = java.util.concurrent.ConcurrentHashMap<String, List<RawMediaItem>>()
        return withContext(Dispatchers.IO) {
            val profileId = activeProfileId()
            val addonBaseById = addonRepository.getAddons(profileId).associate { it.id to it.baseUrl }
            val total = channels.size
            val doneCount = java.util.concurrent.atomic.AtomicInteger(0)
            // Harvest a few channels at a time so one slow addon doesn't serialize the whole guide.
            val harvestSemaphore = Semaphore(4)

            coroutineScope {
                channels.map { channel ->
                    async {
                        val result = if (!channel.enabled) {
                            channel.copy(programs = emptyList())
                        } else {
                            val pool = harvestSemaphore.withPermit {
                                harvestPoolForChannel(channel, addonBaseById, newlyHarvested)
                            }
                            if (pool.isEmpty()) {
                                channel.copy(programs = emptyList())
                            } else {
                                val chronological = channel.sortingRule.uppercase() == "CHRONOLOGICAL"
                                val epochDay = scheduleEpochDay()
                                val startPointers =
                                    if (chronological) marathonProgressStore.startPointersFor(profileId, channel.id, epochDay)
                                    else emptyMap()
                                // Movies fall out as singleton "shows", so one round of
                                // 1-episode blocks airs the whole pool before any repeat.
                                val day = buildDaySchedule(
                                    channelId = channel.id,
                                    epochDay = epochDay,
                                    perShow = groupEpisodesByShow(pool),
                                    limit = if (chronological) channel.marathonLimit else 1,
                                    startPointers = startPointers,
                                )
                                if (chronological) {
                                    marathonProgressStore.save(profileId, channel.id, epochDay, startPointers, day.endPointers)
                                }
                                channel.copy(programs = assemble24HourEpgTimeline(channel.id, day.items))
                            }
                        }
                        onProgress(doneCount.incrementAndGet(), total)
                        result
                    }
                }.awaitAll()
            }.also {
                // Harvest writes are memory-only; persist the day's pools in one pass.
                harvestCache.flush()
                syncManager.pushPoolsAsync(activeProfileId(), scheduleEpochDay(), newlyHarvested.toMap())
            }
        }
    }

    private fun assemble24HourEpgTimeline(channelId: String, sourcePool: List<RawMediaItem>): List<Program> {
        val compiledTimelinePrograms = mutableListOf<Program>()
        var currentTimelinePointerMs = LocalDate.now(scheduleZone)
            .atStartOfDay(scheduleZone)
            .toInstant()
            .toEpochMilli()

        val endTimelineBoundaryMs = currentTimelinePointerMs + (24 * 60 * 60 * 1000)
        val dayStartMs = currentTimelinePointerMs
        var currentItemIndex = 0
        val timeFormatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone(scheduleZone)
        }

        while (currentTimelinePointerMs < endTimelineBoundaryMs && sourcePool.isNotEmpty()) {
            val mediaItem = sourcePool[currentItemIndex % sourcePool.size]
            val durationMs = (mediaItem.durationMinutes * 60 * 1000).toLong()

            val computedEndTimeMs = currentTimelinePointerMs + durationMs
            val startTimeStr = timeFormatter.format(java.util.Date(currentTimelinePointerMs))
            val endTimeStr = timeFormatter.format(java.util.Date(computedEndTimeMs))
            val slotMinutes = ((currentTimelinePointerMs - dayStartMs) / 60_000L).toInt()

            compiledTimelinePrograms.add(
                Program(
                    // Deterministic per channel+slot so the same airing keeps its id
                    // across guide reloads on the same day (daily-seeded pool order +
                    // fixed durations make the slot offset stable). Lets My List and
                    // detail-by-id survive a refresh, unlike a random UUID.
                    id = "$channelId@$slotMinutes",
                    title = mediaItem.title,
                    description = mediaItem.description,
                    startTimeMillis = currentTimelinePointerMs,
                    endTimeMillis = computedEndTimeMs,
                    startMinutes = slotMinutes,
                    imageUrl = mediaItem.backdropUrl ?: mediaItem.posterUrl ?: "",
                    mediaType = mediaItem.mediaType,
                    rating = "",
                    displayTimeSlot = "$startTimeStr - $endTimeStr",
                    durationMinutes = mediaItem.durationMinutes.toInt(),
                    imdbId = mediaItem.imdbId,
                    posterUrl = mediaItem.posterUrl,
                    backdropUrl = mediaItem.backdropUrl,
                    episodeTitle = mediaItem.episodeTitle,
                    originalAirDate = mediaItem.originalAirDate
                )
            )
            currentTimelinePointerMs += durationMs
            currentItemIndex++
        }
        return compiledTimelinePrograms
    }

    private suspend fun harvestPoolForChannel(
        channel: Channel,
        addonBaseById: Map<String, String>,
        newlyHarvested: MutableMap<String, List<RawMediaItem>>,
    ): List<RawMediaItem> {
        val cacheKey = "${channel.id}:${channel.sortingRule.uppercase()}"
        harvestCache.read(cacheKey)?.let {
            Log.i(TAG, "📦 ${channel.name}: harvest cache HIT (${it.size} items)")
            return it
        }

        if (channel.sourceType != SourceType.STREMIO_CATALOG) return emptyList()
        val coords = parseStremioSource(channel.sourceValue) ?: return emptyList()
        val baseUrl = addonBaseById[coords.addonId] ?: return emptyList()

        val fetchStartMs = System.currentTimeMillis()
        val metas = catalogSource.fetchCatalog(
            baseUrl, coords.type, coords.catalogId,
            extra = coords.extraName?.let { e -> coords.option?.let { o -> e to o } },
        ).distinctBy { it.imdbId }
        // Episodes carry their series meta's runtime; movies (in either mode) may still
        // hold the 0-sentinel and need their real length from /meta.
        val pool = resolveUnknownRuntimes(
            baseUrl, coords.type,
            if (channel.sortingRule.uppercase() == "CHRONOLOGICAL") expandSeries(baseUrl, metas) else metas,
        )
        Log.i(TAG, "🌐 ${channel.name}: harvest cache MISS, fetched ${pool.size} items in ${System.currentTimeMillis() - fetchStartMs}ms")

        // Cache empty pools too (negative caching, short TTL) so a dead catalog
        // doesn't re-fetch with full network timeouts on every launch.
        harvestCache.write(cacheKey, pool)
        newlyHarvested[cacheKey] = pool
        return pool
    }

    /**
     * Fill in each 0-sentinel runtime from the item's `/meta` resource (concurrency-capped);
     * items whose addon has no runtime anywhere fall back to a per-type default.
     */
    private suspend fun resolveUnknownRuntimes(
        baseUrl: String,
        catalogType: String,
        metas: List<RawMediaItem>,
    ): List<RawMediaItem> = coroutineScope {
        val semaphore = Semaphore(5)
        metas.map { item ->
            async {
                if (item.durationMinutes > 0f) item
                else {
                    val real = semaphore.withPermit {
                        metaSource.fetchRuntimeMinutes(baseUrl, catalogType, item.imdbId)
                    }
                    item.copy(durationMinutes = real ?: if (item.mediaType == "TV Show") 45f else 115f)
                }
            }
        }.awaitAll()
    }

    /** Expand every "TV Show" catalog item into its episodes (concurrency-capped); movies pass through. */
    private suspend fun expandSeries(baseUrl: String, metas: List<RawMediaItem>): List<RawMediaItem> =
        coroutineScope {
            val semaphore = Semaphore(5)
            metas.map { item ->
                async {
                    if (item.mediaType == "TV Show")
                        semaphore.withPermit { metaSource.fetchSeriesEpisodes(baseUrl, item.imdbId) }
                    else listOf(item)
                }
            }.awaitAll().flatten()
        }
}

// ── Stremio catalog channel identity ───────────────────────────────────────────

/** Fetch coordinates for a STREMIO_CATALOG channel, packed into Channel.sourceValue. */
data class StremioSource(
    val addonId: String,
    val type: String,
    val catalogId: String,
    val extraName: String? = null,   // e.g. "genre" for option sub-channels
    val option: String? = null,      // e.g. "Action"
)

private fun optionSuffix(extraName: String?, option: String?, sep: String): String =
    if (extraName != null && option != null) "$sep$extraName=$option" else ""

fun stremioSourceValue(addonId: String, catalog: StremioCatalog, extraName: String? = null, option: String? = null): String =
    "$addonId|${catalog.type}|${catalog.id}" + optionSuffix(extraName, option, "|")

/** Stable mirror identity used to reconcile auto-imported catalog channels across syncs. */
fun stremioSourceKey(addonId: String, catalog: StremioCatalog, extraName: String? = null, option: String? = null): String =
    "stremio:$addonId:${catalog.type}:${catalog.id}" + optionSuffix(extraName, option, ":")

fun parseStremioSource(sourceValue: String): StremioSource? {
    val parts = sourceValue.split("|")
    if (parts.size !in 3..4 || parts.take(3).any { it.isBlank() }) return null
    var extraName: String? = null
    var option: String? = null
    if (parts.size == 4) {
        val eq = parts[3].indexOf('=')
        if (eq <= 0 || eq == parts[3].length - 1) return null
        extraName = parts[3].substring(0, eq)
        option = parts[3].substring(eq + 1)
    }
    return StremioSource(parts[0], parts[1], parts[2], extraName, option)
}

/** Logo-tile initials derived from a channel name (e.g. "Apex Sports" -> "AS"). Data-layer twin of channelInitials. */
fun channelAbbreviation(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(3).uppercase()
        else -> words.take(2).joinToString("") { it.first().uppercaseChar().toString() }
    }
}

/**
 * Single-slot in-memory hold of the last fully-populated guide. Keyed by a string that
 * encodes day + profile + lineup signature, so a stale key never returns a stale guide.
 */
class GuideCache {
    @Volatile private var entry: Pair<String, List<Channel>>? = null

    fun get(key: String): List<Channel>? = entry?.takeIf { it.first == key }?.second

    fun put(key: String, value: List<Channel>) {
        entry = key to value
    }

    fun clear() {
        entry = null
    }
}

/**
 * Stable identity of an enabled lineup. Changes whenever a scheduling-relevant field changes
 * (rename, category via name, sorting, marathon limit, order, enabled), so the in-memory guide
 * cache self-invalidates on any Channel-Manager edit or profile/addon change.
 */
fun lineupSignature(channels: List<Channel>): String =
    channels.joinToString("|") { c ->
        "${c.sourceKey}:${c.name}:${c.category.name}:${c.sortingRule}:${c.marathonLimit ?: -1}:${c.orderIndex}:${c.enabled}"
    }

/** Cache key for a fully-populated guide build: day + profile + lineup signature. */
fun guideCacheKey(day: Long, profileId: String, signature: String): String = "$day:$profileId:$signature"
