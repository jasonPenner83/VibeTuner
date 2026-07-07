package com.jpenner.vibetuner.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/* Uses a store on a temp file: happy paths never touch android.util.Log,
 * so plain JUnit works — same as the other org.json-based store tests. */
class ProfileStoreAdultContentTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun fileStore(name: String = "profiles.json") = ProfileStore(File(tmp.root, name))

    @Test fun enabling_adult_forces_require_pin() {
        val s = fileStore()
        val id = s.profilesNow().first().id
        s.setAdultContent(id, true)
        assertTrue(s.byId(id)!!.adultContent)
        assertTrue(s.byId(id)!!.requirePin)
    }

    @Test fun disabling_require_pin_clears_adult() {
        val s = fileStore()
        val id = s.profilesNow().first().id
        s.setPin(id, "1234")
        s.setAdultContent(id, true)
        s.setRequirePin(id, false)
        assertFalse(s.byId(id)!!.adultContent)
    }

    @Test fun clearing_pin_clears_adult() {
        val s = fileStore()
        val id = s.profilesNow().first().id
        s.setPin(id, "1234")
        s.setAdultContent(id, true)
        s.clearPin(id)
        assertFalse(s.byId(id)!!.adultContent)
        assertFalse(s.byId(id)!!.requirePin)
    }

    @Test fun adult_flag_round_trips_through_the_file() {
        val file = File(tmp.root, "profiles.json")
        val s = ProfileStore(file)
        val id = s.profilesNow().first().id
        s.setAdultContent(id, true)
        assertTrue(ProfileStore(file).byId(id)!!.adultContent)
    }

    @Test fun legacy_json_without_the_key_loads_as_false() {
        val file = File(tmp.root, "profiles.json")
        file.writeText(
            """[{"id":"p1","name":"Alex","gradient":[-12244870],"type":"ADULT","maxRating":"TVMA","allowedTypes":[],"requirePin":false}]"""
        )
        assertFalse(ProfileStore(file).byId("p1")!!.adultContent)
    }
}
