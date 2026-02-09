package net.osmand.shared.gpx

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import kotlin.reflect.KClass

enum class GpxParameter(
	val columnName: String,
	val columnType: String,
	val typeClass: KClass<*>,
	val defaultValue: Any?,
	val analysisParameter: Boolean
) {
	FILE_NAME("fileName", "TEXT", String::class, null, false),
	FILE_DIR("fileDir", "TEXT", String::class, null, false),
	TOTAL_DISTANCE("totalDistance", "double", Double::class, 0.0, true),
	TOTAL_TRACKS("totalTracks", "int", Int::class, 0, true),
	START_TIME("startTime", "bigint", Long::class, Long.MAX_VALUE, true),
	END_TIME("endTime", "bigint", Long::class, Long.MIN_VALUE, true),
	TIME_SPAN("timeSpan", "bigint", Long::class, 0L, true),
	EXPECTED_DURATION("expectedDuration", "bigint", Long::class, 0L, true),
	TIME_MOVING("timeMoving", "bigint", Long::class, 0L, true),
	TOTAL_DISTANCE_MOVING("totalDistanceMoving", "double", Double::class, 0.0, true),
	DIFF_ELEVATION_UP("diffElevationUp", "double", Double::class, 0.0, true),
	DIFF_ELEVATION_DOWN("diffElevationDown", "double", Double::class, 0.0, true),
	AVG_ELEVATION("avgElevation", "double", Double::class, 0.0, true),
	MIN_ELEVATION("minElevation", "double", Double::class, 99999.0, true),
	MAX_ELEVATION("maxElevation", "double", Double::class, -100.0, true),
	MIN_SPEED("minSpeed", "double", Double::class, Float.MAX_VALUE.toDouble(), true),
	MAX_SPEED("maxSpeed", "double", Double::class, 0.0, true),
	AVG_SPEED("avgSpeed", "double", Double::class, 0.0, true),
	POINTS("points", "int", Int::class, 0, true),
	WPT_POINTS("wptPoints", "int", Int::class, 0, true),
	COLOR("color", "TEXT", Int::class, null, false),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "bigint", Long::class, 0L, false),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "bigint", Long::class, 0L, false),
	FILE_CREATION_TIME("fileCreationTime", "bigint", Long::class, -1L, false),
	SPLIT_TYPE("splitType", "int", Int::class, 0, false),
	SPLIT_INTERVAL("splitInterval", "double", Double::class, 0.0, false),
	API_IMPORTED("apiImported", "int", Boolean::class, false, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", String::class, null, true),
	SHOW_AS_MARKERS("showAsMarkers", "int", Boolean::class, false, false),
	JOIN_SEGMENTS("joinSegments", "int", Boolean::class, false, false),
	SHOW_ARROWS("showArrows", "int", Boolean::class, false, false),
	SHOW_START_FINISH("showStartFinish", "int", Boolean::class, true, false),
	TRACK_VISUALIZATION_TYPE("track_visualization_type", "TEXT", String::class, "none", false),
	TRACK_3D_WALL_COLORING_TYPE("track_3d_wall_coloring_type", "TEXT", String::class, "none", false),
	TRACK_3D_LINE_POSITION_TYPE("track_3d_line_position_type", "TEXT", String::class, "top", false),
	ADDITIONAL_EXAGGERATION("additional_exaggeration", "double", Double::class, 1.0, false),
	ELEVATION_METERS("elevation_meters", "double", Double::class, 1000.0, false),
	WIDTH("width", "TEXT", String::class, null, false),
	COLORING_TYPE("gradientScaleType", "TEXT", String::class, null, false),
	COLOR_PALETTE("colorPalette", "TEXT", String::class, null, false),
	SMOOTHING_THRESHOLD("smoothingThreshold", "double", Double::class, Double.NaN, false),
	MIN_FILTER_SPEED("minFilterSpeed", "double", Double::class, Double.NaN, false),
	MAX_FILTER_SPEED("maxFilterSpeed", "double", Double::class, Double.NaN, false),
	MIN_FILTER_ALTITUDE("minFilterAltitude", "double", Double::class, Double.NaN, false),
	MAX_FILTER_ALTITUDE("maxFilterAltitude", "double", Double::class, Double.NaN, false),
	MAX_FILTER_HDOP("maxFilterHdop", "double", Double::class, Double.NaN, false),
	START_LAT("startLat", "double", Double::class, null, true),
	START_LON("startLon", "double", Double::class, null, true),
	NEAREST_CITY_NAME("nearestCityName", "TEXT", String::class, null, false),
	ACTIVITY_TYPE("activityType", "TEXT", String::class, null, false),
	MAX_SENSOR_TEMPERATURE("maxSensorTemperature", "int", Int::class, 0, true),
	AVG_SENSOR_TEMPERATURE("avgSensorTemperature", "double", Double::class, 0.0, true),
	MAX_SENSOR_SPEED("maxSensorSpeed", "double", Double::class, 0.0, true),
	AVG_SENSOR_SPEED("avgSensorSpeed", "double", Double::class, 0.0, true),
	MAX_SENSOR_POWER("maxSensorPower", "int", Int::class, 0, true),
	AVG_SENSOR_POWER("avgSensorPower", "double", Double::class, 0.0, true),
	MAX_SENSOR_CADENCE("maxSensorCadence", "double", Double::class, 0.0, true),
	AVG_SENSOR_CADENCE("avgSensorCadence", "double", Double::class, 0.0, true),
	MAX_SENSOR_HEART_RATE("maxSensorHr", "int", Int::class, 0, true),
	MIN_SENSOR_HEART_RATE("minSensorHr", "int", Int::class, 0, true),
	AVG_SENSOR_HEART_RATE("avgSensorHr", "double", Double::class, 0.0, true),
	DATA_VERSION("dataVersion", "int", Int::class, 0, false),
	APPEARANCE_LAST_MODIFIED_TIME("appearanceLastModifiedTime", "bigint", Long::class, 0L, false),

	AVG_OBD_ENGINE_LOAD("avgVmEload", "double", Double::class, 0.0, true),
	MAX_OBD_ENGINE_LOAD("maxVmEload", "double", Double::class, 0.0, true),
	AVG_OBD_THROTTLE_POSITION("avgVmTpos", "double", Double::class, 0.0, true),
	MAX_OBD_THROTTLE_POSITION("maxVmTpos", "double", Double::class, 0.0, true),
	AVG_OBD_ENGINE_OIL_TEMPERATURE("avgVmEotemp", "double", Double::class, 0.0, true),
	MAX_OBD_ENGINE_OIL_TEMPERATURE("maxVmEotemp", "int", Int::class, 0, true),
	AVG_OBD_FUEL_PRESSURE("avgVmFpress", "double", Double::class, 0.0, true),
	MAX_OBD_FUEL_PRESSURE("maxVmFpress", "int", Int::class, 0, true),
	AVG_OBD_BATTERY_VOLTAGE("avgVmBvol", "double", Double::class, 0.0, true),
	MAX_OBD_BATTERY_VOLTAGE("maxVmBvol", "double", Double::class, 0.0, true),
	AVG_OBD_AMBIENT_AIR_TEMPERATURE("avgVmAtemp", "double", Double::class, 0.0, true),
	MAX_OBD_AMBIENT_AIR_TEMPERATURE("maxVmAtemp", "int", Int::class, 0, true),
	AVG_OBD_ENGINE_RPM("avgVmEspeed", "int", Int::class, 0, true),
	MAX_OBD_ENGINE_RPM("maxVmEspeed", "int", Int::class, 0, true),
	AVG_OBD_ENGINE_RUNTIME("avgVmRuntime", "bigint", Long::class, 0L, true),
	MAX_OBD_ENGINE_RUNTIME("maxVmRuntime", "bigint", Long::class, 0L, true),
	AVG_OBD_VEHICLE_SPEED("avgVmVspeed", "double", Double::class, 0.0, true),
	MAX_OBD_VEHICLE_SPEED("maxVmVspeed", "int", Int::class, 0, true),
	AVG_OBD_AIR_INTAKE_TEMPERATURE("avgVmItemp", "double", Double::class, 0.0, true),
	MAX_OBD_AIR_INTAKE_TEMPERATURE("maxVmItemp", "int", Int::class, 0, true),
	AVG_OBD_ENGINE_COOLANT_TEMPERATURE("avgVmCtemp", "double", Double::class, 0.0, true),
	MAX_OBD_ENGINE_COOLANT_TEMPERATURE("maxVmCtemp", "int", Int::class, 0, true),
	AVG_OBD_FUEL_CONSUMPTION_RATE("avgVmFcons", "double", Double::class, 0.0, true),
	MAX_OBD_FUEL_CONSUMPTION_RATE("maxVmFcons", "double", Double::class, 0.0, true),
	AVG_OBD_FUEL_LEVEL("avgVmFuel", "double", Double::class, 0.0, true),
	MAX_OBD_FUEL_LEVEL("maxVmFuel", "double", Double::class, 0.0, true);

	val log = LoggerFactory.getLogger("GpxParameter")

	fun isNullSupported(): Boolean = defaultValue == null

	fun isAnalysisRecalculationNeeded(): Boolean {
		return this == JOIN_SEGMENTS
	}

	fun convertToDbValue(value: Any?): Any? {
		return when {
			value != null && typeClass == Boolean::class -> if (value as Boolean) 1 else 0
			this == COLOR && value is Int -> if (value == 0) "" else KAlgorithms.colorToString(value)
			else -> value
		}
	}

	fun isAppearanceParameter(): Boolean = APPEARANCE_PARAMETERS.contains(this)

	fun isGpxDirParameter(): Boolean = GPX_DIR_PARAMETERS.contains(this)

	fun <T : Comparable<T>>getComparableValue(value: Any): T {
		if (value is String) {
			return getValueFromString(value)
		} else if (value is Number) {
			return when (typeClass) {
				Int::class -> check<T>(value.toInt()) as T
				Double::class -> check<T>(value.toDouble()) as T
				Long::class -> check<T>(value.toLong()) as T
				Float::class -> check<T>(value.toFloat()) as T
				else -> throw IllegalArgumentException("Can not cast $value to $typeClass")
			}
		}
		throw IllegalArgumentException("$value is not a number")
	}

	fun <T>getValueFromString(value: String): T {
		var numberValue: Comparable<*>? = null
		try {
			numberValue = when (typeClass) {
				Double::class -> value.toDouble()
				Float::class -> value.toFloat()
				Int::class -> value.toInt()
				Long::class -> value.toLong()
				else -> null
			}
		} catch (_: Throwable) {
			log.error("Can't parse $value for type $typeClass")
		}

		val convertedValue: T? = when (typeClass) {
			Double::class -> check(numberValue ?: 0.0)
			Float::class -> check(numberValue ?: 0f)
			Int::class -> check(numberValue ?: 0)
			Long::class -> check(numberValue ?: 0L)
			else -> null
		}
		if (convertedValue != null) {
			return convertedValue
		} else {
			throw IllegalArgumentException("value can not be cast to $typeClass")
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <T>check(value: Comparable<*>): T? {
		return try {
			value as T
		} catch (_: ClassCastException) {
			null
		}
	}

	companion object {

		private val APPEARANCE_PARAMETERS = listOf(
			COLOR, WIDTH, COLORING_TYPE, SHOW_ARROWS,
			SHOW_START_FINISH, SPLIT_TYPE, SPLIT_INTERVAL,
			TRACK_3D_LINE_POSITION_TYPE, TRACK_VISUALIZATION_TYPE, TRACK_3D_WALL_COLORING_TYPE, COLOR_PALETTE
		)

		private val GPX_DIR_PARAMETERS: List<GpxParameter> = buildList {
			add(FILE_NAME)
			add(FILE_DIR)
			add(FILE_LAST_MODIFIED_TIME)
			addAll(APPEARANCE_PARAMETERS)
			add(APPEARANCE_LAST_MODIFIED_TIME)
		}

		fun getAppearanceParameters(): List<GpxParameter> = APPEARANCE_PARAMETERS

		fun getGpxDirParameters(): List<GpxParameter> = GPX_DIR_PARAMETERS

		fun getObdParameters(): List<GpxParameter> {
			return listOf(
				MAX_OBD_ENGINE_LOAD,
				AVG_OBD_ENGINE_LOAD,
				MAX_OBD_THROTTLE_POSITION,
				AVG_OBD_THROTTLE_POSITION,
				MAX_OBD_ENGINE_OIL_TEMPERATURE,
				AVG_OBD_ENGINE_OIL_TEMPERATURE,
				MAX_OBD_FUEL_PRESSURE,
				AVG_OBD_FUEL_PRESSURE,
				MAX_OBD_BATTERY_VOLTAGE,
				AVG_OBD_BATTERY_VOLTAGE,
				MAX_OBD_AMBIENT_AIR_TEMPERATURE,
				AVG_OBD_AMBIENT_AIR_TEMPERATURE,
				MAX_OBD_ENGINE_RPM,
				AVG_OBD_ENGINE_RPM,
				MAX_OBD_ENGINE_RUNTIME,
				AVG_OBD_ENGINE_RUNTIME,
				MAX_OBD_VEHICLE_SPEED,
				AVG_OBD_VEHICLE_SPEED,
				MAX_OBD_AIR_INTAKE_TEMPERATURE,
				AVG_OBD_AIR_INTAKE_TEMPERATURE,
				MAX_OBD_ENGINE_COOLANT_TEMPERATURE,
				AVG_OBD_ENGINE_COOLANT_TEMPERATURE,
				MAX_OBD_FUEL_CONSUMPTION_RATE,
				AVG_OBD_FUEL_CONSUMPTION_RATE,
				MAX_OBD_FUEL_LEVEL,
				AVG_OBD_FUEL_LEVEL
			)
		}
	}
}
