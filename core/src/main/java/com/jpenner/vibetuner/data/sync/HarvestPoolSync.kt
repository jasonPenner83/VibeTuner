package com.jpenner.vibetuner.data.sync

import android.util.Log
import com.jpenner.vibetuner.data.cache.rawItemsFromJson
import com.jpenner.vibetuner.data.cache.rawItemsToJson
import com.jpenner.vibetuner.data.model.RawMediaItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.json.JSONArray

@Serializable
data class HarvestPoolRow(
    @SerialName("profile_id") val profileId: String,
    val day: Long,
    @SerialName("source_key") val sourceKey: String,
    val payload: JsonElement,
)

private const val TAG = "VibeTuner Sync"
private const val TABLE = "harvest_pools"

/**
 * Day-scoped shared harvest pools: the first device to harvest a channel each
 * day defines its pool for every device (insert with ignoreDuplicates), which
 * makes the deterministic schedule pipeline produce identical guides.
 */
class HarvestPoolSync(private val client: SupabaseClient) {

    /** Fetch today's pools and hand each to [write] (the HarvestCache seeder). Returns the count. */
    suspend fun pullInto(
        profileId: String,
        day: Long,
        write: suspend (sourceKey: String, pool: List<RawMediaItem>) -> Unit,
    ): Int {
        val rows = client.from(TABLE).select {
            filter {
                eq("profile_id", profileId)
                eq("day", day)
            }
        }.decodeList<HarvestPoolRow>()
        rows.forEach { row ->
            runCatching { rawItemsFromJson(JSONArray(row.payload.toString())) }
                .onSuccess { pool -> if (pool.isNotEmpty()) write(row.sourceKey, pool) }
                .onFailure { Log.e(TAG, "🔥 pool parse error (${row.sourceKey}): ${it.message}") }
        }
        Log.i(TAG, "📦 pulled ${rows.size} shared pools for day $day")
        return rows.size
    }

    /** First-writer-wins push of freshly harvested pools; empty pools (negative caches) are skipped. */
    suspend fun push(profileId: String, day: Long, pools: Map<String, List<RawMediaItem>>) {
        val rows = pools.filterValues { it.isNotEmpty() }.map { (sourceKey, pool) ->
            HarvestPoolRow(
                profileId = profileId,
                day = day,
                sourceKey = sourceKey,
                payload = Json.parseToJsonElement(rawItemsToJson(pool).toString()),
            )
        }
        if (rows.isNotEmpty()) {
            client.from(TABLE).upsert(rows) { ignoreDuplicates = true }
            Log.i(TAG, "📤 pushed ${rows.size} pools for day $day")
        }
        // Opportunistic cleanup: previous days' pools are dead weight.
        client.from(TABLE).delete { filter { lt("day", day) } }
    }

    /** Rebuild Guide: drop today's shared pools so the rebuild repropagates fresh ones. */
    suspend fun deleteDay(profileId: String, day: Long) {
        client.from(TABLE).delete {
            filter {
                eq("profile_id", profileId)
                eq("day", day)
            }
        }
    }
}
