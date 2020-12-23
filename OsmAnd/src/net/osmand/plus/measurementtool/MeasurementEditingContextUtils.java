package net.osmand.plus.measurementtool;

import net.osmand.GPXUtilities;
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

	static int findPointIndex(GPXUtilities.WptPt point, List<GPXUtilities.WptPt> points, int firstIndex) {
		double minDistance = Double.MAX_VALUE;
		int index = 0;
		for (int i = Math.max(0, firstIndex); i < points.size(); i++) {
			double distance = MapUtils.getDistance(point.lat, point.lon, points.get(i).lat, points.get(i).lon);
			if (distance < minDistance) {
				minDistance = distance;
				index = i;
			}
		}
		return index;
	}

	static void addPointToArray(List<GPXUtilities.WptPt> points, RouteSegmentResult seg, int index, float[] heightArray) {
		LatLon l = seg.getPoint(index);
		GPXUtilities.WptPt pt = new GPXUtilities.WptPt();
		if (heightArray != null && heightArray.length > index * 2 + 1) {
			pt.ele = heightArray[index * 2 + 1];
		}
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		points.add(pt);
	}

	static void fillPointsArray(List<GPXUtilities.WptPt> points, RouteSegmentResult seg, boolean includeEndPoint) {
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
