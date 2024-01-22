package net.osmand.gpx;

import net.osmand.util.Algorithms;

public enum GpxParameter {

	FILE_NAME("fileName", "TEXT", String.class, null, false),
	FILE_DIR("fileDir", "TEXT", String.class, null, false),
	TOTAL_DISTANCE("totalDistance", "double", Double.class, 0d, true),
	TOTAL_TRACKS("totalTracks", "int", Integer.class, 0, true),
	START_TIME("startTime", "long", Long.class, Long.MAX_VALUE, true),
	END_TIME("endTime", "long", Long.class, Long.MIN_VALUE, true),
	TIME_SPAN("timeSpan", "long", Long.class, 0L, true),
	EXPECTED_ROUTE_DURATION("expectedRouteDuration", "long", Long.class, -1L, true),
	TIME_MOVING("timeMoving", "long", Long.class, 0L, true),
	TOTAL_DISTANCE_MOVING("totalDistanceMoving", "double", Double.class, 0d, true),
	DIFF_ELEVATION_UP("diffElevationUp", "double", Double.class, 0d, true),
	DIFF_ELEVATION_DOWN("diffElevationDown", "double", Double.class, 0d, true),
	AVG_ELEVATION("avgElevation", "double", Double.class, 0d, true),
	MIN_ELEVATION("minElevation", "double", Double.class, 99999d, true),
	MAX_ELEVATION("maxElevation", "double", Double.class, -100d, true),
	MIN_SPEED("minSpeed", "double", Double.class, (double) Float.MAX_VALUE, true),
	MAX_SPEED("maxSpeed", "double", Double.class, 0d, true),
	AVG_SPEED("avgSpeed", "double", Double.class, 0d, true),
	POINTS("points", "int", Integer.class, 0, true),
	WPT_POINTS("wptPoints", "int", Integer.class, 0, true),
	COLOR("color", "TEXT", Integer.class, 0, false),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "long", Long.class, 0L, false),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "long", Long.class, 0L, false),
	FILE_CREATION_TIME("fileCreationTime", "long", Long.class, -1L, false),
	SPLIT_TYPE("splitType", "int", Integer.class, 0, false),
	SPLIT_INTERVAL("splitInterval", "double", Double.class, 0d, false),
	API_IMPORTED("apiImported", "int", Boolean.class, false, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", String.class, null, true),
	SHOW_AS_MARKERS("showAsMarkers", "int", Boolean.class, false, false),
	JOIN_SEGMENTS("joinSegments", "int", Boolean.class, false, false),
	SHOW_ARROWS("showArrows", "int", Boolean.class, false, false),
	SHOW_START_FINISH("showStartFinish", "int", Boolean.class, true, false),
	WIDTH("width", "TEXT", String.class, null, false),
	COLORING_TYPE("gradientScaleType", "TEXT", String.class, null, false),
	SMOOTHING_THRESHOLD("smoothingThreshold", "double", Double.class, Double.NaN, false),
	MIN_FILTER_SPEED("minFilterSpeed", "double", Double.class, Double.NaN, false),
	MAX_FILTER_SPEED("maxFilterSpeed", "double", Double.class, Double.NaN, false),
	MIN_FILTER_ALTITUDE("minFilterAltitude", "double", Double.class, Double.NaN, false),
	MAX_FILTER_ALTITUDE("maxFilterAltitude", "double", Double.class, Double.NaN, false),
	MAX_FILTER_HDOP("maxFilterHdop", "double", Double.class, Double.NaN, false),
	START_LAT("startLat", "double", Double.class, null, true),
	START_LON("startLon", "double", Double.class, null, true),
	NEAREST_CITY_NAME("nearestCityName", "TEXT", String.class, null, false),
	MAX_SENSOR_TEMPERATURE("maxSensorTemperature", "int", Integer.class, 0, true),
	AVG_SENSOR_TEMPERATURE("avgSensorTemperature", "double", Double.class, 0d, true),
	MAX_SENSOR_SPEED("maxSensorSpeed", "double", Double.class, 0d, true),
	AVG_SENSOR_SPEED("avgSensorSpeed", "double", Double.class, 0d, true),
	MAX_SENSOR_POWER("maxSensorPower", "int", Integer.class, 0, true),
	AVG_SENSOR_POWER("avgSensorPower", "double", Double.class, 0d, true),
	MAX_SENSOR_CADENCE("maxSensorCadence", "double", Double.class, 0d, true),
	AVG_SENSOR_CADENCE("avgSensorCadence", "double", Double.class, 0d, true),
	MAX_SENSOR_HEART_RATE("maxSensorHr", "int", Integer.class, 0, true),
	AVG_SENSOR_HEART_RATE("avgSensorHr", "double", Double.class, 0d, true),
	DATA_VERSION("dataVersion", "int", Integer.class, 0, true);


	private final String columnName;
	private final String columnType;
	private final Class<?> typeClass;
	private final Object defaultValue;
	private final boolean analysisParameter;

	GpxParameter(String columnName, String columnType,
	             Class<?> typeClass, Object defaultValue,
	             boolean isAnalysisParameter) {
		this.columnName = columnName;
		this.columnType = columnType;
		this.typeClass = typeClass;
		this.defaultValue = defaultValue;
		this.analysisParameter = isAnalysisParameter;
	}


	public String getColumnName() {
		return columnName;
	}


	public String getColumnType() {
		return columnType;
	}

	public Class<?> getTypeClass() {
		return typeClass;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public boolean isNullSupported() {
		return defaultValue == null;
	}

	public boolean isAnalysisParameter() {
		return analysisParameter;
	}

	public boolean isValidValue(Object value) {
		return value == null && isNullSupported() || value != null && getTypeClass() == value.getClass();
	}

	public Object convertToDbValue(Object value) {
		if (getTypeClass() == Boolean.class) {
			return value instanceof Boolean && ((Boolean) value) ? 1 : 0;  // 1 = true, 0 = false
		} else if (this == COLOR) {
			if (value instanceof Integer) {
				int color = (Integer) value;
				return color == 0 ? "" : Algorithms.colorToString(color);
			}
		}
		return value;
	}

	public int getSelectColumnIndex() {
		return ordinal();
	}


	@Override
	public String toString() {
		return columnName;
	}
}
