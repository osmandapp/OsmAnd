package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public enum GpxParameter {

	FILE_NAME("fileName", "TEXT", String.class, null),
	FILE_DIR("fileDir", "TEXT", String.class, null),
	TOTAL_DISTANCE("totalDistance", "double", Double.class, 0d),
	TOTAL_TRACKS("totalTracks", "int", Integer.class, 0),
	START_TIME("startTime", "long", Long.class, Long.MAX_VALUE),
	END_TIME("endTime", "long", Long.class, Long.MIN_VALUE),
	TIME_SPAN("timeSpan", "long", Long.class, 0L),
	EXPECTED_ROUTE_DURATION("expectedRouteDuration", "long", Long.class, -1L),
	TIME_MOVING("timeMoving", "long", Long.class, 0L),
	TOTAL_DISTANCE_MOVING("totalDistanceMoving", "double", Double.class, 0d),
	DIFF_ELEVATION_UP("diffElevationUp", "double", Double.class, 0d),
	DIFF_ELEVATION_DOWN("diffElevationDown", "double", Double.class, 0d),
	AVG_ELEVATION("avgElevation", "double", Double.class, 0d),
	MIN_ELEVATION("minElevation", "double", Double.class, 99999d),
	MAX_ELEVATION("maxElevation", "double", Double.class, -100d),
	MAX_SPEED("maxSpeed", "double", Double.class, 0d),
	AVG_SPEED("avgSpeed", "double", Double.class, 0d),
	POINTS("points", "int", Integer.class, 0),
	WPT_POINTS("wptPoints", "int", Integer.class, 0),
	COLOR("color", "TEXT", Integer.class, 0),
	FILE_LAST_MODIFIED_TIME("fileLastModifiedTime", "long", Long.class, 0L),
	FILE_LAST_UPLOADED_TIME("fileLastUploadedTime", "long", Long.class, 0L),
	FILE_CREATION_TIME("fileCreationTime", "long", Long.class, -1L),
	SPLIT_TYPE("splitType", "int", Integer.class, 0),
	SPLIT_INTERVAL("splitInterval", "double", Double.class, 0d),
	API_IMPORTED("apiImported", "int", Boolean.class, false),
	WPT_CATEGORY_NAMES("wptCategoryNames", "TEXT", String.class, null),
	SHOW_AS_MARKERS("showAsMarkers", "int", Boolean.class, false),
	JOIN_SEGMENTS("joinSegments", "int", Boolean.class, false),
	SHOW_ARROWS("showArrows", "int", Boolean.class, false),
	SHOW_START_FINISH("showStartFinish", "int", Boolean.class, true),
	WIDTH("width", "TEXT", String.class, null),
	COLORING_TYPE("gradientScaleType", "TEXT", String.class, null),
	SMOOTHING_THRESHOLD("smoothingThreshold", "double", Double.class, Double.NaN),
	MIN_FILTER_SPEED("minFilterSpeed", "double", Double.class, Double.NaN),
	MAX_FILTER_SPEED("maxFilterSpeed", "double", Double.class, Double.NaN),
	MIN_FILTER_ALTITUDE("minFilterAltitude", "double", Double.class, Double.NaN),
	MAX_FILTER_ALTITUDE("maxFilterAltitude", "double", Double.class, Double.NaN),
	MAX_FILTER_HDOP("maxFilterHdop", "double", Double.class, Double.NaN),
	START_LAT("startLat", "double", String.class, null),
	START_LON("startLon", "double", String.class, null),
	NEAREST_CITY_NAME("nearestCityName", "TEXT", String.class, null);


	@NonNull
	private final String columnName;
	@NonNull
	private final String columnType;
	@NonNull
	private final Class<?> typeClass;
	@Nullable
	private final Object defaultValue;

	GpxParameter(@NonNull String columnName, @NonNull String columnType,
	                     @NonNull Class<?> typeClass, @Nullable Object defaultValue) {
		this.columnName = columnName;
		this.columnType = columnType;
		this.typeClass = typeClass;
		this.defaultValue = defaultValue;
	}

	@NonNull
	public String getColumnName() {
		return columnName;
	}

	@NonNull
	public String getColumnType() {
		return columnType;
	}

	@NonNull
	public Class<?> getTypeClass() {
		return typeClass;
	}

	@Nullable
	public Object getDefaultValue() {
		return defaultValue;
	}

	public boolean isNullSupported() {
		return defaultValue == null;
	}

	public boolean isValidValue(@Nullable Object value) {
		return value == null && isNullSupported() || value != null && getTypeClass() == value.getClass();
	}

	@Nullable
	public Object convertToDbValue(@Nullable Object value) {
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

	@NonNull
	@Override
	public String toString() {
		return columnName;
	}
}
