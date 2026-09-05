package net.osmand.plus.plugins.aistracker

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.shared.settings.enums.MetricsConstants

/**
 * Formats the AIS timeouts and distances the way the plugin screens show them.
 * AIS works in nautical miles; the label follows the unit setting of the app, so a user on
 * metric units is not left guessing what "nm" means.
 */
object AisFormatter {

	private const val METERS_IN_NAUTICAL_MILE = 1852f

	@JvmStatic
	fun formatMinutes(app: OsmandApplication, minutes: Int): String =
		app.getString(R.string.ais_minutes_short, minutes)

	@JvmStatic
	fun formatNauticalMiles(app: OsmandApplication, miles: Float): String {
		val metrics = app.settings.METRIC_SYSTEM.get()
		if (metrics == MetricsConstants.NAUTICAL_MILES_AND_METERS
			|| metrics == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			return app.getString(R.string.ais_nautical_miles_short, trimZeros(miles))
		}
		return OsmAndFormatter.getFormattedDistance(miles * METERS_IN_NAUTICAL_MILE, app)
	}

	private fun trimZeros(value: Float): String {
		val text = when {
			value >= 1f -> String.format("%.0f", value)
			value >= 0.1f -> String.format("%.1f", value)
			else -> String.format("%.2f", value)
		}
		return text
	}
}
