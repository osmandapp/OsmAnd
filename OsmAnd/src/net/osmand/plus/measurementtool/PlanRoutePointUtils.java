package net.osmand.plus.measurementtool;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.List;
import java.util.Map;

public class PlanRoutePointUtils {

	@NonNull
	public String getPointTitle(@Nullable MapActivity activity, int position) {
		if (activity == null) return "";
		MeasurementEditingContext editingCtx = activity.getMapLayers().getMeasurementToolLayer().getEditingCtx();

		String pointName = editingCtx.getPoints().get(position).getName();
		if (!TextUtils.isEmpty(pointName)) {
			return pointName;
		}
		String prefix = activity.getString(R.string.plugin_distance_point);
		return activity.getString(R.string.ltr_or_rtl_combine_via_dash, prefix, String.valueOf(position + 1));
	}

	@NonNull
	public String getPointSummary(@Nullable MapActivity mapActivity, int position, boolean before) {
		if (mapActivity == null) return "";
		OsmandApplication app = mapActivity.getApp();
		MeasurementEditingContext editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();

		StringBuilder description = new StringBuilder();
		List<WptPt> points = editingCtx.getPoints();
		WptPt pt = points.get(position);
		String pointDesc = pt.getDesc();
		if (!TextUtils.isEmpty(pointDesc)) {
			description.append(pointDesc);
		} else if (position < 1 && before) {
			description.append(app.getString(R.string.start_point));
		} else {
			float distance = getTrimmedDistance(editingCtx, position, before);
			description.append(OsmAndFormatter.getFormattedDistance(distance, app));
		}
		double elevation = pt.getEle();
		if (!Double.isNaN(elevation)) {
			description.append("  ").append((app.getString(R.string.altitude)).charAt(0)).append(": ");
			description.append(OsmAndFormatter.getFormattedAlt(elevation, app));
		}
		float speed = (float) pt.getSpeed();
		if (speed != 0) {
			description.append("  ").append((app.getString(R.string.shared_string_speed)).charAt(0)).append(": ");
			description.append(OsmAndFormatter.getFormattedSpeed(speed, app));
		}
		return description.toString();
	}

	private float getTrimmedDistance(@NonNull MeasurementEditingContext editingCtx,
	                                 int position, boolean before) {
		List<WptPt> points = editingCtx.getPoints();
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
