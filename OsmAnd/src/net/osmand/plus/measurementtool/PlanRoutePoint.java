package net.osmand.plus.measurementtool;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.List;
import java.util.Map;

public record PlanRoutePoint(int position) {

	@NonNull
	public String getTitle(@Nullable MapActivity activity) {
		if (activity == null) return "";
		MeasurementEditingContext editingCtx = activity.getMapLayers().getMeasurementToolLayer().getEditingCtx();
		List<WptPt> points = editingCtx.getPoints();
		if (position < 0 || position >= points.size()) {
			return "";
		}
		String pointName = points.get(position).getName();
		if (!TextUtils.isEmpty(pointName)) {
			return pointName;
		}
		String prefix = activity.getString(R.string.plugin_distance_point);
		return activity.getString(R.string.ltr_or_rtl_combine_via_dash, prefix, String.valueOf(position + 1));
	}

	@NonNull
	public String getSummary(@Nullable MapActivity mapActivity, boolean before) {
		if (mapActivity == null) return "";
		OsmandApplication app = AndroidUtils.getApp(mapActivity);
		MeasurementEditingContext editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();

		List<WptPt> points = editingCtx.getPoints();
		if (position < 0 || position >= points.size()) {
			return "";
		}
		WptPt point = points.get(position);
		StringBuilder builder = new StringBuilder();
		String description = point.getDesc();
		if (!TextUtils.isEmpty(description)) {
			builder.append(description);
		} else if (position < 1 && before) {
			builder.append(app.getString(R.string.start_point));
		} else {
			float distance = getTrimmedDistance(editingCtx, position, before);
			builder.append(OsmAndFormatter.getFormattedDistance(distance, app));
		}
		double elevation = point.getEle();
		if (!Double.isNaN(elevation)) {
			builder.append("  ").append((app.getString(R.string.altitude)).charAt(0)).append(": ");
			builder.append(OsmAndFormatter.getFormattedAlt(elevation, app));
		}
		float speed = (float) point.getSpeed();
		if (speed != 0) {
			builder.append("  ").append((app.getString(R.string.shared_string_speed)).charAt(0)).append(": ");
			builder.append(OsmAndFormatter.getFormattedSpeed(speed, app));
		}
		return builder.toString();
	}

	private float getTrimmedDistance(@NonNull MeasurementEditingContext editingCtx, int position, boolean before) {
		List<WptPt> points = editingCtx.getPoints();
		if (points.isEmpty() || position >= points.size()) {
			return 0;
		}
		Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = editingCtx.getRoadSegmentData();
		float dist = 0;
		int startIdx;
		int endIdx;
		if (before) {
			startIdx = 1;
			endIdx = position;
		} else {
			startIdx = position + 1;
			endIdx = points.size() - 1;
		}
		for (int i = startIdx; i <= endIdx; i++) {
			WptPt first = points.get(i - 1);
			WptPt second = points.get(i);
			Pair<WptPt, WptPt> pair = Pair.create(first, second);
			RoadSegmentData segment = roadSegmentData.get(pair);
			boolean routeSegmentBuilt = segment != null && segment.getDistance() > 0;
			dist += (float) (routeSegmentBuilt
					? segment.getDistance()
					: MapUtils.getDistance(first.getLat(), first.getLon(), second.getLat(), second.getLon()));
		}
		return dist;
	}
}
