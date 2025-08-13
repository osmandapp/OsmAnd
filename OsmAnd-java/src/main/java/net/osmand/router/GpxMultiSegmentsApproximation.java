package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.set.hash.TLongHashSet;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.MapUtils;

// TEST missing roads, performance, start-end points (precise)
public class GpxMultiSegmentsApproximation {
	// ALGORITHM CONSTANTS //
	private static final int MAX_DEPTH_ROLLBACK = 500; // 500 m rollback
	private static final double MIN_BRANCHING_DIST = 10; // 5 m for branching
	private static boolean EAGER_ALGORITHM = false;
	private static boolean PRIORITY_ALGORITHM = !EAGER_ALGORITHM;
	private final boolean TEST_SHIFT_GPX_POINTS = false;
	private static boolean DEBUG = false;
	/////////////////////////
	
	private static final int ROUTE_POINTS = 12;
	private static final int GPX_MAX = 30; // 1M
	
	final Comparator<RouteSegmentAppr> METRICS_COMPARATOR = new Comparator<RouteSegmentAppr>() {

		@Override
		public int compare(RouteSegmentAppr o1, RouteSegmentAppr o2) {
			return Double.compare(o1.metric(), o2.metric());
		}
	};

	/// Evaluation variables
	private final RoutePlannerFrontEnd frontEnd;
	private final GpxRouteApproximation gctx;
	private final List<GpxPoint> gpxPoints;
	private final float minPointApproximation;
	private final float initDist;
	java.util.PriorityQueue<RouteSegmentAppr> queue = new java.util.PriorityQueue<>(METRICS_COMPARATOR); 
	TLongHashSet visited = new TLongHashSet();

	private static class RouteSegmentAppr {
		private final RouteSegment segment;
		private final RouteSegmentAppr parent;

		private final int gpxStart;
		private int gpxLen = 0;
		private double maxDistToGpx = 0;
		
		private int gpxNext() {
			return gpxStart + gpxLen + 1;
		}
		
		private RouteSegmentAppr(int start, RouteSegmentPoint pnt) {
			this.parent = null;
			this.segment = pnt;
			this.gpxStart = start;
		}
		
		private RouteSegmentAppr(RouteSegmentAppr parent, RouteSegment segment) {
			this.parent = parent;
			this.segment = segment;
			this.gpxStart = parent.gpxStart + parent.gpxLen;
		}
		
		private double metric() {
			return maxDistToGpx / Math.sqrt(gpxLen + 1); // heuristics for eager algorithm
		}

		@Override
		public String toString() {
			return String.format("%d -> %d  ( %s ) %.2f", gpxStart, gpxNext(), segment, maxDistToGpx);
		}
	}
	
