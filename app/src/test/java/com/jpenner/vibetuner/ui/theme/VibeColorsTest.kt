package com.jpenner.vibetuner.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class VibeColorsTest {
    @Test
    fun midnightAzure_hasExpectedTokenValues() {
        assertEquals(Color(0xFF0D0F14), MidnightAzure.bg)
        assertEquals(Color(0xFF151821), MidnightAzure.surface)
        assertEquals(Color(0xFF1D212C), MidnightAzure.raised)
        assertEquals(Color(0xFF2A2F3C), MidnightAzure.line)
        assertEquals(Color(0xFFEEF1F6), MidnightAzure.txt)
        assertEquals(Color(0xFFA7AFBF), MidnightAzure.txt2)
        assertEquals(Color(0xFF6B7384), MidnightAzure.txt3)
        assertEquals(Color(0xFF3D9BFF), MidnightAzure.accent)
        assertEquals(Color(0xFF8FC7FF), MidnightAzure.accent2)
        assertEquals(Color(0xFF06121F), MidnightAzure.accentInk)
        assertEquals(Color(0x803D9BFF), MidnightAzure.glow)
        assertEquals(Color(0xFFFF5D5D), MidnightAzure.danger)
        assertEquals(Color(0xFF46D18B), MidnightAzure.success)
        assertEquals(Color(0xFFFFB454), MidnightAzure.warn)
    }
}
