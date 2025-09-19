package net.osmand.plus.settings.backend.backup;

import static net.osmand.shared.gpx.GpxParameter.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.shared.util.KAlgorithms;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class GpxAppearanceInfo {
	public static final String TAG_COLOR = "color";
	public static final String TAG_WIDTH = "width";
	public static final String TAG_SHOW_ARROWS = "show_arrows";
	public static final String TAG_START_FINISH = "show_start_finish";
	public static final String TAG_SPLIT_TYPE = "split_type";
	public static final String TAG_SPLIT_INTERVAL = "split_interval";
	public static final String TAG_COLORING_TYPE = "coloring_type";
	public static final String TAG_COLOR_PALETTE = "color_palette";
	public static final String TAG_LINE_3D_VISUALIZATION_BY_TYPE = "line_3d_visualization_by_type";
	public static final String TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE = "line_3d_visualization_wall_color_type";
	public static final String TAG_LINE_3D_VISUALIZATION_POSITION_TYPE = "line_3d_visualization_position_type";
	public static final String TAG_VERTICAL_EXAGGERATION_SCALE = "vertical_exaggeration_scale";
	public static final String TAG_ELEVATION_METERS = "elevation_meters";
	public static final String TAG_TIME_SPAN = "time_span";
	public static final String TAG_WPT_POINTS = "wpt_points";
	public static final String TAG_TOTAL_DISTANCE = "total_distance";
	public static final String TAG_GRADIENT_SCALE_TYPE = "gradient_scale_type";
	public static final String TAG_SMOOTHING_THRESHOLD = "smoothing_threshold";
	public static final String TAG_MIN_FILTER_SPEED = "min_filter_speed";
	public static final String TAG_MAX_FILTER_SPEED = "max_filter_speed";
	public static final String TAG_MIN_FILTER_ALTITUDE = "min_filter_altitude";
	public static final String TAG_MAX_FILTER_ALTITUDE = "max_filter_altitude";
	public static final String TAG_MAX_FILTER_HDOP = "max_filter_hdop";
	public static final String TAG_IS_JOIN_SEGMENTS = "is_join_segments";

	public static final Set<String> gpxAppearanceTags = Set.of(
			TAG_COLOR,
			TAG_WIDTH,
			TAG_SHOW_ARROWS,
			TAG_START_FINISH,
			TAG_SPLIT_TYPE,
			TAG_SPLIT_INTERVAL,
			TAG_COLORING_TYPE,
			TAG_COLOR_PALETTE,
			TAG_LINE_3D_VISUALIZATION_BY_TYPE,
			TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE,
			TAG_LINE_3D_VISUALIZATION_POSITION_TYPE,
			TAG_VERTICAL_EXAGGERATION_SCALE,
			TAG_ELEVATION_METERS,
			TAG_TIME_SPAN,
			TAG_WPT_POINTS,
			TAG_TOTAL_DISTANCE,
			TAG_GRADIENT_SCALE_TYPE,
			TAG_SMOOTHING_THRESHOLD,
			TAG_MIN_FILTER_SPEED,
			TAG_MAX_FILTER_SPEED,
			TAG_MIN_FILTER_ALTITUDE,
			TAG_MAX_FILTER_ALTITUDE,
			TAG_MAX_FILTER_HDOP,
			TAG_IS_JOIN_SEGMENTS
	);

	public static boolean isGpxAppearanceTag(@NonNull String tag) {
		return gpxAppearanceTags.contains(tag);
	}

	public String width;
	public String coloringType;
	public String gradientPaletteName;
	public Integer color;
	public int splitType;
	public double splitInterval;
	public boolean showArrows;
	public boolean showStartFinish;

	public long timeSpan;
	public int wptPoints;
	public float totalDistance;

	public double smoothingThreshold = Double.NaN;
	public double minFilterSpeed = Double.NaN;
	public double maxFilterSpeed = Double.NaN;
	public double minFilterAltitude = Double.NaN;
	public double maxFilterAltitude = Double.NaN;
	public double maxFilterHdop = Double.NaN;
	private Gpx3DVisualizationType trackVisualizationType = Gpx3DVisualizationType.NONE;
	private Gpx3DWallColorType trackWallColorType = Gpx3DWallColorType.NONE;
	private Gpx3DLinePositionType trackLinePositionType = Gpx3DLinePositionType.TOP;
	private float verticalExaggeration = 1f;
	private float elevationMeters = 1000f;

	public GpxAppearanceInfo() {
	}

	public GpxAppearanceInfo(@NonNull OsmandApplication app, @NonNull GpxDataItem item) {
		GpxAppearanceHelper helper = new GpxAppearanceHelper(app);
		color = helper.getParameter(item, COLOR);
		width = helper.getParameter(item, WIDTH);
		showArrows = helper.requireParameter(item, SHOW_ARROWS);
		showStartFinish = helper.requireParameter(item, SHOW_START_FINISH);
		splitType = helper.requireParameter(item, SPLIT_TYPE);
		splitInterval = helper.requireParameter(item, SPLIT_INTERVAL);
		coloringType = helper.getParameter(item, COLORING_TYPE);
		gradientPaletteName = helper.getParameter(item, COLOR_PALETTE);
		trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(helper.getParameter(item, TRACK_VISUALIZATION_TYPE));
		trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(helper.getParameter(item, TRACK_3D_WALL_COLORING_TYPE));
		trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(helper.getParameter(item, TRACK_3D_LINE_POSITION_TYPE));
		verticalExaggeration = ((Double) helper.requireParameter(item, ADDITIONAL_EXAGGERATION)).floatValue();
		elevationMeters = ((Double) helper.requireParameter(item, ELEVATION_METERS)).floatValue();

		GpxTrackAnalysis analysis = item.getAnalysis();
		if (analysis != null) {
			timeSpan = analysis.getTimeSpan();
			wptPoints = analysis.getWptPoints();
			totalDistance = analysis.getTotalDistance();
		}
		smoothingThreshold = item.requireParameter(SMOOTHING_THRESHOLD);
		minFilterSpeed = item.requireParameter(MIN_FILTER_SPEED);
		maxFilterSpeed = item.requireParameter(MAX_FILTER_SPEED);
		minFilterAltitude = item.requireParameter(MIN_FILTER_ALTITUDE);
		maxFilterAltitude = item.requireParameter(MAX_FILTER_ALTITUDE);
		maxFilterHdop = item.requireParameter(MAX_FILTER_HDOP);
	}

	public void toJson(@NonNull JSONObject json) throws JSONException {
		if (color != null && color != 0) {
			writeParam(json, TAG_COLOR, KAlgorithms.INSTANCE.colorToString(color));
		}
		writeParam(json, TAG_WIDTH, width);
		writeParam(json, TAG_SHOW_ARROWS, showArrows);
		writeParam(json, TAG_START_FINISH, showStartFinish);
		writeParam(json, TAG_SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(splitType).getTypeName());
		writeParam(json, TAG_SPLIT_INTERVAL, splitInterval);
		writeParam(json, TAG_COLORING_TYPE, coloringType);
		writeParam(json, TAG_COLOR_PALETTE, gradientPaletteName);
		writeParam(json, TAG_LINE_3D_VISUALIZATION_BY_TYPE, trackVisualizationType.getTypeName());
		writeParam(json, TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE, trackWallColorType.getTypeName());
		writeParam(json, TAG_LINE_3D_VISUALIZATION_POSITION_TYPE, trackLinePositionType.getTypeName());
		writeParam(json, TAG_VERTICAL_EXAGGERATION_SCALE, verticalExaggeration);
		writeParam(json, TAG_ELEVATION_METERS, elevationMeters);

		writeParam(json, TAG_TIME_SPAN, timeSpan);
		writeParam(json, TAG_WPT_POINTS, wptPoints);
		writeParam(json, TAG_TOTAL_DISTANCE, totalDistance);

		writeValidDouble(json, TAG_SMOOTHING_THRESHOLD, smoothingThreshold);
		writeValidDouble(json, TAG_MIN_FILTER_SPEED, minFilterSpeed);
		writeValidDouble(json, TAG_MAX_FILTER_SPEED, maxFilterSpeed);
		writeValidDouble(json, TAG_MIN_FILTER_ALTITUDE, minFilterAltitude);
		writeValidDouble(json, TAG_MAX_FILTER_ALTITUDE, maxFilterSpeed);
		writeValidDouble(json, TAG_MAX_FILTER_HDOP, maxFilterHdop);
	}

	public static GpxAppearanceInfo fromJson(@NonNull JSONObject json) {
		GpxAppearanceInfo gpxAppearanceInfo = new GpxAppearanceInfo();
		boolean hasAnyParam = json.has(TAG_COLOR);
		gpxAppearanceInfo.color = GpxUtilities.INSTANCE.parseColor(json.optString(TAG_COLOR));
		hasAnyParam |= json.has(TAG_WIDTH);
		gpxAppearanceInfo.width = json.optString(TAG_WIDTH);
		hasAnyParam |= json.has(TAG_SHOW_ARROWS);
		gpxAppearanceInfo.showArrows = json.optBoolean(TAG_SHOW_ARROWS);
		hasAnyParam |= json.has(TAG_START_FINISH);
		gpxAppearanceInfo.showStartFinish = json.optBoolean(TAG_START_FINISH);
		hasAnyParam |= json.has(TAG_SPLIT_TYPE);
		gpxAppearanceInfo.splitType = GpxSplitType.getSplitTypeByName(json.optString(TAG_SPLIT_TYPE)).getType();
		hasAnyParam |= json.has(TAG_SPLIT_INTERVAL);
		gpxAppearanceInfo.splitInterval = json.optDouble(TAG_SPLIT_INTERVAL);
		hasAnyParam |= json.has(TAG_COLORING_TYPE);
		gpxAppearanceInfo.coloringType = json.optString(TAG_COLORING_TYPE);
		if (ColoringType.Companion.valueOf(ColoringPurpose.TRACK, gpxAppearanceInfo.coloringType) == null) {
			hasAnyParam |= json.has(TAG_GRADIENT_SCALE_TYPE);
			GradientScaleType scaleType = getScaleType(json.optString(TAG_GRADIENT_SCALE_TYPE));
			ColoringType coloringType = ColoringType.Companion.valueOf(scaleType);
			gpxAppearanceInfo.coloringType = coloringType == null
					? null : coloringType.getName(null);
		}

		hasAnyParam |= json.has(TAG_COLOR_PALETTE);
		gpxAppearanceInfo.gradientPaletteName = json.optString(TAG_COLOR_PALETTE);
		hasAnyParam |= json.has(TAG_LINE_3D_VISUALIZATION_BY_TYPE);
		String trackVisualizationType = json.optString(TAG_LINE_3D_VISUALIZATION_BY_TYPE);
		gpxAppearanceInfo.trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
		hasAnyParam |= json.has(TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE);
		String trackWallColorType = json.optString(TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE);
		gpxAppearanceInfo.trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(trackWallColorType);
		hasAnyParam |= json.has(TAG_LINE_3D_VISUALIZATION_POSITION_TYPE);
		String trackLinePositionType = json.optString(TAG_LINE_3D_VISUALIZATION_POSITION_TYPE);
		gpxAppearanceInfo.trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
		hasAnyParam |= json.has(TAG_VERTICAL_EXAGGERATION_SCALE);
		gpxAppearanceInfo.verticalExaggeration = (float) json.optDouble(TAG_VERTICAL_EXAGGERATION_SCALE, SRTMPlugin.MIN_VERTICAL_EXAGGERATION);
		hasAnyParam |= json.has(TAG_ELEVATION_METERS);
		gpxAppearanceInfo.elevationMeters = (float) json.optDouble(TAG_ELEVATION_METERS);

		hasAnyParam |= json.has(TAG_TIME_SPAN);
		gpxAppearanceInfo.timeSpan = json.optLong(TAG_TIME_SPAN);
		hasAnyParam |= json.has(TAG_WPT_POINTS);
		gpxAppearanceInfo.wptPoints = json.optInt(TAG_WPT_POINTS);
		hasAnyParam |= json.has(TAG_TOTAL_DISTANCE);
		gpxAppearanceInfo.totalDistance = (float) json.optDouble(TAG_TOTAL_DISTANCE);

		hasAnyParam |= json.has(TAG_SMOOTHING_THRESHOLD);
		gpxAppearanceInfo.smoothingThreshold = json.optDouble(TAG_SMOOTHING_THRESHOLD);
		hasAnyParam |= json.has(TAG_MIN_FILTER_SPEED);
		gpxAppearanceInfo.minFilterSpeed = json.optDouble(TAG_MIN_FILTER_SPEED);
		hasAnyParam |= json.has(TAG_MAX_FILTER_SPEED);
		gpxAppearanceInfo.maxFilterSpeed = json.optDouble(TAG_MAX_FILTER_SPEED);
		hasAnyParam |= json.has(TAG_MIN_FILTER_ALTITUDE);
		gpxAppearanceInfo.minFilterAltitude = json.optDouble(TAG_MIN_FILTER_ALTITUDE);
		hasAnyParam |= json.has(TAG_MAX_FILTER_ALTITUDE);
		gpxAppearanceInfo.maxFilterAltitude = json.optDouble(TAG_MAX_FILTER_ALTITUDE);
		hasAnyParam |= json.has(TAG_MAX_FILTER_HDOP);
		gpxAppearanceInfo.maxFilterHdop = json.optDouble(TAG_MAX_FILTER_HDOP);

		return hasAnyParam ? gpxAppearanceInfo : null;
	}

	private static GradientScaleType getScaleType(String name) {
		if (!Algorithms.isEmpty(name)) {
			try {
				return GradientScaleType.valueOf(name);
			} catch (IllegalStateException e) {
				SettingsHelper.LOG.error("Failed to read gradientScaleType", e);
			}
		}
		return null;
	}

	private static void writeParam(@NonNull JSONObject json, @NonNull String name,
			@Nullable Object value) throws JSONException {
		if (value instanceof Integer) {
			if ((Integer) value != 0) {
				json.putOpt(name, value);
			}
		} else if (value instanceof Long) {
			if ((Long) value != 0) {
				json.putOpt(name, value);
			}
		} else if (value instanceof Double) {
			if ((Double) value != 0.0 && !Double.isNaN((Double) value)) {
				json.putOpt(name, value);
			}
		} else if (value instanceof String) {
			if (!Algorithms.isEmpty((String) value)) {
				json.putOpt(name, value);
			}
		} else if (value != null) {
			json.putOpt(name, value);
		}
	}

	private static void writeValidDouble(@NonNull JSONObject json, @NonNull String name,
			double value) throws JSONException {
		if (!Double.isNaN(value)) {
			json.putOpt(name, value);
		}
	}
}