package com.jpenner.vibetuner.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncStateStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun store(name: String = "state.json") = SyncStateStore(File(tmp.root, name))

    @Test
    fun `unknown doc returns defaults`() {
        val s = store()
        assertEquals(DocState(), s.get("p1", "profile"))
    }

    @Test
    fun `markDirty then clearDirty sets lastSeen`() {
        val s = store()
        s.markDirty("p1", "addons")
        assertTrue(s.get("p1", "addons").dirty)
        s.clearDirty("p1", "addons", "2026-07-04T12:00:00+00:00")
        val st = s.get("p1", "addons")
        assertFalse(st.dirty)
        assertEquals("2026-07-04T12:00:00+00:00", st.lastSeen)
    }

    @Test
    fun `markDeleted flags a dirty tombstone on the profile kind`() {
        val s = store()
        s.markDeleted("p1")
        val st = s.get("p1", "profile")
        assertTrue(st.dirty)
        assertTrue(st.deleted)
    }

    @Test
    fun `state survives a new instance over the same file`() {
        val f = File(tmp.root, "persist.json")
        SyncStateStore(f).markDirty("p2", "overrides")
        assertTrue(SyncStateStore(f).get("p2", "overrides").dirty)
    }

    @Test
    fun `remove drops all kinds for the profile`() {
        val s = store()
        s.markDirty("p1", "profile"); s.markDirty("p1", "addons"); s.markDirty("p2", "profile")
        s.remove("p1")
        assertEquals(setOf("p2:profile"), s.all().keys)
    }

    @Test
    fun `clearAll empties the store`() {
        val s = store()
        s.markDirty("p1", "profile")
        s.clearAll()
        assertTrue(s.all().isEmpty())
    }
}
