package com.jpenner.vibetuner.data.sync

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The signed-in household account, or null. */
data class SyncAccount(val email: String)

/**
 * Wraps supabase-kt auth: Google ID token in, session out. The Auth plugin
 * persists the session on-device and refreshes it, so sign-in is once per device.
 */
class SupabaseAuthManager(private val client: SupabaseClient, scope: CoroutineScope) {

    val account: StateFlow<SyncAccount?> = client.auth.sessionStatus
        .map { status ->
            (status as? SessionStatus.Authenticated)
                ?.session?.user?.email?.let { SyncAccount(it) }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val isSignedIn: Boolean get() = account.value != null

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
    }.onFailure { Log.e("VibeTuner Sync", "🔥 sign-in failed: ${it.message}") }

    suspend fun signOut() {
        runCatching { client.auth.signOut() }
            .onFailure { Log.e("VibeTuner Sync", "🔥 sign-out failed: ${it.message}") }
    }
}
