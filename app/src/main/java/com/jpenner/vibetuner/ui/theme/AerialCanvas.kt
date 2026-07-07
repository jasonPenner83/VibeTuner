package com.jpenner.vibetuner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The Aerial design handoff is authored on a 1920-px-wide canvas where px ≈ dp.
 * Rather than scale every value by hand, this overrides the density for [content]
 * so that "1.dp == 1 design px" — the kit's literal dp/sp values then render at the
 * intended size on any TV (e.g. 960×540 dp @ density 2.0 -> effective density 1.0).
 *
 * Wrap every Aerial screen's root in this so scale stays uniform across packages
 * (Guide, Home, Detail, Settings). It is the single source of truth for the canvas
 * mapping — change [Dimens.DesignCanvasWidth] here, not per-screen.
 */
@Composable
fun AerialCanvas(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val screenWidthPx = base.density * LocalConfiguration.current.screenWidthDp
    val designDensity = Density(
        density = screenWidthPx / Dimens.DesignCanvasWidth * Dimens.DesignScale,
        fontScale = base.fontScale,
    )
    CompositionLocalProvider(LocalDensity provides designDensity, content = content)
}
