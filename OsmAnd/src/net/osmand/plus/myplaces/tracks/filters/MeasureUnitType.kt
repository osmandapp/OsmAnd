package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.utils.OsmAndFormatter

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

	fun getFilterUnitText(app: OsmandApplication): String {
		val unitResId = when (this) {
			TIME_DURATION -> R.string.shared_string_minute_lowercase
			DISTANCE -> getDistanceUnits(app)
			ALTITUDE -> getAltitudeUnits(app)
			SPEED -> getSpeedUnits(app)
			TEMPERATURE -> getTemperatureUnits()
			ROTATIONS -> getRotationUnits()
			BPM -> getBPMUnits()
			POWER -> getPowerUnits()
			else -> 0
		}

		return if (unitResId > 0) app.getString(unitResId) else ""
	}

	private fun getDistanceUnits(app: OsmandApplication): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		return when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> R.string.mile

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm

			MetricsConstants.KILOMETERS_AND_METERS -> R.string.km
		}
	}

	private fun getPowerUnits(): Int {
		return R.string.power_watts_unit
	}

	private fun getTemperatureUnits(): Int {
		return R.string.degree_celsius
	}

	private fun getBPMUnits(): Int {
		return R.string.beats_per_minute_short
	}

	private fun getRotationUnits(): Int {
		return R.string.revolutions_per_minute_unit
	}

	private fun getSpeedUnits(app: OsmandApplication): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		return when (mc!!) {
			MetricsConstants.MILES_AND_METERS,
			MetricsConstants.MILES_AND_FEET,
			MetricsConstants.MILES_AND_YARDS -> R.string.mile_per_hour

			MetricsConstants.NAUTICAL_MILES_AND_FEET,
			MetricsConstants.NAUTICAL_MILES_AND_METERS -> R.string.nm_h

			MetricsConstants.KILOMETERS_AND_METERS -> R.string.km_h
		}
	}

	private fun getAltitudeUnits(app: OsmandApplication): Int {
		val settings = app.settings
		val mc = settings.METRIC_SYSTEM.get()
		val useFeet =
			mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.MILES_AND_YARDS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET
		return if (useFeet) {
			R.string.foot
		} else {
			R.string.m
		}
	}

	fun getBaseValueFromFormatted(app: OsmandApplication, value: String): Float {
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

	fun getFormattedValue(app: OsmandApplication, value: String): OsmAndFormatter.FormattedValue {
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