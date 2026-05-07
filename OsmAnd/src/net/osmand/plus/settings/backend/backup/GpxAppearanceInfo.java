package net.osmand.plus.settings.backend.backup;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;
import static net.osmand.shared.gpx.GpxParameter.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.DataItem;
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
	public Integer splitType;
	public Double splitInterval;
	public Boolean showArrows;
	public Boolean showStartFinish;

	public Long timeSpan;
	public Integer wptPoints;
	public Float totalDistance;

	public Double smoothingThreshold;
	public Double minFilterSpeed;
	public Double maxFilterSpeed;
	public Double minFilterAltitude;
	public Double maxFilterAltitude;
	public Double maxFilterHdop;
	private Gpx3DVisualizationType trackVisualizationType;
	private Gpx3DWallColorType trackWallColorType;
	private Gpx3DLinePositionType trackLinePositionType;
	private Float verticalExaggeration;
	private Float elevationMeters;

	public GpxAppearanceInfo(@NonNull JSONObject json) {
		fromJson(json);
	}

	public GpxAppearanceInfo(@NonNull OsmandApplication app, @NonNull DataItem item) {
		color = item.getParameter(COLOR);
		width = item.getParameter(WIDTH);
		showArrows = item.getParameter(SHOW_ARROWS);
		showStartFinish = item.getParameter(SHOW_START_FINISH);
		splitType = item.getParameter(SPLIT_TYPE);
		splitInterval = item.getParameter(SPLIT_INTERVAL);
		coloringType = item.getParameter(COLORING_TYPE);
		gradientPaletteName = item.getParameter(COLOR_PALETTE);

		trackVisualizationType = item.hasParameter(TRACK_VISUALIZATION_TYPE) ? Gpx3DVisualizationType.get3DVisualizationType(item.getParameter(TRACK_VISUALIZATION_TYPE)) : null;
		trackWallColorType = item.hasParameter(TRACK_3D_WALL_COLORING_TYPE) ? Gpx3DWallColorType.Companion.get3DWallColorType(item.getParameter(TRACK_3D_WALL_COLORING_TYPE)) : null;
		trackLinePositionType = item.hasParameter(TRACK_3D_LINE_POSITION_TYPE) ? Gpx3DLinePositionType.get3DLinePositionType(item.getParameter(TRACK_3D_LINE_POSITION_TYPE)) : null;
		verticalExaggeration = item.hasParameter(ADDITIONAL_EXAGGERATION) ? ((Double) item.getParameter(ADDITIONAL_EXAGGERATION)).floatValue() : null;
		elevationMeters = item.hasParameter(ELEVATION_METERS) ? ((Double) item.getParameter(ELEVATION_METERS)).floatValue() : null;

		if (item instanceof GpxDataItem gpxDataItem) {
			GpxTrackAnalysis analysis = gpxDataItem.getAnalysis();
			if (analysis != null) {
				timeSpan = analysis.getTimeSpan();
				wptPoints = analysis.getWptPoints();
				totalDistance = analysis.getTotalDistance();
			}
		}
		smoothingThreshold = item.getParameter(SMOOTHING_THRESHOLD);
		minFilterSpeed = item.getParameter(MIN_FILTER_SPEED);
		maxFilterSpeed = item.getParameter(MAX_FILTER_SPEED);
		minFilterAltitude = item.getParameter(MIN_FILTER_ALTITUDE);
		maxFilterAltitude = item.getParameter(MAX_FILTER_ALTITUDE);
		maxFilterHdop = item.getParameter(MAX_FILTER_HDOP);
	}

	public void toJson(@NonNull JSONObject json) throws JSONException {
		if (color != null && color != 0) {
			writeParam(json, TAG_COLOR, KAlgorithms.INSTANCE.colorToString(color));
		}
		writeParam(json, TAG_WIDTH, width);
		writeParam(json, TAG_SHOW_ARROWS, showArrows);
		writeParam(json, TAG_START_FINISH, showStartFinish);
		if (splitType != null) {
			writeParam(json, TAG_SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(splitType).getTypeName());
		}
		writeParam(json, TAG_SPLIT_INTERVAL, splitInterval);
		writeParam(json, TAG_COLORING_TYPE, coloringType);
		writeParam(json, TAG_COLOR_PALETTE, gradientPaletteName);

		if (trackVisualizationType != null) {
			writeParam(json, TAG_LINE_3D_VISUALIZATION_BY_TYPE, trackVisualizationType.getTypeName());
		}
		if (trackWallColorType != null) {
			writeParam(json, TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE, trackWallColorType.getTypeName());
		}
		if (trackLinePositionType != null) {
			writeParam(json, TAG_LINE_3D_VISUALIZATION_POSITION_TYPE, trackLinePositionType.getTypeName());
		}
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

	public void setParameters(@NonNull DataItem dataItem) {
		dataItem.setParameter(COLOR, color);
		dataItem.setParameter(WIDTH, width);
		dataItem.setParameter(SHOW_ARROWS, showArrows);
		dataItem.setParameter(SHOW_START_FINISH, showStartFinish);
		if (splitType != null) {
			dataItem.setParameter(SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(splitType).getType());
		}
		dataItem.setParameter(SPLIT_INTERVAL, splitInterval);
		dataItem.setParameter(COLORING_TYPE, coloringType);
		dataItem.setParameter(COLOR_PALETTE, gradientPaletteName);
	}

	private void fromJson(@NonNull JSONObject json) {
		if (json.has(TAG_COLOR)) {
			color = GpxUtilities.INSTANCE.parseColor(json.optString(TAG_COLOR));
		}
		if (json.has(TAG_WIDTH)) {
			width = json.optString(TAG_WIDTH);
		}
		if (json.has(TAG_SHOW_ARROWS)) {
			showArrows = json.optBoolean(TAG_SHOW_ARROWS);
		}
		if (json.has(TAG_START_FINISH)) {
			showStartFinish = json.optBoolean(TAG_START_FINISH);
		}
		if (json.has(TAG_SPLIT_TYPE)) {
			splitType = GpxSplitType.getSplitTypeByName(json.optString(TAG_SPLIT_TYPE)).getType();
		}
		if (json.has(TAG_SPLIT_INTERVAL)) {
			splitInterval = json.optDouble(TAG_SPLIT_INTERVAL);
		}

		if (json.has(TAG_COLORING_TYPE)) {
			String coloringTypeStr = json.optString(TAG_COLORING_TYPE);
			if (ColoringType.Companion.valueOf(ColoringPurpose.TRACK, coloringTypeStr) != null) {
				coloringType = coloringTypeStr;
			}
		}
		if (coloringType == null && json.has(TAG_GRADIENT_SCALE_TYPE)) {
			GradientScaleType scaleType = getScaleType(json.optString(TAG_GRADIENT_SCALE_TYPE));
			ColoringType coloringType = ColoringType.Companion.valueOf(scaleType);
			this.coloringType = coloringType != null ? coloringType.getName(null) : null;
		}
		if (json.has(TAG_COLOR_PALETTE)) {

			gradientPaletteName = json.optString(TAG_COLOR_PALETTE);
		}
		if (json.has(TAG_LINE_3D_VISUALIZATION_BY_TYPE)) {
			String trackVisualizationType = json.optString(TAG_LINE_3D_VISUALIZATION_BY_TYPE);
			this.trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
		}
		if (json.has(TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE)) {
			String trackWallColorType = json.optString(TAG_LINE_3D_VISUALIZATION_WALL_COLOR_TYPE);
			this.trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(trackWallColorType);
		}
		if (json.has(TAG_LINE_3D_VISUALIZATION_POSITION_TYPE)) {
			String trackLinePositionType = json.optString(TAG_LINE_3D_VISUALIZATION_POSITION_TYPE);
			this.trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
		}
		if (json.has(TAG_VERTICAL_EXAGGERATION_SCALE)) {
			verticalExaggeration = (float) json.optDouble(TAG_VERTICAL_EXAGGERATION_SCALE, MIN_VERTICAL_EXAGGERATION);
		}
		if (json.has(TAG_ELEVATION_METERS)) {
			elevationMeters = (float) json.optDouble(TAG_ELEVATION_METERS);
		}
		if (json.has(TAG_TIME_SPAN)) {
			timeSpan = json.optLong(TAG_TIME_SPAN);
		}
		if (json.has(TAG_WPT_POINTS)) {
			wptPoints = json.optInt(TAG_WPT_POINTS);
		}
		if (json.has(TAG_TOTAL_DISTANCE)) {
			totalDistance = (float) json.optDouble(TAG_TOTAL_DISTANCE);
		}
		if (json.has(TAG_SMOOTHING_THRESHOLD)) {
			smoothingThreshold = json.optDouble(TAG_SMOOTHING_THRESHOLD);
		}
		if (json.has(TAG_MIN_FILTER_SPEED)) {
			minFilterSpeed = json.optDouble(TAG_MIN_FILTER_SPEED);
		}
		if (json.has(TAG_MAX_FILTER_SPEED)) {
			maxFilterSpeed = json.optDouble(TAG_MAX_FILTER_SPEED);
		}
		if (json.has(TAG_MIN_FILTER_ALTITUDE)) {
			minFilterAltitude = json.optDouble(TAG_MIN_FILTER_ALTITUDE);
		}
		if (json.has(TAG_MAX_FILTER_ALTITUDE)) {
			maxFilterAltitude = json.optDouble(TAG_MAX_FILTER_ALTITUDE);
			if (json.has(TAG_MAX_FILTER_HDOP)) {
				maxFilterHdop = json.optDouble(TAG_MAX_FILTER_HDOP);
			}
		}
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