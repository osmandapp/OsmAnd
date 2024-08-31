package net.osmand.shared.filters

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

	fun getFilterUnitText(mc: KMetricsConstants): String {
		val unitResId = when (this) {
			TIME_DURATION -> PlatformUtil.getStringResource("shared_string_minute_lowercase")
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

	private fun getDistanceUnits(mc: KMetricsConstants): String {
		return when (mc) {
			KMetricsConstants.MILES_AND_METERS,
			KMetricsConstants.MILES_AND_FEET,
			KMetricsConstants.MILES_AND_YARDS -> PlatformUtil.getStringResource("mile")

			KMetricsConstants.NAUTICAL_MILES_AND_FEET,
			KMetricsConstants.NAUTICAL_MILES_AND_METERS -> PlatformUtil.getStringResource("nm")

			KMetricsConstants.KILOMETERS_AND_METERS -> PlatformUtil.getStringResource("km")
		}
	}

	private fun getPowerUnits(): String {
		return PlatformUtil.getStringResource("power_watts_unit")
	}

	private fun getTemperatureUnits(): String {
		return PlatformUtil.getStringResource("degree_celsius")
	}

	private fun getBPMUnits(): String {
		return PlatformUtil.getStringResource("beats_per_minute_short")
	}

	private fun getRotationUnits(): String {
		return PlatformUtil.getStringResource("revolutions_per_minute_unit")
	}

	private fun getSpeedUnits(mc: KMetricsConstants): String {
		return when (mc) {
			KMetricsConstants.MILES_AND_METERS,
			KMetricsConstants.MILES_AND_FEET,
			KMetricsConstants.MILES_AND_YARDS -> PlatformUtil.getStringResource("mile_per_hour")

			KMetricsConstants.NAUTICAL_MILES_AND_FEET,
			KMetricsConstants.NAUTICAL_MILES_AND_METERS -> PlatformUtil.getStringResource("nm_h")

			KMetricsConstants.KILOMETERS_AND_METERS -> PlatformUtil.getStringResource("km_h")
		}
	}

	private fun getAltitudeUnits(mc: KMetricsConstants): String {
		val useFeet =
			mc == KMetricsConstants.MILES_AND_FEET || mc == KMetricsConstants.MILES_AND_YARDS || mc == KMetricsConstants.NAUTICAL_MILES_AND_FEET
		return if (useFeet) {
			PlatformUtil.getStringResource("foot")
		} else {
			PlatformUtil.getStringResource("m")
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