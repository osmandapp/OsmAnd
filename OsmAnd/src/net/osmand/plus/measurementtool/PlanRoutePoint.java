package net.osmand.plus.measurementtool;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.List;
import java.util.Map;

public class PlanRoutePoint {
	
	private final int index;
	private final MeasurementEditingContext editingCtx;
	
	public PlanRoutePoint(int index, @NonNull MeasurementEditingContext editingCtx) {
		this.index = index;
		this.editingCtx = editingCtx;
	}

	@NonNull
	public String getTitle(@NonNull Context context) {
		String pointName = editingCtx.getPoints().get(index).getName();
		if (!TextUtils.isEmpty(pointName)) {
			return pointName;
		}
		String prefix = context.getString(R.string.plugin_distance_point);
		return context.getString(R.string.ltr_or_rtl_combine_via_dash, prefix, String.valueOf(index + 1));
	}

	@NonNull
	public String getDescription(boolean before, @NonNull OsmandApplication app) {
		StringBuilder description = new StringBuilder();
		List<WptPt> points = editingCtx.getPoints();
		WptPt pt = points.get(index);
		String pointDesc = pt.getDesc();
		if (!TextUtils.isEmpty(pointDesc)) {
			description.append(pointDesc);
		} else if (index < 1 && before) {
			description.append(app.getString(R.string.start_point));
		} else {
			float distance = getTrimmedDistance(editingCtx, before);
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

	private float getTrimmedDistance(@NonNull MeasurementEditingContext editingCtx, boolean before) {
		List<WptPt> points = editingCtx.getPoints();
		Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = editingCtx.getRoadSegmentData();
		float dist = 0;
		int startIdx;
		int endIdx;
		if (before) {
			startIdx = 1;
			endIdx = index;
		} else {
			startIdx = index + 1;
			endIdx = points.size() - 1;
		}
		for (int i = startIdx; i <= endIdx; i++) {
			WptPt first = points.get(i - 1);
			WptPt second = points.get(i);
			Pair<WptPt, WptPt> pair = Pair.create(first, second);
			RoadSegmentData segment = roadSegmentData.get(pair);
			boolean routeSegmentBuilt = segment != null && segment.getDistance() > 0;
			dist += routeSegmentBuilt
					? segment.getDistance()
					: MapUtils.getDistance(first.getLat(), first.getLon(), second.getLat(), second.getLon());
		}
		return dist;
	}

	public int getIndex() {
		return index;
	}
}
