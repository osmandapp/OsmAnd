package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.util.MapUtils;

// DONE Native lib - required
// DONE use minPointApproximation to restart after "lost" gpx segments with initRoutingPoint
// DONE "same" loadRouteSegment() segments are actually "sorted" with DILUTE_BY_SEGMENT_DISTANCE
// DONE fixed Map Creator gpx "gaps" (the bug was lost results of splitRoadsAndAttachRoadSegments)

// TODO fix minor "Points are not connected" (~0.01m)
// TODO remove usage of finalPoints in Android/iOS (really ?)

// TO-THINK ? think about "bearing" in addition to LOOKUP_AHEAD to keep sharp/loop-shaped gpx parts
// TO-THINK ? makePrecise for start / end segments (just check how correctly they are calculated)

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10;
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private final double DILUTE_BY_SEGMENT_DISTANCE = 0.001; // add a fraction of seg dist to pnt-to-gpx dist (0.001)

	// if (DEBUG_IDS.indexOf((int)(pnt.getRoad().getId() / 64)) >= 0) { ... }
	// private List<Integer> DEBUG_IDS = Arrays.asList(499257893, 126338247, 237816930); // good, wrong, turn

	public GpxRouteApproximation fastGpxApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx,
	                                                  List<GpxPoint> gpxPoints) throws IOException {
		long timeToCalculate = System.nanoTime();

		initGpxPointsXY31(gpxPoints);

		float minPointApproximation = gctx.ctx.config.minPointApproximation;
		GpxPoint currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, 0);

		while (currentPoint != null && currentPoint.pnt != null) {
			double minDistSqrSegment = 0;
			RouteSegmentResult fres = null;
			int minNextInd = -1;
			for (int j = currentPoint.ind + 1; j < Math.min(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size()); j++) {
				RouteSegmentResult[] res = new RouteSegmentResult[1];
				double minDistSqr = Double.POSITIVE_INFINITY;
				GpxPoint ps = gpxPoints.get(j);
				minDistSqr = minDistResult(res, minDistSqr, currentPoint.pnt, ps);
				if (currentPoint.pnt.others != null) {
					for (RouteSegmentPoint oth : currentPoint.pnt.others) {
						minDistSqr = minDistResult(res, minDistSqr, oth, ps);
					}
				}
				if (fres == null || minDistSqr <= minDistSqrSegment) {
					fres = res[0];
					minDistSqrSegment = minDistSqr;
					minNextInd = j;
				}
			}
			if (minNextInd < 0) {
				break;
			}
			if (minDistSqrSegment > minPointApproximation * minPointApproximation) {
				final int nextIndex = currentPoint.ind + 1;
				currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, nextIndex);
				continue;
			}
			currentPoint.routeToTarget = new ArrayList<RouteSegmentResult>();
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

	private double minDistResult(RouteSegmentResult[] res, double minDistSqr, RouteSegmentPoint pnt, GpxPoint loc) {
		int segmentEnd = -1;
		double dist = 0;
		int start = Math.max(0, pnt.getSegmentStart() - LOOKUP_AHEAD);
		int end = Math.min(pnt.getRoad().getPointsLength(), pnt.getSegmentStart() + LOOKUP_AHEAD);
		for (int i = start; i < end; i++) {
			if (i == pnt.getSegmentStart()) {
				continue;
			}
			double d = MapUtils.squareDist31TileMetric(loc.x31, loc.y31,
					pnt.getRoad().getPoint31XTile(i), pnt.getRoad().getPoint31YTile(i));
			if (segmentEnd < 0 || d < dist) {
				segmentEnd = i;
				dist = d;
			}
		}
		dist += pnt.distToProj; // distToProj > 0 is only for pnt(s) after findRouteSegment

		// Sometimes, more than 1 segment from (pnt+others) to next-gpx-point might have the same distance.
		// To make difference, a small fraction (1/1000) of real-segment-distance is added as "dilution" value.
		// Such a small dilution prevents from interfering with main searching of minimal distance to gpx-point.
		// https://test.osmand.net/map/?start=52.481439,13.386036&end=52.483094,13.386060&profile=rescuetrack&params=rescuetrack,geoapproximation#18/52.48234/13.38672
		dist += sumPntDistanceSqr(pnt, pnt.getSegmentStart(), segmentEnd) * DILUTE_BY_SEGMENT_DISTANCE;

		if ((res[0] == null || dist < minDistSqr) && segmentEnd >= 0) {
			minDistSqr = dist;
			res[0] = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), segmentEnd);
		}
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

	public static void updateFinalPointsWithExternalTimestamps(List<GpxPoint> finalPoints,
	                                                           List<WptPt> sourcePoints) {
		if (!validateExternalTimestamps(sourcePoints)) {
			System.out.printf("Error: updateGpxPointsByExternalTimestamps() got invalid sourcePoints");
			return;
		}
		for (GpxPoint gp : finalPoints) {
			for (RouteSegmentResult seg : gp.routeToTarget) {
				seg.setSegmentSpeed(calcSegmentSpeedByExternalTimestamps(seg, sourcePoints));
			}
			RouteResultPreparation.recalculateTimeDistance(gp.routeToTarget);
		}
	}

	private static float calcSegmentSpeedByExternalTimestamps(RouteSegmentResult seg,
	                                                          List<WptPt> waypoints) {
		float speed = seg.getSegmentSpeed();

		int sx = seg.getStartPointX(), sy = seg.getStartPointY();
		int ex = seg.getEndPointX(), ey = seg.getEndPointY();
		double minDistStart = Double.POSITIVE_INFINITY;
		double minDistEnd = Double.POSITIVE_INFINITY;
		int indexStart = -1, indexEnd = -1;

		for (int i = 0; i < waypoints.size(); i++) {
			int wx = MapUtils.get31TileNumberX(waypoints.get(i).getLongitude());
			int wy = MapUtils.get31TileNumberY(waypoints.get(i).getLatitude());
			double distStart = MapUtils.squareRootDist31(sx, sy, wx, wy);
			double distEnd = MapUtils.squareRootDist31(ex, ey, wx, wy);
			if (distStart < minDistStart) {
				minDistStart = distStart;
				indexStart = i;
			}
			if (distEnd < minDistEnd) {
				minDistEnd = distEnd;
				indexEnd = i;
			}
		}

		if (indexStart != -1 && indexEnd != -1 && indexStart < indexEnd) {
			long time = waypoints.get(indexEnd).time - waypoints.get(indexStart).time;
			if (time > 0) {
				double distance = 0;
				for (int i = indexStart; i < indexEnd; i++) {
					distance += MapUtils.getDistance(
							waypoints.get(i).getLatitude(), waypoints.get(i).getLongitude(),
							waypoints.get(i + 1).getLatitude(), waypoints.get(i + 1).getLongitude());
				}
				if (distance > 0) {
					speed = (float) distance / ((float) time / 1000); // update based on external timestamps
				}
			}
		}

		return speed;
	}

	private static boolean validateExternalTimestamps(List<WptPt> waypoints) {
		if (waypoints == null || waypoints.isEmpty()) {
			return false;
		}
		long last = 0;
		for (WptPt wpt : waypoints) {
			if (wpt.time == 0 || wpt.time < last) {
				return false;
			}
			last = wpt.time;
		}
		return true;
	}
}
