package com.jpenner.vibetuner.data.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelOverrideJsonTest {
    @Test fun round_trips_all_fields() {
        val ov = ChannelOverride("My Name", "Horror", "CHRONOLOGICAL", 3, false, 5)
        val restored = channelOverrideFromJson(ov.toJson())
        assertEquals("My Name", restored.name)
        assertEquals("Horror", restored.category)
        assertEquals("CHRONOLOGICAL", restored.mode)
        assertEquals(3, restored.marathonLimit)
        assertEquals(false, restored.enabled)
        assertEquals(5, restored.orderIndex)
    }

    @Test fun preserves_nulls_including_marathon_none_and_enabled() {
        val ov = ChannelOverride(name = "Only Name")
        val restored = channelOverrideFromJson(ov.toJson())
        assertEquals("Only Name", restored.name)
        assertNull(restored.category)
        assertNull(restored.mode)
        assertNull(restored.marathonLimit) // None
        assertNull(restored.enabled)
        assertNull(restored.orderIndex)
    }

    @Test fun reads_missing_keys_as_null() {
        val restored = channelOverrideFromJson(JSONObject("{}"))
        assertNull(restored.name)
        assertNull(restored.enabled)
        assertNull(restored.orderIndex)
    }
}
