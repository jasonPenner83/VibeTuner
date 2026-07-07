package com.jpenner.vibetuner.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressTrackerTest {

    @Test fun short_press_does_not_fire_long_press_and_onUp_reports_short() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val tracker = LongPressTracker(scope, thresholdMs = 200L)
        var longPressFired = false

        tracker.onDown(onLongPress = { longPressFired = true })
        delay(20L) // released well before the threshold
        val wasShortPress = tracker.onUp()

        delay(250L) // give a wrongly-still-running job a chance to fire
        assertTrue("onUp must report a short press when released early", wasShortPress)
        assertFalse("long-press callback must not fire for a short press", longPressFired)
    }

    @Test fun holding_past_threshold_fires_long_press() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val tracker = LongPressTracker(scope, thresholdMs = 30L)
        var longPressFired = false

        tracker.onDown(onLongPress = { longPressFired = true })
        delay(80L) // held well past the threshold
        val wasShortPress = tracker.onUp()

        assertFalse("onUp must not report a short press once long-press fired", wasShortPress)
        assertTrue("long-press callback must have fired", longPressFired)
    }
}
