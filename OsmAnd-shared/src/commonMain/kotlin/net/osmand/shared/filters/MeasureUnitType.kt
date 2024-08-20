package net.osmand.shared.filters

//import net.osmand.shared.filters.KMetricsConstants;

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
			TIME_DURATION -> "shared_string_minute_lowercase"
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
//		val settings = app.settings
//		val mc = settings.METRIC_SYSTEM.get()
		return when (mc) {
			KMetricsConstants.MILES_AND_METERS,
			KMetricsConstants.MILES_AND_FEET,
			KMetricsConstants.MILES_AND_YARDS -> "mile"

			KMetricsConstants.NAUTICAL_MILES_AND_FEET,
			KMetricsConstants.NAUTICAL_MILES_AND_METERS -> "nm"

			KMetricsConstants.KILOMETERS_AND_METERS -> "km"
		}
	}

	private fun getPowerUnits(): String {
		return "power_watts_unit"
	}

	private fun getTemperatureUnits(): String {
		return "degree_celsius"
	}

	private fun getBPMUnits(): String {
		return "beats_per_minute_short"
	}

	private fun getRotationUnits(): String {
		return "revolutions_per_minute_unit"
	}

	private fun getSpeedUnits(mc: KMetricsConstants): String {
		return when (mc) {
			KMetricsConstants.MILES_AND_METERS,
			KMetricsConstants.MILES_AND_FEET,
			KMetricsConstants.MILES_AND_YARDS -> "mile_per_hour"

			KMetricsConstants.NAUTICAL_MILES_AND_FEET,
			KMetricsConstants.NAUTICAL_MILES_AND_METERS -> "nm_h"

			KMetricsConstants.KILOMETERS_AND_METERS -> "km_h"
		}
	}

	private fun getAltitudeUnits(mc: KMetricsConstants): String {
		val useFeet =
			mc == KMetricsConstants.MILES_AND_FEET || mc == KMetricsConstants.MILES_AND_YARDS || mc == KMetricsConstants.NAUTICAL_MILES_AND_FEET
		return if (useFeet) {
			"foot"
		} else {
			"m"
		}
	}

	fun getBaseValueFromFormatted(mc: KMetricsConstants, value: String): Float {
		val metricsConstants: MetricsConstants = app.settings.METRIC_SYSTEM.get()
		val mode = app.settings.applicationMode
		val speedConstant = app.settings.SPEED_SYSTEM.getModeValue(mode)
		return when (this) {
			SPEED -> OsmAndFormatter.convertSpeedToMetersPerSecond(
				value.toFloat(),
				speedConstant)

			ALTITUDE -> OsmAndFormatter.getMetersFromFormattedAltitudeValue(
				value.toFloat(),
				metricsConstants)

			DISTANCE -> OsmAndFormatter.convertToMeters(
				value.toFloat(),
				metricsConstants)

			TIME_DURATION -> value.toFloat() * 1000 * 60

			else -> value.toFloat()
		}

	}

	fun getFormattedValue(mc: KMetricsConstants, value: String): OsmAndFormatter.FormattedValue {
		val metricsConstants: MetricsConstants = app.settings.METRIC_SYSTEM.get()
		return when (this) {
			SPEED -> OsmAndFormatter.getFormattedSpeedValue(value.toFloat(), app)
			ALTITUDE -> OsmAndFormatter.getFormattedAltitudeValue(
				value.toDouble(),
				app,
				metricsConstants)

			DISTANCE -> OsmAndFormatter.getFormattedDistanceValue(
				value.toFloat(),
				app)

			TIME_DURATION -> OsmAndFormatter.FormattedValue(
				value.toFloat() / 1000 / 60,
				value,
				"")

			else -> OsmAndFormatter.FormattedValue(value.toFloat(), value, "")
		}
	}
}