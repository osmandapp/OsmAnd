package net.osmand.plus.measurementtool;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.data.LatLon;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

import java.util.List;

public class MeasurementEditingContextUtils {

	static boolean isLastGpxPoint(List<RoutePlannerFrontEnd.GpxPoint> gpxPoints, int index) {
		if (index == gpxPoints.size() - 1) {
			return true;
		} else {
			for (int i = index + 1; i < gpxPoints.size(); i++) {
				RoutePlannerFrontEnd.GpxPoint gp = gpxPoints.get(i);
				for (int k = 0; k < gp.routeToTarget.size(); k++) {
					RouteSegmentResult seg = gp.routeToTarget.get(k);
					if (seg.getStartPointIndex() != seg.getEndPointIndex()) {
						return false;
					}
				}

			}
		}
		return true;
	}

	static int findPointIndex(WptPt point, List<WptPt> points, int firstIndex) {
		double minDistance = Double.MAX_VALUE;
		int index = 0;
		for (int i = Math.max(0, firstIndex); i < points.size(); i++) {
			double distance = MapUtils.getDistance(point.getLat(), point.getLon(), points.get(i).getLat(), points.get(i).getLon());
			if (distance < minDistance) {
				minDistance = distance;
				index = i;
			}
		}
		return index;
	}

	static long addPointToArray(List<WptPt> points, RouteSegmentResult seg, int index, float[] heightArray, long ts) {
		LatLon ll = seg.getPoint(index);
		WptPt pt = new WptPt();
		if (heightArray != null && heightArray.length > index * 2 + 1) {
			pt.setEle(heightArray[index * 2 + 1]);
		}
		pt.setLat(ll.getLatitude());
		pt.setLon(ll.getLongitude());
		if (ts > 0 && index != seg.getStartPointIndex()) {
			LatLon prevLatLon = seg.getPoint(index - (seg.isForwardDirection() ? +1 : -1));
			double distance = MapUtils.getDistance(ll, prevLatLon);
			float speed = seg.getSegmentSpeed();
			if (speed > 0) {
				ts += (long)(distance / speed * 1000.0);
			}
		}
		if (ts > 0) {
			pt.setTime(ts);
		}
		points.add(pt);
		return ts;
	}

	static long fillPointsArray(List<WptPt> points, RouteSegmentResult seg, boolean includeEndPoint, long timestamp) {
		int ind = seg.getStartPointIndex();
		boolean plus = seg.isForwardDirection();
		float[] heightArray = seg.getObject().calculateHeightArray();
		while (ind != seg.getEndPointIndex()) {
			timestamp = addPointToArray(points, seg, ind, heightArray, timestamp);
			ind = plus ? ind + 1 : ind - 1;
		}
		if (includeEndPoint) {
			timestamp = addPointToArray(points, seg, ind, heightArray, timestamp);
		}
		return timestamp;
	}
}
