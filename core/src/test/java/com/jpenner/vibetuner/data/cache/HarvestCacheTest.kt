package com.jpenner.vibetuner.data.cache

import com.jpenner.vibetuner.data.model.RawMediaItem
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HarvestCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sample = listOf(
        RawMediaItem(
            title = "Star Trek: TNG S01E01",
            description = "Maiden voyage.",
            durationMinutes = 45f,
            mediaType = "Episode",
            imdbId = "tt0092455",
            episodeTitle = "Encounter at Farpoint",
            originalAirDate = "1987-09-28",
            season = 1,
            episodeNumber = 1
        )
    )

    @Test
    fun write_then_read_same_day_round_trips_all_fields() = runBlocking {
        val file = File(tmp.root, "cache.json")
        val cache = HarvestCache(file, clock = { 100L })

        cache.write("ch1", sample)
        val read = cache.read("ch1")

        assertEquals(1, read?.size)
        val item = read!![0]
        assertEquals("Star Trek: TNG S01E01", item.title)
        assertEquals("Encounter at Farpoint", item.episodeTitle)
        assertEquals("1987-09-28", item.originalAirDate)
        assertEquals(1, item.season)
        assertEquals(1, item.episodeNumber)
        assertEquals(45f, item.durationMinutes, 0.001f)
        assertEquals("Episode", item.mediaType)
        assertEquals("tt0092455", item.imdbId)
    }

    @Test
    fun read_returns_null_when_day_rolls_over() = runBlocking {
        val file = File(tmp.root, "cache.json")
        HarvestCache(file, clock = { 100L }).apply {
            write("ch1", sample)
            flush()
        }

        val nextDay = HarvestCache(file, clock = { 101L })
        assertNull(nextDay.read("ch1"))
    }

    @Test
    fun read_returns_null_for_unknown_channel() = runBlocking {
        val file = File(tmp.root, "cache.json")
        HarvestCache(file, clock = { 100L }).apply {
            write("ch1", sample)
            flush()
        }

        val cache = HarvestCache(file, clock = { 100L })
        assertNull(cache.read("other"))
    }

    // ── Batched persistence (write is memory-only until flush) ──────────────

    @Test
    fun write_alone_does_not_touch_disk() = runBlocking {
        val file = File(tmp.root, "cache.json")
        val cache = HarvestCache(file, clock = { 100L })

        cache.write("ch1", sample)

        assertEquals(false, file.exists())
        // The writing instance still serves the entry from memory.
        assertEquals(1, cache.read("ch1")?.size)
    }

    @Test
    fun flush_persists_writes_for_new_instances() = runBlocking {
        val file = File(tmp.root, "cache.json")
        HarvestCache(file, clock = { 100L }).apply {
            write("ch1", sample)
            flush()
        }

        val fresh = HarvestCache(file, clock = { 100L })
        assertEquals(1, fresh.read("ch1")?.size)
    }

    @Test
    fun flush_prunes_entries_from_previous_days() = runBlocking {
        val file = File(tmp.root, "cache.json")
        HarvestCache(file, clock = { 100L }).apply {
            write("stale", sample)
            flush()
        }

        HarvestCache(file, clock = { 101L }).apply {
            write("today", sample)
            flush()
        }

        val persisted = file.readText()
        assertEquals(true, persisted.contains("today"))
        assertEquals(false, persisted.contains("stale"))
    }

    // ── Negative caching (empty pools) ───────────────────────────────────────

    @Test
    fun empty_pool_is_cached_and_honored_within_retry_ttl() = runBlocking {
        val file = File(tmp.root, "cache.json")
        var now = 1_000_000L
        val cache = HarvestCache(file, clock = { 100L }, nowMs = { now })

        cache.write("dead", emptyList())
        now += HarvestCache.EMPTY_RETRY_TTL_MS - 1

        val read = cache.read("dead")
        assertNotNull(read)
        assertEquals(0, read!!.size)
    }

    @Test
    fun empty_pool_expires_after_retry_ttl() = runBlocking {
        val file = File(tmp.root, "cache.json")
        var now = 1_000_000L
        val cache = HarvestCache(file, clock = { 100L }, nowMs = { now })

        cache.write("dead", emptyList())
        now += HarvestCache.EMPTY_RETRY_TTL_MS + 1

        assertNull(cache.read("dead"))
    }

    @Test
    fun empty_pool_survives_new_instance_within_ttl() = runBlocking {
        val file = File(tmp.root, "cache.json")
        var now = 1_000_000L
        HarvestCache(file, clock = { 100L }, nowMs = { now }).apply {
            write("dead", emptyList())
            flush()
        }

        now += HarvestCache.EMPTY_RETRY_TTL_MS / 2
        val fresh = HarvestCache(file, clock = { 100L }, nowMs = { now })
        assertNotNull(fresh.read("dead"))
    }

    @Test
    fun non_empty_pool_stays_valid_past_retry_ttl_same_day() = runBlocking {
        val file = File(tmp.root, "cache.json")
        var now = 1_000_000L
        val cache = HarvestCache(file, clock = { 100L }, nowMs = { now })

        cache.write("ch1", sample)
        now += HarvestCache.EMPTY_RETRY_TTL_MS * 10

        assertEquals(1, cache.read("ch1")?.size)
    }

    // ── Single-parse write-through memory ────────────────────────────────────

    @Test
    fun reads_serve_memory_after_first_load_ignoring_disk_mutation() = runBlocking {
        val file = File(tmp.root, "cache.json")
        val cache = HarvestCache(file, clock = { 100L })

        cache.write("ch1", sample)
        cache.flush()
        // Clobber the file behind the cache's back: reads must still serve memory.
        file.writeText("{ not valid json }")

        assertEquals(1, cache.read("ch1")?.size)
    }

    // ── Legacy format compatibility ──────────────────────────────────────────

    @Test
    fun legacy_entry_without_harvestedAtMs_still_reads() = runBlocking {
        val file = File(tmp.root, "cache.json")
        val legacyItem = JSONObject()
            .put("title", "Old Movie")
            .put("description", "")
            .put("durationMinutes", 90.0)
            .put("mediaType", "Movie")
            .put("imdbId", "tt0000001")
        val legacyEntry = JSONObject()
            .put("epochDay", 100L)
            .put("items", JSONArray().put(legacyItem))
        file.writeText(JSONObject().put("ch1", legacyEntry).toString())

        val cache = HarvestCache(file, clock = { 100L })
        assertEquals("Old Movie", cache.read("ch1")?.firstOrNull()?.title)
    }

    @Test
    fun clear_drops_entries_and_deletes_the_file() = runBlocking {
        val file = File(tmp.root, "cache.json")
        val cache = HarvestCache(file, clock = { 100L })

        cache.write("ch1", sample)
        cache.flush()
        cache.clear()

        assertNull(cache.read("ch1"))
        assertEquals(false, file.exists())
    }
}
