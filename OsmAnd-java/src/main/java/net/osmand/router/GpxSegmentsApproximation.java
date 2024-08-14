package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import gnu.trove.set.hash.TLongHashSet;
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
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private static final int MAX_DEPTH_ROLLBACK = 15; // TODO rollback in meters?
	private static final int POINT_CF = 2; // TODO replace with meaningfull constant

	// TODO structures could be optimized (visited for single visited) 
	// TODO implement hops (missing roads) - TEST
	// ToDO implement precise start / end
	private static class RouteSegmentAppr {
		private final RouteSegment segment;
		private final RouteSegmentAppr parent;

		private List<RouteSegmentAppr> connected = null;
		private TLongHashSet visited = null;
		private int gpxStart;
		private int gpxLen = 0;
		private double maxDistToGpx;
		
		private int gpxNext() {
			return gpxStart + gpxLen + 1;
		}
		
		private RouteSegmentAppr(RouteSegment segment, int start) {
			this.parent = null;
			this.segment = segment;
			this.gpxStart = start;
		}
		
		private RouteSegmentAppr(RouteSegmentAppr parent, RouteSegment segment) {
			this.parent = parent;
			this.segment = segment;
				this.gpxStart = parent.gpxStart + parent.gpxLen;
		}
		
		public void loadConnections(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints, float minPointApproximation) {
			if (connected != null) {
				return;
			}
			connected = new ArrayList<>();
			RouteSegment sg = gctx.ctx.loadRouteSegment(segment.getEndPointX(), segment.getEndPointY(),
					gctx.ctx.config.memoryLimitation);
			while(sg != null) {
				addSegment(sg.initRouteSegment(!sg.isPositive()), gpxPoints, minPointApproximation);
				addSegment(sg, gpxPoints, minPointApproximation);
				sg = sg.getNext();
			}
		}
		
		private void addSegment(RouteSegment sg, List<GpxPoint> gpxPoints, float minPointApproximation) {
			if (sg == null) {
				return;
			}
			if (sg.getRoad().getId() != segment.road.getId() || Math.min(sg.getSegmentStart(),
					sg.getSegmentEnd()) != Math.min(segment.getSegmentStart(), segment.getSegmentEnd())) {
				if (parent == null || !parent.isVisited(sg)) {
					RouteSegmentAppr c = new RouteSegmentAppr(this, sg);
					boolean accept = approximateSegment(c, gpxPoints, minPointApproximation);
//					System.out.printf("** %d -> %d  ( %s ) %.2f - %s \n", c.gpxStart, c.gpxNext(), c.segment,
//							c.maxDistToGpx, accept); // DEBUG
					if (accept) {
						connected.add(c);
					}
				}
			}
		}
		
		public void visit(RouteSegment r) {
			if (visited == null) {
				visited = new TLongHashSet();
			}
			visited.add(calculateRoutePointId(r));
		}
		
		public boolean isVisited(RouteSegment r) {
			if (parent != null && parent.isVisited(r)) {
				return true;
			}
			return visited != null && visited.contains(calculateRoutePointId(r));
		}
	}
	
	
	private static final int ROUTE_POINTS = 12;
	private static long calculateRoutePointInternalId(final RouteDataObject road, int pntId, int nextPntId) {
		int positive = nextPntId - pntId;
		int pntLen = road.getPointsLength();
		if (pntId < 0 || nextPntId < 0 || pntId >= pntLen || nextPntId >= pntLen || (positive != -1 && positive != 1)) {
			// should be assert
			throw new IllegalStateException("Assert failed");
		}
		return (road.getId() << ROUTE_POINTS) + (pntId << 1) + (positive > 0 ? 1 : 0);
	}
	
	private static long calculateRoutePointId(RouteSegment segm) {
		return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), 
				segm.isPositive() ? segm.getSegmentStart() + 1 : segm.getSegmentStart() - 1);
	}

	public GpxRouteApproximation gpxApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx,
	                                                  List<GpxPoint> gpxPoints) throws IOException {
		long timeToCalculate = System.nanoTime();
		initGpxPointsXY31(gpxPoints);
		float minPointApproximation = gctx.ctx.config.minPointApproximation;
		float initDist = minPointApproximation / 2;
		GpxPoint currentPoint = findNextRoutablePoint(frontEnd, gctx, initDist, gpxPoints, 0);
		if (currentPoint == null) {
			return gctx;
		}
		RouteSegmentAppr last = new RouteSegmentAppr(currentPoint.pnt, 0);
		approximateSegment(last, gpxPoints, minPointApproximation);
		RouteSegmentAppr bestRoute = null;
		while (last.gpxNext() < gpxPoints.size()) { 
			last.loadConnections(gctx, gpxPoints, minPointApproximation);
 			RouteSegmentAppr bestNext = null;
			for(RouteSegmentAppr c : last.connected) {
				if (last.isVisited(c.segment)) {
					continue;
				}
				if (bestNext == null) {
					bestNext = c;
				} else if (c.maxDistToGpx / Math.sqrt(c.gpxLen + 1) < bestNext.maxDistToGpx / Math.sqrt(bestNext.gpxLen + 1)) {
					bestNext = c;
				}
 			}
			if (bestNext == null) {
				if (last.parent != null && (bestRoute != null && bestRoute.gpxNext() - last.parent.gpxNext() < MAX_DEPTH_ROLLBACK)) {
//					System.out.print(" ^ "); // DEBUG
					last = last.parent;
					continue;
				} else if (bestRoute != null) {
					wrapupRoute(gpxPoints, bestRoute);
				}
				GpxPoint pnt = findNextRoutablePoint(frontEnd, gctx, initDist, gpxPoints,
						bestRoute != null ? bestRoute.gpxNext() : last.gpxNext());
				if (pnt == null) {
//					System.out.println("------"); // DEBUG
					break;
				} else {
//					System.out.println("\n!!! " + pnt.ind + " " + pnt.loc + " " + pnt.pnt); // DEBUG
					last = new RouteSegmentAppr(pnt.pnt, pnt.ind);
					approximateSegment(last, gpxPoints, minPointApproximation);
					bestRoute = null;
				}
			} else {
//				System.out.printf("%d -> %d %s %s - %.2f \n", bestNext.gpxStart, bestNext.gpxStart + bestNext.gpxLen,
//						gpxPoints.get(bestNext.gpxStart + bestNext.gpxLen).loc, bestNext.segment,
//						bestNext.maxDistToGpx); // DEBUG
				last.visit(bestNext.segment);
				if (bestRoute == null || bestRoute.gpxNext() < last.gpxNext()) {
					bestRoute = last;
				}
				last = bestNext;
			}

		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		if (bestRoute != null) {
			wrapupRoute(gpxPoints, bestRoute);
		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);
		return gctx;
	}

	private void wrapupRoute(List<GpxPoint> gpxPoints, RouteSegmentAppr bestRoute) {
		List<RouteSegmentResult> res = new ArrayList<>();
		int startInd = 0;
		int last = bestRoute.gpxNext();
		while (bestRoute != null) {
			startInd = bestRoute.gpxStart;
			res.add(new RouteSegmentResult(bestRoute.segment.road, bestRoute.segment.getSegmentStart(),
					bestRoute.segment.getSegmentEnd()));
			bestRoute = bestRoute.parent;
		}
		Collections.reverse(res);
		gpxPoints.get(startInd).routeToTarget = res;
		gpxPoints.get(startInd).targetInd = last;
	}

	private static boolean approximateSegment(RouteSegmentAppr c, List<GpxPoint> gpxPoints, float minPointApproximation) {
		int pointInd = c.gpxStart + 1;
		for (; pointInd < gpxPoints.size(); pointInd++) {
			GpxPoint p = gpxPoints.get(pointInd);
			if (p.x31 == c.segment.getEndPointX() && p.y31 == c.segment.getEndPointY()) {
				c.gpxLen++;
				continue;
			}
			QuadPointDouble pp = MapUtils.getProjectionPoint31(p.x31, p.y31, c.segment.getStartPointX(),
					c.segment.getStartPointY(), c.segment.getEndPointX(), c.segment.getEndPointY());
			boolean beforeStart = (pp.x == c.segment.getStartPointX() && pp.y == c.segment.getStartPointY());
			boolean farEnd = (pp.x == c.segment.getEndPointX() && pp.y == c.segment.getEndPointY());
			if (farEnd) {
				break;
			}
			double dist = BinaryRoutePlanner.squareRootDist((int) pp.x, (int) pp.y, p.x31, p.y31);
			if (dist > minPointApproximation) {
				if (beforeStart || c.gpxLen > 0 ) {
					break;
				}
				return false;
			}
			c.maxDistToGpx = Math.max(c.maxDistToGpx, dist);
			c.gpxLen++;
		}
		// calculate dist for last segment (end point is exactly in between prev gpx / next gpx) 
		// because next gpx point doesn't project onto segment
		if (pointInd < gpxPoints.size()) {
			QuadPointDouble pp = MapUtils.getProjectionPoint31(c.segment.getEndPointX(), c.segment.getEndPointY(),
					gpxPoints.get(pointInd - 1).x31, gpxPoints.get(pointInd - 1).y31, gpxPoints.get(pointInd).x31,
					gpxPoints.get(pointInd).y31);
			double dist = BinaryRoutePlanner.squareRootDist((int) pp.x, (int) pp.y, c.segment.getEndPointX(),
					c.segment.getEndPointY());
			c.maxDistToGpx = Math.max(c.maxDistToGpx, dist);
			if (dist > minPointApproximation) {
				return false;
			}
		}
		return true;
	}


	private boolean initRoutingPoint(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx, GpxPoint start,
	                                 double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			double dir = start.object.directionRoute(start.ind, true);
			RouteSegmentPoint rsp = frontEnd.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(),
					gctx.ctx, null, false);
			if (rsp != null) {
				if (MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp;
					double dirc = rsp.road.directionRoute(rsp.getSegmentStart(), rsp.isPositive());
					if (Math.abs(MapUtils.alignAngleDifference(dir - dirc)) < Math.PI / 2) {
						start.pnt = rsp;
						rsp.others = null;
					} else {
						start.pnt = new RouteSegmentPoint(rsp.getRoad(), rsp.getSegmentEnd(), rsp.getSegmentStart(),
								rsp.distToProj);
					}
				}
			}
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
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
