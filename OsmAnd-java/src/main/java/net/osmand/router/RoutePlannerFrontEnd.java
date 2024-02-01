package net.osmand.router;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.RouteCalculationProgress.HHIteration;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.util.MapUtils;


public class RoutePlannerFrontEnd {

	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
	// Check issue #8649
	protected static final double GPS_POSSIBLE_ERROR = 7;
	static boolean TRACE_ROUTING = false;
	
	private static final HHRoutingConfig DEFAULT_ROUTING_CONFIG = HHRoutingConfig.astar(0).calcDetailed(HHRoutingConfig.CALCULATE_ALL_DETAILED);
//	private static final HHRoutingConfig DEFAULT_ROUTING_CONFIG = HHRoutingConfig.dijkstra(0).calcDetailed(HHRoutingConfig.CALCULATE_ALL_DETAILED);
	private boolean useSmartRouteRecalculation = true;
	private boolean useNativeApproximation = true;
	private boolean useOnlyHHRouting = false;
	private HHRoutingConfig hhRoutingConfig = null;
	

	public RoutePlannerFrontEnd() {
	}
	
	public enum RouteCalculationMode {
		BASE,
		NORMAL,
		COMPLEX
	}
	
	
	public static class GpxRouteApproximation {
		// ! MAIN parameter to approximate (35m good for custom recorded tracks) 
		public double MINIMUM_POINT_APPROXIMATION = 200; // 35 m good for small deviations
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
			return String.format(">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m unmatched",
					routeCalculations, routeDistCalculations, routePointsSearched, routeDistance, routeDistanceUnmatched);
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

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RouteCalculationMode rm) {
		return new RoutingContext(config, nativeLibrary, map, rm);
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		return new RoutingContext(config, nativeLibrary, map, RouteCalculationMode.NORMAL);
	}


