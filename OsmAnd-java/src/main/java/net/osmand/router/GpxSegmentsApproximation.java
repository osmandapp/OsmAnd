package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.MapUtils;

// TO-THINK ? fix minor "Points are not connected" (~0.01m)
// TO-THINK ? makePrecise for start / end segments (just check how correctly they are calculated)

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10;
	private final boolean TEST_SHIFT_GPX_POINTS = false;

	private static class RouteSegmentApproximationResult {
		private double minDist;
		private RouteSegmentResult segment;
		public GpxPoint nextPoint;
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
			RouteSegmentApproximationResult bestSegment = null;
			int start = currentPoint.ind + 1;
			int end = Math.min(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size());
			for (int j = start; j < end; j++) {
				GpxPoint ps = gpxPoints.get(j);
				RouteSegmentApproximationResult currentSegment = findMinDistInLoadedPoints(currentPoint, ps, minPointApproximation);
				System.out.printf("? %d %.2f %s %s\n", j, currentSegment.minDist, ps.loc, currentSegment.segment); // DEBUG
				if (bestSegment == null || currentSegment.minDist <= bestSegment.minDist) {
					bestSegment = currentSegment;
				}
				if (currentSegment.minDist > minPointApproximation) {
					break; // avoid shortcutting of loops
				}
			}
			if (bestSegment == null) {
				break;
			}
			System.out.printf("%d -> %d ( %s ) %s %.2f \n", currentPoint.ind, bestSegment.nextPoint.ind,
					bestSegment.nextPoint.loc, bestSegment.segment, bestSegment.minDist); // DEBUG
			if (bestSegment.minDist > minPointApproximation) {
				final int nextIndex = currentPoint.ind + 1;
				currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, nextIndex);
				continue;
			}

			RouteSegmentResult fres = bestSegment.segment;
			fres.setGpxPointIndex(currentPoint.ind);

			currentPoint.routeToTarget = new ArrayList<>();
			currentPoint.routeToTarget.add(fres);
			currentPoint.targetInd = bestSegment.nextPoint.ind;
			
			currentPoint = bestSegment.nextPoint; // next point
			currentPoint.pnt = new RouteSegmentPoint(fres.getObject(),
					fres.getEndPointIndex() + (fres.isForwardDirection() ? -1 : 1), fres.getEndPointIndex(), 0);
			RouteSegment sg = gctx.ctx.loadRouteSegment(fres.getEndPointX(), fres.getEndPointY(),
					gctx.ctx.config.memoryLimitation);
			while (sg != null) {
				addSegment(currentPoint, sg);
				addSegment(currentPoint, sg.initRouteSegment(!sg.isPositive()));
				sg = sg.getNext();
			}
		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		removeOverlappingSegments(gpxPoints);
