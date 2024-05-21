package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.util.*;

public class GpxRouteApproximation {
	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
	public final RoutingContext ctx;
	public int routeCalculations = 0;
	public int routePointsSearched = 0;
	public int routeDistCalculations = 0;
	public List<RoutePlannerFrontEnd.GpxPoint> finalPoints = new ArrayList<>();
	public List<RouteSegmentResult> fullRoute = new ArrayList<>();
	public int routeDistance;
	public int routeGapDistance;
	public int routeDistanceUnmatched;

	public GpxRouteApproximation(RoutingContext ctx) {
		this.ctx = ctx;
	}

	public GpxRouteApproximation(GpxRouteApproximation gctx) {
		this.ctx = gctx.ctx;
		this.routeDistance = gctx.routeDistance;
	}

	@Override
	public String toString() {
		return String.format(">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m unmatched",
				routeCalculations, routeDistCalculations, routePointsSearched, routeDistance, routeDistanceUnmatched);
	}

	public double distFromLastPoint(LatLon pnt) {
		if (fullRoute.size() > 0) {
			return MapUtils.getDistance(getLastPoint(), pnt);
		}
		return 0;
	}

	public LatLon getLastPoint() {
		if (fullRoute.size() > 0) {
			return fullRoute.get(fullRoute.size() - 1).getEndPoint();
		}
		return null;
	}

	public void applyExternalTimestamps(List<RoutePlannerFrontEnd.GpxPoint> sourcePoints) {
		if (!validateExternalTimestamps(sourcePoints)) {
			log.warn("applyExternalTimestamps() got invalid sourcePoints");
			return;
		}
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			for (RouteSegmentResult seg : gp.routeToTarget) {
				seg.setSegmentSpeed(calcSegmentSpeedByExternalTimestamps(gp, seg, sourcePoints));
			}
			RouteResultPreparation.recalculateTimeDistance(gp.routeToTarget);
		}
	}

	private float calcSegmentSpeedByExternalTimestamps(RoutePlannerFrontEnd.GpxPoint gp, RouteSegmentResult seg,
	                                                   List<RoutePlannerFrontEnd.GpxPoint> sourcePoints) {
		float speed = seg.getSegmentSpeed();
		int indexStart = gp.ind, indexEnd = gp.targetInd;

		if (indexEnd == -1 && indexStart >= 0 && indexStart + 1 < sourcePoints.size()) {
			indexEnd = indexStart + 1; // this is straight line
		}

		if (indexStart >=0 && indexEnd > 0 && indexStart < indexEnd) {
			long time = sourcePoints.get(indexEnd).time - sourcePoints.get(indexStart).time;
			if (time > 0) {
				double distance = 0;
				for (int i = indexStart; i < indexEnd; i++) {
					distance += MapUtils.getDistance(sourcePoints.get(i).loc, sourcePoints.get(i + 1).loc);
				}
				if (distance > 0) {
					speed = (float) distance / ((float) time / 1000); // update based on external timestamps
				}
			}
		}

		return speed;
	}

	private boolean validateExternalTimestamps(List<RoutePlannerFrontEnd.GpxPoint> points) {
		if (points == null || points.isEmpty()) {
			return false;
		}
		long last = 0;
		for (RoutePlannerFrontEnd.GpxPoint p : points) {
			if (p.time == 0 || p.time < last) {
				return false;
			}
			last = p.time;
		}
		return true;
	}

	public List<RouteSegmentResult> collectFinalPointsAsRoute() {
		List<RouteSegmentResult> route = new ArrayList<RouteSegmentResult>();
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			route.addAll(gp.routeToTarget);
		}
		return Collections.unmodifiableList(route);
	}

	public void reconstructFinalPointsFromFullRoute() {
		// create gpx-to-final index map, clear routeToTarget(s)
		Map<Integer, Integer> gpxIndexFinalIndex = new HashMap<>();
		for (int i = 0; i < finalPoints.size(); i++) {
			gpxIndexFinalIndex.put(finalPoints.get(i).ind, i);
			finalPoints.get(i).routeToTarget.clear();
		}

		// reconstruct routeToTarget from scratch
		int lastIndex = 0;
		for (RouteSegmentResult seg : fullRoute) {
			int index = seg.getGpxPointIndex();
			if (index == -1) {
				index = lastIndex;
			} else {
				lastIndex = index;
			}
			finalPoints.get(gpxIndexFinalIndex.get(index)).routeToTarget.add(seg);
		}

		// finally remove finalPoints with empty route
		List<RoutePlannerFrontEnd.GpxPoint> emptyFinalPoints = new ArrayList<>();
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			if (gp.routeToTarget != null) {
				if (gp.routeToTarget.size() == 0) {
					emptyFinalPoints.add(gp);
				}
			}
		}
		if (emptyFinalPoints.size() > 0) {
			finalPoints.removeAll(emptyFinalPoints);
		}
	}
}
