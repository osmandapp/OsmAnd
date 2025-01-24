package net.osmand.shared.gpx

import net.osmand.shared.util.KAlgorithms
import kotlin.reflect.KClass

enum class GpxParameter(
	val columnName: String,
	val columnType: String,
	val typeClass: KClass<*>,
	val defaultValue: Any?,
	val analysisParameter: Boolean,
	val analysisCalculation: Boolean
) {
	FILE_NAME("fileName", "TEXT", String::class, null, false, false),
	FILE_DIR("fileDir", "TEXT", String::class, null, false, false),
	TOTAL_DISTANCE("totalDistance", "double", Double::class, 0.0, true, false),
	TOTAL_TRACKS("totalTracks", "int", Int::class, 0, true, false),
	START_TIME("startTime", "bigint", Long::class, Long.MAX_VALUE, true, false),
	END_TIME("endTime", "bigint", Long::class, Long.MIN_VALUE, true, false),
	TIME_SPAN("timeSpan", "bigint", Long::class, 0L, true, false),
	EXPECTED_DURATION("expectedDuration", "bigint", Long::class, 0L, true, false),
	TIME_MOVING("timeMoving", "bigint", Long::class, 0L, true, false),
	TOTAL_DISTANCE_MOVING("totalDistanceMoving", "double", Double::class, 0.0, true, false),
	DIFF_ELEVATION_UP("diffElevationUp", "double", Double::class, 0.0, true, false),
	DIFF_ELEVATION_DOWN("diffElevationDown", "double", Double::class, 0.0, true, false),
	AVG_ELEVATION("avgElevation", "double", Double::class, 0.0, true, false),
	MIN_ELEVATION("minElevation", "double", Double::class, 99999.0, true, false),
	MAX_ELEVATION("maxElevation", "double", Double::class, -100.0, true, false),
	MIN_SPEED("minSpeed", "double", Double::class, Float.MAX_VALUE.toDouble(), true, false),
	MAX_SPEED("maxSpeed", "double", Double::class, 0.0, true, false),
	AVG_SPEED("avgSpeed", "double", Double::class, 0.0, true, false),
	POINTS("points", "int", Int::class, 0, true, false),
	WPT_POINTS("wptPoints", "int", Int::class, 0, true, false),
	COLOR("color", "TEXT", Int::class, null, false, false),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "bigint", Long::class, 0L, false, false),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "bigint", Long::class, 0L, false, false),
	FILE_CREATION_TIME("fileCreationTime", "bigint", Long::class, -1L, false, false),
	SPLIT_TYPE("splitType", "int", Int::class, 0, false, false),
	SPLIT_INTERVAL("splitInterval", "double", Double::class, 0.0, false, false),
	API_IMPORTED("apiImported", "int", Boolean::class, false, false, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", String::class, null, true, false),
	SHOW_AS_MARKERS("showAsMarkers", "int", Boolean::class, false, false, false),
	JOIN_SEGMENTS("joinSegments", "int", Boolean::class, false, false, true),
	SHOW_ARROWS("showArrows", "int", Boolean::class, false, false, false),
	SHOW_START_FINISH("showStartFinish", "int", Boolean::class, true, false, false),
	TRACK_VISUALIZATION_TYPE("track_visualization_type", "TEXT", String::class, "none", false, false),
	TRACK_3D_WALL_COLORING_TYPE("track_3d_wall_coloring_type", "TEXT", String::class, "none", false, false),
	TRACK_3D_LINE_POSITION_TYPE("track_3d_line_position_type", "TEXT", String::class, "top", false, false),
	ADDITIONAL_EXAGGERATION("additional_exaggeration", "double", Double::class, 1.0, false, false),
	ELEVATION_METERS("elevation_meters", "double", Double::class, 1000.0, false, false),
	WIDTH("width", "TEXT", String::class, null, false, false),
	COLORING_TYPE("gradientScaleType", "TEXT", String::class, null, false, false),
	COLOR_PALETTE("colorPalette", "TEXT", String::class, null, false, false),
	SMOOTHING_THRESHOLD("smoothingThreshold", "double", Double::class, Double.NaN, false, false),
	MIN_FILTER_SPEED("minFilterSpeed", "double", Double::class, Double.NaN, false, false),
	MAX_FILTER_SPEED("maxFilterSpeed", "double", Double::class, Double.NaN, false, false),
	MIN_FILTER_ALTITUDE("minFilterAltitude", "double", Double::class, Double.NaN, false, false),
	MAX_FILTER_ALTITUDE("maxFilterAltitude", "double", Double::class, Double.NaN, false, false),
	MAX_FILTER_HDOP("maxFilterHdop", "double", Double::class, Double.NaN, false, false),
	START_LAT("startLat", "double", Double::class, null, true, false),
	START_LON("startLon", "double", Double::class, null, true, false),
	NEAREST_CITY_NAME("nearestCityName", "TEXT", String::class, null, false, false),
	ACTIVITY_TYPE("activityType", "TEXT", String::class, null, false, false),
	MAX_SENSOR_TEMPERATURE("maxSensorTemperature", "int", Int::class, 0, true, false),
	AVG_SENSOR_TEMPERATURE("avgSensorTemperature", "double", Double::class, 0.0, true, false),
	MAX_SENSOR_SPEED("maxSensorSpeed", "double", Double::class, 0.0, true, false),
	AVG_SENSOR_SPEED("avgSensorSpeed", "double", Double::class, 0.0, true, false),
	MAX_SENSOR_POWER("maxSensorPower", "int", Int::class, 0, true, false),
	AVG_SENSOR_POWER("avgSensorPower", "double", Double::class, 0.0, true, false),
	MAX_SENSOR_CADENCE("maxSensorCadence", "double", Double::class, 0.0, true, false),
	AVG_SENSOR_CADENCE("avgSensorCadence", "double", Double::class, 0.0, true, false),
	MAX_SENSOR_HEART_RATE("maxSensorHr", "int", Int::class, 0, true, false),
	AVG_SENSOR_HEART_RATE("avgSensorHr", "double", Double::class, 0.0, true, false),
	DATA_VERSION("dataVersion", "int", Int::class, 0, false, false);

	fun isNullSupported(): Boolean = defaultValue == null

	fun convertToDbValue(value: Any?): Any? {
		return when {
			value != null && typeClass == Boolean::class -> if (value as Boolean) 1 else 0
			this == COLOR && value is Int -> if (value == 0) "" else KAlgorithms.colorToString(value)
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
