package com.jpenner.vibetuner.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Generic hold-to-act tracker: [onDown] schedules [onLongPress] after [thresholdMs] of holding;
 * [onUp] cancels that pending job if the key was released first, and reports whether this was a
 * short press so the caller can dispatch its normal short-press action instead. Mirrors the
 * existing hold-to-open-switcher idiom in PlayerScreen's Enter-key handling, generalized and made
 * independently testable.
 */
class LongPressTracker(
    private val scope: CoroutineScope,
    private val thresholdMs: Long = 500L,
) {
    private var job: Job? = null
    private var longPressFired = false

    /** Call on key-down (ignore auto-repeat key-downs for a press already being tracked). */
    fun onDown(onLongPress: () -> Unit) {
        longPressFired = false
        job = scope.launch {
            delay(thresholdMs)
            longPressFired = true
            onLongPress()
        }
    }

    /** Call on key-up. Returns true if this was a short press (long-press did not fire), so the
     *  caller should still dispatch its normal short-press action. */
    fun onUp(): Boolean {
        job?.cancel()
        job = null
        return !longPressFired
    }
}
