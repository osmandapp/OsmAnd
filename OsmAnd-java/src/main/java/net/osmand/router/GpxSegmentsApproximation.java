package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.MapUtils;

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10;
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private final double MAX_PENALTY_BY_GPX_ANGLE_M = 25; // penalty by the angle between gpx and road (25)
	private final double CRUSH_SEGMENTS_BY_DISTANCE_M = 25; // crush road segments to match gpx points better (25)

	private class MinDistResult {
		private double minDist;
		private RouteSegmentResult segment;
		// private int preciseIndex, preciseX, preciseY;
	}

	// if (DEBUG_IDS.indexOf((int)(pnt.getRoad().getId() / 64)) >= 0) { ... }
	// private List<Integer> DEBUG_IDS = Arrays.asList(499257893, 126338247, 237816930); // good, wrong, turn

	public GpxRouteApproximation fastGpxApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx,
	                                                  List<GpxPoint> gpxPoints) throws IOException {
		long timeToCalculate = System.nanoTime();

		initGpxPointsXY31(gpxPoints);

		float minPointApproximation = gctx.ctx.config.minPointApproximation;
		GpxPoint currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, 0);

		while (currentPoint != null && currentPoint.pnt != null) {
			double minDistAhead = Double.POSITIVE_INFINITY;
			MinDistResult bestMinDistResult = null;
			int minNextInd = -1;

			int start = currentPoint.ind + 1;
			int end = Math.min(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size());

			for (int j = start; j < end; j++) {
				GpxPoint ps = gpxPoints.get(j);

				double gpxAngle = Double.NaN;
				if (j > 0) {
					GpxPoint prev = gpxPoints.get(j - 1);
					gpxAngle = Math.atan2(ps.y31 - prev.y31, ps.x31 - prev.x31);
					if (gpxAngle < 0) gpxAngle += Math.PI;
				}

				MinDistResult currentMinDistResult = findMinDistInLoadedPoints(currentPoint, ps, gpxAngle);
				if (currentMinDistResult.minDist <= minDistAhead) {
					minDistAhead = currentMinDistResult.minDist;
					bestMinDistResult = currentMinDistResult;
					minNextInd = j;
				}
				if (MapUtils.getDistance(currentPoint.loc, gpxPoints.get(j).loc) > minPointApproximation) {
					break; // avoid shortcutting of loops
				}
			}

			if (minNextInd < 0) {
				break;
			}

			if (minDistAhead > minPointApproximation * minPointApproximation) {
				final int nextIndex = currentPoint.ind + 1;
				currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, nextIndex);
				continue;
			}

			RouteSegmentResult fres = bestMinDistResult.segment;
			fres.setGpxPointIndex(currentPoint.ind);

			currentPoint.routeToTarget = new ArrayList<>();
			currentPoint.routeToTarget.add(fres);
			currentPoint.targetInd = minNextInd;

			currentPoint = gpxPoints.get(minNextInd); // next point

			RouteSegment sg = gctx.ctx.loadRouteSegment(fres.getEndPointX(), fres.getEndPointY(),
					gctx.ctx.config.memoryLimitation);

			while (sg != null) {
				if (sg.getRoad().getId() != fres.getObject().getId() || sg.getSegmentEnd() != fres.getEndPointIndex()) {
					RouteSegmentPoint p = new RouteSegmentPoint(sg.getRoad(), sg.getSegmentStart(), sg.getSegmentEnd(),
							0);
					if (currentPoint.pnt == null) {
						currentPoint.pnt = p;
					} else {
						if (currentPoint.pnt.others == null) {
							currentPoint.pnt.others = new ArrayList<>();
						}
						currentPoint.pnt.others.add(p);
					}
				}
				sg = sg.getNext();
			}
		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);
		return gctx;
	}

	private boolean initRoutingPoint(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx, GpxPoint start,
	                                 double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			RouteSegmentPoint rsp = frontEnd.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(),
					gctx.ctx, null, false);
			if (rsp != null) {
				if (MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp;
				}
			}
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
	}

	private MinDistResult findMinDistInLoadedPoints(GpxPoint loadedPoint, GpxPoint nextPoint, double gpxAngle) {
		MinDistResult best = findMinDistInOnePoint(Double.POSITIVE_INFINITY, loadedPoint.pnt, nextPoint, gpxAngle);
		if (loadedPoint.pnt.others != null) {
			for (RouteSegmentPoint oth : loadedPoint.pnt.others) {
				MinDistResult fresh = findMinDistInOnePoint(best.minDist, oth, nextPoint, gpxAngle);
				if (fresh != null) {
					best = fresh;
				}
			}
		}
		return best;
	}

	private MinDistResult findMinDistInOnePoint(double minDistSqr, RouteSegmentPoint pnt, GpxPoint loc, double gpxAngle) {
		double newMinDist = 0;
		int bestSegmentEnd = -1;

		int startPointIndex = Math.max(0, pnt.getSegmentStart() - LOOKUP_AHEAD);
		int endPointIndex = Math.min(pnt.getRoad().getPointsLength(), pnt.getSegmentStart() + LOOKUP_AHEAD);

		for (int i = startPointIndex; i < endPointIndex; i++) {
			double dist = findMinDistInCrushedSegments(loc, pnt, i);
			if (bestSegmentEnd < 0 || dist < newMinDist) {
				bestSegmentEnd = i;
				newMinDist = dist;
			}
		}

		newMinDist += pnt.distToProj; // distToProj > 0 is only for pnt(s) after findRouteSegment

		// Add penalty by the difference between angle-to-next-gpx-point (gpxAngle) and average-road-segments-angle.
		double penalty = calcGpxAnglePenalty(pnt, pnt.getSegmentStart(), bestSegmentEnd, gpxAngle);
		newMinDist += Math.pow(penalty * MAX_PENALTY_BY_GPX_ANGLE_M, 2);

		MinDistResult result = new MinDistResult();

		if (newMinDist < minDistSqr && bestSegmentEnd >= 0) {
			result.segment = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), bestSegmentEnd);
			result.minDist = newMinDist;
			return result;
		}

		return null;
	}

	private double findMinDistInCrushedSegments(GpxPoint loc, RouteSegmentPoint pnt, int endIndex) {
		int x1 = pnt.getRoad().getPoint31XTile(endIndex);
		int y1 = pnt.getRoad().getPoint31YTile(endIndex);
		int x2 = pnt.getRoad().getPoint31XTile(endIndex > 0 ? endIndex - 1 : 0);
		int y2 = pnt.getRoad().getPoint31YTile(endIndex > 0 ? endIndex - 1 : 0);
		double segmentDistanceMeters = MapUtils.squareRootDist31(x1, y1, x2, y2);

		int nVirtualSegments = CRUSH_SEGMENTS_BY_DISTANCE_M > 0
				? (int) (segmentDistanceMeters / CRUSH_SEGMENTS_BY_DISTANCE_M) : 0;

		double minDistSqr = Double.POSITIVE_INFINITY;

		for (int i = 0; i <= nVirtualSegments; i++) {
			int px = nVirtualSegments > 0 ? (x1 - (x1 - x2) * i / nVirtualSegments) : x1;
			int py = nVirtualSegments > 0 ? (y1 - (y1 - y2) * i / nVirtualSegments) : y1;

			double distSqr = MapUtils.squareDist31TileMetric(px, py, loc.x31, loc.y31);

			if (distSqr < minDistSqr) {
				minDistSqr = distSqr;
			}
		}

		return minDistSqr;
	}

	private double calcGpxAnglePenalty(RouteSegmentPoint pnt, int start, int end, double gpxAngle) {
		if (Double.isNaN(gpxAngle)) return 0;
		if (start == end) return 0;
		if (start > end) {
			int swap = start;
			start = end;
			end = swap;
		}
		int counter = 0;
		double roadAngle = 0;
		for (int i = start; i < end; i++) {
			int sx = pnt.getRoad().getPoint31XTile(i);
			int sy = pnt.getRoad().getPoint31YTile(i);
			int ex = pnt.getRoad().getPoint31XTile(i + 1);
			int ey = pnt.getRoad().getPoint31YTile(i + 1);
			double segmentAngle = Math.atan2(ey - sy, ex - sx);
			if (segmentAngle < 0) segmentAngle += Math.PI;
			roadAngle += segmentAngle;
			counter++;
		}
		roadAngle /= counter;
		return Math.abs(gpxAngle - roadAngle) / Math.PI;
	}

	private GpxPoint findNextRoutablePoint(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx,
	                                       double distThreshold, List<GpxPoint> gpxPoints, int searchStart) throws IOException {
		for (int i = searchStart; i < gpxPoints.size(); i++) {
			if (initRoutingPoint(frontEnd, gctx, gpxPoints.get(i), distThreshold)) {
				return gpxPoints.get(i);
			}
		}
		return null;
	}

	private void initGpxPointsXY31(List<GpxPoint> gpxPoints) {
		for (GpxPoint p : gpxPoints) {
			if (TEST_SHIFT_GPX_POINTS) {
				final double shift = 0.00015; // shift ~15 meters to check attached geometry visually
				p.loc = new LatLon(p.loc.getLatitude() - shift, p.loc.getLongitude() + shift);
			}
			p.x31 = MapUtils.get31TileNumberX(p.loc.getLongitude());
			p.y31 = MapUtils.get31TileNumberY(p.loc.getLatitude());
		}
	}
}