	public GpxMultiSegmentsApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx, List<GpxPoint> gpxPoints) {
		this.frontEnd = frontEnd;
		this.gctx = gctx;
		this.gpxPoints = gpxPoints;
		minPointApproximation = gctx.ctx.config.minPointApproximation;
		initDist = minPointApproximation / 2;
		
	}
	
	public void loadConnections(RouteSegmentAppr last, List<RouteSegmentAppr> connected) {
		connected.clear();
		if (last.parent == null) {
			RouteSegmentPoint pnt = ((RouteSegmentPoint)last.segment);
			addSegmentInternal(last, pnt, connected);
			if (pnt.others != null) {
				for (RouteSegmentPoint o : pnt.others) {
					addSegmentInternal(last, o, connected);
				}
			}
		} else {
			RouteSegment sg = gctx.ctx.loadRouteSegment(last.segment.getEndPointX(), last.segment.getEndPointY(),
					gctx.ctx.config.memoryLimitation);
			while (sg != null) {
				addSegment(last, sg.initRouteSegment(!sg.isPositive()), connected);
				addSegment(last, sg, connected);
				sg = sg.getNext();
			}
		}
	}
	
	private void addSegmentInternal(RouteSegmentAppr last, RouteSegment sg, List<RouteSegmentAppr> connected) {
		boolean accept = approximateSegment(last, sg, connected);
		if (DEBUG && !accept) {
			System.out.println("** " + sg + " - not accepted");
		}
	}

	private void addSegment(RouteSegmentAppr last, RouteSegment sg, List<RouteSegmentAppr> connected) {
		if (sg == null) {
			return;
		}
		int oneway = gctx.ctx.getRouter().isOneWay(sg.getRoad());
		if ((sg.isPositive() && oneway < 0) || (!sg.isPositive() && oneway > 0)) {
			// don't allow passing wrong way
			return;
		}
		// Disable loops:
		// min(sg.getSegmentStart(), sg.getSegmentEnd()) != min(last.segment.getSegmentStart(), last.segment.getSegmentEnd())
		if (sg.getRoad().getId() != last.segment.road.getId()
				|| sg.getSegmentStart() != last.segment.getSegmentStart()) {
			addSegmentInternal(last, sg, connected);
		}
	}

	public void visit(RouteSegmentAppr r) {
		visited.add(calculateRoutePointId(r));
	}
	
	public boolean isVisited(RouteSegmentAppr r) {
		return visited.contains(calculateRoutePointId(r));
	}

	public GpxRouteApproximation gpxApproximation() throws IOException {
		long timeToCalculate = System.nanoTime();
		initGpxPointsXY31(gpxPoints);
		
		GpxPoint currentPoint = findNextRoutablePoint(0);
		if (currentPoint == null) {
			return gctx;
		}
		RouteSegmentAppr last = new RouteSegmentAppr(0, currentPoint.pnt);
		List<RouteSegmentAppr> connected = new ArrayList<>();
		RouteSegmentAppr bestRoute = null;
		while (last.gpxNext() < gpxPoints.size()) { 
			RouteSegmentAppr bestNext = null;
			if (!isVisited(last)) {
				visit(last);
				loadConnections(last, connected);
				if (connected.size() > 0) {
					if (EAGER_ALGORITHM) {
						Collections.sort(connected, METRICS_COMPARATOR);
						bestNext = connected.get(0);
						queue.addAll(connected.subList(1, connected.size()));
					} else if (PRIORITY_ALGORITHM) {
						queue.addAll(connected);
					}
				}
			}
			bestNext = peakMinFromQueue(bestRoute, bestNext); // try to revert to MAX_DEPTH_ROLLBACK if bestNext null
			if (bestNext != null) {
				if (DEBUG) {
					System.out.println(bestNext + " " + gpxPoints.get(bestNext.gpxStart + bestNext.gpxLen).loc);
				}
				if (bestRoute == null || bestRoute.gpxNext() < last.gpxNext()) {
					bestRoute = last;
				}
				last = bestNext;
			} else { 
				if (bestRoute != null) {
					wrapupRoute(gpxPoints, bestRoute);
				}
				GpxPoint pnt = findNextRoutablePoint(bestRoute != null ? bestRoute.gpxNext() : last.gpxNext());
				visited = new TLongHashSet();
				if (pnt == null) {
					debugln("------------------");
					break;
				} else {
					debugln("\n!!! " + pnt.ind + " " + pnt.loc + " " + pnt.pnt);
					last = new RouteSegmentAppr(pnt.ind, pnt.pnt);
					bestRoute = null;
				}
			}

		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		if (bestRoute == null || bestRoute.gpxNext() < last.gpxNext()) {
			bestRoute = last; // prefer the farthest end-of-the-route
		}
		if (bestRoute != null) {
			wrapupRoute(gpxPoints, bestRoute);
		}
		System.out.printf("Approximation took %.2f seconds (%d route points searched)\n",
				(System.nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched);
		return gctx;
	}

	private RouteSegmentAppr peakMinFromQueue(RouteSegmentAppr bestRoute, RouteSegmentAppr bestNext) {
		while (!queue.isEmpty() && bestNext == null) {
			bestNext = queue.remove();
			if ((bestRoute != null && gpxDist(bestRoute.gpxNext(), bestNext.gpxNext()) > MAX_DEPTH_ROLLBACK)) {
				bestNext = null;
			}
		}
		return bestNext;
	}

	private void debugln(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}

	private double gpxDist(int gpxL1, int gpxL2) {
		return gpxPoints.get(Math.min(gpxL1, gpxPoints.size() - 1)).cumDist - 
				gpxPoints.get(Math.min(gpxL2, gpxPoints.size() - 1)).cumDist; 
	}

	private boolean approximateSegment(RouteSegmentAppr parent, RouteSegment sg, List<RouteSegmentAppr> connected) {
		RouteSegmentAppr c = new RouteSegmentAppr(parent, sg);
		boolean added = false;
		for (int pointInd = c.gpxStart + 1; pointInd < gpxPoints.size(); pointInd++) {
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
				if (beforeStart || c.gpxLen > 0) {
					break;
				}
				return added;
			}
			if (dist > MIN_BRANCHING_DIST && dist > c.maxDistToGpx && c.gpxLen > 0) {
				RouteSegmentAppr altShortBranch = new RouteSegmentAppr(parent, sg);
				altShortBranch.maxDistToGpx = c.maxDistToGpx;
				altShortBranch.gpxLen = c.gpxLen;
				added |= addConnected(parent, altShortBranch, connected);
			}
			c.maxDistToGpx = Math.max(c.maxDistToGpx, dist);
			c.gpxLen++;
		}
		added |= addConnected(parent, c, connected);
		return added;
	}

	private boolean addConnected(RouteSegmentAppr parent, RouteSegmentAppr c, List<RouteSegmentAppr> connected) {
		if (isVisited(c)) {
			return false;
		}
		int pointInd = c.gpxNext();
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
				if (DEBUG) {
					System.out.println("** " + c + " - ignore " + dist);
				}
				return false;
			}
		}
		connected.add(c);
		if (DEBUG) {
			System.out.println("** " + c + " - accept");
		}
		return true;
	}

	public GpxPoint findNextRoutablePoint(int searchStart) throws IOException {
		for (int i = searchStart; i < gpxPoints.size(); i++) {
			if (initRoutingPoint(gpxPoints.get(i), initDist)) {
				return gpxPoints.get(i);
			}
		}
		return null;
	}
	
	private boolean initRoutingPoint(GpxPoint start, double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			double gpxDir = start.object.directionRoute(start.ind, true);
			RouteSegmentPoint rsp = frontEnd.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(),
					gctx.ctx, null, false);
			if (rsp == null || MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) > distThreshold) {
				return false;
			}
			start.pnt = initStartPoint(start, gpxDir, rsp);
			if (rsp.others != null) {
				start.pnt.others = new ArrayList<>();
				for (RouteSegmentPoint o : rsp.others) {
					if (MapUtils.getDistance(o.getPreciseLatLon(), start.loc) < distThreshold) {
						start.pnt.others.add(initStartPoint(start, gpxDir, o));
					}
				}
			}
			return true;
		}
		return false;
	}

	private RouteSegmentPoint initStartPoint(GpxPoint start, double gpxDir, RouteSegmentPoint rsp) {
		double dirc = rsp.road.directionRoute(rsp.getSegmentStart(), rsp.isPositive());
		boolean direct = Math.abs(MapUtils.alignAngleDifference(gpxDir - dirc)) < Math.PI / 2;
		return new RouteSegmentPoint(rsp.getRoad(), direct ? rsp.getSegmentStart() : rsp.getSegmentEnd(),
				direct ? rsp.getSegmentEnd() : rsp.getSegmentStart(), rsp.distToProj);
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

	private void wrapupRoute(List<GpxPoint> gpxPoints, RouteSegmentAppr bestRoute) {
		if (bestRoute.parent == null) {
			return;
		}
		List<RouteSegmentResult> res = new ArrayList<>();
		int startInd = 0;
		int last = Math.min(bestRoute.gpxNext(), gpxPoints.size() - 1);
		// combining segments doesn't seem to have any effect on tests
		RouteSegmentResult lastRes = null;
		while (bestRoute != null && bestRoute.parent != null) {
			startInd = bestRoute.gpxStart;
			int end = bestRoute.segment.getSegmentEnd();
			if (lastRes != null && bestRoute.segment.getRoad().getId() == lastRes.getObject().getId()) {
				if (lastRes.getStartPointIndex() == bestRoute.segment.getSegmentEnd()
						&& lastRes.isForwardDirection() == bestRoute.segment.isPositive()) {
					end = lastRes.getEndPointIndex();
					res.remove(res.size() - 1);
				}
			}
			RouteSegmentResult routeRes = new RouteSegmentResult(bestRoute.segment.road,
					bestRoute.segment.getSegmentStart(), end);
			res.add(routeRes);
			bestRoute = bestRoute.parent;
			lastRes = routeRes;
		}
		Collections.reverse(res);
		if (DEBUG) {
			System.out.printf("ROUTE %d -> %d :\n", startInd, last);
			for (RouteSegmentResult r : res) {
				System.out.println(" " + r);
			}
		}
		for (RouteSegmentResult r : res) {
			r.setGpxPointIndex(startInd); // required for reconstructFinalPointsFromFullRoute()
		}
		gpxPoints.get(startInd).routeToTarget = res;
		gpxPoints.get(startInd).targetInd = last; // keep straight line
	}

	private static long calculateRoutePointId(RouteSegmentAppr segm) {
		long segId = 0;
		if (segm.parent != null) {
			boolean positive = segm.segment.isPositive();
			segId = (segm.segment.getRoad().getId() << ROUTE_POINTS) + (segm.segment.getSegmentStart() << 1)
					+ (positive ? 1 : 0);
		}

		return (segId << GPX_MAX) + (segm.gpxStart + segm.gpxLen);
	}
	
}
