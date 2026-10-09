package net.osmand.plus.routing;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

import java.util.List;

public class RouteSegmentSearchResult {
	private final int roadIndex;
	private final int segmentIndex;
	private final QuadPointDouble point;

	RouteSegmentSearchResult(int roadIndex, int segmentIndex, QuadPointDouble point) {
		this.roadIndex = roadIndex;
		this.segmentIndex = segmentIndex;
		this.point = point;
	}

	public int getRoadIndex() {
		return roadIndex;
	}

	public int getSegmentIndex() {
		return segmentIndex;
	}

	public QuadPointDouble getPoint() {
		return point;
	}

	public static RouteSegmentSearchResult searchRouteSegment(double latitude, double longitude, double maxDist, List<RouteSegmentResult> roads) {
		int roadIndex = -1;
		QuadPointDouble point = null;
		int segmentIndex = -1;
		int px = MapUtils.get31TileNumberX(longitude);
		int py = MapUtils.get31TileNumberY(latitude);
		double dist = maxDist < 0 ? 1000 : maxDist;
		for (int i = 0; i < roads.size(); i++) {
			RouteSegmentResult road = roads.get(i);
			int startPointIndex = Math.min(road.getStartPointIndex(), road.getEndPointIndex());
			int endPointIndex = Math.max(road.getEndPointIndex(), road.getStartPointIndex());
			RouteDataObject obj = road.getObject();
			for (int j = startPointIndex + 1; j <= endPointIndex; j++) {
				QuadPointDouble proj = MapUtils.getProjectionPoint31(px, py, obj.getPoint31XTile(j - 1), obj.getPoint31YTile(j - 1),
						obj.getPoint31XTile(j), obj.getPoint31YTile(j));
				double dd = MapUtils.squareRootDist31((int) proj.x, (int) proj.y, px, py);
				if (dd < dist) {
					dist = dd;
					roadIndex = i;
					segmentIndex = j;
					point = proj;
				}
			}
		}
		return roadIndex != -1 ? new RouteSegmentSearchResult(roadIndex, segmentIndex, point) : null;
	}
}
