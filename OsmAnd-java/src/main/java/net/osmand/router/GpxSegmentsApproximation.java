package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

// TODO fix overlapped segments/U-turn (use precise X/Y)
// TODO init distToProjSquare (new RouteSegmentPoint) to next-gpx-point
// TODO distToProj (any) should be replaced with distToProj to next-gpx-point

// TO-THINK ? fix minor "Points are not connected" (~0.01m)
// TO-THINK ? makePrecise for start / end segments (just check how correctly they are calculated)

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10;
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private final double MAX_PENALTY_BY_GPX_ANGLE_M = 25; // penalty by the difference between gpx and road angle (25)
	private final double CRUSH_SEGMENTS_BY_DISTANCE_M = 25; // crush road segments to match gpx points better (25)

	private class MinDistResult {
		private double minDist;
		private RouteSegmentResult segment;
		private int preciseIndex, preciseX, preciseY; // use to fix overlapped segments
		private double penalty; // DEBUG: count penalties > 0.1 (rescuetrack should be empty)
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

		connectGpxPoints(gpxPoints);

		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);

		return gctx;
	}

	private void connectGpxPoints(List<GpxPoint> gpxPoints) {
		for (int i = 0, x = -1, y = -1; i < gpxPoints.size();) {
			GpxPoint p = gpxPoints.get(i);
			if (x >= 0 && y >= 0 && !Algorithms.isEmpty(p.routeToTarget)) {
				RouteSegmentResult segment = p.getFirstRouteRes();
				int px = segment.getStartPointX();
				int py = segment.getStartPointY();
				double dist = MapUtils.squareRootDist31(x, y, px, py);
				if (dist > 0) {
//					System.err.printf("WARN: XXX [%d] !!! %.2f\n", i, dist);
					segment.getObject().pointsX[segment.getStartPointIndex()] = x;
					segment.getObject().pointsY[segment.getStartPointIndex()] = y;
				}
			}
			if (!Algorithms.isEmpty(p.routeToTarget)) {
				RouteSegmentResult segment = p.getLastRouteRes();
				x = segment.getEndPointX();
				y = segment.getEndPointY();
				i = p.targetInd;
//				System.err.printf("WARN: XXX [%d] route (%d, %d) - %d (%s)\n", i, x, y, p.targetInd, p.routeToTarget);
			} else {
				p = gpxPoints.get(i - 1);
				x = MapUtils.get31TileNumberX(p.loc.getLongitude());
				y = MapUtils.get31TileNumberY(p.loc.getLatitude());
//				System.err.printf("WARN: XXX [%d] gpx (%d, %d) - %d\n", i, x, y, p.targetInd);
				i++;
			}
		}
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
		MinDistResult best = findOneMinDist(Double.POSITIVE_INFINITY, loadedPoint.pnt, nextPoint, gpxAngle);
		if (loadedPoint.pnt.others != null) {
			for (RouteSegmentPoint oth : loadedPoint.pnt.others) {
				MinDistResult fresh = findOneMinDist(best.minDist, oth, nextPoint, gpxAngle);
				if (fresh != null) {
					best = fresh;
				}
			}
		}
		return best;
	}

	private MinDistResult findOneMinDist(double minDistSqr, RouteSegmentPoint pnt, GpxPoint loc, double gpxAngle) {
		double newMinDist = 0;
		int bestSegmentEnd = -1;
		int preciseX = -1, preciseY = -1;

		int startPointIndex = Math.max(0, pnt.getSegmentStart() - LOOKUP_AHEAD);
		int endPointIndex = Math.min(pnt.getRoad().getPointsLength(), pnt.getSegmentStart() + LOOKUP_AHEAD);

		for (int i = startPointIndex; i < endPointIndex; i++) {
			int[] resultPreciseXY = { -1, -1 };
			double dist = findPreciseMinDist(loc, pnt, i, resultPreciseXY);
			if (bestSegmentEnd < 0 || dist < newMinDist) {
				preciseX = resultPreciseXY[0];
				preciseY = resultPreciseXY[1];
				bestSegmentEnd = i;
				newMinDist = dist;
			}
		}

		newMinDist += pnt.distToProj; // distToProj > 0 is only for pnt(s) after findRouteSegment

		// Add penalty by the difference between angle-to-next-gpx-point (gpxAngle) and average-road-segments-angle.
		// Maximum is limited to MAX_PENALTY_BY_GPX_ANGLE_M and should be not less than default minPointApproximation.
		double penalty = calcGpxAnglePenalty(pnt, pnt.getSegmentStart(), bestSegmentEnd, gpxAngle);
		newMinDist += Math.pow(penalty * MAX_PENALTY_BY_GPX_ANGLE_M, 2);

		MinDistResult result = new MinDistResult();

		if (newMinDist < minDistSqr && bestSegmentEnd >= 0) {
			if (preciseX != -1 && preciseY != -1) {
				result.preciseX = preciseX;
				result.preciseY = preciseY;
				result.preciseIndex = bestSegmentEnd;
				result.penalty = penalty;
			}
			result.segment = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), bestSegmentEnd);
			result.minDist = newMinDist;
			return result;
		}

		return null;
	}

	private double findPreciseMinDist(GpxPoint loc, RouteSegmentPoint pnt, int endIndex, int [] resultXY) {
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
				resultXY[0] = px;
				resultXY[1] = py;
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
