package com.jpenner.vibetuner.data.repository

import com.jpenner.vibetuner.data.model.profileFromJson
import com.jpenner.vibetuner.data.sync.SyncHooks
import com.jpenner.vibetuner.data.sync.SyncListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProfileStoreSyncHooksTest {

    @get:Rule val tmp = TemporaryFolder()

    private val events = mutableListOf<String>()

    private fun listen() {
        SyncHooks.listener = object : SyncListener {
            override fun onDocChanged(profileId: String, kind: String) { events += "changed:$profileId:$kind" }
            override fun onProfileDeleted(profileId: String) { events += "deleted:$profileId" }
        }
    }

    @After fun tearDown() { SyncHooks.listener = null }

    private fun store() = ProfileStore(File(tmp.root, "profiles.json"))

    @Test
    fun `create and edit notify changed`() {
        val s = store()
        listen()
        val p = s.create("New")
        s.rename(p.id, "Renamed")
        assertTrue(events.contains("changed:${p.id}:profile"))
        assertEquals(2, events.count { it == "changed:${p.id}:profile" })
    }

    @Test
    fun `delete notifies profile deleted`() {
        val s = store()
        val p = s.create("Doomed")
        listen()
        s.delete(p.id)
        assertEquals(listOf("deleted:${p.id}"), events)
    }

    @Test
    fun `import upserts without notifying`() {
        val s = store()
        val json = s.exportProfile(s.profilesNow().first().id)!!
        listen()
        s.importProfile(json.put("name", "FromCloud"))
        assertTrue(events.isEmpty())
        assertEquals("FromCloud", s.byId(json.getString("id"))!!.name)
    }

    @Test
    fun `import creates a profile that did not exist`() {
        val s = store()
        val incoming = s.exportProfile(s.profilesNow().first().id)!!
            .put("id", "p_remote01").put("name", "Remote")
        s.importProfile(incoming)
        assertNotNull(s.byId("p_remote01"))
    }

    @Test
    fun `removeFromSync deletes silently but keeps the last profile`() {
        val s = store()
        s.create("Second")
        listen()
        val ids = s.profilesNow().map { it.id }
        ids.dropLast(1).forEach { s.removeFromSync(it) }
        s.removeFromSync(ids.last())   // guard: last profile survives
        assertEquals(1, s.profilesNow().size)
        assertNull(s.byId(ids.first()))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `export returns null for unknown id`() {
        assertNull(store().exportProfile("nope"))
    }
}
