package net.osmand.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

import net.osmand.wear.R

/**
 * OsmAnd's Material 3 roles, wired into the Wear Compose colour scheme.
 *
 * The phone declares the same roles as theme attributes in OsmandMaterialDarkTheme
 * (colorPrimary, colorSurfaceContainer, colorOnSurfaceVariant and the rest), each pointing at a
 * colour in OsmAnd/res/values/colors.xml. That file is staged into this module at build time by
 * copyPhoneResources, so the watch reads the same entries the phone's theme does — the attributes
 * themselves are not readable here, since this module carries none of the phone's styles.
 *
 * Every slot is filled on purpose. Wear's ColorScheme has 29 of them and defaults the rest to
 * Google's baseline lavender, so a half-filled scheme leaves any component that reaches for an
 * unset role — an outline, a dim state, an error — drawing in a palette that is not OsmAnd's.
 */
@Composable
private fun osmAndColorScheme(): ColorScheme {
	val primary = colorResource(R.color.primary_dark)
	val onPrimary = colorResource(R.color.on_primary_dark)
	val primaryContainer = colorResource(R.color.primary_container_dark)
	val onPrimaryContainer = colorResource(R.color.on_primary_container_dark)

	val secondary = colorResource(R.color.secondary_dark)
	val onSecondary = colorResource(R.color.on_secondary_dark)
	val secondaryContainer = colorResource(R.color.secondary_container_dark)
	val onSecondaryContainer = colorResource(R.color.on_secondary_container_dark)

	val brandAccent = colorResource(R.color.brand_accent_dark)

	val surface = colorResource(R.color.surface_dark)
	val surfaceContainer = colorResource(R.color.surface_container_dark)
	val surfaceContainerHighest = colorResource(R.color.surface_container_highest_dark)
	val onSurface = colorResource(R.color.on_surface_dark)
	val onSurfaceVariant = colorResource(R.color.on_surface_variant_dark)

	val outline = colorResource(R.color.outline_dark)
	val outlineVariant = colorResource(R.color.outline_variant_dark)

	val error = colorResource(R.color.error_dark)
	val errorIcon = colorResource(R.color.error_icon_dark)
	val onError = colorResource(R.color.on_error_dark)

	// Remembered rather than rebuilt: MaterialTheme hands the scheme down through a
	// CompositionLocal, and a fresh instance on every recomposition would invalidate the whole
	// subtree below it.
	return remember(
		primary, onPrimary, primaryContainer, onPrimaryContainer,
		secondary, onSecondary, secondaryContainer, onSecondaryContainer,
		brandAccent, surface, surfaceContainer, surfaceContainerHighest,
		onSurface, onSurfaceVariant, outline, outlineVariant, error, errorIcon, onError
	) {
		ColorScheme(
			primary = primary,
			// OsmAnd has no dim tier — its pressed variants go brighter, where Wear's dim goes
			// darker — so the muted companion blue of the same family stands in for it.
			primaryDim = secondary,
			primaryContainer = primaryContainer,
			onPrimary = onPrimary,
			onPrimaryContainer = onPrimaryContainer,

			secondary = secondary,
			secondaryDim = secondary,
			secondaryContainer = secondaryContainer,
			onSecondary = onSecondary,
			onSecondaryContainer = onSecondaryContainer,

			// OsmAnd declares no tertiary; its brand accent is the third accent in practice,
			// and it is what the menu icons are drawn in.
			tertiary = brandAccent,
			tertiaryDim = brandAccent,
			// No orange-tinted container exists either, so this degrades to a neutral surface
			// rather than to Google's baseline.
			tertiaryContainer = surfaceContainerHighest,
			onTertiary = onPrimary,
			onTertiaryContainer = onSurface,

			// The phone maps colorSurfaceContainerLow to the same colour as colorSurfaceContainer,
			// and colorSurfaceContainerHigh to its highest tier.
			surfaceContainerLow = surfaceContainer,
			surfaceContainer = surfaceContainer,
			surfaceContainerHigh = surfaceContainerHighest,
			onSurface = onSurface,
			onSurfaceVariant = onSurfaceVariant,

			outline = outline,
			outlineVariant = outlineVariant,

			// The phone sets android:colorBackground to colorSurface.
			background = surface,
			onBackground = onSurface,

			error = error,
			errorDim = errorIcon,
			errorContainer = errorIcon,
			onError = onError,
			onErrorContainer = onError
		)
	}
}

/**
 * The two colours that name a state rather than a Material role: OsmAnd has no M3 slot for
 * "recording", and the phone's own track widget uses these same entries.
 */
object OsmAndWearColors {

	val RecActive: Color
		@Composable get() = colorResource(R.color.recording_color)

	val RecPaused: Color
		@Composable get() = colorResource(R.color.warning_dark)
}

@Composable
fun OsmAndWearTheme(content: @Composable () -> Unit) {
	MaterialTheme(colorScheme = osmAndColorScheme(), content = content)
}
