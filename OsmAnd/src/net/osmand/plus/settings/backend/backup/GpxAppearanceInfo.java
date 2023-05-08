package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import static net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter.*;

public class GpxAppearanceInfo {

	public String width;
	public String coloringType;
	public int color;
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

	public GpxAppearanceInfo() {
	}

	public GpxAppearanceInfo(@NonNull GpxDataItem dataItem) {
		color = dataItem.getColor();
		width = dataItem.getWidth();
		showArrows = dataItem.isShowArrows();
		showStartFinish = dataItem.isShowStartFinish();
		splitType = dataItem.getSplitType();
		splitInterval = dataItem.getSplitInterval();
		coloringType = dataItem.getColoringType();

		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		if (analysis != null) {
			timeSpan = analysis.timeSpan;
			wptPoints = analysis.wptPoints;
			totalDistance = analysis.totalDistance;
		}

		smoothingThreshold = dataItem.getSmoothingThreshold();
		minFilterSpeed = dataItem.getMinFilterSpeed();
		maxFilterSpeed = dataItem.getMaxFilterSpeed();
		minFilterAltitude = dataItem.getMinFilterAltitude();
		maxFilterAltitude = dataItem.getMaxFilterAltitude();
		maxFilterHdop = dataItem.getMaxFilterHdop();
	}

	public void toJson(@NonNull JSONObject json) throws JSONException {
		writeParam(json, "color", color);
		writeParam(json, "width", width);
		writeParam(json, "show_arrows", showArrows);
		writeParam(json, "show_start_finish", showStartFinish);
		writeParam(json, "split_type", GpxSplitType.getSplitTypeByTypeId(splitType).getTypeName());
		writeParam(json, "split_interval", splitInterval);
		writeParam(json, "coloring_type", coloringType);

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
		if (ColoringType.getNullableTrackColoringTypeByName(gpxAppearanceInfo.coloringType) == null) {
			hasAnyParam |= json.has("gradient_scale_type");
			GradientScaleType scaleType = getScaleType(json.optString("gradient_scale_type"));
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			gpxAppearanceInfo.coloringType = coloringType == null
					? null : coloringType.getName(null);
		}

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

	private static void writeValidDouble(@NonNull JSONObject json, @NonNull String name, double value) throws JSONException{
		if (!Double.isNaN(value)) {
			json.putOpt(name, value);
		}
	}
}