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

	/** Used by the profile picker and the Stop button in the mockups. */
	val AltChipContainer = Color(0xFF12192E)
	val AltAccent = Color(0xFF237BFF)
}

@Composable
fun OsmAndWearTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = ColorScheme(
			primary = OsmAndWearColors.Accent,
			onPrimary = OsmAndWearColors.Background,
			background = OsmAndWearColors.Background,
			onBackground = OsmAndWearColors.ChipContent,
			surfaceContainer = OsmAndWearColors.ChipContainer,
			onSurface = OsmAndWearColors.ChipContent,
			onSurfaceVariant = OsmAndWearColors.HeaderContent
		),
		content = content
	)
}
