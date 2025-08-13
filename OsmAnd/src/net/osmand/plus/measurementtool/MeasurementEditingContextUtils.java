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

	static WptPt addPointToArray(List<WptPt> points, RouteSegmentResult seg, int index, float[] heightArray) {
		LatLon l = seg.getPoint(index);
		WptPt pt = new WptPt();
		if (heightArray != null && heightArray.length > index * 2 + 1) {
			pt.setEle(heightArray[index * 2 + 1]);
		}
		pt.setLat(l.getLatitude());
		pt.setLon(l.getLongitude());
		points.add(pt);
		return pt;
	}

	static void fillPointsArray(List<WptPt> points, RouteSegmentResult seg, boolean includeEndPoint) {
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
}
