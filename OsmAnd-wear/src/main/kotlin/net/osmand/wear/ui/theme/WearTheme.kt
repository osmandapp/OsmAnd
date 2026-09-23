package net.osmand.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Palette sampled from the mockups attached to OsmAnd-Issues#2821.
 *
 * Two accents are in play there: warm grey chips with orange glyphs for the main menu, and
 * navy chips with blue glyphs for profile pickers and the navigation Stop action.
 */
object OsmAndWearColors {
	val Background = Color(0xFF000000)
	val Accent = Color(0xFFFF8800)
	val ChipContainer = Color(0xFF38332E)
	val ChipContent = Color(0xFFFFFFFF)
	val HeaderContent = Color(0xFFDADCE0)

	/** Used by the profile picker, trip recording and the Stop button in the mockups. */
	val AltChipContainer = Color(0xFF12192E)
	val AltAccent = Color(0xFF237BFF)

	/** Recording status colours: writing points, and holding a paused session. */
	val RecActive = Color(0xFFE5484D)
	val RecPaused = Color(0xFFE8A33D)

	/** Neutral button on a confirmation dialog, where the accent belongs to the confirm side. */
	val DialogDismiss = Color(0xFF3C3A38)
}

/**
 * Built once rather than inside the composable: MaterialTheme hands the scheme down through a
 * CompositionLocal, and a fresh instance on every recomposition invalidates the whole subtree
 * below it. The colours are constants, so there is nothing to rebuild.
 */
private val OsmAndColorScheme = ColorScheme(
	primary = OsmAndWearColors.Accent,
	onPrimary = OsmAndWearColors.Background,
	background = OsmAndWearColors.Background,
	onBackground = OsmAndWearColors.ChipContent,
	surfaceContainer = OsmAndWearColors.ChipContainer,
	onSurface = OsmAndWearColors.ChipContent,
	onSurfaceVariant = OsmAndWearColors.HeaderContent
)

@Composable
fun OsmAndWearTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = OsmAndColorScheme,
		content = content
	)
}
