package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.util.MapUtils;

public class GpxRoutingApproximation {
	protected static final Log log = PlatformUtil.getLog(GpxRoutingApproximation.class);
	private RoutePlannerFrontEnd routePlanner;
	
	public GpxRoutingApproximation(RoutePlannerFrontEnd routePlannerFrontEnd) {
		this.routePlanner = routePlannerFrontEnd;
	}
	
	public static class GpxApproximationContext {
		// ! MAIN parameter to approximate (35m good for custom recorded tracks)
		public double MINIMUM_POINT_APPROXIMATION = 200; // 35 m good for small deviations
		// This parameter could speed up or slow down evaluation (better to make bigger
		// for long routes and smaller for short)
		public double MAXIMUM_STEP_APPROXIMATION = 3000;
		// don't search subsegments shorter than specified distance (also used to step
		// back for car turns)
		public double MINIMUM_STEP_APPROXIMATION = 100;
		// Parameter to smoother the track itself (could be 0 if it's not recorded
		// track)
		public double SMOOTHEN_POINTS_NO_ROUTE = 5;

		public final RoutingContext ctx;
		public int routeCalculations = 0;
		public int routePointsSearched = 0;
		public int routeDistCalculations = 0;
		public List<GpxPoint> finalPoints = new ArrayList<GpxPoint>();
		public List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		public int routeDistance;
		public int routeGapDistance;
		public int routeDistanceUnmatched;

		public GpxApproximationContext(RoutingContext ctx) {
			this.ctx = ctx;
		}

		public GpxApproximationContext(GpxApproximationContext gctx) {
			this.ctx = gctx.ctx;
			this.routeDistance = gctx.routeDistance;
		}

		@Override
		public String toString() {
			return String.format(
					">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m unmatched",
					routeCalculations, routeDistCalculations, routePointsSearched, routeDistance,
					routeDistanceUnmatched);
		}

		public double distFromLastPoint(LatLon pnt) {
			if (result.size() > 0) {
				return MapUtils.getDistance(getLastPoint(), pnt);
			}
			return 0;
		}

		public LatLon getLastPoint() {
			if (result.size() > 0) {
				return result.get(result.size() - 1).getEndPoint();
			}
			return null;
		}
	}
	
	
	public static class GpxPoint {
		public int ind;
		public LatLon loc;
		public double cumDist;
		public RouteSegmentPoint pnt;
		public List<RouteSegmentResult> routeToTarget;
		public List<RouteSegmentResult> stepBackRoute;
		public int targetInd = -1;
		public boolean straightLine = false;

		public GpxPoint() {
		}

		public RouteSegmentResult getFirstRouteRes() {
			if (routeToTarget == null || routeToTarget.isEmpty()) {
				return null;
			}
			return routeToTarget.get(0);
		}

		public RouteSegmentResult getLastRouteRes() {
			if (routeToTarget == null || routeToTarget.isEmpty()) {
				return null;
			}
			return routeToTarget.get(routeToTarget.size() - 1);
		}

		public GpxPoint(GpxPoint point) {
			this.ind = point.ind;
			this.loc = point.loc;
			this.cumDist = point.cumDist;
		}
	}
	

