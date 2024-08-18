package net.osmand.shared.gpx

import net.osmand.shared.gpx.GpxUtilities.GPXTPX_PREFIX
import net.osmand.shared.gpx.GpxUtilities.OSMAND_EXTENSIONS_PREFIX

class PointAttributes(
	var distance: Float, val timeDiff: Float, val firstPoint: Boolean, val lastPoint: Boolean
) {

	companion object {

		const val SENSOR_TAG_HEART_RATE = GPXTPX_PREFIX + "hr"
		const val SENSOR_TAG_SPEED = OSMAND_EXTENSIONS_PREFIX + "speed_sensor"
		const val SENSOR_TAG_CADENCE = GPXTPX_PREFIX + "cad"
		const val SENSOR_TAG_BIKE_POWER = GPXTPX_PREFIX + "power"
		const val SENSOR_TAG_TEMPERATURE = "temp_sensor"
		const val SENSOR_TAG_TEMPERATURE_W = GPXTPX_PREFIX + "wtemp"
		const val SENSOR_TAG_TEMPERATURE_A = GPXTPX_PREFIX + "atemp"
		const val SENSOR_TAG_DISTANCE = OSMAND_EXTENSIONS_PREFIX + "bike_distance_sensor"

		const val DEV_RAW_ZOOM = "raw_zoom"
		const val DEV_ANIMATED_ZOOM = "animated_zoom"
		const val DEV_INTERPOLATION_OFFSET_N = "offset"

		const val POINT_SPEED = "point_speed"
		const val POINT_ELEVATION = "point_elevation"
	}

	var speed: Float = Float.NaN
	var elevation: Float = Float.NaN
	var heartRate: Float = Float.NaN
	var sensorSpeed: Float = Float.NaN
	var bikeCadence: Float = Float.NaN
	var bikePower: Float = Float.NaN
	var waterTemperature: Float = Float.NaN
	var airTemperature: Float = Float.NaN

	var rawZoom: Float = Float.NaN
	var animatedZoom: Float = Float.NaN
	var interpolationOffsetN: Float = Float.NaN

	fun getAttributeValue(tag: String): Float? {
		return when (tag) {
			POINT_SPEED -> speed
			POINT_ELEVATION -> elevation
			SENSOR_TAG_HEART_RATE -> heartRate
			SENSOR_TAG_SPEED -> sensorSpeed
			SENSOR_TAG_CADENCE -> bikeCadence
			SENSOR_TAG_BIKE_POWER -> bikePower
			SENSOR_TAG_TEMPERATURE -> getTemperature()
			SENSOR_TAG_TEMPERATURE_W -> waterTemperature
			SENSOR_TAG_TEMPERATURE_A -> airTemperature
			DEV_RAW_ZOOM -> rawZoom
			DEV_ANIMATED_ZOOM -> animatedZoom
			DEV_INTERPOLATION_OFFSET_N -> interpolationOffsetN
			else -> null
		}
	}

	fun setAttributeValue(tag: String, value: Float) {
		when (tag) {
			POINT_SPEED -> speed = value
			POINT_ELEVATION -> elevation = value
			SENSOR_TAG_HEART_RATE -> heartRate = value
			SENSOR_TAG_SPEED -> sensorSpeed = value
			SENSOR_TAG_CADENCE -> bikeCadence = value
			SENSOR_TAG_BIKE_POWER -> bikePower = value
			SENSOR_TAG_TEMPERATURE_W -> waterTemperature = value
			SENSOR_TAG_TEMPERATURE_A -> airTemperature = value
			DEV_RAW_ZOOM -> rawZoom = value
			DEV_ANIMATED_ZOOM -> animatedZoom = value
			DEV_INTERPOLATION_OFFSET_N -> interpolationOffsetN = value
		}
	}

	fun getTemperature(): Float {
		return if (!airTemperature.isNaN()) {
			if (!waterTemperature.isNaN()) {
				maxOf(waterTemperature, airTemperature)
			} else {
				airTemperature
			}
		} else {
			waterTemperature
		}
	}

	fun hasValidValue(tag: String): Boolean {
		val value = getAttributeValue(tag) ?: return false
		return if (tag in listOf(
				SENSOR_TAG_TEMPERATURE,
				SENSOR_TAG_TEMPERATURE_W,
				SENSOR_TAG_TEMPERATURE_A,
				POINT_ELEVATION
			)
		) {
			!value.isNaN()
		} else {
			value > 0
		}
	}
}
