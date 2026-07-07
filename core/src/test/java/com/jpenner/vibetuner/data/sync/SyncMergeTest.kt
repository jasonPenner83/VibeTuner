package com.jpenner.vibetuner.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private val t1 = "2026-07-04T10:00:00.123456+00:00"   // PostgREST timestamp shape
    private val t2 = "2026-07-04T11:00:00+00:00"

    // ── isNewer ──────────────────────────────────────────────────────────────

    @Test
    fun `isNewer handles microsecond offsets and null lastSeen`() {
        assertTrue(isNewer(t2, t1))
        assertFalse(isNewer(t1, t2))
        assertFalse(isNewer(t1, t1))
        assertTrue(isNewer(t1, null))
    }

    // ── decideDocAction ──────────────────────────────────────────────────────

    @Test
    fun `dirty local always pushes`() {
        assertEquals(
            SyncAction.PushLocal,
            decideDocAction(t2, false, DocState(dirty = true, lastSeen = t1), localExists = true),
        )
    }

    @Test
    fun `remote tombstone with local profile applies tombstone`() {
        assertEquals(
            SyncAction.ApplyTombstone,
            decideDocAction(t2, true, DocState(lastSeen = t1), localExists = true),
        )
    }

    @Test
    fun `remote tombstone without local profile skips`() {
        assertEquals(
            SyncAction.Skip,
            decideDocAction(t2, true, DocState(), localExists = false),
        )
    }

    @Test
    fun `newer remote applies`() {
        assertEquals(
            SyncAction.ApplyRemote,
            decideDocAction(t2, false, DocState(lastSeen = t1), localExists = true),
        )
    }

    @Test
    fun `never-seen remote applies`() {
        assertEquals(
            SyncAction.ApplyRemote,
            decideDocAction(t1, false, DocState(), localExists = false),
        )
    }

    @Test
    fun `already-seen remote skips`() {
        assertEquals(
            SyncAction.Skip,
            decideDocAction(t1, false, DocState(lastSeen = t1), localExists = true),
        )
    }

    @Test
    fun `no remote row but local doc exists pushes (first-sync seeding)`() {
        assertEquals(
            SyncAction.PushLocal,
            decideDocAction(null, false, DocState(), localExists = true),
        )
    }

    @Test
    fun `no remote row and no local doc skips`() {
        assertEquals(
            SyncAction.Skip,
            decideDocAction(null, false, DocState(), localExists = false),
        )
    }
}