	public GpxApproximationContext searchGpxRoute(GpxApproximationContext gctx, List<GpxPoint> gpxPoints,
			ResultMatcher<GpxApproximationContext> resultMatcher) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		NativeLibrary nativeLib = gctx.ctx.nativeLib;
		if (nativeLib != null && routePlanner.isUseNativeApproximation()) {
			gctx = nativeLib.runNativeSearchGpxRoute(gctx, gpxPoints);
		} else {
			gctx.ctx.keepNativeRoutingContext = true;
			if (gctx.ctx.calculationProgress == null) {
				gctx.ctx.calculationProgress = new RouteCalculationProgress();
			}
			GpxPoint start = null;
			GpxPoint prev = null;
			if (gpxPoints.size() > 0) {
				gctx.ctx.calculationProgress.totalApproximateDistance = (float) gpxPoints.get(gpxPoints.size() - 1).cumDist;
				start = gpxPoints.get(0);
			}
			float minPointApproximation = gctx.ctx.config.minPointApproximation;
			while (start != null && !gctx.ctx.calculationProgress.isCancelled) {
				double routeDist = gctx.ctx.config.maxStepApproximation;
				GpxPoint next = findNextGpxPointWithin(gpxPoints, start, routeDist);
				boolean routeFound = false;
				if (next != null && initRoutingPoint(start, gctx, minPointApproximation)) {
					while (routeDist >= gctx.ctx.config.minStepApproximation && !routeFound) {
						routeFound = initRoutingPoint(next, gctx, minPointApproximation);
						if (routeFound) {
							routeFound = findGpxRouteSegment(gctx, gpxPoints, start, next, prev != null);
							if (routeFound) {
								routeFound = isRouteCloseToGpxPoints(minPointApproximation, gpxPoints, start, next);
								if (!routeFound) {
									start.routeToTarget = null;
								}
							}
							if (routeFound && next.ind < gpxPoints.size() - 1) {
								// route is found - cut the end of the route and move to next iteration
								// start.stepBackRoute = new ArrayList<RouteSegmentResult>();
								// boolean stepBack = true;
								boolean stepBack = stepBackAndFindPrevPointInRoute(gctx, gpxPoints, start, next);
								if (!stepBack) {
									// not supported case (workaround increase routing.xml maxStepApproximation)
									log.info("Consider to increase routing.xml maxStepApproximation to: " + routeDist * 2);
									start.routeToTarget = null;
									routeFound = false;
								} else {
									if (gctx.ctx.getVisitor() != null) {
										gctx.ctx.getVisitor().visitApproximatedSegments(start.routeToTarget, start, next);
									}
								}
							}
						}
						if (!routeFound) {
							// route is not found move next point closer to start point (distance / 2)
							routeDist = routeDist / 2;
							if (routeDist < gctx.ctx.config.minStepApproximation
									&& routeDist > gctx.ctx.config.minStepApproximation / 2 + 1) {
								routeDist = gctx.ctx.config.minStepApproximation;
							}
							next = findNextGpxPointWithin(gpxPoints, start, routeDist);
							if (next != null) {
								routeDist = Math.min(next.cumDist - start.cumDist, routeDist);
							}
						}
					}
				}
				// route is not found skip segment and keep it as straight line on display
				if (!routeFound && next != null) {
					// route is not found, move start point by
					next = findNextGpxPointWithin(gpxPoints, start, gctx.ctx.config.minStepApproximation);
					if (prev != null) {
						prev.routeToTarget.addAll(prev.stepBackRoute);
//						makeSegmentPointPrecise(prev.routeToTarget.get(prev.routeToTarget.size() - 1), start.loc, false);
						if (next != null) {
							log.warn("NOT found route from: " + start.pnt.getRoad() + " at "
									+ start.pnt.getSegmentStart());
						}
					}
					prev = null;
				} else {
					prev = start;
				}
				start = next;
				if (gctx.ctx.calculationProgress != null && start != null) {
					gctx.ctx.calculationProgress.approximatedDistance = (float) start.cumDist;
				}
			}
			if (gctx.ctx.calculationProgress != null) {
				gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
			}
			gctx.ctx.deleteNativeRoutingContext();
			calculateGpxRoute(gctx, gpxPoints);
			if (!gctx.result.isEmpty() && !gctx.ctx.calculationProgress.isCancelled) {
				RouteResultPreparation.printResults(gctx.ctx, gpxPoints.get(0).loc,
						gpxPoints.get(gpxPoints.size() - 1).loc, gctx.result);
				log.info(gctx);
			}
		}
		if (resultMatcher != null) {
			resultMatcher.publish(gctx.ctx.calculationProgress.isCancelled ? null : gctx);
		}
		return gctx;
	}

	private boolean isRouteCloseToGpxPoints(float minPointApproximation, List<GpxPoint> gpxPoints, GpxPoint start,
			GpxPoint next) {
		boolean routeIsClose = true;
		for (RouteSegmentResult r : start.routeToTarget) {
			int st = r.getStartPointIndex();
			int end = r.getEndPointIndex();
			while (st != end) {
				LatLon point = r.getPoint(st);
				boolean pointIsClosed = false;
				int delta = 3, startInd = Math.max(0, start.ind - delta),
						nextInd = Math.min(gpxPoints.size() - 1, next.ind + delta);
				for (int k = startInd; !pointIsClosed && k < nextInd; k++) {
					pointIsClosed = pointCloseEnough(minPointApproximation, point, gpxPoints.get(k),
							gpxPoints.get(k + 1));
				}
				if (!pointIsClosed) {
					routeIsClose = false;
					break;
				}
				st += ((st < end) ? 1 : -1);
			}
		}
		return routeIsClose;
	}

	private boolean stepBackAndFindPrevPointInRoute(GpxApproximationContext gctx, List<GpxPoint> gpxPoints,
			GpxPoint start, GpxPoint next) throws IOException {
		// step back to find to be sure
		// 1) route point is behind GpxPoint - minPointApproximation (end route point
		// could slightly ahead)
		// 2) we don't miss correct turn i.e. points could be attached to multiple
		// routes
		// 3) to make sure that we perfectly connect to RoadDataObject points
		double STEP_BACK_DIST = Math.max(gctx.ctx.config.minPointApproximation, gctx.ctx.config.minStepApproximation);
		double d = 0;
		int segmendInd = start.routeToTarget.size() - 1;
		boolean search = true;
		start.stepBackRoute = new ArrayList<RouteSegmentResult>();
		mainLoop: for (; segmendInd >= 0 && search; segmendInd--) {
			RouteSegmentResult rr = start.routeToTarget.get(segmendInd);
			boolean minus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int nextInd;
			for (int j = rr.getEndPointIndex(); j != rr.getStartPointIndex(); j = nextInd) {
				nextInd = minus ? j - 1 : j + 1;
				d += MapUtils.getDistance(rr.getPoint(j), rr.getPoint(nextInd));
				if (d > STEP_BACK_DIST) {
					if (nextInd == rr.getStartPointIndex()) {
						segmendInd--;
					} else {
						start.stepBackRoute.add(new RouteSegmentResult(rr.getObject(), nextInd, rr.getEndPointIndex()));
						rr.setEndPointIndex(nextInd);
					}
					search = false;
					break mainLoop;
				}
			}
		}
		if (segmendInd == -1) {
			// here all route segments - 1 is longer than needed distance to step back
			return false;
		}
		while (start.routeToTarget.size() > segmendInd + 1) {
			RouteSegmentResult removed = start.routeToTarget.remove(segmendInd + 1);
			start.stepBackRoute.add(removed);
		}
		RouteSegmentResult res = start.routeToTarget.get(segmendInd);
		int end = res.getEndPointIndex();
		int beforeEnd = res.isForwardDirection() ? end - 1 : end + 1;
//		res.setEndPointIndex(beforeEnd);
		next.pnt = new RouteSegmentPoint(res.getObject(), beforeEnd, end, 0);
		// use start point as it overlaps
		// as we step back we can't use precise coordinates
//		next.pnt.preciseX = MapUtils.get31TileNumberX(next.loc.getLongitude());
//		next.pnt.preciseY = MapUtils.get31TileNumberY(next.loc.getLatitude());
		next.pnt.preciseX = next.pnt.getEndPointX();
		next.pnt.preciseY = next.pnt.getEndPointY();
		return true;
	}

	private void calculateGpxRoute(GpxApproximationContext gctx, List<GpxPoint> gpxPoints) {
		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		List<LatLon> lastStraightLine = null;
		GpxPoint straightPointStart = null;
		for (int i = 0; i < gpxPoints.size() && !gctx.ctx.calculationProgress.isCancelled;) {
			GpxPoint pnt = gpxPoints.get(i);
			if (pnt.routeToTarget != null && !pnt.routeToTarget.isEmpty()) {
				routePlanner.makeSegmentPointPrecise(pnt.getFirstRouteRes(), pnt.loc, true);
				LatLon startPoint = pnt.getFirstRouteRes().getStartPoint();
				if (lastStraightLine != null) {
					lastStraightLine.add(startPoint);
					addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
					lastStraightLine = null;
				}
				if (gctx.distFromLastPoint(startPoint) > 1) {
					gctx.routeGapDistance += gctx.distFromLastPoint(startPoint);
					System.out.println(String.format("?? gap of route point = %f, gap of actual gpxPoint = %f, %s ",
							gctx.distFromLastPoint(startPoint), gctx.distFromLastPoint(pnt.loc), pnt.loc));
				}
				gctx.finalPoints.add(pnt);
				gctx.result.addAll(pnt.routeToTarget);
				i = pnt.targetInd;
				routePlanner.makeSegmentPointPrecise(pnt.getLastRouteRes(), gpxPoints.get(i).loc, false);
			} else {
				// add straight line from i -> i+1
				LatLon lastPoint = null;
				if (lastStraightLine == null) {
					lastStraightLine = new ArrayList<LatLon>();
					straightPointStart = pnt;
					// make smooth connection
					lastPoint = gctx.getLastPoint();
				}
				if (lastPoint == null) {
					lastPoint = pnt.loc;
				}
				lastStraightLine.add(pnt.loc);
				i++;
			}
		}
		if (lastStraightLine != null) {
			addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
			lastStraightLine = null;
		}
		// clean turns to recaculate them
		cleanupResultAndAddTurns(gctx);
	}

	public static RouteSegmentResult generateStraightLineSegment(float averageSpeed, List<LatLon> points) {
		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		RouteDataObject rdo = new RouteDataObject(reg);
		int size = points.size();
		TIntArrayList x = new TIntArrayList(size);
		TIntArrayList y = new TIntArrayList(size);
		double distance = 0;
		double distOnRoadToPass = 0;
		LatLon prev = null;
		for (int i = 0; i < size; i++) {
			LatLon l = points.get(i);
			if (l != null) {
				x.add(MapUtils.get31TileNumberX(l.getLongitude()));
				y.add(MapUtils.get31TileNumberY(l.getLatitude()));
				if (prev != null) {
					double d = MapUtils.getDistance(l, prev);
					distance += d;
					distOnRoadToPass += d / averageSpeed;
				}
			}
			prev = l;
		}
		rdo.pointsX = x.toArray();
		rdo.pointsY = y.toArray();
		rdo.types = new int[] { 0 };
		rdo.id = -1;
		RouteSegmentResult segment = new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1);
		segment.setSegmentTime((float) distOnRoadToPass);
		segment.setSegmentSpeed(averageSpeed);
		segment.setDistance((float) distance);
		segment.setTurnType(TurnType.straight());
		return segment;
	}


	private void cleanupResultAndAddTurns(GpxApproximationContext gctx) {
		// cleanup double joints
		int LOOK_AHEAD = 4;
		for (int i = 0; i < gctx.result.size() && !gctx.ctx.calculationProgress.isCancelled; i++) {
			RouteSegmentResult s = gctx.result.get(i);
			for (int j = i + 2; j <= i + LOOK_AHEAD && j < gctx.result.size(); j++) {
				RouteSegmentResult e = gctx.result.get(j);
				if (e.getStartPoint().equals(s.getEndPoint())) {
					while ((--j) != i) {
						gctx.result.remove(j);
					}
					break;
				}
			}
		}
		RouteResultPreparation preparation = new RouteResultPreparation();
		for (RouteSegmentResult r : gctx.result) {
			r.setTurnType(null);
			r.clearDescription();
		}
		if (!gctx.ctx.calculationProgress.isCancelled) {
			preparation.prepareTurnResults(gctx.ctx, gctx.result);
		}
		for (RouteSegmentResult r : gctx.result) {
			r.clearAttachedRoutes();
			r.clearPreattachedRoutes();
		}
	}

	private void addStraightLine(GpxApproximationContext gctx, List<LatLon> lastStraightLine, GpxPoint strPnt,
			RouteRegion reg) {
		RouteDataObject rdo = new RouteDataObject(reg);
		if (gctx.ctx.config.smoothenPointsNoRoute > 0) {
			simplifyDouglasPeucker(lastStraightLine, gctx.ctx.config.smoothenPointsNoRoute, 0,
					lastStraightLine.size() - 1);
		}
		int s = lastStraightLine.size();
		TIntArrayList x = new TIntArrayList(s);
		TIntArrayList y = new TIntArrayList(s);
		for (int i = 0; i < s; i++) {
			if (lastStraightLine.get(i) != null) {
				LatLon l = lastStraightLine.get(i);
				int t = x.size() - 1;
				x.add(MapUtils.get31TileNumberX(l.getLongitude()));
				y.add(MapUtils.get31TileNumberY(l.getLatitude()));
				if (t >= 0) {
					double dist = MapUtils.squareRootDist31(x.get(t), y.get(t), x.get(t + 1), y.get(t + 1));
					gctx.routeDistanceUnmatched += dist;
				}
			}
		}
		rdo.pointsX = x.toArray();
		rdo.pointsY = y.toArray();
		rdo.types = new int[] { 0 };
		rdo.id = -1;
		strPnt.routeToTarget = new ArrayList<>();
		strPnt.straightLine = true;
		strPnt.routeToTarget.add(new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1));
		RouteResultPreparation preparation = new RouteResultPreparation();
		try {
			preparation.prepareResult(gctx.ctx, strPnt.routeToTarget); // line
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// VIEW: comment to see road without straight connections
		gctx.finalPoints.add(strPnt);
		gctx.result.addAll(strPnt.routeToTarget);
	}

	private void simplifyDouglasPeucker(List<LatLon> l, double eps, int start, int end) {
		double dmax = -1;
		int index = -1;
		LatLon s = l.get(start);
		LatLon e = l.get(end);
		for (int i = start + 1; i <= end - 1; i++) {
			LatLon ip = l.get(i);
			double dist = MapUtils.getOrthogonalDistance(ip.getLatitude(), ip.getLongitude(), s.getLatitude(),
					s.getLongitude(), e.getLatitude(), e.getLongitude());
			if (dist > dmax) {
				dmax = dist;
				index = i;
			}
		}
		if (dmax >= eps) {
			simplifyDouglasPeucker(l, eps, start, index);
			simplifyDouglasPeucker(l, eps, index, end);
		} else {
			for (int i = start + 1; i < end; i++) {
				l.set(i, null);
			}
		}
	}

	private boolean initRoutingPoint(GpxPoint start, GpxApproximationContext gctx, double distThreshold)
			throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			RouteSegmentPoint rsp = routePlanner.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(), gctx.ctx, null,
					false);
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

	private GpxPoint findNextGpxPointWithin(List<GpxPoint> gpxPoints, GpxPoint start, double dist) {
		// returns first point with that has slightly more than dist or last point
		int plus = dist > 0 ? 1 : -1;
		int targetInd = start.ind + plus;
		GpxPoint target = null;
		while (targetInd < gpxPoints.size() && targetInd >= 0) {
			target = gpxPoints.get(targetInd);
			if (Math.abs(target.cumDist - start.cumDist) > Math.abs(dist)) {
				break;
			}
			targetInd = targetInd + plus;
		}
		return target;
	}

	private boolean findGpxRouteSegment(GpxApproximationContext gctx, List<GpxPoint> gpxPoints, GpxPoint start,
			GpxPoint target, boolean prevRouteCalculated) throws IOException, InterruptedException {
		RouteCalcResult res = null;
		boolean routeIsCorrect = false;
		if (start.pnt != null && target.pnt != null) {
			start.pnt = new RouteSegmentPoint(start.pnt);
			target.pnt = new RouteSegmentPoint(target.pnt);
			gctx.routeDistCalculations += (target.cumDist - start.cumDist);
			gctx.routeCalculations++;
			RoutingContext local = new RoutingContext(gctx.ctx);
			res = routePlanner.searchRouteAndPrepareTurns(local, start.pnt, target.pnt, null);
			// BinaryRoutePlanner.printDebugMemoryInformation(gctx.ctx);
			routeIsCorrect = res != null && res.isCorrect();
			for (int k = start.ind + 1; routeIsCorrect && k < target.ind; k++) {
				GpxPoint ipoint = gpxPoints.get(k);
				if (!pointCloseEnough(gctx, ipoint, res)) {
					routeIsCorrect = false;
				}
			}
			if (routeIsCorrect) {
				RouteSegmentResult firstSegment = res.detailed.get(0);
				// correct start point though don't change end point
				if (prevRouteCalculated) {
					if (firstSegment.getObject().getId() == start.pnt.getRoad().getId()) {
						// start point is end point of prev route
						firstSegment.setStartPointIndex(start.pnt.getSegmentEnd());
						if (firstSegment.getObject().getPointsLength() != start.pnt.getRoad().getPointsLength()) {
							firstSegment.setObject(start.pnt.road);
						}
						if (firstSegment.getStartPointIndex() == firstSegment.getEndPointIndex()) {
							res.detailed.remove(0);
						}
					} else {
						// for native routing this is possible when point lies on intersection of 2 lines
						// solution here could be to pass to native routing id of the route
						System.out.println("??? not found " + start.pnt.getRoad().getId() + " instead "
								+ firstSegment.getObject().getId());
					}
				}
				start.routeToTarget = res.detailed;
				start.targetInd = target.ind;
			}
			if (gctx.ctx.getVisitor() != null) {
				gctx.ctx.getVisitor().visitApproximatedSegments(res.detailed, start, target);
			}
		}
		return routeIsCorrect;
	}

	private boolean pointCloseEnough(float minPointApproximation, LatLon point, GpxPoint gpxPoint,
			GpxPoint gpxPointNext) {
		LatLon gpxPointLL = gpxPoint.loc; 
		LatLon gpxPointNextLL = gpxPointNext.loc;
//		LatLon gpxPointLL = gpxPoint.pnt != null ? gpxPoint.pnt.getPreciseLatLon() : gpxPoint.loc;
//		LatLon gpxPointNextLL = gpxPointNext.pnt != null ? gpxPointNext.pnt.getPreciseLatLon() : gpxPointNext.loc;
		double orthogonalDistance = MapUtils.getOrthogonalDistance(point.getLatitude(), point.getLongitude(),
				gpxPointLL.getLatitude(), gpxPointLL.getLongitude(), gpxPointNextLL.getLatitude(),
				gpxPointNextLL.getLongitude());
//		System.out.printf("%.1f %s ( ntNext.ind, gpxPointLL, gpxPointNextLL);
		return orthogonalDistance <= minPointApproximation;
	}

	private boolean pointCloseEnough(GpxApproximationContext gctx, GpxPoint ipoint, RouteCalcResult res) {
		int px = MapUtils.get31TileNumberX(ipoint.loc.getLongitude());
		int py = MapUtils.get31TileNumberY(ipoint.loc.getLatitude());
		double SQR = gctx.ctx.config.minPointApproximation;
		SQR = SQR * SQR;
		for (RouteSegmentResult sr : res.detailed) {
			int start = sr.getStartPointIndex();
			int end = sr.getEndPointIndex();
			if (sr.getStartPointIndex() > sr.getEndPointIndex()) {
				start = sr.getEndPointIndex();
				end = sr.getStartPointIndex();
			}
			for (int i = start; i < end; i++) {
				RouteDataObject r = sr.getObject();
				QuadPointDouble pp = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(i), r.getPoint31YTile(i),
						r.getPoint31XTile(i + 1), r.getPoint31YTile(i + 1));
				double currentsDist = RoutePlannerFrontEnd.squareDist((int) pp.x, (int) pp.y, px, py);
				if (currentsDist <= SQR) {
					return true;
				}
			}
		}
		return false;
	}

}