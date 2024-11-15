package net.osmand.gpx;

import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Deprecated
public enum GpxParameter {

	FILE_NAME("fileName", "TEXT", String.class, null, false),
	FILE_DIR("fileDir", "TEXT", String.class, null, false),
	TOTAL_DISTANCE("totalDistance", "double", Double.class, 0d, true),
	TOTAL_TRACKS("totalTracks", "int", Integer.class, 0, true),
	START_TIME("startTime", "bigint", Long.class, Long.MAX_VALUE, true),
	END_TIME("endTime", "bigint", Long.class, Long.MIN_VALUE, true),
	TIME_SPAN("timeSpan", "bigint", Long.class, 0L, true),
	TIME_MOVING("timeMoving", "bigint", Long.class, 0L, true),
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
	COLOR("color", "TEXT", Integer.class, null, false),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "bigint", Long.class, 0L, false),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "bigint", Long.class, 0L, false),
	FILE_CREATION_TIME("fileCreationTime", "bigint", Long.class, -1L, false),
	SPLIT_TYPE("splitType", "int", Integer.class, 0, false),
	SPLIT_INTERVAL("splitInterval", "double", Double.class, 0d, false),
	API_IMPORTED("apiImported", "int", Boolean.class, false, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", String.class, null, true),
	SHOW_AS_MARKERS("showAsMarkers", "int", Boolean.class, false, false),
	JOIN_SEGMENTS("joinSegments", "int", Boolean.class, false, false),
	SHOW_ARROWS("showArrows", "int", Boolean.class, false, false),
	SHOW_START_FINISH("showStartFinish", "int", Boolean.class, true, false),
	TRACK_VISUALIZATION_TYPE("track_visualization_type", "TEXT", String.class, "none", false),
	TRACK_3D_WALL_COLORING_TYPE("track_3d_wall_coloring_type", "TEXT", String.class, "none", false),
	TRACK_3D_LINE_POSITION_TYPE("track_3d_line_position_type", "TEXT", String.class, "top", false),
	ADDITIONAL_EXAGGERATION("additional_exaggeration", "double", Double.class, 1d, false),
	ELEVATION_METERS("elevation_meters", "double", Double.class, 1000d, false),
	WIDTH("width", "TEXT", String.class, null, false),
	COLORING_TYPE("gradientScaleType", "TEXT", String.class, null, false),
	COLOR_PALETTE("colorPalette", "TEXT", String.class, null, false),
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
	DATA_VERSION("dataVersion", "int", Integer.class, 0, false);


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

	public Object convertToDbValue(Object value) {
		if (value != null) {
			if (getTypeClass() == Boolean.class) {
				return value instanceof Boolean && ((Boolean) value) ? 1 : 0;  // 1 = true, 0 = false
			} else if (this == COLOR) {
				if (value instanceof Integer) {
					int color = (Integer) value;
					return color == 0 ? "" : Algorithms.colorToString(color);
				}
			}
		}
		return value;
	}

	@Override
	public String toString() {
		return columnName;
	}

	public boolean isAppearanceParameter() {
		return CollectionUtils.containsAny(getAppearanceParameters(), this);
	}

	public static List<GpxParameter> getAppearanceParameters() {
		return Arrays.asList(COLOR, WIDTH, COLORING_TYPE, SHOW_ARROWS,
				SHOW_START_FINISH, SPLIT_TYPE, SPLIT_INTERVAL, TRACK_3D_LINE_POSITION_TYPE,
				TRACK_VISUALIZATION_TYPE, TRACK_3D_WALL_COLORING_TYPE, COLOR_PALETTE);
	}

	public static List<GpxParameter> getGpxDirParameters() {
		List<GpxParameter> list = new ArrayList<>();
		list.add(FILE_NAME);
		list.add(FILE_DIR);
		list.add(FILE_LAST_MODIFIED_TIME);
		list.addAll(getAppearanceParameters());
		return list;
	}
}
