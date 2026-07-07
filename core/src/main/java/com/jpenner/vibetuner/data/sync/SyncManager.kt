package com.jpenner.vibetuner.data.sync

import android.content.Context
import android.util.Log
import com.jpenner.vibetuner.data.model.RawMediaItem
import com.jpenner.vibetuner.data.repository.AddonRepository
import com.jpenner.vibetuner.data.repository.ChannelOverrideStore
import com.jpenner.vibetuner.data.repository.ProfileRepository
import com.jpenner.vibetuner.data.repository.ProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private const val TAG = "VibeTuner Sync"
private const val PUSH_DEBOUNCE_MS = 3_000L

/**
 * Process-wide sync coordinator: listens to store writes via SyncHooks, marks
 * docs dirty, debounces pushes, and runs pulls on demand. Everything no-ops
 * when Supabase isn't configured or nobody is signed in — the app works
 * exactly as before sync existed.
 */
class SyncManager private constructor(context: Context) : SyncListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isConfigured: Boolean = SupabaseProvider.isConfigured

    private val stateStore = SyncStateStore(File(context.filesDir, "vibetuner_sync_state.json"))

    val auth: SupabaseAuthManager? =
        if (isConfigured) SupabaseAuthManager(SupabaseProvider.client, scope) else null

    private val engine: SyncEngine? = if (isConfigured) SyncEngine(
        client = SupabaseProvider.client,
        stateStore = stateStore,
        profileStore = ProfileStore.get(context),
        addonRepository = AddonRepository(context),
        overrideStore = ChannelOverrideStore(context),
        profileRepository = ProfileRepository(context),
    ) else null

    private val poolSync: HarvestPoolSync? =
        if (isConfigured) HarvestPoolSync(SupabaseProvider.client) else null

    private val _lastSyncMs = MutableStateFlow<Long?>(null)
    val lastSyncMs: StateFlow<Long?> = _lastSyncMs.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private var pushJob: Job? = null
    private var poolPushJob: Job? = null

    init {
        SyncHooks.listener = this
    }

    private val active: Boolean get() = engine != null && auth?.isSignedIn == true

    // ── SyncListener (store write hooks) ──────────────────────────────────────

    override fun onDocChanged(profileId: String, kind: String) {
        if (!active) return
        stateStore.markDirty(profileId, kind)
        schedulePush()
    }

    override fun onProfileDeleted(profileId: String) {
        if (!active) return
        stateStore.markDeleted(profileId)
        schedulePush()
    }

    private fun schedulePush() {
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(PUSH_DEBOUNCE_MS)
            runSync { engine!!.pushDirty() }
        }
    }

    // ── entry points ──────────────────────────────────────────────────────────

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        val a = auth ?: return Result.failure(IllegalStateException("Sync not configured"))
        return a.signInWithGoogleIdToken(idToken).onSuccess { pullAllQuietly() }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            pushJob?.cancel()
            if (active) runCatching { engine!!.pushDirty() }
                .onFailure { Log.e(TAG, "🔥 final flush failed: ${it.message}") }
            auth?.signOut()
            stateStore.clearAll()
        }
        _lastSyncMs.value = null
        _syncError.value = null
    }

    /** Startup-safe pull: bounded, never throws, silently skips when signed out. */
    suspend fun pullAllQuietly(timeoutMs: Long = 8_000) {
        if (!active) return
        withTimeoutOrNull(timeoutMs) { withContext(Dispatchers.IO) { runSync { engine!!.pullAll() } } }
    }

    fun pullAllAsync() {
        if (!active) return
        scope.launch { runSync { engine!!.pullAll() } }
    }

    /** Settings' explicit Sync Now: pull + push, error surfaced to [syncError]. */
    suspend fun syncNow() {
        if (!active) return
        withContext(Dispatchers.IO) { runSync { engine!!.pullAll() } }
    }

    // ── harvest pools (shared schedules) ──────────────────────────────────────

    /** Guide-build pre-pull: bounded and silent — a miss just means we harvest locally. */
    suspend fun pullPoolsQuietly(
        profileId: String,
        day: Long,
        write: suspend (sourceKey: String, pool: List<RawMediaItem>) -> Unit,
    ) {
        if (!active) return
        withTimeoutOrNull(8_000) {
            runCatching { poolSync!!.pullInto(profileId, day, write) }
                .onFailure { Log.e(TAG, "🔥 pool pull failed: ${it.message}") }
        }
    }

    /** Fire-and-forget push of this build's fresh harvests. */
    fun pushPoolsAsync(profileId: String, day: Long, pools: Map<String, List<RawMediaItem>>) {
        if (!active || pools.isEmpty()) return
        poolPushJob = scope.launch {
            runCatching { poolSync!!.push(profileId, day, pools) }
                .onFailure { Log.e(TAG, "🔥 pool push failed: ${it.message}") }
        }
    }

    /** Rebuild Guide clears the day's shared pools before repopulating. */
    suspend fun deletePoolsForDay(profileId: String, day: Long) {
        if (!active) return
        withContext(Dispatchers.IO) {
            // A push from the just-finished build may still be in flight; let it land
            // first so the delete below can't be trailed by a stale re-insert.
            withTimeoutOrNull(10_000) { poolPushJob?.join() }
            runCatching { poolSync!!.deleteDay(profileId, day) }
                .onFailure { Log.e(TAG, "🔥 pool delete failed: ${it.message}") }
        }
    }

    private suspend fun runSync(block: suspend () -> Unit) {
        try {
            block()
            _lastSyncMs.value = System.currentTimeMillis()
            _syncError.value = null
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.e(TAG, "🔥 sync failed: ${t.message}")
            _syncError.value = t.message ?: "Sync failed"
        }
    }

    companion object {
        @Volatile private var instance: SyncManager? = null

        fun get(context: Context): SyncManager =
            instance ?: synchronized(this) {
                instance ?: SyncManager(context.applicationContext).also { instance = it }
            }
    }
}