//		for (GpxPoint p : gpxPoints) { // DEBUG
//			if (p.routeToTarget != null) {
//				System.out.println(p.ind + " " + p.targetInd + " " + p.routeToTarget);
//			}
//		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);
		return gctx;
	}

	private void removeOverlappingSegments(List<GpxPoint> gpxPoints) {
		GpxPoint prev = null;
		for (GpxPoint p : gpxPoints) {
			if (p.routeToTarget != null) {
				if (prev != null) {
					RouteSegmentResult prevLast = prev.routeToTarget.get(prev.routeToTarget.size() - 1);
					RouteSegmentResult firstNext = p.routeToTarget.get(0);
					if (prevLast.getObject().getId() == firstNext.getObject().getId()) {
						// check overlap
						if (prevLast.isForwardDirection()
								&& prevLast.getEndPointIndex() > firstNext.getStartPointIndex()) {
							firstNext.setStartPointIndex(prevLast.getEndPointIndex());
						} else if (!prevLast.isForwardDirection()
								&& prevLast.getEndPointIndex() < firstNext.getStartPointIndex()) {
							firstNext.setStartPointIndex(prevLast.getEndPointIndex());
						}
					}
					if (firstNext.getStartPointIndex() == firstNext.getEndPointIndex()) {
						p.routeToTarget.remove(0);
					}
				}
				if (p.routeToTarget.size() > 0) {
					prev = p;
				} else {
					p.routeToTarget = null;
					prev.targetInd = p.targetInd;
				}
			}
		}
	}

	private void addSegment(GpxPoint currentPoint, RouteSegment sg) {
		if (sg == null) {
			return;
		}
		if (sg.getRoad().getId() != currentPoint.pnt.road.getId() || Math.min(sg.getSegmentStart(),
				sg.getSegmentEnd()) != Math.min(currentPoint.pnt.getSegmentStart(), currentPoint.pnt.getSegmentEnd())) {
			RouteSegmentPoint p = new RouteSegmentPoint(sg.getRoad(), sg.getSegmentStart(), sg.getSegmentEnd(), 0);
			if (currentPoint.pnt.others == null) {
				currentPoint.pnt.others = new ArrayList<>();
			}
			currentPoint.pnt.others.add(p);
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
					List<RouteSegmentPoint> negs = new ArrayList<>();
					negs.add(new RouteSegmentPoint(rsp.getRoad(), rsp.getSegmentEnd(), rsp.getSegmentStart(), rsp.distToProj));
					Iterator<RouteSegmentPoint> it = rsp.others.iterator();
					while (it.hasNext()) {
						RouteSegmentPoint o = it.next();
						if (MapUtils.getDistance(o.getPreciseLatLon(), start.loc) < distThreshold) {
							negs.add(new RouteSegmentPoint(o.getRoad(), o.getSegmentEnd(), o.getSegmentStart(),
									o.distToProj));
						} else {
							it.remove();
						}
					}
					rsp.others.addAll(negs);
				}
			}
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
	}

	private RouteSegmentApproximationResult findMinDistInLoadedPoints(GpxPoint loadedPoint, GpxPoint nextPoint,
			float minThreshold) {
		double gpxAngle = loadedPoint.object.directionRoute(loadedPoint.ind, true);
		RouteSegmentApproximationResult best = findBestSegmentMinDist(null, loadedPoint.pnt, nextPoint, gpxAngle,
				minThreshold);
		if (loadedPoint.pnt.others != null) {
			for (RouteSegmentPoint oth : loadedPoint.pnt.others) {
				best = findBestSegmentMinDist(best, oth, nextPoint, gpxAngle, minThreshold);
			}
		}
		return best;
	}

	private RouteSegmentApproximationResult findBestSegmentMinDist(RouteSegmentApproximationResult res,
			RouteSegmentPoint pnt, GpxPoint loc, double gpxAngle, float minThreshold) {
		int pointIndex = pnt.getSegmentStart();
		int nextInd = pnt.isPositive() ? pointIndex + 1 : pointIndex - 1; 
		while (nextInd < pnt.getRoad().getPointsLength() && nextInd >= 0
				&& Math.abs(pointIndex - nextInd) < LOOKUP_AHEAD) {
			RouteDataObject r = pnt.getRoad();
			QuadPointDouble pp = MapUtils.getProjectionPoint31(loc.x31, loc.y31, r.getPoint31XTile(pointIndex),
					r.getPoint31YTile(pointIndex), r.getPoint31XTile(nextInd), r.getPoint31YTile(nextInd));
			double currentsDist = BinaryRoutePlanner.squareRootDist((int) pp.x, (int) pp.y, loc.x31, loc.y31);
			// Add penalty by the difference between angle-to-next-gpx-point (gpxAngle)
			double dirRoute = pnt.getRoad().directionRoute(pointIndex, nextInd > pointIndex);
			double penalty = (1 - Math.cos(MapUtils.alignAngleDifference(gpxAngle - dirRoute))); // [0 - 2]
			currentsDist += penalty * minThreshold / 3; // maximum penalty 2/3
			if (res == null || currentsDist < res.minDist) {
				res = new RouteSegmentApproximationResult();
				res.segment = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), nextInd);
				res.nextPoint = loc;
				res.minDist = currentsDist;
			}
			pointIndex = nextInd;
			nextInd = pnt.isPositive() ? pointIndex + 1 : pointIndex - 1;
		}

		return res;
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
