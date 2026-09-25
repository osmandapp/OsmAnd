package net.osmand.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

import net.osmand.wear.R

/**
 * The watch takes its colours from the phone app's palette rather than carrying its own.
 *
 * OsmAnd/res/values/colors.xml is staged into this module at build time by copyPhoneResources, so
 * retuning a colour in the main app reaches the watch by itself — and a renamed entry breaks the
 * build here instead of quietly leaving the watch on a stale shade.
 */
object OsmAndWearColors {

	val Background: Color
		@Composable get() = colorResource(R.color.widget_background_color_dark)

	val ChipContainer: Color
		@Composable get() = colorResource(R.color.chip_bg_dark)

	val ChipContent: Color
		@Composable get() = colorResource(R.color.text_color_primary_dark)

	val HeaderContent: Color
		@Composable get() = colorResource(R.color.text_color_secondary_dark)

	val Accent: Color
		@Composable get() = colorResource(R.color.icon_color_osmand_light)

	/** Trip recording follows OsmAnd's blue accent rather than the orange of the main menu. */
	val AltChipContainer: Color
		@Composable get() = colorResource(R.color.btn_bg_accent_pressed_dark)

	val AltAccent: Color
		@Composable get() = colorResource(R.color.icon_color_active_light)

	val RecActive: Color
		@Composable get() = colorResource(R.color.recording_color)

	val RecPaused: Color
		@Composable get() = colorResource(R.color.profile_icon_color_yellow_light)

	/** Neutral button on a confirmation dialog, where the accent belongs to the confirm side. */
	val DialogDismiss: Color
		@Composable get() = colorResource(R.color.inactive_buttons_and_links_bg_dark)
}

@Composable
fun OsmAndWearTheme(content: @Composable () -> Unit) {
	val accent = OsmAndWearColors.Accent
	val background = OsmAndWearColors.Background
	val chipContainer = OsmAndWearColors.ChipContainer
	val chipContent = OsmAndWearColors.ChipContent
	val headerContent = OsmAndWearColors.HeaderContent

	// Remembered rather than rebuilt: MaterialTheme hands the scheme down through a
	// CompositionLocal, and a fresh instance on every recomposition would invalidate the whole
	// subtree below it.
	val colorScheme = remember(accent, background, chipContainer, chipContent, headerContent) {
		ColorScheme(
			primary = accent,
			onPrimary = background,
			background = background,
			onBackground = chipContent,
			surfaceContainer = chipContainer,
			onSurface = chipContent,
			onSurfaceVariant = headerContent
		)
	}

	MaterialTheme(colorScheme = colorScheme, content = content)
}
