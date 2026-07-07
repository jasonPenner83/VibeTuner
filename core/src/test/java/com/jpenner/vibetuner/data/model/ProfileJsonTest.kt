package com.jpenner.vibetuner.data.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileJsonTest {

    private val profile = Profile(
        id = "p_test1234",
        name = "Alex",
        gradient = listOf(Color(0xFF46507A), Color(0xFF2A3052)),
        type = ProfileType.TEEN,
        maxRating = Rating.TV14,
        allowedTypes = setOf("movie", "series"),
        pinHash = "aabb:ccdd",
        requirePin = true,
        adultContent = false,
    )

    @Test
    fun `round trip preserves every field`() {
        val restored = profileFromJson(profile.toJson())!!
        assertEquals(profile, restored)
    }

    @Test
    fun `missing id returns null`() {
        assertNull(profileFromJson(org.json.JSONObject()))
    }

    @Test
    fun `null pinHash survives round trip`() {
        val open = profile.copy(pinHash = null, requirePin = false)
        assertEquals(open, profileFromJson(open.toJson()))
    }
}
