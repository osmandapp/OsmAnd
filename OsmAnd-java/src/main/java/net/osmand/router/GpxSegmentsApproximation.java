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

public class GpxSegmentsApproximation {
	private final int LOOKUP_AHEAD = 10; // the number of gpx points and road segment points to discover (10)
	private final boolean USE_BIDIRECTIONAL_SEGMENTS = false; // follow direction for gpxAngle (twice slower) (true)
	private final boolean USE_PROJECTION_TO_SEGMENT = false; // prefer projection-to-seg instead of pnt-pnt-dist (true)
	private final double CRUSH_UNIDIRECTIONAL_SEGMENTS_M = 25; // crush road segments to match gpx points better (25)
	private final double MAX_PENALTY_BY_GPX_ANGLE_M = 25; // penalty by the angle between gpx and road (25)
	private final boolean TEST_SHIFT_GPX_POINTS = false; // shift gpx by ~15 meters before approximation

	private static class MinDistResult {
		private double minDist;
		private GpxPoint nextPoint;
		private RouteSegmentResult segment;
	}

	// if (DEBUG_IDS.indexOf((int)(pnt.getRoad().getId() / 64)) >= 0) { ... }
	// private List<Integer> DEBUG_IDS = Arrays.asList(499257893, 126338247, 237816930); // good, wrong, turn

	public GpxRouteApproximation fastGpxApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx,
	                                                  List<GpxPoint> gpxPoints) throws IOException {
		long timeToCalculate = System.nanoTime();
		float minPointApproximation = gctx.ctx.config.minPointApproximation;

		initGpxPointsXY31(gpxPoints);

		GpxPoint currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, 0);
		GpxPoint previousGpxPoint = currentPoint;

		while (currentPoint != null && currentPoint.pnt != null) {
			MinDistResult bestSegment = null;
			int start = currentPoint.ind + 1;
			int end = Math.min(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size());

			for (int j = start; j < end; j++) {
				MinDistResult currentSegment = findMinDistInLoadedPoints(currentPoint, gpxPoints.get(j));

				if (bestSegment == null || currentSegment.minDist < bestSegment.minDist) {
					bestSegment = currentSegment;
				}

				if (MapUtils.getDistance(currentPoint.loc, gpxPoints.get(j).loc) > minPointApproximation) {
					break; // avoid shortcutting of loops
				}
			}

			if (bestSegment == null) {
				break;
			}

			if (bestSegment.minDist > minPointApproximation) {
				final int nextIndex = currentPoint.ind + 1;
				currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation, gpxPoints, nextIndex);
				continue;
			}

			RouteSegmentResult fres = bestSegment.segment;
			int nextPointIndexIncrement = fres.isForwardDirection() ? -1 : 1;

			correctOverlappingSegments(previousGpxPoint, fres);

			if (previousGpxPoint != null && fres.getStartPointIndex() == fres.getEndPointIndex()) {
				previousGpxPoint.targetInd = bestSegment.nextPoint.ind; // ignore empty segment
//				nextPointIndexIncrement = 0; // TODO try to ignore in better way (holes in zlzk7)
			} else {
				currentPoint.targetInd = bestSegment.nextPoint.ind;
				currentPoint.routeToTarget = new ArrayList<>();
				fres.setGpxPointIndex(currentPoint.ind);
				currentPoint.routeToTarget.add(fres);
				previousGpxPoint = currentPoint;
			}

			currentPoint = bestSegment.nextPoint; // go to next point

			int nextEndPointIndex = fres.getEndPointIndex();
			int nextStartPointIndex = nextEndPointIndex + nextPointIndexIncrement;

			currentPoint.pnt = new RouteSegmentPoint(fres.getObject(), nextStartPointIndex, nextEndPointIndex, 0);
			currentPoint.pnt.others = new ArrayList<>();

			RouteSegment sg = gctx.ctx.loadRouteSegment(fres.getEndPointX(), fres.getEndPointY(),
					gctx.ctx.config.memoryLimitation);
			while (sg != null) {
				addSegment(currentPoint, sg);
				if (USE_BIDIRECTIONAL_SEGMENTS) {
					addSegment(currentPoint, sg.initRouteSegment(!sg.isPositive()));
				}
				sg = sg.getNext();
			}
		}

//		removeOverlappingSegments(gpxPoints); // TODO remove method

