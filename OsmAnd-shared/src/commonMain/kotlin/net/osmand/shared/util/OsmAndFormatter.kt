package net.osmand.shared.util

import net.osmand.shared.settings.enums.AltitudeMetrics
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants.KILOMETERS_PER_HOUR
import net.osmand.shared.settings.enums.SpeedConstants.MILES_PER_HOUR
import net.osmand.shared.settings.enums.SpeedConstants.NAUTICALMILES_PER_HOUR

object OsmAndFormatter {
	const val METERS_IN_KILOMETER = 1000f
	const val METERS_IN_ONE_MILE = 1609.344f // 1609.344

	const val METERS_IN_ONE_NAUTICALMILE = 1852f // 1852


	const val YARDS_IN_ONE_METER = 1.0936f
	const val FEET_IN_ONE_METER = YARDS_IN_ONE_METER * 3f
	const val INCHES_IN_ONE_METER = FEET_IN_ONE_METER * 12

	const val KILOGRAMS_IN_ONE_TON = 1000
	const val POUNDS_IN_ONE_KILOGRAM = 2.2046f


	fun convertSpeedToMetersPerSecond(
		formattedValueSrc: Float): Float {
		val mc = PlatformUtil.getOsmAndContext().getSpeedSystem()
		return when (mc) {
			KILOMETERS_PER_HOUR -> formattedValueSrc / 3.6f
			MILES_PER_HOUR -> (formattedValueSrc * METERS_IN_ONE_MILE / 3.6 / 1000).toFloat()
			NAUTICALMILES_PER_HOUR -> (formattedValueSrc * METERS_IN_ONE_NAUTICALMILE / 3.6 / 1000).toFloat()
			else -> formattedValueSrc
		}
	}

	fun getMetersFromFormattedAltitudeValue(altitude: Float): Float {
		val am = PlatformUtil.getOsmAndContext().getAltitudeMetric()
		val useFeet = AltitudeMetrics.FEET == am
		return if (useFeet) {
			altitude / FEET_IN_ONE_METER
		} else {
			altitude
		}
	}

	fun convertToMeters(distance: Float): Float {
		val mainUnitInMeters: Float
		val mc = PlatformUtil.getOsmAndContext().getMetricSystem()
		mainUnitInMeters =
			when (mc) {
				MetricsConstants.MILES_AND_FEET, MetricsConstants.MILES_AND_METERS -> METERS_IN_ONE_MILE
				MetricsConstants.NAUTICAL_MILES_AND_METERS, MetricsConstants.NAUTICAL_MILES_AND_FEET -> METERS_IN_ONE_NAUTICALMILE
				else -> METERS_IN_KILOMETER
			}
		return distance * mainUnitInMeters
	}


}