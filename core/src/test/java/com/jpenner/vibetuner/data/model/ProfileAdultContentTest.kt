package com.jpenner.vibetuner.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAdultContentTest {
    private fun profile(
        type: ProfileType = ProfileType.ADULT,
        adultContent: Boolean = false,
        requirePin: Boolean = false,
    ) = Profile(
        id = "p1", name = "Alex", gradient = Profile.SampleGradients[0],
        type = type, adultContent = adultContent, requirePin = requirePin,
    )

    @Test fun allowsAdult_only_when_enabled_and_type_adult() {
        assertTrue(profile(adultContent = true).allowsAdult)
        assertFalse(profile(adultContent = false).allowsAdult)
        assertFalse(profile(type = ProfileType.TEEN, adultContent = true).allowsAdult)
        assertFalse(profile(type = ProfileType.KID, adultContent = true).allowsAdult)
    }

    @Test fun summary_appends_18plus_when_effectively_unlocked() {
        assertEquals("Adult · 18+", profile(adultContent = true, requirePin = true).summary())
    }

    @Test fun summary_hides_18plus_when_suppressed_by_type() {
        assertEquals("Teen", profile(type = ProfileType.TEEN, adultContent = true, requirePin = true).summary())
    }

    @Test fun summary_unchanged_for_unrestricted_profile() {
        assertEquals("All content", profile().summary())
    }
}
