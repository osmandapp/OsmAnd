package net.osmand.shared.gpx

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
	AVG_SENSOR_HEART_RATE("avgSensorHr", "double", Double::class, 0.0, true),
	DATA_VERSION("dataVersion", "int", Int::class, 0, false);

	fun isNullSupported(): Boolean = defaultValue == null

	fun convertToDbValue(value: Any?): Any? {
		return when {
			value != null && typeClass == Boolean::class -> if (value as Boolean) 1 else 0
			this == COLOR && value is Int -> if (value == 0) "" else value.toString()
			else -> value
		}
	}

	fun isAppearanceParameter(): Boolean = appearanceParameters.contains(this)

	companion object {
		private val appearanceParameters = listOf(
			COLOR, WIDTH, COLORING_TYPE, SHOW_ARROWS,
			SHOW_START_FINISH, SPLIT_TYPE, SPLIT_INTERVAL,
			TRACK_3D_LINE_POSITION_TYPE, TRACK_VISUALIZATION_TYPE, TRACK_3D_WALL_COLORING_TYPE, COLOR_PALETTE
		)

		fun getAppearanceParameters(): List<GpxParameter> = appearanceParameters

		fun getGpxDirParameters(): List<GpxParameter> {
			val list = mutableListOf(
				FILE_NAME, FILE_DIR, FILE_LAST_MODIFIED_TIME
			)
			list.addAll(getAppearanceParameters())
			return list
		}
	}
}
