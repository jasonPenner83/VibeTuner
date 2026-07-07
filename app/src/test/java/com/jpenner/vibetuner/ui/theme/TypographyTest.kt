package com.jpenner.vibetuner.ui.theme

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun displayLarge_usesSoraAt64sp() {
        assertEquals(64.sp, AerialTypography.displayLarge.fontSize)
        assertEquals(Sora, AerialTypography.displayLarge.fontFamily)
    }

    @Test
    fun bodyLarge_usesSoraAt17sp() {
        assertEquals(17.sp, AerialTypography.bodyLarge.fontSize)
        assertEquals(Sora, AerialTypography.bodyLarge.fontFamily)
    }

    @Test
    fun monoLabel_usesMono() {
        assertEquals(Mono, MonoLabel.fontFamily)
        assertEquals(Mono, MonoMeta.fontFamily)
    }
}
