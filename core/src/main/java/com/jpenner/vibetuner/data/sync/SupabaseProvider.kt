package com.jpenner.vibetuner.data.sync

import com.jpenner.vibetuner.core.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * One SupabaseClient per process. [isConfigured] is false when local.properties
 * lacks the supabase.* keys (e.g. a fresh checkout) — every sync entry point
 * checks it and no-ops, so the app never crashes on a missing config.
 */
object SupabaseProvider {

    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)        // persists + auto-refreshes the session on Android
            install(Postgrest)
        }
    }
}
