package net.osmand.shared.gpx.filters

import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.util.Localization
import net.osmand.shared.util.OsmAndFormatter
import net.osmand.shared.util.PlatformUtil

enum class MeasureUnitType {
	TIME_DURATION,
	DATE,
	SPEED,
	ALTITUDE,
	DISTANCE,
	ROTATIONS,
	POWER,
	TEMPERATURE,
	BPM,
	NONE;

	fun getFilterUnitText(mc: MetricsConstants): String {
		val unitResId = when (this) {
			TIME_DURATION -> Localization.getString("shared_string_minute_lowercase")
			DISTANCE -> getDistanceUnits(mc)
			ALTITUDE -> getAltitudeUnits(mc)
			SPEED -> getSpeedUnits(mc)
			TEMPERATURE -> getTemperatureUnits()
			ROTATIONS -> getRotationUnits()
			BPM -> getBPMUnits()
			POWER -> getPowerUnits()
			else -> ""
		}

		return unitResId
	}

	private fun getDistanceUnits(mc: MetricsConstants): String {
		return when (mc) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> Localization.getString("mile")

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> Localization.getString("nm")

			MetricsConstants.KILOMETERS_AND_METERS -> Localization.getString("km")
		}
	}

	private fun getPowerUnits(): String {
		return Localization.getString("power_watts_unit")
	}

	private fun getTemperatureUnits(): String {
		return Localization.getString("degree_celsius")
	}

	private fun getBPMUnits(): String {
		return Localization.getString("beats_per_minute_short")
	}

	private fun getRotationUnits(): String {
		return Localization.getString("revolutions_per_minute_unit")
	}

	private fun getSpeedUnits(mc: MetricsConstants): String {
		return when (mc) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> Localization.getString("mile_per_hour")

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> Localization.getString("nm_h")

			MetricsConstants.KILOMETERS_AND_METERS -> Localization.getString("km_h")
		}
	}

	private fun getAltitudeUnits(mc: MetricsConstants): String {
		val useFeet =
			mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.MILES_AND_YARDS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET
		return if (useFeet) {
			Localization.getString("foot")
		} else {
			Localization.getString("m")
		}
	}

	fun getBaseValueFromFormatted(value: String): Float {
		return when (this) {
			SPEED -> OsmAndFormatter.convertSpeedToMetersPerSecond(
				value.toFloat())

			ALTITUDE -> OsmAndFormatter.getMetersFromFormattedAltitudeValue(
				value.toFloat())

			DISTANCE -> OsmAndFormatter.convertToMeters(
				value.toFloat())

			TIME_DURATION -> value.toFloat() * 1000 * 60

			else -> value.toFloat()
		}
	}
}