	private static double squareDist(int x1, int y1, int x2, int y2) {
		return MapUtils.squareDist31TileMetric(x1, y1, x2, y2);
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
					QuadPointDouble pr = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(j - 1),
							r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j));
					double currentsDistSquare = squareDist((int) pr.x, (int) pr.y, px, py);
					if (road == null || currentsDistSquare < road.distToProj) {
						RouteDataObject ro = new RouteDataObject(r);
						road = new RouteSegmentPoint(ro, j - 1, j, currentsDistSquare);
						road.preciseX = (int) pr.x;
						road.preciseY = (int) pr.y;
					}
				}
				if (road != null) {
					if (!transportStop) {
						float prio = ctx.getRouter().defineDestinationPriority(road.road);
						if (prio > 0) {
							road.distToProj = (road.distToProj + GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR)
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
				return Double.compare(o1.distToProj, o2.distToProj);
			}
		});
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.timeToFindInitialSegments += (System.nanoTime() - now);
		}
		if (list.size() > 0) {
			RouteSegmentPoint ps = null;
			if (ctx.publicTransport) {
				for (RouteSegmentPoint p : list) {
					if (transportStop && p.distToProj > GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR) {
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
			list.remove(ps); // remove cyclic link to itself to avoid memory leaks (C++ backport)
			ps.others = list;
			return ps;
		}
		return null;
	}

	public RouteCalcResult searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) throws IOException, InterruptedException {
		return searchRoute(ctx, start, end, intermediates, null);
	}

	public RoutePlannerFrontEnd setUseFastRecalculation(boolean use) {
		useSmartRouteRecalculation = use;
		return this;
	}
	
	public RoutePlannerFrontEnd setHHRoutingConfig(HHRoutingConfig hhRoutingConfig) {
		// null means don't use hh 
		this.hhRoutingConfig = hhRoutingConfig;
		return this;
	}

	public RoutePlannerFrontEnd disableHHRoutingConfig() {
		this.hhRoutingConfig = null;
		return this;
	}

	public boolean isHHRoutingConfigured() {
		return this.hhRoutingConfig != null;
	}

	public void setDefaultHHRoutingConfig() {
		this.hhRoutingConfig = DEFAULT_ROUTING_CONFIG;
	}
	
	public RoutePlannerFrontEnd setUseOnlyHHRouting(boolean useOnlyHHRouting) {
		this.useOnlyHHRouting = useOnlyHHRouting;
		if (useOnlyHHRouting && hhRoutingConfig == null) {
			this.hhRoutingConfig = DEFAULT_ROUTING_CONFIG;
		}
		return this;
	}

	public RoutePlannerFrontEnd setUseNativeApproximation(boolean useNativeApproximation) {
		this.useNativeApproximation = useNativeApproximation;
		return this;
	}
	
	public boolean isUseNativeApproximation() {
		return useNativeApproximation;
	}

	public GpxRouteApproximation searchGpxRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints, ResultMatcher<GpxRouteApproximation> resultMatcher) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		NativeLibrary nativeLib = gctx.ctx.nativeLib;
		if (nativeLib != null && useNativeApproximation) {
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
										gctx.ctx.getVisitor().visitApproximatedSegments(start.routeToTarget, start,
												next);
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
							log.warn("NOT found route from: " + start.pnt.getRoad() + " at " + start.pnt.getSegmentStart());
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
				RouteResultPreparation.printResults(gctx.ctx, gpxPoints.get(0).loc, gpxPoints.get(gpxPoints.size() - 1).loc, gctx.result);
				log.info(gctx);
			}
		}
		if (resultMatcher != null) {
			resultMatcher.publish(gctx.ctx.calculationProgress.isCancelled ? null : gctx);
		}
		return gctx;
	}

	private boolean isRouteCloseToGpxPoints(float minPointApproximation, List<GpxPoint> gpxPoints,
	                                        GpxPoint start, GpxPoint next) {
		boolean routeIsClose = true;
		for (RouteSegmentResult r : start.routeToTarget) {
			int st = r.getStartPointIndex();
			int end = r.getEndPointIndex();
			while (st != end) {
				LatLon point = r.getPoint(st);
				boolean pointIsClosed = false;
				int delta = 0, startInd = Math.max(0, start.ind - delta), nextInd = Math.min(gpxPoints.size(), next.ind + delta);
				for (int k = startInd; !pointIsClosed && k < nextInd; k++) {
					pointIsClosed = pointCloseEnough(minPointApproximation, point, gpxPoints.get(k), gpxPoints.get(k + 1));
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

	private boolean stepBackAndFindPrevPointInRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
	                                                GpxPoint start, GpxPoint next) throws IOException {
		// step back to find to be sure 
		// 1) route point is behind GpxPoint - minPointApproximation (end route point could slightly ahead)
		// 2) we don't miss correct turn i.e. points could be attached to multiple routes
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

	private void calculateGpxRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints) {
		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		List<LatLon> lastStraightLine = null;
		GpxPoint straightPointStart = null;
		for (int i = 0; i < gpxPoints.size() && !gctx.ctx.calculationProgress.isCancelled; ) {
			GpxPoint pnt = gpxPoints.get(i);
			if (pnt.routeToTarget != null && !pnt.routeToTarget.isEmpty()) {
				makeSegmentPointPrecise(pnt.getFirstRouteRes(), pnt.loc, true);
				LatLon startPoint = pnt.getFirstRouteRes().getStartPoint();
				if (lastStraightLine != null) {
					lastStraightLine.add(startPoint);
					System.out.println(startPoint);
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
				makeSegmentPointPrecise(pnt.getLastRouteRes(), gpxPoints.get(i).loc, false);
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

	private void addStraightLine(GpxRouteApproximation gctx, List<LatLon> lastStraightLine, GpxPoint strPnt, RouteRegion reg) {
		RouteDataObject rdo = new RouteDataObject(reg);
		if (gctx.ctx.config.smoothenPointsNoRoute > 0) {
			simplifyDouglasPeucker(lastStraightLine, gctx.ctx.config.smoothenPointsNoRoute,
					0, lastStraightLine.size() - 1);
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
		rdo.types = new int[] { 0 } ;
		rdo.id = -1;
		strPnt.routeToTarget = new ArrayList<>();
		strPnt.straightLine = true;
		strPnt.routeToTarget.add(new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1));
		RouteResultPreparation preparation = new RouteResultPreparation();
		try {
			preparation.prepareResult(gctx.ctx, strPnt.routeToTarget);
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

	private boolean findGpxRouteSegment(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
			GpxPoint start, GpxPoint target, boolean prevRouteCalculated) throws IOException, InterruptedException {
		RouteCalcResult res = null;
		boolean routeIsCorrect = false;
		if (start.pnt != null && target.pnt != null) {
			start.pnt = new RouteSegmentPoint(start.pnt);
			target.pnt = new RouteSegmentPoint(target.pnt);
			gctx.routeDistCalculations += (target.cumDist - start.cumDist);
			gctx.routeCalculations++;
			RoutingContext local = new RoutingContext(gctx.ctx);
			res = searchRouteInternalPrepare(local, start.pnt, target.pnt, null);
			//BinaryRoutePlanner.printDebugMemoryInformation(gctx.ctx);
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
					} else {
						// for native routing this is possible when point lies on intersection of 2 lines
						// solution here could be to pass to native routing id of the route
						// though it should not create any issue
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

	private boolean pointCloseEnough(float minPointApproximation, LatLon point, GpxPoint gpxPoint, GpxPoint gpxPointNext) {
		LatLon gpxPointLL = gpxPoint.pnt != null ? gpxPoint.pnt.getPreciseLatLon() : gpxPoint.loc;
		LatLon gpxPointNextLL = gpxPointNext.pnt != null ? gpxPointNext.pnt.getPreciseLatLon() : gpxPointNext.loc;
		double orthogonalDistance = MapUtils.getOrthogonalDistance(point.getLatitude(), point.getLongitude(),
				gpxPointLL.getLatitude(), gpxPointLL.getLongitude(),
				gpxPointNextLL.getLatitude(), gpxPointNextLL.getLongitude());
		return orthogonalDistance <= minPointApproximation;
	}

	private boolean pointCloseEnough(GpxRouteApproximation gctx, GpxPoint ipoint, RouteCalcResult res) {
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
		if (ctx.nativeLib != null) {
			int size = points.size();
			int[] y31Coordinates = new int[size];
			int[] x31Coordinates = new int[size];
			for (int i = 0; i < size; i++) {
				y31Coordinates[i] = MapUtils.get31TileNumberY(points.get(i).getLatitude());
				x31Coordinates[i] = MapUtils.get31TileNumberX(points.get(i).getLongitude());
			}
			res = ctx.nativeLib.needRequestPrivateAccessRouting(ctx, x31Coordinates, y31Coordinates);
		} else {
			GeneralRouter router = (GeneralRouter) ctx.getRouter();
			if (router == null) {
				return false;
			}
			Map<String, RoutingParameter> parameters = router.getParameters();
			String allowPrivateKey = null;
			if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE)) {
				allowPrivateKey = GeneralRouter.ALLOW_PRIVATE;
			} else if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK)) {
				allowPrivateKey = GeneralRouter.ALLOW_PRIVATE;
			}
			if (!router.isAllowPrivate() && allowPrivateKey != null) {
				ctx.unloadAllData();
				LinkedHashMap<String, String> mp = new LinkedHashMap<String, String>();
				mp.put(allowPrivateKey, "true");
				mp.put(GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED, "true");
				ctx.setRouter(new GeneralRouter(router.getProfile(), mp));
				for (LatLon latLon : points) {
					RouteSegmentPoint rp = findRouteSegment(latLon.getLatitude(), latLon.getLongitude(), ctx, null);
					if (rp != null && rp.road != null) {
						if (rp.road.hasPrivateAccess(ctx.config.router.getProfile())) {
							res = true;
							break;
						}
					}
				}
				ctx.unloadAllData();
				ctx.setRouter(router);
			}
		}
		return res;
	}

	public RouteCalcResult searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates,
	                                            PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		if (ctx.calculationProgress == null) {
			ctx.calculationProgress = new RouteCalculationProgress();
		}
		boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
		List<LatLon> targets = new ArrayList<>();
		if (!intermediatesEmpty) {
			targets.addAll(intermediates);
		}
		targets.add(end);
		if (needRequestPrivateAccessRouting(ctx, targets)) {
			ctx.calculationProgress.requestPrivateAccessRouting = true;
		}
		if (hhRoutingConfig != null) {
			HHRoutePlanner<NetworkDBPoint> routePlanner = HHRoutePlanner.create(ctx);
			HHNetworkRouteRes r = null;
			Double dir = ctx.config.initialDirection ;
			for (int i = 0; i < targets.size(); i++) {
				float previousTime = ctx.routingTime;
				double initialPenalty = ctx.config.penaltyForReverseDirection;
				if (i > 0) {
					ctx.config.penaltyForReverseDirection /= 2; // relax reverse-penalty (only for inter-points)
				}
				HHNetworkRouteRes res = calculateHHRoute(routePlanner, ctx, i == 0 ? start : targets.get(i - 1),
						targets.get(i), dir);
				ctx.config.penaltyForReverseDirection = initialPenalty;
				if (r == null) {
					r = res;
				} else {
					r.append(res);
				}
				if (r == null || !r.isCorrect()) {
					break;
				}
				if (r.detailed.size() > 0) {
					dir = (r.detailed.get(r.detailed.size() - 1).getBearingEnd() / 180.0) * Math.PI;
				}
				ctx.routingTime += previousTime;
			}
			if (r != null && r.isCorrect() || useOnlyHHRouting) {
				return r;
			}
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
			RouteCalcResult res = searchRoute(nctx, start, end, intermediates);
			routeDirection = PrecalculatedRouteDirection.build(res.detailed, RoutingConfiguration.DEVIATION_RADIUS, ctx.getRouter().getMaxSpeed());
			ctx.calculationProgressFirstPhase = RouteCalculationProgress.capture(ctx.calculationProgress);
		}
		RouteCalcResult res ;
		if (intermediatesEmpty && ctx.nativeLib != null) {
			ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
			ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
			ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
			ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
			RouteSegmentPoint recalculationEnd = getRecalculationEnd(ctx);
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
				return new RouteCalcResult("Start point is not located");
			}
			if (intermediates != null) {
				for (LatLon l : intermediates) {
					if (!addSegment(l, ctx, indexNotFound++, points, false)) {
						System.out.println(points.get(points.size() - 1).getRoad().toString());
						return new RouteCalcResult("Intermediate point is not located");
					}
				}
			}
			if (!addSegment(end, ctx, indexNotFound++, points, ctx.targetTransportStop)) {
				return new RouteCalcResult("End point is not located");
			}
			ctx.calculationProgress.nextIteration();
			res = searchRouteImpl(ctx, points, routeDirection);
		}
		ctx.calculationProgress.timeToCalculate = (System.nanoTime() - timeToCalculate);
		RouteResultPreparation.printResults(ctx, start, end, res.detailed);
		return res;
	}

	private HHNetworkRouteRes calculateHHRoute(HHRoutePlanner<NetworkDBPoint> routePlanner, RoutingContext ctx,
			LatLon start, LatLon end, Double dir) throws InterruptedException, IOException {
		NativeLibrary nativeLib = ctx.nativeLib;
		ctx.nativeLib = null; // keep null to interfere with detailed 
		try {
			HHRoutingConfig cfg = HHRoutePlanner.prepareDefaultRoutingConfig(hhRoutingConfig);
			cfg.INITIAL_DIRECTION = dir;
			HHNetworkRouteRes res = routePlanner.runRouting(start, end, cfg);
			if (res != null && res.error == null) {
				ctx.calculationProgress.hhIteration(HHIteration.DONE);
				makeStartEndPointsPrecise(res, start, end, new ArrayList<LatLon>());
				return res;
			}
			ctx.calculationProgress.hhIteration(null);
		} catch (SQLException e) {
			throw new IOException(e.getMessage(), e);
		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
			if (useOnlyHHRouting) {
				return new HHNetworkRouteRes("Error during routing calculation : " + e.getMessage());
			}
		} finally {
			ctx.nativeLib = nativeLib;
		}
		return null;
	}

	protected void makeStartEndPointsPrecise(RouteCalcResult res, LatLon start, LatLon end, List<LatLon> intermediates) {
		if (res.detailed.size() > 0) {
			makeSegmentPointPrecise(res.detailed.get(0), start, true);
			makeSegmentPointPrecise(res.detailed.get(res.detailed.size() - 1), end, false);
		}
	}

	protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
		RouteSegmentResult sr = res.get(k);
		RouteDataObject r = sr.getObject();
		QuadPointDouble pp = MapUtils.getProjectionPoint31(px, py,
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
		QuadPointDouble before = null;
		QuadPointDouble after = null;
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
		QuadPointDouble i = null;
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

	private RouteCalcResult searchRouteInternalPrepare(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end,
	                                                            PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		RouteSegmentPoint recalculationEnd = getRecalculationEnd(ctx);
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
			ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(ctx, start, recalculationEnd != null ? recalculationEnd : end, null);
			RouteResultPreparation rrp = new RouteResultPreparation();
			// 4. Route is found : collect all segments and prepare result
			List<RouteSegmentResult> result  = rrp.convertFinalSegmentToResults(ctx, ctx.finalRouteSegment);
			addPrecalculatedToResult(recalculationEnd, result);
			return rrp.prepareResult(ctx, result);
		}
	}

	public RouteSegmentPoint getRecalculationEnd(final RoutingContext ctx) {
		RouteSegmentPoint recalculationEnd = null;
		boolean runRecalculation = ctx.previouslyCalculatedRoute != null && !ctx.previouslyCalculatedRoute.isEmpty()
				&& ctx.config.recalculateDistance != 0;
		if (runRecalculation) {
			List<RouteSegmentResult> rlist = new ArrayList<>();
			float distanceThreshold = ctx.config.recalculateDistance;
			float threshold = 0;
			for (RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
				threshold += rr.getDistance();
				if (threshold > distanceThreshold) {
					rlist.add(rr);
				}
			}

			if (!rlist.isEmpty()) {
				RouteSegment previous = null;
				for (int i = 0; i < rlist.size(); i++) {
					RouteSegmentResult rr = rlist.get(i);
					if (previous != null) {
						RouteSegment segment = new RouteSegment(rr.getObject(), rr.getStartPointIndex(),rr.getEndPointIndex());
						previous.setParentRoute(segment);
						previous = segment;
					} else {
						recalculationEnd = new RouteSegmentPoint(rr.getObject(), rr.getStartPointIndex(), 0);
						if (Math.abs(rr.getEndPointIndex() - rr.getStartPointIndex()) > 1) {
							RouteSegment segment = new RouteSegment(rr.getObject(), recalculationEnd.segEnd, rr.getEndPointIndex());
							recalculationEnd.setParentRoute(segment);
							previous = segment;
						} else {
							previous = recalculationEnd;
						}
					}
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

	private RouteCalcResult runNativeRouting(final RoutingContext ctx, RouteSegment recalculationEnd) throws IOException {
		refreshProgressDistance(ctx);
		if (recalculationEnd != null) {
			if (TRACE_ROUTING) {
				log.info("RecalculationEnd = " + recalculationEnd.road + " ind=" + recalculationEnd.getSegmentStart() + "->" + recalculationEnd.getSegmentEnd());
			}
		}
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new RouteRegion[0]);
		// long time = System.currentTimeMillis();
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx, regions, ctx.calculationMode == RouteCalculationMode.BASE);
		if (TRACE_ROUTING) {
			log.info("Native routing result!");
			for (RouteSegmentResult r : res) {
				log.info("Road = " + r.getObject().id / 64 + " " + r.getStartPointIndex() + "->" + r.getEndPointIndex());
			}
		}
		//	log.info("Native routing took " + (System.currentTimeMillis() - time) / 1000f + " seconds");
		List<RouteSegmentResult> result = new ArrayList<>(Arrays.asList(res));
		if (TRACE_ROUTING) {
			log.info("RecalculationEnd result!");
		}
		addPrecalculatedToResult(recalculationEnd, result);
		ctx.routingTime += ctx.calculationProgress.routingCalculatedTime;
		return new RouteResultPreparation().prepareResult(ctx, result);
	}

	private void addPrecalculatedToResult(RouteSegment recalculationEnd, List<RouteSegmentResult> result) {
		if (recalculationEnd != null) {
			log.info("Native routing use precalculated route");
			RouteSegment current = recalculationEnd;
			if (!hasSegment(result, current)) {
				if (TRACE_ROUTING) {
					log.info("Add recalculationEnd to result = " + current.getRoad() + " " + current.getSegmentStart() + "->" + current.getSegmentEnd());
				}
				result.add(new RouteSegmentResult(current.getRoad(), current.getSegmentStart(), current.getSegmentEnd()));
			}
			while (current.getParentRoute() != null) {
				RouteSegment pr = current.getParentRoute();
				result.add(new RouteSegmentResult(pr.getRoad(), pr.getSegmentStart(), pr.getSegmentEnd()));
				if (TRACE_ROUTING) {
					log.info("Road = " + pr.getRoad() + " " + pr.getSegmentStart() + "->" + pr.getSegmentEnd());
				}
				current = pr;
			}
		}
	}

	private boolean hasSegment(List<RouteSegmentResult> result, RouteSegment current) {
		for (RouteSegmentResult r : result) {
			long currentId = r.getObject().id;
			if (currentId == current.getRoad().id && r.getStartPointIndex() == current.getSegmentStart()
					&& r.getEndPointIndex() == current.getSegmentEnd()) {
				return true;
			}
		}
		return false;
	}

	private RouteCalcResult searchRouteImpl(final RoutingContext ctx, List<RouteSegmentPoint> points, PrecalculatedRouteDirection routeDirection)
			throws IOException, InterruptedException {
		if (points.size() <= 2) {
			// simple case 2 points only
			if (!useSmartRouteRecalculation) {
				ctx.previouslyCalculatedRoute = null;
			}
			pringGC(ctx, true);
			RouteCalcResult res = searchRouteInternalPrepare(ctx, points.get(0), points.get(1), routeDirection);
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
		RouteCalcResult results = new RouteCalcResult(new ArrayList<RouteSegmentResult>());
		for (int i = 0; i < points.size() - 1; i++) {
			RoutingContext local = new RoutingContext(ctx);
			if (i == 0 && ctx.nativeLib == null) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
			}
			RouteCalcResult res = searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), routeDirection);
			makeStartEndPointsPrecise(res, points.get(i).getPreciseLatLon(), points.get(i + 1).getPreciseLatLon(), null);
			results.detailed.addAll(res.detailed);
			ctx.routingTime += local.routingTime;
//			local.unloadAllData(ctx);
			if (restPartRecalculatedRoute != null) {
				results.detailed.addAll(restPartRecalculatedRoute);
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
