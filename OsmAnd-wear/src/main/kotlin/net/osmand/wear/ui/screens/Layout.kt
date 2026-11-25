package net.osmand.wear.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wear guidelines require margins expressed as a share of the screen, so that layouts scale
 * proportionally across watch sizes instead of clipping on the smallest ones.
 */
@Composable
fun Int.percentOfWidth(): Dp {
	val screenWidth = LocalConfiguration.current.screenWidthDp
	return (screenWidth * this / 100f).dp
}

/**
 * The vertical counterpart. A round screen takes its corners from the top and bottom rows as
 * well, and the watch's own clock sits in the first of them, so a full-height column needs its
 * own margin rather than only the horizontal one.
 */
@Composable
fun Int.percentOfHeight(): Dp {
	val screenHeight = LocalConfiguration.current.screenHeightDp
	return (screenHeight * this / 100f).dp
}
