package net.osmand.plus.settings.backend.backup;

import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_MAX_FILTER_ALTITUDE;
import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_MAX_FILTER_HDOP;
import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_MAX_FILTER_SPEED;
import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_MIN_FILTER_ALTITUDE;
import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_MIN_FILTER_SPEED;
import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.TAG_SMOOTHING_THRESHOLD;
import static net.osmand.shared.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.shared.gpx.GpxParameter.ELEVATION_METERS;
import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_ALTITUDE;
import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_HDOP;
import static net.osmand.shared.gpx.GpxParameter.MAX_FILTER_SPEED;
import static net.osmand.shared.gpx.GpxParameter.MIN_FILTER_ALTITUDE;
import static net.osmand.shared.gpx.GpxParameter.MIN_FILTER_SPEED;
import static net.osmand.shared.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.shared.gpx.GpxParameter.SMOOTHING_THRESHOLD;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_LINE_POSITION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_WALL_COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

public class GpxAppearanceInfo {

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
		writeParam(json, "color", color);
		writeParam(json, "width", width);
		writeParam(json, "show_arrows", showArrows);
		writeParam(json, "show_start_finish", showStartFinish);
		writeParam(json, "split_type", GpxSplitType.getSplitTypeByTypeId(splitType).getTypeName());
		writeParam(json, "split_interval", splitInterval);
		writeParam(json, "coloring_type", coloringType);
		writeParam(json, "color_palette", gradientPaletteName);
		writeParam(json, "line_3d_visualization_by_type", trackVisualizationType.getTypeName());
		writeParam(json, "line_3d_visualization_wall_color_type", trackWallColorType.getTypeName());
		writeParam(json, "line_3d_visualization_position_type", trackLinePositionType.getTypeName());
		writeParam(json, "vertical_exaggeration_scale", verticalExaggeration);
		writeParam(json, "elevation_meters", elevationMeters);

		writeParam(json, "time_span", timeSpan);
		writeParam(json, "wpt_points", wptPoints);
		writeParam(json, "total_distance", totalDistance);

		writeValidDouble(json, TAG_SMOOTHING_THRESHOLD, smoothingThreshold);
		writeValidDouble(json, TAG_MIN_FILTER_SPEED, minFilterSpeed);
		writeValidDouble(json, TAG_MAX_FILTER_SPEED, maxFilterSpeed);
		writeValidDouble(json, TAG_MIN_FILTER_ALTITUDE, minFilterAltitude);
		writeValidDouble(json, TAG_MAX_FILTER_ALTITUDE, maxFilterSpeed);
		writeValidDouble(json, TAG_MAX_FILTER_HDOP, maxFilterHdop);
	}

	public static GpxAppearanceInfo fromJson(@NonNull JSONObject json) {
		GpxAppearanceInfo gpxAppearanceInfo = new GpxAppearanceInfo();
		boolean hasAnyParam = json.has("color");
		gpxAppearanceInfo.color = json.optInt("color");
		hasAnyParam |= json.has("width");
		gpxAppearanceInfo.width = json.optString("width");
		hasAnyParam |= json.has("show_arrows");
		gpxAppearanceInfo.showArrows = json.optBoolean("show_arrows");
		hasAnyParam |= json.has("show_start_finish");
		gpxAppearanceInfo.showStartFinish = json.optBoolean("show_start_finish");
		hasAnyParam |= json.has("split_type");
		gpxAppearanceInfo.splitType = GpxSplitType.getSplitTypeByName(json.optString("split_type")).getType();
		hasAnyParam |= json.has("split_interval");
		gpxAppearanceInfo.splitInterval = json.optDouble("split_interval");
		hasAnyParam |= json.has("coloring_type");
		gpxAppearanceInfo.coloringType = json.optString("coloring_type");
		if (ColoringType.Companion.valueOf(ColoringPurpose.TRACK, gpxAppearanceInfo.coloringType) == null) {
			hasAnyParam |= json.has("gradient_scale_type");
			GradientScaleType scaleType = getScaleType(json.optString("gradient_scale_type"));
			ColoringType coloringType = ColoringType.Companion.valueOf(scaleType);
			gpxAppearanceInfo.coloringType = coloringType == null
					? null : coloringType.getName(null);
		}

		hasAnyParam |= json.has("color_palette");
		gpxAppearanceInfo.gradientPaletteName = json.optString("color_palette");
		hasAnyParam |= json.has("line_3d_visualization_by_type");
		String trackVisualizationType = json.optString("line_3d_visualization_by_type");
		gpxAppearanceInfo.trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
		hasAnyParam |= json.has("line_3d_visualization_wall_color_type");
		String trackWallColorType = json.optString("line_3d_visualization_wall_color_type");
		gpxAppearanceInfo.trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(trackWallColorType);
		hasAnyParam |= json.has("line_3d_visualization_position_type");
		String trackLinePositionType = json.optString("line_3d_visualization_position_type");
		gpxAppearanceInfo.trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
		hasAnyParam |= json.has("vertical_exaggeration_scale");
		gpxAppearanceInfo.verticalExaggeration = (float) json.optDouble("vertical_exaggeration_scale", SRTMPlugin.MIN_VERTICAL_EXAGGERATION);
		hasAnyParam |= json.has("elevation_meters");
		gpxAppearanceInfo.elevationMeters = (float) json.optDouble("elevation_meters");

		hasAnyParam |= json.has("time_span");
		gpxAppearanceInfo.timeSpan = json.optLong("time_span");
		hasAnyParam |= json.has("wpt_points");
		gpxAppearanceInfo.wptPoints = json.optInt("wpt_points");
		hasAnyParam |= json.has("total_distance");
		gpxAppearanceInfo.totalDistance = (float) json.optDouble("total_distance");

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

	private static void writeParam(@NonNull JSONObject json, @NonNull String name, @Nullable Object value) throws JSONException {
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

	private static void writeValidDouble(@NonNull JSONObject json, @NonNull String name, double value) throws JSONException {
		if (!Double.isNaN(value)) {
			json.putOpt(name, value);
		}
	}
}