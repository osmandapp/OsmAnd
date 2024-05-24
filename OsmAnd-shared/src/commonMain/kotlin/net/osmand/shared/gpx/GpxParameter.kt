package net.osmand.shared.gpx

enum class GpxParameter(
	val columnName: String,
	val columnType: String,
	val defaultValue: Any?,
	val isAnalysisParameter: Boolean
) {
	FILE_NAME("fileName", "TEXT", null, false),
	FILE_DIR("fileDir", "TEXT", null, false),
	TOTAL_DISTANCE("totalDistance", "double", 0.0, true),
	TOTAL_TRACKS("totalTracks", "int", 0, true),
	START_TIME("startTime", "bigint", Long.MAX_VALUE, true),
	END_TIME("endTime", "bigint", Long.MIN_VALUE, true),
	TIME_SPAN("timeSpan", "bigint", 0L, true),
	TIME_MOVING("timeMoving", "bigint", 0L, true),
	TOTAL_DISTANCE_MOVING("totalDistanceMoving", "double", 0.0, true),
	DIFF_ELEVATION_UP("diffElevationUp", "double", 0.0, true),
	DIFF_ELEVATION_DOWN("diffElevationDown", "double", 0.0, true),
	AVG_ELEVATION("avgElevation", "double", 0.0, true),
	MIN_ELEVATION("minElevation", "double", 99999.0, true),
	MAX_ELEVATION("maxElevation", "double", -100.0, true),
	MIN_SPEED("minSpeed", "double", Float.MAX_VALUE.toDouble(), true),
	MAX_SPEED("maxSpeed", "double", 0.0, true),
	AVG_SPEED("avgSpeed", "double", 0.0, true),
	POINTS("points", "int", 0, true),
	WPT_POINTS("wptPoints", "int", 0, true),
	COLOR("color", "TEXT", 0, false),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "bigint", 0L, false),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "bigint", 0L, false),
	FILE_CREATION_TIME("fileCreationTime", "bigint", -1L, false),
	SPLIT_TYPE("splitType", "int", 0, false),
	SPLIT_INTERVAL("splitInterval", "double", 0.0, false),
	API_IMPORTED("apiImported", "int", false, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", null, true),
	SHOW_AS_MARKERS("showAsMarkers", "int", false, false),
	JOIN_SEGMENTS("joinSegments", "int", false, false),
	SHOW_ARROWS("showArrows", "int", false, false),
	SHOW_START_FINISH("showStartFinish", "int", true, false),
	TRACK_VISUALIZATION_TYPE("track_visualization_type", "TEXT", "none", false),
	TRACK_3D_WALL_COLORING_TYPE("track_3d_wall_coloring_type", "TEXT", "none", false),
	TRACK_3D_LINE_POSITION_TYPE("track_3d_line_position_type", "TEXT", "top", false),
	ADDITIONAL_EXAGGERATION("additional_exaggeration", "double", 1.0, false),
	WIDTH("width", "TEXT", null, false),
	COLORING_TYPE("gradientScaleType", "TEXT", null, false),
	SMOOTHING_THRESHOLD("smoothingThreshold", "double", Double.NaN, false),
	MIN_FILTER_SPEED("minFilterSpeed", "double", Double.NaN, false),
	MAX_FILTER_SPEED("maxFilterSpeed", "double", Double.NaN, false),
	MIN_FILTER_ALTITUDE("minFilterAltitude", "double", Double.NaN, false),
	MAX_FILTER_ALTITUDE("maxFilterAltitude", "double", Double.NaN, false),
	MAX_FILTER_HDOP("maxFilterHdop", "double", Double.NaN, false),
	START_LAT("startLat", "double", null, true),
	START_LON("startLon", "double", null, true),
	NEAREST_CITY_NAME("nearestCityName", "TEXT", null, false),
	MAX_SENSOR_TEMPERATURE("maxSensorTemperature", "int", 0, true),
	AVG_SENSOR_TEMPERATURE("avgSensorTemperature", "double", 0.0, true),
	MAX_SENSOR_SPEED("maxSensorSpeed", "double", 0.0, true),
	AVG_SENSOR_SPEED("avgSensorSpeed", "double", 0.0, true),
	MAX_SENSOR_POWER("maxSensorPower", "int", 0, true),
	AVG_SENSOR_POWER("avgSensorPower", "double", 0.0, true),
	MAX_SENSOR_CADENCE("maxSensorCadence", "double", 0.0, true),
	AVG_SENSOR_CADENCE("avgSensorCadence", "double", 0.0, true),
	MAX_SENSOR_HEART_RATE("maxSensorHr", "int", 0, true),
	AVG_SENSOR_HEART_RATE("avgSensorHr", "double", 0.0, true),
	DATA_VERSION("dataVersion", "int", 0, false);

	fun isNullSupported(): Boolean {
		return defaultValue == null
	}

	fun convertToDbValue(value: Any?): Any? {
		if (value != null) {
			when {
				typeClass == Boolean::class.java -> return if (value is Boolean && value) 1 else 0
				this == COLOR -> if (value is Int) {
					val color = value
					return if (color == 0) "" else Algorithms.colorToString(color)
				}
			}
		}
		return value
	}

	override fun toString(): String {
		return columnName
	}

	fun isAppearanceParameter(): Boolean {
		return getAppearanceParameters().contains(this)
	}

	companion object {
		fun getAppearanceParameters(): List<GpxParameter> {
			return listOf(
				COLOR,
				WIDTH,
				COLORING_TYPE,
				SHOW_ARROWS,
				SHOW_START_FINISH,
				SPLIT_TYPE,
				SPLIT_INTERVAL,
				TRACK_3D_LINE_POSITION_TYPE,
				TRACK_VISUALIZATION_TYPE,
				TRACK_3D_WALL_COLORING_TYPE
			)
		}

		fun getGpxDirParameters(): List<GpxParameter> {
			return mutableListOf(FILE_NAME, FILE_DIR, FILE_LAST_MODIFIED_TIME).apply {
				addAll(getAppearanceParameters())
			}
		}
	}
}
