package net.osmand.plus.plugins.aistracker

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.settings.enums.MetricsConstants
import java.util.Locale

/**
 * Formats the AIS timeouts, distances and identifiers the way the plugin screens show them.
 * AIS works in nautical miles; the label follows the unit setting of the app, so a user on
 * metric units is not left guessing what "nm" means.
 */
object AisFormatter {

	private const val METERS_IN_NAUTICAL_MILE = 1852f
	private const val MMSI_LENGTH = 9

	@JvmStatic
	fun formatMinutes(app: OsmandApplication, minutes: Int): String =
		app.getString(R.string.ltr_or_rtl_combine_via_space, minutes.toString(),
			app.getString(R.string.shared_string_minute_lowercase))

	/** @param mode the profile whose units are used - the one the screen was opened for. */
	@JvmStatic
	fun formatNauticalMiles(app: OsmandApplication, miles: Float, mode: ApplicationMode): String {
		val metrics = app.settings.METRIC_SYSTEM.getModeValue(mode)
		if (metrics == MetricsConstants.NAUTICAL_MILES_AND_METERS
			|| metrics == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			return app.getString(R.string.ltr_or_rtl_combine_via_space, formatMiles(miles),
				app.getString(R.string.nm))
		}
		return OsmAndFormatter.getFormattedDistance(miles * METERS_IN_NAUTICAL_MILE, app, null, metrics)
	}

	/**
	 * A MMSI is always 9 digits, but it is stored as a number - a coast station or a group
	 * identifier starts with zeros that the number does not keep.
	 */
	@JvmStatic
	fun formatMmsi(mmsi: Int): String = String.format(Locale.US, "%0${MMSI_LENGTH}d", mmsi)

	/** The scale of the safe distance runs from 0.02 to 2 miles, so the precision follows the value. */
	private fun formatMiles(value: Float): String = when {
		value >= 1f -> String.format(Locale.getDefault(), "%.0f", value)
		value >= 0.1f -> String.format(Locale.getDefault(), "%.1f", value)
		else -> String.format(Locale.getDefault(), "%.2f", value)
	}
}
