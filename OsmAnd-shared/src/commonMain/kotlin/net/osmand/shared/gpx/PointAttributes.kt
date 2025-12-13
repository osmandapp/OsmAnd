package net.osmand.shared.gpx

import net.osmand.shared.gpx.GpxUtilities.GPXTPX_PREFIX
import net.osmand.shared.gpx.GpxUtilities.OSMAND_EXTENSIONS_PREFIX
import net.osmand.shared.obd.OBDCommand
import kotlin.reflect.KMutableProperty1

class PointAttributes(
	var distance: Float, var timeDiff: Float, var firstPoint: Boolean, var lastPoint: Boolean
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

	var intakeTemp: Float = Float.NaN
	var ambientTemp: Float = Float.NaN
	var coolantTemp: Float = Float.NaN
	var engineOilTemp: Float = Float.NaN
	var rpmSpeed: Float = Float.NaN
	var runtimeEngine: Float = Float.NaN
	var engineLoad: Float = Float.NaN
	var fuelPressure = Float.NaN
	var fuelConsumption = Float.NaN
	var fuelRemaining = Float.NaN
	var batteryVoltage = Float.NaN
	var vehicleSpeed = Float.NaN
	var throttlePosition = Float.NaN

	private var anyValueSet: Boolean = false
	private var anyVehicleMetricsSet: Boolean = false

	fun hasAnyValueSet(): Boolean = anyValueSet
	fun hasAnyMetricSetSet(): Boolean = anyVehicleMetricsSet

	private val commonAttrMap: Map<String, KMutableProperty1<PointAttributes, Float>> = mapOf(
		POINT_SPEED to PointAttributes::speed,
		POINT_ELEVATION to PointAttributes::elevation,
		SENSOR_TAG_HEART_RATE to PointAttributes::heartRate,
		SENSOR_TAG_SPEED to PointAttributes::sensorSpeed,
		SENSOR_TAG_CADENCE to PointAttributes::bikeCadence,
		SENSOR_TAG_BIKE_POWER to PointAttributes::bikePower,
		SENSOR_TAG_TEMPERATURE_W to PointAttributes::waterTemperature,
		SENSOR_TAG_TEMPERATURE_A to PointAttributes::airTemperature,
		DEV_RAW_ZOOM to PointAttributes::rawZoom,
		DEV_ANIMATED_ZOOM to PointAttributes::animatedZoom,
		DEV_INTERPOLATION_OFFSET_N to PointAttributes::interpolationOffsetN
	)

	private val obdAttrMap: Map<String, KMutableProperty1<PointAttributes, Float>> = mapOf(
		OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND.gpxTag!! to PointAttributes::intakeTemp,
		OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND.gpxTag!! to PointAttributes::ambientTemp,
		OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND.gpxTag!! to PointAttributes::coolantTemp,
		OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND.gpxTag!! to PointAttributes::engineOilTemp,
		OBDCommand.OBD_RPM_COMMAND.gpxTag!! to PointAttributes::rpmSpeed,
		OBDCommand.OBD_ENGINE_RUNTIME_COMMAND.gpxTag!! to PointAttributes::runtimeEngine,
		OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND.gpxTag!! to PointAttributes::engineLoad,
		OBDCommand.OBD_FUEL_PRESSURE_COMMAND.gpxTag!! to PointAttributes::fuelPressure,
		OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND.gpxTag!! to PointAttributes::fuelConsumption,
		OBDCommand.OBD_FUEL_LEVEL_COMMAND.gpxTag!! to PointAttributes::fuelRemaining,
		OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND.gpxTag!! to PointAttributes::batteryVoltage,
		OBDCommand.OBD_SPEED_COMMAND.gpxTag!! to PointAttributes::vehicleSpeed,
		OBDCommand.OBD_THROTTLE_POSITION_COMMAND.gpxTag!! to PointAttributes::throttlePosition
	)

	fun getAttributeValue(tag: String): Float? {
		return commonAttrMap[tag]?.get(this) ?: obdAttrMap[tag]?.get(this)
	}

	fun setAttributeValue(tag: String, value: Float) {
		when {
			commonAttrMap.containsKey(tag) -> commonAttrMap[tag]?.set(this, value)
			obdAttrMap.containsKey(tag) -> {
				obdAttrMap[tag]?.set(this, value)
				anyVehicleMetricsSet = true
			}
		}
		anyValueSet = true
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
