package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.MapUtils;

// TO-THINK ? fix minor "Points are not connected" (~0.01m)
// TO-THINK ? think about "bearing" in addition to LOOKUP_AHEAD to keep sharp/loop-shaped gpx parts
// TO-THINK ? makePrecise for start / end segments (just check how correctly they are calculated)

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10;
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private final double DILUTE_BY_SEGMENT_DISTANCE = 0.001; // add a fraction of seg dist to pnt-to-gpx dist (0.001)
	private final double CRUSH_SEGMENTS_BY_DISTANCE_M = 10; // crush road segments to match GPX points better (meters)

	private class MinDistResult {
		private double minDist;
		private RouteSegmentResult segment;
		private int preciseIndex, preciseX, preciseY;
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

			double bonus = 1.0; // TODO remove

			int start = currentPoint.ind + 1;
			int end = Math.min(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size());

			for (int j = start; j < end; j++) {
				GpxPoint ps = gpxPoints.get(j);
				MinDistResult currentMinDistResult = findMinDistInLoadedPoints(currentPoint, ps);
//				System.err.printf("WARN: XXX min %.2f cur %.2f (%.2f) bonus %.2f\n",
//						minDistAhead, currentMinDistResult.minDist, currentMinDistResult.minDist * bonus, bonus);
				if (currentMinDistResult.minDist * bonus <= minDistAhead) {
					minDistAhead = currentMinDistResult.minDist;
					bestMinDistResult = currentMinDistResult;
					minNextInd = j;
				}
				if (MapUtils.getDistance(currentPoint.loc, gpxPoints.get(j).loc) > minPointApproximation) {
					break; // avoid shortcutting of loops
				}
//				bonus *= 0.8;
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
							0); // TODO init distToProjSquare to next-gpx-point
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

	private MinDistResult findMinDistInLoadedPoints(GpxPoint loadedPoint, GpxPoint nextPoint) {
		MinDistResult best = findOneMinDist(Double.POSITIVE_INFINITY, loadedPoint.pnt, nextPoint);
		if (loadedPoint.pnt.others != null) {
			for (RouteSegmentPoint oth : loadedPoint.pnt.others) {
				MinDistResult fresh = findOneMinDist(best.minDist, oth, nextPoint);
				if (fresh != null) {
					best = fresh;
				}
			}
		}
		return best;
	}

	private MinDistResult findOneMinDist(double minDistSqr, RouteSegmentPoint pnt, GpxPoint loc) {
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

		// TODO distToProj (any) must be replaced with distToProj to the next-gpx-point
		// TODO dilution should be replaced with distToProj for non-findRouteSegment segments
		newMinDist += pnt.distToProj; // distToProj > 0 is only for pnt(s) after findRouteSegment

		// Sometimes, more than 1 segment from (pnt+others) to next-gpx-point might have the same distance.
		// To make difference, a small fraction (1/1000) of real-segment-distance is added as "dilution" value.
		// Such a small dilution prevents from interfering with main searching of minimal distance to gpx-point.
		// https://test.osmand.net/map/?start=52.481439,13.386036&end=52.483094,13.386060&profile=rescuetrack&params=rescuetrack,geoapproximation#18/52.48234/13.38672
		newMinDist += sumPntDistanceSqr(pnt, pnt.getSegmentStart(), bestSegmentEnd) * DILUTE_BY_SEGMENT_DISTANCE;

		MinDistResult result = new MinDistResult();

		if (newMinDist < minDistSqr && bestSegmentEnd >= 0) {
			if (preciseX != -1 && preciseY != -1) {
				result.preciseX = preciseX;
				result.preciseY = preciseY;
				result.preciseIndex = bestSegmentEnd;
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

		int tmp = -1;

		for (int i = 0; i <= nVirtualSegments; i++) {
			int px = nVirtualSegments > 0 ? (x1 - (x1 - x2) * i / nVirtualSegments) : x1;
			int py = nVirtualSegments > 0 ? (y1 - (y1 - y2) * i / nVirtualSegments) : y1;

			double distSqr = MapUtils.squareDist31TileMetric(px, py, loc.x31, loc.y31);

			if (distSqr < minDistSqr) {
				tmp = i;
				resultXY[0] = px;
				resultXY[1] = py;
				minDistSqr = distSqr;
			}
		}

		double oldDist = MapUtils.squareDist31TileMetric(x1, y1, loc.x31, loc.y31);

//		if(oldDist != minDistSqr)
//			System.err.printf("WARN: XXX [%d/%d] old %.2f new %.2f\n",
//				tmp, nVirtualSegments, Math.sqrt(oldDist), Math.sqrt(minDistSqr));

//		System.err.printf("WARN: XXX [%d] %.2f (%d) = %.2f (%s)\n",
//				endIndex, segmentDistanceMeters, nVirtualSegments, Math.sqrt(minDistSqr), pnt);
//
		return minDistSqr;
	}

	private double sumPntDistanceSqr(RouteSegmentPoint pnt, int start, int end) {
		if (start == end) return 0;
		if (start > end) {
			int swap = start;
			start = end;
			end = swap;
		}
		double dist = 0;
		for (int i = start; i < end; i++) {
			dist += MapUtils.squareRootDist31(
					pnt.getRoad().getPoint31XTile(i), pnt.getRoad().getPoint31YTile(i),
					pnt.getRoad().getPoint31XTile(i + 1), pnt.getRoad().getPoint31YTile(i + 1));
		}
		return dist * dist;
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