//		for (GpxPoint p : gpxPoints) { // DEBUG
//			if (p.routeToTarget != null) {
//				System.err.println("XXX " + p.ind + " " + p.targetInd + " " + p.routeToTarget);
//			}
//		}

		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);
		return gctx;
	}

	private void addSegment(GpxPoint currentPoint, RouteSegment sg) {
		if (sg != null && currentPoint.pnt.others != null && (
				sg.getRoad().getId() != currentPoint.pnt.getRoad().getId() ||
						Math.min(sg.getSegmentStart(), sg.getSegmentEnd()) !=
								Math.min(currentPoint.pnt.getSegmentStart(), currentPoint.pnt.getSegmentEnd())
		)) {
			RouteSegmentPoint p = new RouteSegmentPoint(sg.getRoad(), sg.getSegmentStart(), sg.getSegmentEnd(), 0);
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
					if (USE_BIDIRECTIONAL_SEGMENTS) {
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
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
	}

	private MinDistResult findMinDistInLoadedPoints(GpxPoint loadedPoint, GpxPoint nextPoint) {
		double gpxAngle = Math.atan2(nextPoint.y31 - loadedPoint.y31, nextPoint.x31 - loadedPoint.x31);
		MinDistResult best = findMinDistInOnePoint(null, loadedPoint.pnt, nextPoint, gpxAngle);
		for (RouteSegmentPoint oth : loadedPoint.pnt.others) {
			best = findMinDistInOnePoint(best, oth, nextPoint, gpxAngle);
		}
		return best;
	}

	private MinDistResult findMinDistInOnePoint(MinDistResult res, RouteSegmentPoint pnt, GpxPoint loc, double gpxAngle) {
		if (false) { // TODO remove old block
			double newMinDist = 0;
			int bestSegmentEnd = -1;

			int nLookupPoints = Math.max(LOOKUP_AHEAD, Math.abs(pnt.getSegmentEnd() - pnt.getSegmentStart()));
			int startPointIndex = Math.max(0, pnt.getSegmentStart() - nLookupPoints);
			int endPointIndex = Math.min(pnt.getRoad().getPointsLength(), pnt.getSegmentStart() + nLookupPoints);
			for (int i = startPointIndex; i < endPointIndex; i++) {
				double dist = findMinDistInCrushedSegments(loc, pnt, i, i > 0 ? i - 1 : 0);
				if (bestSegmentEnd < 0 || dist < newMinDist) {
					bestSegmentEnd = i;
					newMinDist = dist;
				}
			}

			double penalty = calcGpxAnglePenalty(pnt, pnt.getSegmentStart(), bestSegmentEnd, gpxAngle);
			newMinDist += penalty * MAX_PENALTY_BY_GPX_ANGLE_M; // should be fixed const

			if (bestSegmentEnd >= 0 && (res == null || newMinDist < res.minDist)) {
				if (res == null) res = new MinDistResult();
				res.segment = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), bestSegmentEnd);
				res.minDist = newMinDist;
				res.nextPoint = loc;
			}

		} else {
			int increment = (pnt.segEnd >= pnt.segStart ? +1 : -1);
			int	pointIndex = pnt.getSegmentStart();
			int nextIndex = pointIndex + increment;
			int	nLookupAhead = LOOKUP_AHEAD;

			if (!USE_BIDIRECTIONAL_SEGMENTS && increment > 0) {
				pointIndex = Math.max(0, pnt.getSegmentStart() - LOOKUP_AHEAD);
				nextIndex = pointIndex + increment;
				nLookupAhead = LOOKUP_AHEAD * 2;
			}

			while (nextIndex < pnt.getRoad().getPointsLength() && nextIndex >= 0
					&& Math.abs(pointIndex - nextIndex) < nLookupAhead) {
				double currentDist = 0;

				if (USE_PROJECTION_TO_SEGMENT) {
					RouteDataObject r = pnt.getRoad();
					QuadPointDouble pp = MapUtils.getProjectionPoint31(loc.x31, loc.y31, r.getPoint31XTile(pointIndex),
							r.getPoint31YTile(pointIndex), r.getPoint31XTile(nextIndex), r.getPoint31YTile(nextIndex));
					currentDist = MapUtils.squareRootDist31((int) pp.x, (int) pp.y, loc.x31, loc.y31);
				} else {
					currentDist = findMinDistInCrushedSegments(loc, pnt, pointIndex, nextIndex);
				}

				if (MAX_PENALTY_BY_GPX_ANGLE_M > 0 && (res == null || currentDist < res.minDist)) {
					// apply penalty by the difference between gpx-angle and road-angle (select best road)
					if (USE_BIDIRECTIONAL_SEGMENTS) {
						double segmentAngle = Math.atan2(
								pnt.getRoad().getPoint31YTile(nextIndex) - pnt.getRoad().getPoint31YTile(pointIndex),
								pnt.getRoad().getPoint31XTile(nextIndex) - pnt.getRoad().getPoint31XTile(pointIndex));
						double penalty = (1 - Math.cos(MapUtils.alignAngleDifference(gpxAngle - segmentAngle))); // [0 - 2]
						currentDist += penalty * MAX_PENALTY_BY_GPX_ANGLE_M / 3; // maximum penalty 2/3
					} else {
						double penalty = calcGpxAnglePenalty(pnt, pnt.getSegmentStart(), nextIndex, gpxAngle); // [0 - 1]
						currentDist += penalty * MAX_PENALTY_BY_GPX_ANGLE_M; // should be fixed const
					}
				}

				if (res == null || currentDist < res.minDist) {
					if (res == null) res = new MinDistResult();
					res.segment = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), nextIndex);
					res.minDist = currentDist;
					res.nextPoint = loc;
				}

				pointIndex = nextIndex;
				nextIndex = pointIndex + increment;
			}
		}

		return res;
	}

	private double findMinDistInCrushedSegments(GpxPoint loc, RouteSegmentPoint pnt, int startIndex, int endIndex) {
		int sx = pnt.getRoad().getPoint31XTile(Math.min(startIndex, endIndex));
		int sy = pnt.getRoad().getPoint31YTile(Math.min(startIndex, endIndex));
		int ex = pnt.getRoad().getPoint31XTile(Math.max(startIndex, endIndex));
		int ey = pnt.getRoad().getPoint31YTile(Math.max(startIndex, endIndex));
		double segmentDistanceMeters = MapUtils.squareRootDist31(sx, sy, ex, ey);

		int nVirtualSegments = CRUSH_UNIDIRECTIONAL_SEGMENTS_M > 0
				? (int) (segmentDistanceMeters / CRUSH_UNIDIRECTIONAL_SEGMENTS_M) : 0;

		double minDist = Double.POSITIVE_INFINITY;

		// TODO optimize: break if minDist raises
		for (int i = 0; i <= nVirtualSegments; i++) {
			int px = nVirtualSegments > 0 ? (ex - (ex - sx) * i / nVirtualSegments) : ex;
			int py = nVirtualSegments > 0 ? (ey - (ey - sy) * i / nVirtualSegments) : ey;

			double dist = MapUtils.squareRootDist31(px, py, loc.x31, loc.y31);

			if (dist < minDist) {
				minDist = dist;
			}
		}

		return minDist;
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
			roadAngle += segmentAngle;
			counter++;
		}
		roadAngle /= counter;

		// unidirectional comparison
		if (gpxAngle < 0) gpxAngle += Math.PI;
		if (roadAngle < 0) roadAngle += Math.PI;
		double penalty = Math.abs(gpxAngle - roadAngle) / Math.PI;

		return penalty;
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
				final double shift = 0.00015;
				p.loc = new LatLon(p.loc.getLatitude() - shift, p.loc.getLongitude() + shift);
			}
			p.x31 = MapUtils.get31TileNumberX(p.loc.getLongitude());
			p.y31 = MapUtils.get31TileNumberY(p.loc.getLatitude());
		}
	}

	private void correctOverlappingSegments(GpxPoint prevGpxPoint, RouteSegmentResult next) {
		if (prevGpxPoint != null && prevGpxPoint.routeToTarget != null) {
			RouteSegmentResult prev = prevGpxPoint.routeToTarget.get(prevGpxPoint.routeToTarget.size() - 1);
			if (prev.getObject().getId() == next.getObject().getId()) {
				if ((prev.isForwardDirection() && next.getStartPointIndex() < prev.getEndPointIndex()) ||
						(!prev.isForwardDirection() && next.getStartPointIndex() > prev.getEndPointIndex())) {
					next.setStartPointIndex(prev.getEndPointIndex());
				}
			}
		}
	}

