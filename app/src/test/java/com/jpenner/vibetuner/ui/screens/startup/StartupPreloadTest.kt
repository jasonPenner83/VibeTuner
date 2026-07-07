package com.jpenner.vibetuner.ui.screens.startup

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPreloadTest {

    @Test fun stageFor_maps_progress_across_all_five_stages() {
        assertEquals(0, stageFor(0f))
        assertEquals(0, stageFor(0.19f))
        assertEquals(1, stageFor(0.2f))
        assertEquals(2, stageFor(0.5f))
        assertEquals(3, stageFor(0.75f))
        assertEquals(4, stageFor(0.99f))
    }

    @Test fun stageFor_clamps_to_valid_indices() {
        assertEquals(0, stageFor(-1f))                 // below 0
        assertEquals(4, stageFor(1f))                  // exactly full -> last, not size
        assertEquals(4, stageFor(2f))                  // above 1
        assertEquals(STARTUP_STAGES.lastIndex, stageFor(1f))
    }

    @Test fun preload_progress_is_monotonic_and_completes_done() = runBlocking {
        val warmer = GuideWarmer { onProgress ->
            onProgress(0, 1, "Alex", 1, 3)
            onProgress(0, 1, "Alex", 2, 3)
            onProgress(0, 1, "Alex", 3, 3)
        }
        val states = mutableListOf<StartupUiState>()

        runStartupPreload(warmer, timeoutMs = 5_000L) { states.add(it) }

        val progresses = states.map { it.progress }
        assertEquals("progress must never decrease", progresses.sorted(), progresses)
        assertTrue("must finish done", states.last().done)
        assertEquals(1f, states.last().progress, 0.0001f)
    }

    @Test fun preload_progress_spans_0_to_1_across_multiple_profiles() = runBlocking {
        val warmer = GuideWarmer { onProgress ->
            onProgress(0, 3, "Alex", 1, 1)
            onProgress(1, 3, "Sam", 1, 1)
            onProgress(2, 3, "Jamie", 1, 1)
        }
        val states = mutableListOf<StartupUiState>()

        runStartupPreload(warmer, timeoutMs = 5_000L) { states.add(it) }

        val progresses = states.map { it.progress }
        assertEquals("progress must never decrease across profiles", progresses.sorted(), progresses)
        assertEquals("last profile's completion must reach full progress", 1f, states.last().progress, 0.0001f)
        assertEquals("Jamie", states[states.size - 2].profileName)
        assertEquals(2, states[states.size - 2].profileIndex)
        assertEquals(3, states[states.size - 2].profileTotal)
    }

    @Test fun preload_completes_via_timeout_when_warmer_hangs() = runBlocking {
        val warmer = GuideWarmer { _ -> delay(10_000) } // never finishes within the cap
        val states = mutableListOf<StartupUiState>()

        runStartupPreload(warmer, timeoutMs = 50L) { states.add(it) }

        assertTrue("timeout must still resolve done", states.last().done)
    }

    @Test fun preload_completes_when_warmer_throws() = runBlocking {
        val warmer = GuideWarmer { _ -> throw RuntimeException("boom") }
        val states = mutableListOf<StartupUiState>()
        runStartupPreload(warmer, timeoutMs = 5_000L) { states.add(it) }
        assertTrue("a thrown warmer must still resolve done", states.last().done)
    }
}
