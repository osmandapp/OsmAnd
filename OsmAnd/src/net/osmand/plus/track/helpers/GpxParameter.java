package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GpxParameter<T> {

	public static final List<GpxParameter<?>> parameters = new ArrayList<>();

	public static final GpxParameter<String> FILE_NAME = new GpxParameter<>("fileName", "TEXT", String.class, null);
	public static final GpxParameter<String> FILE_DIR = new GpxParameter<>("fileDir", "TEXT", String.class, null);
	public static final GpxParameter<Double> TOTAL_DISTANCE = new GpxParameter<>("totalDistance", "double", Double.class, 0d);
	public static final GpxParameter<Integer> TOTAL_TRACKS = new GpxParameter<>("totalTracks", "int", Integer.class, 0);
	public static final GpxParameter<Long> START_TIME = new GpxParameter<>("startTime", "long", Long.class, Long.MAX_VALUE);
	public static final GpxParameter<Long> END_TIME = new GpxParameter<>("endTime", "long", Long.class, Long.MIN_VALUE);
	public static final GpxParameter<Long> TIME_SPAN = new GpxParameter<>("timeSpan", "long", Long.class, 0l);
	public static final GpxParameter<Long> TIME_MOVING = new GpxParameter<>("timeMoving", "long", Long.class, 0l);
	public static final GpxParameter<Double> TOTAL_DISTANCE_MOVING = new GpxParameter<>("totalDistanceMoving", "double", Double.class, 0d);
	public static final GpxParameter<Double> DIFF_ELEVATION_UP = new GpxParameter<>("diffElevationUp", "double", Double.class, 0d);
	public static final GpxParameter<Double> DIFF_ELEVATION_DOWN = new GpxParameter<>("diffElevationDown", "double", Double.class, 0d);
	public static final GpxParameter<Double> AVG_ELEVATION = new GpxParameter<>("avgElevation", "double", Double.class, 0d);
	public static final GpxParameter<Double> MIN_ELEVATION = new GpxParameter<>("minElevation", "double", Double.class, 99999d);
	public static final GpxParameter<Double> MAX_ELEVATION = new GpxParameter<>("maxElevation", "double", Double.class, -100d);
	public static final GpxParameter<Double> MAX_SPEED = new GpxParameter<>("maxSpeed", "double", Double.class, 0d);
	public static final GpxParameter<Double> AVG_SPEED = new GpxParameter<>("avgSpeed", "double", Double.class, 0d);
	public static final GpxParameter<Integer> POINTS = new GpxParameter<>("points", "int", Integer.class, 0);
	public static final GpxParameter<Integer> WPT_POINTS = new GpxParameter<>("wptPoints", "int", Integer.class, 0);
	public static final GpxParameter<Integer> COLOR = new GpxParameter<>("color", "TEXT", Integer.class, 0);
	public static final GpxParameter<Long> FILE_LAST_MODIFIED_TIME = new GpxParameter<>("fileLastModifiedTime", "long", Long.class, 0l);
	public static final GpxParameter<Long> FILE_LAST_UPLOADED_TIME = new GpxParameter<>("fileLastUploadedTime", "long", Long.class, 0l);
	public static final GpxParameter<Long> FILE_CREATION_TIME = new GpxParameter<>("fileCreationTime", "long", Long.class, -1l);
	public static final GpxParameter<Integer> SPLIT_TYPE = new GpxParameter<>("splitType", "int", Integer.class, 0);
	public static final GpxParameter<Double> SPLIT_INTERVAL = new GpxParameter<>("splitInterval", "double", Double.class, 0d);
	public static final GpxParameter<Boolean> API_IMPORTED = new GpxParameter<>("apiImported", "int", Boolean.class, false);
	public static final GpxParameter<String> WPT_CATEGORY_NAMES = new GpxParameter<>("wptCategoryNames", "TEXT", String.class, null);
	public static final GpxParameter<Boolean> SHOW_AS_MARKERS = new GpxParameter<>("showAsMarkers", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> JOIN_SEGMENTS = new GpxParameter<>("joinSegments", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> SHOW_ARROWS = new GpxParameter<>("showArrows", "int", Boolean.class, false);
	public static final GpxParameter<Boolean> SHOW_START_FINISH = new GpxParameter<>("showStartFinish", "int", Boolean.class, true);
	public static final GpxParameter<String> WIDTH = new GpxParameter<>("width", "TEXT", String.class, null);
	public static final GpxParameter<String> COLORING_TYPE = new GpxParameter<>("gradientScaleType", "TEXT", String.class, null);
	public static final GpxParameter<Double> SMOOTHING_THRESHOLD = new GpxParameter<>("smoothingThreshold", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> MIN_FILTER_SPEED = new GpxParameter<>("minFilterSpeed", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> MAX_FILTER_SPEED = new GpxParameter<>("maxFilterSpeed", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> MIN_FILTER_ALTITUDE = new GpxParameter<>("minFilterAltitude", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> MAX_FILTER_ALTITUDE = new GpxParameter<>("maxFilterAltitude", "double", Double.class, Double.NaN);
	public static final GpxParameter<Double> MAX_FILTER_HDOP = new GpxParameter<>("maxFilterHdop", "double", Double.class, Double.NaN);
	public static final GpxParameter<String> START_LAT = new GpxParameter<>("startLat", "double", String.class, null);
	public static final GpxParameter<String> START_LON = new GpxParameter<>("startLon", "double", String.class, null);
	public static final GpxParameter<String> NEAREST_CITY_NAME = new GpxParameter<>("nearestCityName", "TEXT", String.class, null);


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
		} else if (this == COLOR) {
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
		return Arrays.asList(TOTAL_DISTANCE, TOTAL_TRACKS, START_TIME, END_TIME,
				TIME_SPAN, TIME_MOVING, TOTAL_DISTANCE_MOVING, DIFF_ELEVATION_UP,
				DIFF_ELEVATION_DOWN, AVG_ELEVATION, MIN_ELEVATION, MAX_ELEVATION,
				MAX_SPEED, AVG_SPEED, POINTS, WPT_POINTS, WPT_CATEGORY_NAMES,
				START_LAT, START_LON);
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
