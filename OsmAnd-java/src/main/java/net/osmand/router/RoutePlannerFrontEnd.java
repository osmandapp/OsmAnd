package net.osmand.router;


import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class RoutePlannerFrontEnd {

	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
	// Check issue #8649
	protected static final double GPS_POSSIBLE_ERROR = 7;
	public boolean useSmartRouteRecalculation = true;

	
	public RoutePlannerFrontEnd() {
	}
	
	public enum RouteCalculationMode {
		BASE,
		NORMAL,
		COMPLEX
	}
	
	public static class GpxRouteApproximation {
		// ! MAIN parameter to approximate (35m good for custom recorded tracks) 
		public double MINIMUM_POINT_APPROXIMATION = 50; // 35 m good for small deviations
		// This parameter could speed up or slow down evaluation (better to make bigger for long routes and smaller for short)
		public double MAXIMUM_STEP_APPROXIMATION = 3000;
		// don't search subsegments shorter than specified distance (also used to step back for car turns)
		public double MINIMUM_STEP_APPROXIMATION = 100;
		// Parameter to smoother the track itself (could be 0 if it's not recorded track)
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


		public GpxRouteApproximation(RoutingContext ctx) {
			this.ctx = ctx;
		}
		
		public GpxRouteApproximation(GpxRouteApproximation gctx) {
			this.ctx = gctx.ctx;
			this.routeDistance = gctx.routeDistance;
		}

		@Override
		public String toString() {
			return String.format(">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m umatched",
					routeCalculations, routeDistCalculations, routePointsSearched, routeDistance, routeDistanceUnmatched);
		}

		public double distFromLastPoint(LatLon startPoint) {
			if (result.size() > 0) {
				return MapUtils.getDistance(getLastPoint(), startPoint);
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

		public GpxPoint(GpxPoint point) {
			this.ind = point.ind;
			this.loc = point.loc;
			this.cumDist = point.cumDist;
		}
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RouteCalculationMode rm) {
		return new RoutingContext(config, nativeLibrary, map, rm);
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		return new RoutingContext(config, nativeLibrary, map, RouteCalculationMode.NORMAL);
	}


	private static double squareDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = MapUtils.convert31YToMeters(y1, y2, x1);
		double dx = MapUtils.convert31XToMeters(x1, x2, y1);
		return dx * dx + dy * dy;
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list) throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list, boolean transportStop) throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false, false);
	}
	
	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list, boolean transportStop, 
			boolean allowDuplications) throws IOException {
		long now = System.nanoTime();
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		ArrayList<RouteDataObject> dataObjects = new ArrayList<RouteDataObject>();
		ctx.loadTileData(px, py, 17, dataObjects, allowDuplications);
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 15, dataObjects, allowDuplications);
		}
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 14, dataObjects, allowDuplications);
		}
		if (list == null) {
			list = new ArrayList<BinaryRoutePlanner.RouteSegmentPoint>();
		}
		for (RouteDataObject r : dataObjects) {
			if (r.getPointsLength() > 1) {
				RouteSegmentPoint road = null;
				for (int j = 1; j < r.getPointsLength(); j++) {
					QuadPoint pr = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(j - 1),
							r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j));
					double currentsDistSquare = squareDist((int) pr.x, (int) pr.y, px, py);
					if (road == null || currentsDistSquare < road.distSquare) {
						RouteDataObject ro = new RouteDataObject(r);
						
						road = new RouteSegmentPoint(ro, j, currentsDistSquare);
						road.preciseX = (int) pr.x;
						road.preciseY = (int) pr.y;
					}
				}
				if (road != null) {
					if(!transportStop) {
						float prio = Math.max(ctx.getRouter().defineSpeedPriority(road.road), 0.3f);
						if (prio > 0) {
							road.distSquare = (road.distSquare + GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR)
									/ (prio * prio);
							list.add(road);
						}
					} else {
						list.add(road);
					}
					
				}
			}
		}
		Collections.sort(list, new Comparator<RouteSegmentPoint>() {

			@Override
			public int compare(RouteSegmentPoint o1, RouteSegmentPoint o2) {
				return Double.compare(o1.distSquare, o2.distSquare);
			}
		});
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.timeToFindInitialSegments += (System.nanoTime() - now);
		}
		if (list.size() > 0) {
			RouteSegmentPoint ps = null;
			if (ctx.publicTransport) {
				for (RouteSegmentPoint p : list) {
					if (transportStop && p.distSquare > GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR) {
						break;
					}
					boolean platform = p.road.platform();
					if (transportStop && platform) {
						ps = p;
						break;
					}
					if (!transportStop && !platform) {
						ps = p;
						break;
					}
				}
			}
			if (ps == null) {
				ps = list.get(0);
			}
			ps.others = list;
			return ps;
		}
		return null;
	}

	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) throws IOException, InterruptedException {
		return searchRoute(ctx, start, end, intermediates, null);
	}

	public void setUseFastRecalculation(boolean use) {
		useSmartRouteRecalculation = use;
	}

	public GpxRouteApproximation searchGpxRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints, ResultMatcher<GpxRouteApproximation> resultMatcher) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		gctx.ctx.keepNativeRoutingContext = true;
		if (gctx.ctx.calculationProgress == null) {
			gctx.ctx.calculationProgress = new RouteCalculationProgress();
		}
		GpxPoint start = null;
		GpxPoint prev = null;
		if (gpxPoints.size() > 0) {
			gctx.ctx.calculationProgress.totalIterations = (int) (gpxPoints.get(gpxPoints.size() - 1).cumDist / gctx.MAXIMUM_STEP_APPROXIMATION + 1); 
			start = gpxPoints.get(0); 
		}
		while (start != null && !gctx.ctx.calculationProgress.isCancelled) {
			double routeDist = gctx.MAXIMUM_STEP_APPROXIMATION;
			GpxPoint next = findNextGpxPointWithin(gctx, gpxPoints, start, routeDist);
			boolean routeFound = false;
			if (next != null && initRoutingPoint(start, gctx, gctx.MINIMUM_POINT_APPROXIMATION)) {
				gctx.ctx.calculationProgress.totalEstimatedDistance = 0;
				gctx.ctx.calculationProgress.iteration = (int) (next.cumDist / gctx.MAXIMUM_STEP_APPROXIMATION);
				while (routeDist >= gctx.MINIMUM_STEP_APPROXIMATION && !routeFound) {
					routeFound = initRoutingPoint(next, gctx, gctx.MINIMUM_POINT_APPROXIMATION);
					if (routeFound) {
						routeFound = findGpxRouteSegment(gctx, gpxPoints, start, next, prev != null);
						if (routeFound) {
							// route is found - cut the end of the route and move to next iteration
//							start.stepBackRoute = new ArrayList<RouteSegmentResult>();
//							boolean stepBack = true;
							boolean stepBack = stepBackAndFindPrevPointInRoute(gctx, gpxPoints, start, next);
							if (!stepBack) {
								// not supported case (workaround increase MAXIMUM_STEP_APPROXIMATION)
								log.info("Consider to increase MAXIMUM_STEP_APPROXIMATION to: " + routeDist * 2);
								start.routeToTarget = null;
								routeFound = false;
								break;
							}
						}
					}
					if (!routeFound) {
						// route is not found move next point closer to start point (distance / 2)
						routeDist = routeDist / 2;
						if (routeDist < gctx.MINIMUM_STEP_APPROXIMATION && routeDist > gctx.MINIMUM_STEP_APPROXIMATION / 2 + 1) {
							routeDist = gctx.MINIMUM_STEP_APPROXIMATION;
						}
						next = findNextGpxPointWithin(gctx, gpxPoints, start, routeDist);
						if (next != null) {
							routeDist = Math.min(next.cumDist - start.cumDist, routeDist);
						}
					}
				}
			}
			// route is not found skip segment and keep it as straight line on display
			if (!routeFound) {
				// route is not found, move start point by 
				next = findNextGpxPointWithin(gctx, gpxPoints, start, gctx.MINIMUM_STEP_APPROXIMATION);
				if (prev != null) {
					prev.routeToTarget.addAll(prev.stepBackRoute);
					makeSegmentPointPrecise(prev.routeToTarget.get(prev.routeToTarget.size() - 1), start.loc, false);
					if (next != null) {
						log.warn("NOT found route from: " + start.pnt.getRoad() + " at " + start.pnt.getSegmentStart());
					}
				}
				prev = null;
			} else {
				prev = start;
			}
			start = next;
		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		gctx.ctx.deleteNativeRoutingContext();
		BinaryRoutePlanner.printDebugMemoryInformation(gctx.ctx);
		calculateGpxRoute(gctx, gpxPoints);
		if (!gctx.result.isEmpty() && !gctx.ctx.calculationProgress.isCancelled) {
			new RouteResultPreparation().printResults(gctx.ctx, gpxPoints.get(0).loc, gpxPoints.get(gpxPoints.size() - 1).loc, gctx.result);
			System.out.println(gctx);
		}
		if (resultMatcher != null) {
			resultMatcher.publish(gctx.ctx.calculationProgress.isCancelled ? null : gctx);
		}
		return gctx;
	}

	private boolean stepBackAndFindPrevPointInRoute(GpxRouteApproximation gctx,
			List<GpxPoint> gpxPoints, GpxPoint start, GpxPoint next) throws IOException {
		// step back to find to be sure 
		// 1) route point is behind GpxPoint - MINIMUM_POINT_APPROXIMATION (end route point could slightly ahead)
		// 2) we don't miss correct turn i.e. points could be attached to muliple routes
		// 3) to make sure that we perfectly connect to RoadDataObject points
		double STEP_BACK_DIST = Math.max(gctx.MINIMUM_POINT_APPROXIMATION, gctx.MINIMUM_STEP_APPROXIMATION);
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
		next.pnt = new RouteSegmentPoint(res.getObject(), res.getEndPointIndex(), 0);
		return true;
	}

	private void calculateGpxRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints) {
		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		List<LatLon> lastStraightLine = null;
		GpxPoint straightPointStart = null;
		for (int i = 0; i < gpxPoints.size() && !gctx.ctx.calculationProgress.isCancelled; ) {
			GpxPoint pnt = gpxPoints.get(i);
			if (pnt.routeToTarget != null && !pnt.routeToTarget.isEmpty()) {
				LatLon startPoint = pnt.routeToTarget.get(0).getStartPoint();
				if (lastStraightLine != null) {
					lastStraightLine.add(startPoint);
					addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
					lastStraightLine = null;
				}
				if (gctx.distFromLastPoint(startPoint) > 1) {
					gctx.routeGapDistance += gctx.distFromLastPoint(startPoint);
					System.out.println(String.format("????? gap of route point = %f, gap of actual gpxPoint = %f, %s ",
							gctx.distFromLastPoint(startPoint), gctx.distFromLastPoint(pnt.loc), pnt.loc));
				}
				gctx.finalPoints.add(pnt);
				gctx.result.addAll(pnt.routeToTarget);
				i = pnt.targetInd;
			} else {
				// add straight line from i -> i+1 
				if (lastStraightLine == null) {
					lastStraightLine = new ArrayList<LatLon>();
					straightPointStart = pnt;
					// make smooth connection
					if (gctx.distFromLastPoint(pnt.loc) > 1) {
						lastStraightLine.add(gctx.getLastPoint());
					}
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
		rdo.types = new int[] { 0 } ;
		rdo.id = -1;
		RouteSegmentResult segment = new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1);
		segment.setSegmentTime((float) distOnRoadToPass);
		segment.setSegmentSpeed(averageSpeed);
		segment.setDistance((float) distance);
		segment.setTurnType(TurnType.straight());
		return segment;
	}

	public List<GpxPoint> generateGpxPoints(GpxRouteApproximation gctx, LocationsHolder locationsHolder) {
		List<GpxPoint> gpxPoints = new ArrayList<>(locationsHolder.getSize());
		GpxPoint prev = null;
		for(int i = 0; i < locationsHolder.getSize(); i++) {
			GpxPoint p = new GpxPoint();
			p.ind = i;
			p.loc = locationsHolder.getLatLon(i);
			if (prev != null) {
				p.cumDist = MapUtils.getDistance(p.loc, prev.loc) + prev.cumDist;
			}
			gpxPoints.add(p);
			gctx.routeDistance = (int) p.cumDist;
			prev = p;
		}
		return gpxPoints;
	}
 
	private void cleanupResultAndAddTurns(GpxRouteApproximation gctx) {
		// cleanup double joints
		int LOOK_AHEAD = 4;
		for(int i = 0; i < gctx.result.size() && !gctx.ctx.calculationProgress.isCancelled; i++) {
			RouteSegmentResult s = gctx.result.get(i);
			for(int j = i + 2; j <= i + LOOK_AHEAD && j < gctx.result.size(); j++) {
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
			r.setDescription("");
		}
		if (!gctx.ctx.calculationProgress.isCancelled) {
			preparation.prepareTurnResults(gctx.ctx, gctx.result);
		}
	}

	private void addStraightLine(GpxRouteApproximation gctx, List<LatLon> lastStraightLine, GpxPoint strPnt, RouteRegion reg) {
		RouteDataObject rdo = new RouteDataObject(reg);
		if(gctx.SMOOTHEN_POINTS_NO_ROUTE > 0) {
			simplifyDouglasPeucker(lastStraightLine, gctx.SMOOTHEN_POINTS_NO_ROUTE, 0, lastStraightLine.size() - 1);
		}
		int s = lastStraightLine.size();
		TIntArrayList x = new TIntArrayList(s);
		TIntArrayList y = new TIntArrayList(s);
		for (int i = 0; i < s; i++) {
			if(lastStraightLine.get(i) != null) {
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
		rdo.types = new int[] { 0 } ;
		rdo.id = -1;
		strPnt.routeToTarget = new ArrayList<>();
		strPnt.straightLine = true;
		strPnt.routeToTarget.add(new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1));
		RouteResultPreparation preparation = new RouteResultPreparation();
		try {
			preparation.prepareResult(gctx.ctx, strPnt.routeToTarget, false);
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
			double dist = MapUtils.getOrthogonalDistance(ip.getLatitude(), ip.getLongitude(), s.getLatitude(), s.getLongitude(), 
					e.getLatitude(), e.getLongitude());
			if (dist > dmax) {
				dmax = dist;
				index = i;
			}
		}		
		if (dmax >= eps) {
			simplifyDouglasPeucker(l, eps, start, index);
			simplifyDouglasPeucker(l, eps, index, end);
		} else {
			for(int i = start + 1; i < end; i++ ) {
				l.set(i, null);
			}
		}
	}

	private boolean initRoutingPoint(GpxPoint start, GpxRouteApproximation gctx, double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			RouteSegmentPoint rsp = findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(), gctx.ctx, null, false);
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
	
	private GpxPoint findNextGpxPointWithin(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
			GpxPoint start, double dist) {
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

	private boolean findGpxRouteSegment(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
			GpxPoint start, GpxPoint target, boolean prevRouteCalculated) throws IOException, InterruptedException {
		List<RouteSegmentResult> res = null;
		boolean routeIsCorrect = false;
		if (start.pnt != null && target.pnt != null) {
			start.pnt = new RouteSegmentPoint(start.pnt);
			target.pnt = new RouteSegmentPoint(target.pnt);
			gctx.routeDistCalculations += (target.cumDist - start.cumDist);
			gctx.routeCalculations++;
			res = searchRouteInternalPrepare(gctx.ctx, start.pnt, target.pnt, null);
			//BinaryRoutePlanner.printDebugMemoryInformation(gctx.ctx);
			routeIsCorrect = res != null && !res.isEmpty();
			for (int k = start.ind + 1; routeIsCorrect && k < target.ind; k++) {
				GpxPoint ipoint = gpxPoints.get(k);
				if (!pointCloseEnough(gctx, ipoint, res)) {
					routeIsCorrect = false;
				}
			}
			if (routeIsCorrect) {
				// correct start point though don't change end point
				if (!prevRouteCalculated) {
					// make first position precise
					makeSegmentPointPrecise(res.get(0), start.loc, true);
				} else {
					if(res.get(0).getObject().getId() == start.pnt.getRoad().getId()) {
						// start point could shift to +-1 due to direction
						res.get(0).setStartPointIndex(start.pnt.getSegmentStart());
					} else {
						// for native routing this is possible when point lies on intersection of 2 lines
						// solution here could be to pass to native routing id of the route
						// though it should not create any issue
						System.out.println("??? not found " + start.pnt.getRoad().getId() + " instead "
								+ res.get(0).getObject().getId());
					}
				}
				start.routeToTarget = res;
				start.targetInd = target.ind;
			}
		}
		return routeIsCorrect;
	}

	private boolean pointCloseEnough(GpxRouteApproximation gctx, GpxPoint ipoint, List<RouteSegmentResult> res) {
		int px = MapUtils.get31TileNumberX(ipoint.loc.getLongitude());
		int py = MapUtils.get31TileNumberY(ipoint.loc.getLatitude());
		double SQR = gctx.MINIMUM_POINT_APPROXIMATION;
		SQR = SQR * SQR;
		for (RouteSegmentResult sr : res) {
			int start = sr.getStartPointIndex();
			int end = sr.getEndPointIndex();
			if (sr.getStartPointIndex() > sr.getEndPointIndex()) {
				start = sr.getEndPointIndex();
				end = sr.getStartPointIndex();
			}
			for (int i = start; i < end; i++) {
				RouteDataObject r = sr.getObject();
				QuadPoint pp = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(i), r.getPoint31YTile(i),
						r.getPoint31XTile(i + 1), r.getPoint31YTile(i + 1));
				double currentsDist = squareDist((int) pp.x, (int) pp.y, px, py);
				if (currentsDist <= SQR) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean needRequestPrivateAccessRouting(RoutingContext ctx, List<LatLon> points) throws IOException {
		boolean res = false;
		GeneralRouter router = (GeneralRouter) ctx.getRouter();
		if (router != null && !router.isAllowPrivate() && 
				router.getParameters().containsKey(GeneralRouter.ALLOW_PRIVATE)) {
			ctx.unloadAllData();
			LinkedHashMap<String, String> mp = new LinkedHashMap<String, String>();
			mp.put(GeneralRouter.ALLOW_PRIVATE, "true");
			mp.put(GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED, "true");
			ctx.setRouter(new GeneralRouter(router.getProfile(), mp));
			for (LatLon latLon : points) {
				RouteSegmentPoint rp = findRouteSegment(latLon.getLatitude(), latLon.getLongitude(), ctx, null);
				if (rp != null && rp.road != null) {
					if (rp.road.hasPrivateAccess()) {
						res = true;
						break;
					}
				}
			}
			ctx.unloadAllData();
			ctx.setRouter(router);
		}
		return res;
	}

	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates,
	                                            PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		if (ctx.calculationProgress == null) {
			ctx.calculationProgress = new RouteCalculationProgress();
		}
		boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
		List<LatLon> targets = new ArrayList<>();
		targets.add(end);
		if (!intermediatesEmpty) {
			targets.addAll(intermediates);
		}
		if (needRequestPrivateAccessRouting(ctx, targets)) {
			ctx.calculationProgress.requestPrivateAccessRouting = true;
		}
		double maxDistance = MapUtils.getDistance(start, end);
		if (!intermediatesEmpty) {
			LatLon b = start;
			for (LatLon l : intermediates) {
				maxDistance = Math.max(MapUtils.getDistance(b, l), maxDistance);
				b = l;
			}
		}
		if (ctx.calculationMode == RouteCalculationMode.COMPLEX && routeDirection == null
				&& maxDistance > RoutingConfiguration.DEVIATION_RADIUS * 6) {
			ctx.calculationProgress.totalIterations++;
			RoutingContext nctx = buildRoutingContext(ctx.config, ctx.nativeLib, ctx.getMaps(), RouteCalculationMode.BASE);
			nctx.calculationProgress = ctx.calculationProgress;
			List<RouteSegmentResult> ls = searchRoute(nctx, start, end, intermediates);
			if (ls == null) {
				return null;
			}
			routeDirection = PrecalculatedRouteDirection.build(ls, RoutingConfiguration.DEVIATION_RADIUS, ctx.getRouter().getMaxSpeed());
		}
		List<RouteSegmentResult> res ;
		if (intermediatesEmpty && ctx.nativeLib != null) {
			ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
			ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
			ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
			ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
			RouteSegment recalculationEnd = getRecalculationEnd(ctx);
			if (recalculationEnd != null) {
				ctx.initTargetPoint(recalculationEnd);
			}
			if (routeDirection != null) {
				ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
			}
			ctx.calculationProgress.nextIteration();
			res = runNativeRouting(ctx, recalculationEnd);
			makeStartEndPointsPrecise(res, start, end, intermediates);
		} else {
			int indexNotFound = 0;
			List<RouteSegmentPoint> points = new ArrayList<RouteSegmentPoint>();
			if (!addSegment(start, ctx, indexNotFound++, points, ctx.startTransportStop)) {
				return null;
			}
			if (intermediates != null) {
				for (LatLon l : intermediates) {
					if (!addSegment(l, ctx, indexNotFound++, points, false)) {
						System.out.println(points.get(points.size() - 1).getRoad().toString());
						return null;
					}
				}
			}
			if (!addSegment(end, ctx, indexNotFound++, points, ctx.targetTransportStop)) {
				return null;
			}
			ctx.calculationProgress.nextIteration();
			res = searchRouteImpl(ctx, points, routeDirection);
		}
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.timeToCalculate = (System.nanoTime() - timeToCalculate);
		}
		BinaryRoutePlanner.printDebugMemoryInformation(ctx);
		if (res != null) {
			new RouteResultPreparation().printResults(ctx, start, end, res);
		}
		return res;
	}

	protected void makeStartEndPointsPrecise(List<RouteSegmentResult> res, LatLon start, LatLon end, List<LatLon> intermediates) {
		if (res.size() > 0) {
			makeSegmentPointPrecise(res.get(0), start, true);
			makeSegmentPointPrecise(res.get(res.size() - 1), end, false);
		}
	}

	protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
		RouteSegmentResult sr = res.get(k);
		RouteDataObject r = sr.getObject();
		QuadPoint pp = MapUtils.getProjectionPoint31(px, py,
				r.getPoint31XTile(sr.getStartPointIndex()), r.getPoint31YTile(sr.getStartPointIndex()),
				r.getPoint31XTile(sr.getEndPointIndex()), r.getPoint31YTile(sr.getEndPointIndex()));
		double currentsDist = squareDist((int) pp.x, (int) pp.y, px, py);
		return currentsDist;
	}

	private void makeSegmentPointPrecise(RouteSegmentResult routeSegmentResult, LatLon point, boolean st) {
		int px = MapUtils.get31TileNumberX(point.getLongitude());
		int py = MapUtils.get31TileNumberY(point.getLatitude());
		int pind = st ? routeSegmentResult.getStartPointIndex() : routeSegmentResult.getEndPointIndex();

		RouteDataObject r = new RouteDataObject(routeSegmentResult.getObject());
		routeSegmentResult.setObject(r);
		QuadPoint before = null;
		QuadPoint after = null;
		if (pind > 0) {
			before = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind - 1),
					r.getPoint31YTile(pind - 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind));
		}
		if (pind < r.getPointsLength() - 1) {
			after = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind + 1),
					r.getPoint31YTile(pind + 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind));
		}
		int insert = 0;
		double dd = MapUtils.getDistance(point, MapUtils.get31LatitudeY(r.getPoint31YTile(pind)),
				MapUtils.get31LongitudeX(r.getPoint31XTile(pind)));
		double ddBefore = Double.POSITIVE_INFINITY;
		double ddAfter = Double.POSITIVE_INFINITY;
		QuadPoint i = null;
		if (before != null) {
			ddBefore = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) before.y),
					MapUtils.get31LongitudeX((int) before.x));
			if (ddBefore < dd) {
				insert = -1;
				i = before;
			}
		}

		if (after != null) {
			ddAfter = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) after.y),
					MapUtils.get31LongitudeX((int) after.x));
			if (ddAfter < dd && ddAfter < ddBefore) {
				insert = 1;
				i = after;
			}
		}

		if (insert != 0) {
			if (st && routeSegmentResult.getStartPointIndex() < routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
			}
			if (!st && routeSegmentResult.getStartPointIndex() > routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
			}
			if (insert > 0) {
				r.insert(pind + 1, (int) i.x, (int) i.y);
				if (st) {
					routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
				}
				if (!st) {
					routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
				}
			} else {
				r.insert(pind, (int) i.x, (int) i.y);
			}

		}

	}

	private boolean addSegment(LatLon s, RoutingContext ctx, int indexNotFound, List<RouteSegmentPoint> res, boolean transportStop) throws IOException {
		RouteSegmentPoint f = findRouteSegment(s.getLatitude(), s.getLongitude(), ctx, null, transportStop);
		if (f == null) {
			ctx.calculationProgress.segmentNotFound = indexNotFound;
			return false;
		} else {
			log.info("Route segment found " + f.road);
			res.add(f);
			return true;
		}

	}

	private List<RouteSegmentResult> searchRouteInternalPrepare(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end,
	                                                            PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		RouteSegment recalculationEnd = getRecalculationEnd(ctx);
		if (recalculationEnd != null) {
			ctx.initStartAndTargetPoints(start, recalculationEnd);
		} else {
			ctx.initStartAndTargetPoints(start, end);
		}
		if (routeDirection != null) {
			ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
		}
		if (ctx.nativeLib != null) {
			ctx.startX = start.preciseX;
			ctx.startY = start.preciseY;
			ctx.startRoadId = start.road.id;
			ctx.startSegmentInd  = start.segStart;
			ctx.targetX = end.preciseX;
			ctx.targetY = end.preciseY;
			ctx.targetRoadId = end.road.id;
			ctx.targetSegmentInd  = end.segStart;
			return runNativeRouting(ctx, recalculationEnd);
		} else {
			refreshProgressDistance(ctx);
			// Split into 2 methods to let GC work in between
			ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(ctx, start, end, recalculationEnd);
			// 4. Route is found : collect all segments and prepare result
			return new RouteResultPreparation().prepareResult(ctx, ctx.finalRouteSegment);
		}
	}

	public RouteSegment getRecalculationEnd(final RoutingContext ctx) {
		RouteSegment recalculationEnd = null;
		boolean runRecalculation = ctx.previouslyCalculatedRoute != null && ctx.previouslyCalculatedRoute.size() > 0
				&& ctx.config.recalculateDistance != 0;
		if (runRecalculation) {
			List<RouteSegmentResult> rlist = new ArrayList<RouteSegmentResult>();
			float distanceThreshold = ctx.config.recalculateDistance;
			float threshold = 0;
			for (RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
				threshold += rr.getDistance();
				if (threshold > distanceThreshold) {
					rlist.add(rr);
				}
			}
			runRecalculation = rlist.size() > 0;
			if (rlist.size() > 0) {
				RouteSegment previous = null;
				for (int i = 0; i <= rlist.size() - 1; i++) {
					RouteSegmentResult rr = rlist.get(i);
					RouteSegment segment = new RouteSegment(rr.getObject(), rr.getEndPointIndex());
					if (previous != null) {
						previous.setParentRoute(segment);
						previous.setParentSegmentEnd(rr.getStartPointIndex());
					} else {
						recalculationEnd = segment;
					}
					previous = segment;
				}
			}
		}
		return recalculationEnd;
	}


	private void refreshProgressDistance(RoutingContext ctx) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.distanceFromBegin = 0;
			ctx.calculationProgress.distanceFromEnd = 0;
			ctx.calculationProgress.reverseSegmentQueueSize = 0;
			ctx.calculationProgress.directSegmentQueueSize = 0;
			float rd = (float) MapUtils.squareRootDist31(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY);
			float speed = 0.9f * ctx.config.router.getMaxSpeed();
			ctx.calculationProgress.totalEstimatedDistance = (float) (rd / speed);
		}

	}

	private List<RouteSegmentResult> runNativeRouting(final RoutingContext ctx, RouteSegment recalculationEnd) throws IOException {
		refreshProgressDistance(ctx);
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new RouteRegion[0]);
		ctx.checkOldRoutingFiles(ctx.startX, ctx.startY);
		ctx.checkOldRoutingFiles(ctx.targetX, ctx.targetY);

		// long time = System.currentTimeMillis();
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx, regions, ctx.calculationMode == RouteCalculationMode.BASE);
		//	log.info("Native routing took " + (System.currentTimeMillis() - time) / 1000f + " seconds");
		ArrayList<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>(Arrays.asList(res));
		if (recalculationEnd != null) {
			log.info("Native routing use precalculated route");
			RouteSegment current = recalculationEnd;
			while (current.getParentRoute() != null) {
				RouteSegment pr = current.getParentRoute();
				result.add(new RouteSegmentResult(pr.getRoad(), current.getParentSegmentEnd(), pr.getSegmentStart()));
				current = pr;
			}
		}
		ctx.routingTime += ctx.calculationProgress.routingCalculatedTime;
		return new RouteResultPreparation().prepareResult(ctx, result, recalculationEnd != null);
	}


	private List<RouteSegmentResult> searchRouteImpl(final RoutingContext ctx, List<RouteSegmentPoint> points, PrecalculatedRouteDirection routeDirection)
			throws IOException, InterruptedException {
		if (points.size() <= 2) {
			// simple case 2 points only
			if (!useSmartRouteRecalculation) {
				ctx.previouslyCalculatedRoute = null;
			}
			pringGC(ctx, true);
			List<RouteSegmentResult> res = searchRouteInternalPrepare(ctx, points.get(0), points.get(1), routeDirection);
			pringGC(ctx, false);
			makeStartEndPointsPrecise(res, points.get(0).getPreciseLatLon(), points.get(1).getPreciseLatLon(), null);
			return res;
		}

		ArrayList<RouteSegmentResult> firstPartRecalculatedRoute = null;
		ArrayList<RouteSegmentResult> restPartRecalculatedRoute = null;
		if (ctx.previouslyCalculatedRoute != null) {
			List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
			long id = points.get(1).getRoad().id;
			int ss = points.get(1).getSegmentStart();
			int px = points.get(1).getRoad().getPoint31XTile(ss);
			int py = points.get(1).getRoad().getPoint31YTile(ss);
			for (int i = 0; i < prev.size(); i++) {
				RouteSegmentResult rsr = prev.get(i);
				if (id == rsr.getObject().getId()) {
					if (MapUtils.getDistance(rsr.getPoint(rsr.getEndPointIndex()), MapUtils.get31LatitudeY(py),
							MapUtils.get31LongitudeX(px)) < 50) {
						firstPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(i + 1);
						restPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(prev.size() - i);
						for (int k = 0; k < prev.size(); k++) {
							if (k <= i) {
								firstPartRecalculatedRoute.add(prev.get(k));
							} else {
								restPartRecalculatedRoute.add(prev.get(k));
							}
						}
						System.out.println("Recalculate only first part of the route");
						break;
					}
				}
			}
		}
		List<RouteSegmentResult> results = new ArrayList<RouteSegmentResult>();
		for (int i = 0; i < points.size() - 1; i++) {
			RoutingContext local = new RoutingContext(ctx);
			if (i == 0) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
			}
			local.visitor = ctx.visitor;
			local.calculationProgress = ctx.calculationProgress;
			List<RouteSegmentResult> res = searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), routeDirection);
			makeStartEndPointsPrecise(res, points.get(i).getPreciseLatLon(), points.get(i + 1).getPreciseLatLon(), null);
			results.addAll(res);
			ctx.routingTime += local.routingTime;
//			local.unloadAllData(ctx);
			if (restPartRecalculatedRoute != null) {
				results.addAll(restPartRecalculatedRoute);
				break;
			}
		}
		ctx.unloadAllData();
		return results;

	}

	private void pringGC(final RoutingContext ctx, boolean before) {
		if (RoutingContext.SHOW_GC_SIZE && before) {
			long h1 = RoutingContext.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Used before routing " + h1 / mb + " actual");
		} else if (RoutingContext.SHOW_GC_SIZE && !before) {
			int sz = ctx.global.size;
			log.warn("Subregion size " + ctx.subregionTiles.size() + " " + " tiles " + ctx.indexedSubregions.size());
			RoutingContext.runGCUsedMemory();
			long h1 = RoutingContext.runGCUsedMemory();
			ctx.unloadAllData();
			RoutingContext.runGCUsedMemory();
			long h2 = RoutingContext.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Unload context :  estimated " + sz / mb + " ?= " + (h1 - h2) / mb + " actual");
		}
	}

	

}