//	private void removeOverlappingSegments(List<GpxPoint> gpxPoints) {
//		GpxPoint prev = null;
//		for (GpxPoint p : gpxPoints) {
//			if (p.routeToTarget != null) {
//				if (prev != null) {
//					RouteSegmentResult prevLast = prev.routeToTarget.get(prev.routeToTarget.size() - 1);
//					RouteSegmentResult firstNext = p.routeToTarget.get(0);
//					if (prevLast.getObject().getId() == firstNext.getObject().getId()) {
//						// check overlap
//						if (prevLast.isForwardDirection()
//								&& prevLast.getEndPointIndex() > firstNext.getStartPointIndex()) {
//							firstNext.setStartPointIndex(prevLast.getEndPointIndex());
//						} else if (!prevLast.isForwardDirection()
//								&& prevLast.getEndPointIndex() < firstNext.getStartPointIndex()) {
//							firstNext.setStartPointIndex(prevLast.getEndPointIndex());
//						}
//					}
//					if (firstNext.getStartPointIndex() == firstNext.getEndPointIndex()) {
//						p.routeToTarget.remove(0);
//					}
//				}
//				if (p.routeToTarget.size() > 0) {
//					prev = p;
//				} else {
//					p.routeToTarget = null;
//					prev.targetInd = p.targetInd;
//				}
//			}
//		}
//	}
}
