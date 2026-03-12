package net.osmand.shared.gpx

import net.osmand.shared.gpx.GpxUtilities.GPXTPX_PREFIX
import net.osmand.shared.gpx.GpxUtilities.OSMAND_EXTENSIONS_PREFIX
import net.osmand.shared.obd.OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_ENGINE_RUNTIME_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_FUEL_LEVEL_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_FUEL_PRESSURE_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_RPM_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_SPEED_COMMAND
import net.osmand.shared.obd.OBDCommand.OBD_THROTTLE_POSITION_COMMAND

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

	private var devData: DevData? = null
	private var sensorData: SensorData? = null
	private var vehicleData: VehicleData? = null

	var heartRate: Float
		get() = sensorData?.heartRate ?: Float.NaN
		set(value) = setSensorValue(value) { heartRate = it }

	var sensorSpeed: Float
		get() = sensorData?.sensorSpeed ?: Float.NaN
		set(value) = setSensorValue(value) { sensorSpeed = it }

	var bikeCadence: Float
		get() = sensorData?.bikeCadence ?: Float.NaN
		set(value) = setSensorValue(value) { bikeCadence = it }

	var bikePower: Float
		get() = sensorData?.bikePower ?: Float.NaN
		set(value) = setSensorValue(value) { bikePower = it }

	var waterTemperature: Float
		get() = sensorData?.waterTemperature ?: Float.NaN
		set(value) = setSensorValue(value) { waterTemperature = it }

	var airTemperature: Float
		get() = sensorData?.airTemperature ?: Float.NaN
		set(value) = setSensorValue(value) { airTemperature = it }

	var rawZoom: Float
		get() = devData?.rawZoom ?: Float.NaN
		set(value) = setDevValue(value) { rawZoom = it }

	var animatedZoom: Float
		get() = devData?.animatedZoom ?: Float.NaN
		set(value) = setDevValue(value) { animatedZoom = it }

	var interpolationOffsetN: Float
		get() = devData?.interpolationOffsetN ?: Float.NaN
		set(value) = setDevValue(value) { interpolationOffsetN = it }

	var intakeTemp: Float
		get() = vehicleData?.intakeTemp ?: Float.NaN
		set(value) = setVehicleValue(value) { intakeTemp = it }

	var ambientTemp: Float
		get() = vehicleData?.ambientTemp ?: Float.NaN
		set(value) = setVehicleValue(value) { ambientTemp = it }

	var coolantTemp: Float
		get() = vehicleData?.coolantTemp ?: Float.NaN
		set(value) = setVehicleValue(value) { coolantTemp = it }

	var engineOilTemp: Float
		get() = vehicleData?.engineOilTemp ?: Float.NaN
		set(value) = setVehicleValue(value) { engineOilTemp = it }

	var rpmSpeed: Float
		get() = vehicleData?.rpmSpeed ?: Float.NaN
		set(value) = setVehicleValue(value) { rpmSpeed = it }

	var runtimeEngine: Float
		get() = vehicleData?.runtimeEngine ?: Float.NaN
		set(value) = setVehicleValue(value) { runtimeEngine = it }

	var engineLoad: Float
		get() = vehicleData?.engineLoad ?: Float.NaN
		set(value) = setVehicleValue(value) { engineLoad = it }

	var fuelPressure: Float
		get() = vehicleData?.fuelPressure ?: Float.NaN
		set(value) = setVehicleValue(value) { fuelPressure = it }

	var fuelConsumption: Float
		get() = vehicleData?.fuelConsumption ?: Float.NaN
		set(value) = setVehicleValue(value) { fuelConsumption = it }

	var fuelRemaining: Float
		get() = vehicleData?.fuelRemaining ?: Float.NaN
		set(value) = setVehicleValue(value) { fuelRemaining = it }

	var batteryVoltage: Float
		get() = vehicleData?.batteryVoltage ?: Float.NaN
		set(value) = setVehicleValue(value) { batteryVoltage = it }

	var vehicleSpeed: Float
		get() = vehicleData?.vehicleSpeed ?: Float.NaN
		set(value) = setVehicleValue(value) { vehicleSpeed = it }

	var throttlePosition: Float
		get() = vehicleData?.throttlePosition ?: Float.NaN
		set(value) = setVehicleValue(value) { throttlePosition = it }

	fun hasAnyValueSet(): Boolean = !speed.isNaN() || !elevation.isNaN() ||
			sensorData?.hasAnyValueSet() == true || devData?.hasAnyValueSet() == true || vehicleData?.hasAnyValueSet() == true

	fun getAttributeValue(tag: String): Float? =
		when (tag) {
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

			OBD_AIR_INTAKE_TEMP_COMMAND.gpxTag -> intakeTemp
			OBD_AMBIENT_AIR_TEMPERATURE_COMMAND.gpxTag -> ambientTemp
			OBD_ENGINE_COOLANT_TEMP_COMMAND.gpxTag -> coolantTemp
			OBD_ENGINE_OIL_TEMPERATURE_COMMAND.gpxTag -> engineOilTemp
			OBD_RPM_COMMAND.gpxTag -> rpmSpeed
			OBD_ENGINE_RUNTIME_COMMAND.gpxTag -> runtimeEngine
			OBD_CALCULATED_ENGINE_LOAD_COMMAND.gpxTag -> engineLoad
			OBD_FUEL_PRESSURE_COMMAND.gpxTag -> fuelPressure
			OBD_FUEL_CONSUMPTION_RATE_COMMAND.gpxTag -> fuelConsumption
			OBD_FUEL_LEVEL_COMMAND.gpxTag -> fuelRemaining
			OBD_BATTERY_VOLTAGE_COMMAND.gpxTag -> batteryVoltage
			OBD_SPEED_COMMAND.gpxTag -> vehicleSpeed
			OBD_THROTTLE_POSITION_COMMAND.gpxTag -> throttlePosition

			else -> null
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

			OBD_AIR_INTAKE_TEMP_COMMAND.gpxTag -> intakeTemp = value
			OBD_AMBIENT_AIR_TEMPERATURE_COMMAND.gpxTag -> ambientTemp = value
			OBD_ENGINE_COOLANT_TEMP_COMMAND.gpxTag -> coolantTemp = value
			OBD_ENGINE_OIL_TEMPERATURE_COMMAND.gpxTag -> engineOilTemp = value
			OBD_RPM_COMMAND.gpxTag -> rpmSpeed = value
			OBD_ENGINE_RUNTIME_COMMAND.gpxTag -> runtimeEngine = value
			OBD_CALCULATED_ENGINE_LOAD_COMMAND.gpxTag -> engineLoad = value
			OBD_FUEL_PRESSURE_COMMAND.gpxTag -> fuelPressure = value
			OBD_FUEL_CONSUMPTION_RATE_COMMAND.gpxTag -> fuelConsumption = value
			OBD_FUEL_LEVEL_COMMAND.gpxTag -> fuelRemaining = value
			OBD_BATTERY_VOLTAGE_COMMAND.gpxTag -> batteryVoltage = value
			OBD_SPEED_COMMAND.gpxTag -> vehicleSpeed = value
			OBD_THROTTLE_POSITION_COMMAND.gpxTag -> throttlePosition = value
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
		return when (tag) {
			SENSOR_TAG_TEMPERATURE,
			SENSOR_TAG_TEMPERATURE_W,
			SENSOR_TAG_TEMPERATURE_A,
			POINT_ELEVATION -> !value.isNaN()

			else -> value > 0f
		}
	}

	private fun ensureSensorData() = sensorData ?: SensorData().also { sensorData = it }

	private fun ensureDevData() = devData ?: DevData().also { devData = it }

	private fun ensureVehicleData() = vehicleData ?: VehicleData().also { vehicleData = it }

	private inline fun setSensorValue(value: Float, setter: SensorData.(Float) -> Unit) {
		val data = sensorData
		if (!value.isNaN() || data != null) {
			(data ?: ensureSensorData()).setter(value)
		}
	}

	private inline fun setDevValue(value: Float, setter: DevData.(Float) -> Unit) {
		val data = devData
		if (!value.isNaN() || data != null) {
			(data ?: ensureDevData()).setter(value)
		}
	}

	private inline fun setVehicleValue(value: Float, setter: VehicleData.(Float) -> Unit) {
		val data = vehicleData
		if (!value.isNaN() || data != null) {
			(data ?: ensureVehicleData()).setter(value)
		}
	}

	private class SensorData {
		var heartRate: Float = Float.NaN
		var sensorSpeed: Float = Float.NaN
		var bikeCadence: Float = Float.NaN
		var bikePower: Float = Float.NaN
		var waterTemperature: Float = Float.NaN
		var airTemperature: Float = Float.NaN

		fun hasAnyValueSet(): Boolean =
			!heartRate.isNaN() ||
					!sensorSpeed.isNaN() ||
					!bikeCadence.isNaN() ||
					!bikePower.isNaN() ||
					!waterTemperature.isNaN() ||
					!airTemperature.isNaN()
	}

	private class DevData {
		var rawZoom: Float = Float.NaN
		var animatedZoom: Float = Float.NaN
		var interpolationOffsetN: Float = Float.NaN

		fun hasAnyValueSet(): Boolean = !rawZoom.isNaN() || !animatedZoom.isNaN() || !interpolationOffsetN.isNaN()
	}

	private class VehicleData {
		var intakeTemp: Float = Float.NaN
		var ambientTemp: Float = Float.NaN
		var coolantTemp: Float = Float.NaN
		var engineOilTemp: Float = Float.NaN
		var rpmSpeed: Float = Float.NaN
		var runtimeEngine: Float = Float.NaN
		var engineLoad: Float = Float.NaN
		var fuelPressure: Float = Float.NaN
		var fuelConsumption: Float = Float.NaN
		var fuelRemaining: Float = Float.NaN
		var batteryVoltage: Float = Float.NaN
		var vehicleSpeed: Float = Float.NaN
		var throttlePosition: Float = Float.NaN

		fun hasAnyValueSet(): Boolean =
			!intakeTemp.isNaN() ||
					!ambientTemp.isNaN() ||
					!coolantTemp.isNaN() ||
					!engineOilTemp.isNaN() ||
					!rpmSpeed.isNaN() ||
					!runtimeEngine.isNaN() ||
					!engineLoad.isNaN() ||
					!fuelPressure.isNaN() ||
					!fuelConsumption.isNaN() ||
					!fuelRemaining.isNaN() ||
					!batteryVoltage.isNaN() ||
					!vehicleSpeed.isNaN() ||
					!throttlePosition.isNaN()
	}
}