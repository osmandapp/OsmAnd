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
