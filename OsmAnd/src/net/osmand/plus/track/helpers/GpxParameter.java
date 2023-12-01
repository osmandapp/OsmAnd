package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GpxParameter<T> {

	public static final List<GpxParameter<?>> parameters = new ArrayList<>();

	public static final GpxParameter<String> GPX_COL_NAME = new GpxParameter<>("fileName", "TEXT", String.class, null);
	public static final GpxParameter<String> GPX_COL_DIR = new GpxParameter<>("fileDir", "TEXT", String.class, null);
	public static final GpxParameter<Double> GPX_COL_TOTAL_DISTANCE = new GpxParameter<>("totalDistance", "double", Double.class, 0d);
	public static final GpxParameter<Integer> GPX_COL_TOTAL_TRACKS = new GpxParameter<>("totalTracks", "int", Integer.class, 0);
	public static final GpxParameter<Long> GPX_COL_START_TIME = new GpxParameter<>("startTime", "long", Long.class, Long.MAX_VALUE);
	public static final GpxParameter<Long> GPX_COL_END_TIME = new GpxParameter<>("endTime", "long", Long.class, Long.MIN_VALUE);
	public static final GpxParameter<Long> GPX_COL_TIME_SPAN = new GpxParameter<>("timeSpan", "long", Long.class, 0l);
	public static final GpxParameter<Long> GPX_COL_TIME_MOVING = new GpxParameter<>("timeMoving", "long", Long.class, 0l);
	public static final GpxParameter<Double> GPX_COL_TOTAL_DISTANCE_MOVING = new GpxParameter<>("totalDistanceMoving", "double", Double.class, 0d);
	public static final GpxParameter<Double> GPX_COL_DIFF_ELEVATION_UP = new GpxParameter<>("diffElevationUp", "double", Double.class, 0d);
	public static final GpxParameter<Double> GPX_COL_DIFF_ELEVATION_DOWN = new GpxParameter<>("diffElevationDown", "double", Double.class, 0d);
	public static final GpxParameter<Double> GPX_COL_AVG_ELEVATION = new GpxParameter<>("avgElevation", "double", Double.class, 0d);
	public static final GpxParameter<Double> GPX_COL_MIN_ELEVATION = new GpxParameter<>("minElevation", "double", Double.class, 99999d);
	public static final GpxParameter<Double> GPX_COL_MAX_ELEVATION = new GpxParameter<>("maxElevation", "double", Double.class, -100d);
	public static final GpxParameter<Double> GPX_COL_MAX_SPEED = new GpxParameter<>("maxSpeed", "double", Double.class, 0d);
	public static final GpxParameter<Double> GPX_COL_AVG_SPEED = new GpxParameter<>("avgSpeed", "double", Double.class, 0d);
	public static final GpxParameter<Integer> GPX_COL_POINTS = new GpxParameter<>("points", "int", Integer.class, 0);
	public static final GpxParameter<Integer> GPX_COL_WPT_POINTS = new GpxParameter<>("wptPoints", "int", Integer.class, 0);
	public static final GpxParameter<Integer> GPX_COL_COLOR = new GpxParameter<>("color", "TEXT", Integer.class, 0);
	public static final GpxParameter<Long> GPX_COL_FILE_LAST_MODIFIED_TIME = new GpxParameter<>("fileLastModifiedTime", "long", Long.class, 0l);
	public static final GpxParameter<Long> GPX_COL_FILE_LAST_UPLOADED_TIME = new GpxParameter<>("fileLastUploadedTime", "long", Long.class, 0l);
	public static final GpxParameter<Long> GPX_COL_FILE_CREATION_TIME = new GpxParameter<>("fileCreationTime", "long", Long.class, -1l);
	public static final GpxParameter<Integer> GPX_COL_SPLIT_TYPE = new GpxParameter<>("splitType", "int", Integer.class, 0);
	public static final GpxParameter<Double> GPX_COL_SPLIT_INTERVAL = new GpxParameter<>("splitInterval", "double", Double.class, 0d);
	public static final GpxParameter<Boolean> GPX_COL_API_IMPORTED = new GpxParameter<>("apiImported", "int", Boolean.class, false);
	public static final GpxParameter<String> GPX_COL_WPT_CATEGORY_NAMES = new GpxParameter<>("wptCategoryNames", "TEXT", String.class, null);
	public static final GpxParameter<Boolean> GPX_COL_SHOW_AS_MARKERS = new GpxParameter<>("showAsMarkers", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> GPX_COL_JOIN_SEGMENTS = new GpxParameter<>("joinSegments", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> GPX_COL_SHOW_ARROWS = new GpxParameter<>("showArrows", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> GPX_COL_SHOW_START_FINISH = new GpxParameter<>("showStartFinish", "int", Boolean.class, true);
	public static final GpxParameter<String> GPX_COL_WIDTH = new GpxParameter<>("width", "TEXT", String.class, null);
	public static final GpxParameter<String> GPX_COL_COLORING_TYPE = new GpxParameter<>("gradientScaleType", "TEXT", String.class, null);
	public static final GpxParameter<Double> GPX_COL_SMOOTHING_THRESHOLD = new GpxParameter<>("smoothingThreshold", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> GPX_COL_MIN_FILTER_SPEED = new GpxParameter<>("minFilterSpeed", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> GPX_COL_MAX_FILTER_SPEED = new GpxParameter<>("maxFilterSpeed", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> GPX_COL_MIN_FILTER_ALTITUDE = new GpxParameter<>("minFilterAltitude", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> GPX_COL_MAX_FILTER_ALTITUDE = new GpxParameter<>("maxFilterAltitude", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> GPX_COL_MAX_FILTER_HDOP = new GpxParameter<>("maxFilterHdop", "double", Double.class, Double.NaN);
	public static final GpxParameter<String> GPX_COL_START_LAT = new GpxParameter<>("startLat", "double", String.class, null);
	public static final GpxParameter<String> GPX_COL_START_LON = new GpxParameter<>("startLon", "double", String.class, null);
	public static final GpxParameter<String> GPX_COL_NEAREST_CITY_NAME = new GpxParameter<>("nearestCityName", "TEXT", String.class, null);


	@NonNull
	private final String columnName;
	@NonNull
	private final String columnType;
	@NonNull
	private final Class<T> typeClass;
	@Nullable
	private final T defaultValue;

	private GpxParameter(@NonNull String columnName, @NonNull String columnType,
	                     @NonNull Class<T> typeClass, @Nullable T defaultValue) {
		this.columnName = columnName;
		this.columnType = columnType;
		this.typeClass = typeClass;
		this.defaultValue = defaultValue;
		parameters.add(this);
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
	public T getDefaultValue() {
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
		} else if (this == GPX_COL_COLOR) {
			if (value instanceof Integer) {
				int color = (Integer) value;
				return color == 0 ? "" : Algorithms.colorToString(color);
			}
		}
		return value;
	}

	public boolean isAnalysisParameter() {
		return getParametersForAnalysis().contains(this);
	}

	@NonNull
	public static List<GpxParameter<?>> getParametersForAnalysis() {
		return Arrays.asList(GPX_COL_TOTAL_DISTANCE, GPX_COL_TOTAL_TRACKS, GPX_COL_START_TIME, GPX_COL_END_TIME,
				GPX_COL_TIME_SPAN, GPX_COL_TIME_MOVING, GPX_COL_TOTAL_DISTANCE_MOVING, GPX_COL_DIFF_ELEVATION_UP,
				GPX_COL_DIFF_ELEVATION_DOWN, GPX_COL_AVG_ELEVATION, GPX_COL_MIN_ELEVATION, GPX_COL_MAX_ELEVATION,
				GPX_COL_MAX_SPEED, GPX_COL_AVG_SPEED, GPX_COL_POINTS, GPX_COL_WPT_POINTS, GPX_COL_WPT_CATEGORY_NAMES,
				GPX_COL_START_LAT, GPX_COL_START_LON);
	}

	public int getSelectColumnIndex() {
		return parameters.indexOf(this);
	}

	@NonNull
	@Override
	public String toString() {
		return columnName;
	}
}
