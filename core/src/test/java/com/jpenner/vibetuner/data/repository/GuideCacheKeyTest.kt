package com.jpenner.vibetuner.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GuideCacheKeyTest {

    @Test fun key_combines_day_profile_and_signature() {
        assertEquals("100:alex:sig-abc", guideCacheKey(day = 100, profileId = "alex", signature = "sig-abc"))
    }

    @Test fun key_differs_by_profile_for_same_day_and_signature() {
        val a = guideCacheKey(day = 100, profileId = "alex", signature = "sig-abc")
        val b = guideCacheKey(day = 100, profileId = "sam", signature = "sig-abc")
        assertNotEquals(a, b)
    }
}
