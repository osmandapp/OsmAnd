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

	public static class GpxTimeCalculator {
		private long timestamp = 0;
		private LatLon previousSegmentLastPoint = null;

		public GpxTimeCalculator(long initialTimestamp) {
			this.timestamp = initialTimestamp;
		}

		public void fillPointsArray(List<WptPt> points, RouteSegmentResult seg, boolean includeEndPoint) {
			int ind = seg.getStartPointIndex();
			boolean plus = seg.isForwardDirection();
			float[] heightArray = seg.getObject().calculateHeightArray();
			while (ind != seg.getEndPointIndex()) {
				addPointToArray(points, seg, ind, heightArray);
				ind = plus ? ind + 1 : ind - 1;
			}
			if (includeEndPoint) {
				addPointToArray(points, seg, ind, heightArray);
			}
		}

		private void addPointToArray(List<WptPt> points, RouteSegmentResult seg, int index, float[] heightArray) {
			LatLon ll = seg.getPoint(index);
			WptPt pt = new WptPt();

			pt.setLat(ll.getLatitude());
			pt.setLon(ll.getLongitude());

			// calculate elevation
			if (heightArray != null && heightArray.length > index * 2 + 1) {
				pt.setEle(heightArray[index * 2 + 1]);
			}

			// calculate time of gap between segments (e.g., no GPS in tunnels)
			if (timestamp > 0 && index == seg.getStartPointIndex() && previousSegmentLastPoint != null) {
				timestamp += calcTimeMs(seg, ll, previousSegmentLastPoint);
			}

			if (timestamp > 0) {
				pt.setTime(timestamp);
				pt.setSpeed(seg.getSegmentSpeed());
			}

			// calculate next timestamp inside current segment
			if (timestamp > 0 && index != seg.getEndPointIndex()) {
				LatLon nextLatLon = seg.getPoint(index + (seg.isForwardDirection() ? +1 : -1));
				timestamp += calcTimeMs(seg, ll, nextLatLon);
				previousSegmentLastPoint = nextLatLon;
			}

			points.add(pt);
		}

		private long calcTimeMs(RouteSegmentResult seg, LatLon a, LatLon b) {
			float speed = seg.getSegmentSpeed();
			double distance = MapUtils.getDistance(a, b);
			return distance > 0 && speed > 0 ? (long)(distance / speed * 1000.0) : 0;
		}

	}
}
