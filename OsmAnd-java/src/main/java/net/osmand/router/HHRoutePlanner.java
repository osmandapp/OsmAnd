package net.osmand.router;



import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.MultiFinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
import net.osmand.router.HHRouteDataStructure.HHNetworkSegmentRes;
import net.osmand.router.HHRouteDataStructure.HHRouteRegionPointsCtx;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.HHRouteDataStructure.NetworkDBPointCost;
import net.osmand.router.HHRouteDataStructure.NetworkDBPointRouteInfo;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;
import net.osmand.router.HHRouteDataStructure.RoutingStats;
import net.osmand.router.RouteCalculationProgress.HHIteration;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

public class HHRoutePlanner<T extends NetworkDBPoint> {
	public static int DEBUG_VERBOSE_LEVEL = 0;
	static int DEBUG_ALT_ROUTE_SELECTION = -1;
	static final double MINIMAL_COST = 0.01;
	private static final int PNT_SHORT_ROUTE_START_END = -1000;
	public static final int MAX_POINTS_CLUSTER_ROUTING = 150000;
	// if point is present without map with HH routing it will iterate each time with MAX_POINTS_CLUSTER_ROUTING
	public static final double MAX_INC_COST_CORR = 10.0;
	// this constant should dynamically change if route is not found
	static final double EXCLUDE_PRIORITY_CONSTANT = 0.0; // see comments below
	
	private static boolean ASSERT_COST_INCREASING = false;
	private static boolean ASSERT_AND_CORRECT_DIST_SMALLER = true;
	private final Class<T> pointClass;
	private final HHRouteRegionPointsCtx<T> predefinedRegions;
	private HHRoutingContext<T> currentCtx; // never null
	
	
	public static HHRoutePlanner<NetworkDBPoint> createDB(RoutingContext ctx, HHRoutingDB networkDB) {
		return new HHRoutePlanner<NetworkDBPoint>(ctx,
					new HHRouteRegionPointsCtx<NetworkDBPoint>((short) 0, networkDB), NetworkDBPoint.class);
	}
	
	public static <Ts extends NetworkDBPoint> HHRoutePlanner<Ts> createDB(RoutingContext ctx, HHRoutingDB networkDB, Class<Ts> cl) {
		return new HHRoutePlanner<Ts>(ctx, new HHRouteRegionPointsCtx<Ts>((short) 0, networkDB), cl);
	}
	
	public static HHRoutePlanner<NetworkDBPoint> create(RoutingContext ctx) {
		return new HHRoutePlanner<NetworkDBPoint>(ctx, null, NetworkDBPoint.class);
	}
	
	private HHRoutePlanner(RoutingContext ctx, HHRouteRegionPointsCtx<T> src, Class<T> cl) {
		this.pointClass = cl;
		this.predefinedRegions = src;
		initNewContext(ctx, src == null ? null : Collections.singletonList(src));
	}
	
	private HHRoutingContext<T> initNewContext(RoutingContext ctx, List<HHRouteRegionPointsCtx<T>> regions) {
		currentCtx = new HHRoutingContext<T>();
		currentCtx.rctx = ctx;
		if (regions != null) {
			currentCtx.regions.addAll(regions);
		}
		return currentCtx;
	}

	public void close() throws SQLException {
		if (predefinedRegions != null && predefinedRegions.networkDB != null) {
			try {
				predefinedRegions.networkDB.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		currentCtx.regions.clear();
	}
	
	public static double squareRootDist31(int x1, int y1, int x2, int y2) {
//		return MapUtils.measuredDist31(x1, y1, x2, y2);
		return MapUtils.squareRootDist31(x1, y1, x2, y2);
	}
	
	
	public static RoutingContext prepareContext(String routingProfile) {
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
		Builder builder = RoutingConfiguration.getDefault();
		RoutingMemoryLimits memoryLimit = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		Map<String, String> map = new TreeMap<String, String>();
		RoutingConfiguration config = builder.build(routingProfile, memoryLimit, map);
		config.planRoadDirection = 1;
		config.heuristicCoefficient = 0; // dijkstra
		return router.buildRoutingContext(config, null, new BinaryMapIndexReader[0], RouteCalculationMode.NORMAL);
	}
	
	public static HHRoutingConfig prepareDefaultRoutingConfig(HHRoutingConfig c) {
		if (c == null) {
			c = new HHRoutingConfig();
			// test data for debug swap
//			c = HHRoutingConfig.dijkstra(0); 
			c = HHRoutingConfig.astar(0);
//			c = HHRoutingConfig.ch();
//			c.preloadSegments();
			c.ROUTE_LAST_MILE = true;
			c.calcDetailed(2);
//			c.calcAlternative();
//			c.gc();
			DEBUG_VERBOSE_LEVEL = 0;
			DEBUG_ALT_ROUTE_SELECTION++;
//			c.ALT_EXCLUDE_RAD_MULT_IN = 1;
//			c.ALT_EXCLUDE_RAD_MULT = 0.05;
//			c.INITIAL_DIRECTION = 30 / 180.0 * Math.PI;
//			routingProfile = (routingProfile + 1) % networkDB.getRoutingProfiles().size();
//			HHRoutingContext.USE_GLOBAL_QUEUE = true;
		}
		c.applyCalculateMissingMaps(RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS);
		return c;
	}

	public static <T extends NetworkDBPoint> HHNetworkRouteRes cancelledStatus(HHRoutingContext<T> hctx, TLongObjectHashMap<T> stPoints, TLongObjectHashMap<T> endPoints) {
		hctx.clearAll(stPoints, endPoints);
		return new HHNetworkRouteRes("Routing was cancelled.");
	}
	
	public void printf(boolean cond, String format, Object... args) {
		if (cond) {
			System.out.printf(format, args);
		}
	}
	
	@SuppressWarnings("unchecked")
	public HHNetworkRouteRes runRouting(LatLon start, LatLon end, HHRoutingConfig config) throws SQLException, IOException, InterruptedException {
		long startTime = System.nanoTime();
		int SL = HHRoutingConfig.STATS_VERBOSE_LEVEL;
		RouteCalculationProgress progress = currentCtx.rctx.calculationProgress;
		// important assumption that routing context match!
		if (config.cacheCtx != null && config.cacheCtx.rctx  == currentCtx.rctx) {
			currentCtx = (HHRoutingContext<T>) config.cacheCtx;
		}
		config = prepareDefaultRoutingConfig(config);
		HHRoutingContext<T> hctx = initHCtx(config, start, end);
		if (config.CACHE_CALCULATION_CONTEXT) {
			if (config.cacheCtx != null && config.cacheCtx != hctx) {
				System.out.printf("Recreate routing cache context %s -> %s \n", config.cacheCtx.hashCode() + "",
						hctx == null ? "" : hctx.hashCode() + "");
			}
			config.cacheCtx = (HHRoutingContext<NetworkDBPoint>) hctx;
		}
		if (hctx == null) {
			return new HHNetworkRouteRes("Files for hh routing were not initialized. Route couldn't be calculated.");
		}
		filterPointsBasedOnConfiguration(hctx);

		TLongObjectHashMap<T> stPoints = new TLongObjectHashMap<>(), endPoints = new TLongObjectHashMap<>();
		progress.hhIteration(HHIteration.START_END_POINT);
		findFirstLastSegments(hctx, start, end, stPoints, endPoints, progress);

		RouteResultPreparation rrp = new RouteResultPreparation();
		HHNetworkRouteRes route = null;
		boolean recalc = false;
		double firstIterationTime = 0; 
		int iteration = 0;
		int calcCount = 0;
		while (route == null) {
			progress.hhIteration(HHIteration.ROUTING);
			iteration++;
			if (recalc && firstIterationTime == 0) {
				printf(DEBUG_VERBOSE_LEVEL == 0 && SL > 0, "  Recalculating route due to route structure changes...");
				firstIterationTime = hctx.stats.routingTime;
			}
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && SL > 0, " Routing...");
			long time = System.nanoTime();
			NetworkDBPoint finalPnt = runRoutingPointsToPoints(hctx, stPoints, endPoints);
			if (finalPnt == null) {
				printf(SL > 0, " finalPnt is null (stop)\n");
				hctx.clearAll(stPoints, endPoints);
				return new HHNetworkRouteRes("No finalPnt found (points might be filtered by params)");
			}
			calcCount++;
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints);
			}
			route = createRouteSegmentFromFinalPoint(hctx, finalPnt);
			time = (System.nanoTime() - time) ;
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && SL > 0,"%d segments, cost %.2f, %.2f ms\n", route.segments.size(), route.getHHRoutingTime(), time / 1e6);
			hctx.stats.routingTime += time / 1e6;
			
			progress.hhIteration(HHIteration.DETAILED);
			printf((!recalc || DEBUG_VERBOSE_LEVEL > 0) && SL > 0, " Parse detailed route segments...");
			time = System.nanoTime();
			recalc = retrieveSegmentsGeometry(hctx, rrp, route, hctx.config.ROUTE_ALL_SEGMENTS, progress);
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints);
			}
			time = (System.nanoTime() - time);
			printf((firstIterationTime == 0 || DEBUG_VERBOSE_LEVEL > 0) && SL > 0, "%.2f ms\n", time / 1e6);
			hctx.stats.routingTime += time / 1e6;
			if (recalc) {
				if (calcCount > hctx.config.MAX_COUNT_REITERATION) {
					if (SL >= 0) {
						printFinalMessage(" [too many cancelled]", start, end, startTime, hctx);
					}
					hctx.clearAll(stPoints, endPoints);
					return new HHNetworkRouteRes("Too many recalculations (outdated maps or unsupported parameters).");
				}
				hctx.clearVisited(stPoints, endPoints);
				route = null;
			}
		}
		if (firstIterationTime > 0) {
			printf(DEBUG_VERBOSE_LEVEL == 0 && SL > 0, "%d iterations, %.2f ms\n", iteration, hctx.stats.routingTime - firstIterationTime);
		}
		double altRoutes = 0;
		if (hctx.config.CALC_ALTERNATIVES) {
			progress.hhIteration(HHIteration.ALTERNATIVES);
			printf(SL > 0, " Alternative routes...");
			long time = System.nanoTime();
			calcAlternativeRoute(hctx, route, stPoints, endPoints, progress);
			if (progress.isCancelled) {
				return cancelledStatus(hctx, stPoints, endPoints);
			}
			hctx.stats.altRoutingTime += (System.nanoTime() - time) / 1e6;
			hctx.stats.routingTime += hctx.stats.altRoutingTime;
			printf(SL > 0, "%d %.2f ms\n", route.altRoutes.size(), hctx.stats.altRoutingTime);

			time = System.nanoTime();
			for (HHNetworkRouteRes alt : route.altRoutes) {
				retrieveSegmentsGeometry(hctx, rrp, alt, hctx.config.ROUTE_ALL_ALT_SEGMENTS, progress);
				if (progress.isCancelled) {
					return cancelledStatus(hctx, stPoints, endPoints);
				}
			}
			altRoutes = (System.nanoTime() - time) / 1e6;
			printf(SL > 0, "%.2f ms\n", altRoutes);
			
		}
		long time = System.nanoTime();
		printf(SL > 0, " Prepare results (turns, alt routes)...");

		if (hctx.config.USE_GC_MORE_OFTEN) {
			hctx.unloadAllConnections();
			printGCInformation(true);
		}
		
		prepareRouteResults(hctx, route, start, end, rrp);
		if (DEBUG_VERBOSE_LEVEL >= 1) {
			System.out.println("  Detailed progress: " + hctx.rctx.calculationProgress.getInfo(null));
		}
		if (progress.isCancelled) {
			return cancelledStatus(hctx, stPoints, endPoints);
		}
		if (hctx.config.ROUTE_ALL_SEGMENTS && route.detailed != null) {
			route.detailed = rrp.prepareResult(hctx.rctx, route.detailed).detailed;
		}
		hctx.stats.prepTime += (System.nanoTime() - time) / 1e6;
		printf(SL > 0, "%.2f ms\n", hctx.stats.prepTime);
		printf(SL > 0, "Found final route - cost %.2f (detailed %.2f, %.1f%%), %d depth ( first met %,d, visited %,d (%,d unique) of %,d added vertices )", 
				route.getHHRoutingTime(), route.getHHRoutingDetailed(), 100 * (1 - route.getHHRoutingDetailed() / (route.getHHRoutingTime() + 0.01)),
				route.segments.size(), hctx.stats.firstRouteVisitedVertices, hctx.stats.visitedVertices, hctx.stats.uniqueVisitedVertices, hctx.stats.addedVertices);
		hctx.stats.prepTime += altRoutes;
		if (SL > 0) {
			RouteResultPreparation.printResults(hctx.rctx, start, end, route.detailed);
		}
		printGCInformation(false);
		hctx.clearAll(stPoints, endPoints);
		if (SL >= 0) {
			printFinalMessage("", start, end, startTime, hctx);
		}
		return route;
	}

	private void printFinalMessage(String msg, LatLon start, LatLon end, long startTime, HHRoutingContext<T> hctx) {
		printf(true,
				"Routing%s %.1f ms (ctx %s): load/filter points %.1f ms, last mile %.1f ms, routing %.1f ms (queue  - %.1f ms, %.1f ms - %,d edges), prep result %.1f ms - %s (selected %s)\n",
				msg, (System.nanoTime() - startTime) / 1e6, hctx.hashCode() + "", 
				hctx.stats.loadPointsTime, hctx.stats.searchPointsTime,
				hctx.stats.routingTime, hctx.stats.addQueueTime + hctx.stats.pollQueueTime,
				hctx.stats.loadEdgesTime, hctx.stats.loadEdgesCnt, hctx.stats.prepTime,
				hctx.config.toString(start, end), hctx.getRoutingInfo());
	}

	public static TreeMap<String, String> getFilteredTags(GeneralRouter generalRouter) {
		Map<String, RoutingParameter> parameters = generalRouter.getParameters();
		TreeMap<String, String> tm = new TreeMap<String, String>();
		for (Entry<String, String> es : generalRouter.getParameterValues().entrySet()) {
			String paramId = es.getKey();
			// These parameters don't affect the routing filters
			// This assumption probably shouldn't be used in general and only for popular parameters)
			if (parameters.containsKey(paramId) && !paramId.equals(GeneralRouter.USE_SHORTEST_WAY)
					&& !paramId.equals(GeneralRouter.USE_HEIGHT_OBSTACLES)
					&& !paramId.startsWith(GeneralRouter.GROUP_RELIEF_SMOOTHNESS_FACTOR)) {
				tm.put(paramId, es.getValue());
			}
		}
		return tm;
	}

	private void filterPointsBasedOnConfiguration(HHRoutingContext<T> hctx) {
		TreeMap<String, String> tm = getFilteredTags((GeneralRouter) hctx.rctx.getRouter());
		if (hctx.filterRoutingParameters.equals(tm)) {
			return;
		}
		for (T pnt : hctx.pointsById.valueCollection()) {
			pnt.rtExclude = false;
		}
		if (tm.isEmpty()) {
			// no parameters
			hctx.filterRoutingParameters = tm;
			return;
		}
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, " Filter points based on parameters...");
		long nt = System.nanoTime();
		RouteRegion regR = new RouteRegion();
		TIntArrayList tint = new TIntArrayList(50);
		RouteDataObject rdo = new RouteDataObject(regR);
		for (T pnt : hctx.pointsById.valueCollection()) {
			if (pnt.tagValues != null) {
				for (TagValuePair tp : pnt.tagValues) {
					tp.additionalAttribute = -1;
				}
			}
		}
		int filtered = 0;
		for (T pnt : hctx.pointsById.valueCollection()) {
			if (pnt.tagValues != null) {
				tint.reset();
				for (TagValuePair tp : pnt.tagValues) {
					// reuse additionalAttribute to cache values
					if (tp.additionalAttribute < 0) {
						tp.additionalAttribute = regR.searchRouteEncodingRule(tp.tag, tp.value);
					}
					if (tp.additionalAttribute < 0) {
						tp.additionalAttribute = regR.routeEncodingRules.size();
						regR.initRouteEncodingRule(tp.additionalAttribute, tp.tag, tp.value);
					}
					tint.add(tp.additionalAttribute);
				}
				// here we always copy array but in C++ we could be more efficient
				rdo.types = tint.toArray();
				pnt.rtExclude = !currentCtx.rctx.getRouter().acceptLine(rdo);
				// This might speed up for certain avoid parameters 
				// but produces unpredictably wrong results (prefer_unpaved) when shortcuts are calculated - error 300%  
//				if (!pnt.rtExclude) {
//					// constant should be reduced if route is not found
//					pnt.rtExclude = currentCtx.rctx.getRouter().defineSpeedPriority(rdo, pnt.end > pnt.start) < EXCLUDE_PRIORITY_CONSTANT;
//				}
				if (pnt.rtExclude) {
					filtered++;
				}
			}
		}
		hctx.filterRoutingParameters = tm;
		double time = (System.nanoTime() - nt) / 1e6;
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "%d excluded from %d, %.2f ms\n", filtered, hctx.pointsById.size(), time);
		hctx.stats.loadPointsTime += time;

	}

	private void findFirstLastSegments(HHRoutingContext<T> hctx, LatLon start, LatLon end,
	                                   TLongObjectHashMap<T> stPoints, TLongObjectHashMap<T> endPoints,
	                                   RouteCalculationProgress progress) throws IOException, InterruptedException {
		long time = System.nanoTime();
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, " Finding first / last segments...\n");
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		int startReiterate = -1, endReiterate = -1;
		boolean found = false;
		progress.hhIterationProgress(0.00); // %
		RouteSegmentPoint startPnt = planner.findRouteSegment(start.getLatitude(), start.getLongitude(), hctx.rctx, null);
		if (startPnt == null) {
			return;
		}
		progress.hhIterationProgress(0.25); // %
		RouteSegmentPoint endPnt = planner.findRouteSegment(end.getLatitude(), end.getLongitude(), hctx.rctx, null);
		if (endPnt == null) {
			return;
		}
		List<RouteSegmentPoint> stOthers = startPnt.others, endOthers = endPnt.others;
		while (!found) {
			if (startReiterate + endReiterate >= hctx.config.MAX_START_END_REITERATIONS) {
				break;
			}
			for (T p : stPoints.valueCollection()) {
				p.clearRouting();
			}
			stPoints.clear();
			for (T p : endPoints.valueCollection()) {
				p.clearRouting();
			}
			endPoints.clear();
			RouteSegmentPoint startP = startPnt;
			if (startReiterate >= 0) {
				if (stOthers != null && startReiterate < stOthers.size()) {
					startP = stOthers.get(startReiterate);
				} else {
					break;
				}
			}
			RouteSegmentPoint endP = endPnt;
			if (endReiterate >= 0) {
				if (endOthers != null && endReiterate < endOthers.size()) {
					endP = endOthers.get(endReiterate);
				} else {
					break;
				}
			}
			Double prev = hctx.rctx.config.initialDirection;
			hctx.rctx.config.initialDirection = hctx.config.INITIAL_DIRECTION;
			hctx.boundaries.put(calcRPId(endP, endP.getSegmentEnd(), endP.getSegmentStart()), null);
			hctx.boundaries.put(calcRPId(endP, endP.getSegmentStart(), endP.getSegmentEnd()), null);
			progress.hhIterationProgress(0.50); // %
			initStart(hctx, startP, false, stPoints);
			hctx.rctx.config.initialDirection = prev;
			if (stPoints.isEmpty()) {
				printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "   Reiterate with next start point: " + startP + "\n");
				startReiterate++;
				found = false;
				continue;
			}

			hctx.boundaries.remove(calcRPId(endP, endP.getSegmentEnd(), endP.getSegmentStart()));
			hctx.boundaries.remove(calcRPId(endP, endP.getSegmentStart(), endP.getSegmentEnd()));
			if (stPoints.containsKey(PNT_SHORT_ROUTE_START_END)) {
				endPoints.put(PNT_SHORT_ROUTE_START_END, stPoints.get(PNT_SHORT_ROUTE_START_END));
			}
			progress.hhIterationProgress(0.75); // %
			initStart(hctx, endP, true, endPoints);
			if (endPoints.isEmpty()) {
				printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "   Reiterate with next end point: " + endP + "\n");
				endReiterate++;
				found = false;
				continue;
			}
			found = true;
		}
		
		hctx.stats.searchPointsTime = (System.nanoTime() - time) / 1e6;
		if (DEBUG_VERBOSE_LEVEL > 1) {
			TLongObjectIterator<T> it = stPoints.iterator();
			while(it.hasNext()) {
				it.advance();
				NetworkDBPointRouteInfo pi = it.value().rt(false);
				printf(DEBUG_VERBOSE_LEVEL > 1, "   Start point %d cost %.2f, dist = %.2f\n",
						it.value().index, pi.rtCost, pi.rtDistanceFromStart);	
			}
			it = endPoints.iterator();
			while(it.hasNext()) {
				it.advance();
				NetworkDBPointRouteInfo pi = it.value().rt(true);
				printf(DEBUG_VERBOSE_LEVEL > 1, "   End point %d cost %.2f, dist = %.2f\n",
						it.value().index, pi.rtCost, pi.rtDistanceFromStart);	
			}
		}
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, " Finding first (%d) / last (%d) segments...%.2f ms\n",
				stPoints.size(), endPoints.size(), hctx.stats.searchPointsTime);
	}

	private void calcAlternativeRoute(HHRoutingContext<T> hctx, HHNetworkRouteRes route, TLongObjectHashMap<T> stPoints,
			TLongObjectHashMap<T> endPoints, RouteCalculationProgress progress) throws SQLException, IOException {
		List<NetworkDBPoint>  exclude = new ArrayList<>();
		try {
			HHNetworkRouteRes rt = route;
			// distances between all points and start/end
			List<NetworkDBPoint> points = new ArrayList<>();
			for (int i = 0; i < route.segments.size(); i++) {
				NetworkDBSegment s = route.segments.get(i).segment;
				if (s == null) {
					continue;
				}
				if(points.size() == 0) {
					points.add(s.start);
				}
				points.add(s.end);
			}
			double[] distances = new double[points.size()];
			NetworkDBPoint prev = null;
			for (int i = 0; i < distances.length; i++) {
				NetworkDBPoint pnt = points.get(i);
				if (i == 0) {
					distances[i] = squareRootDist31(hctx.startX, hctx.startY, pnt.midX(), pnt.midY());
				} else if (i == distances.length - 1) {
					distances[i] = squareRootDist31(hctx.endX, hctx.endY, pnt.midX(), pnt.midY());
				} else {
					distances[i] = squareRootDist31(prev.midX(), prev.midY(), pnt.midX(), pnt.midY());
				}
				prev = pnt;
			}
			// calculate min(cumPos, cumNeg) distance
			double[] cdistPos = new double[distances.length];
			double[] cdistNeg = new double[distances.length];
			for (int i = 0; i < distances.length; i++) {
				if(i == 0) {
					cdistPos[0] = distances[i];
					cdistNeg[distances.length - 1] = distances[distances.length - 1];
				} else {
					cdistPos[i] = cdistPos[i - 1] + distances[i];
					cdistNeg[distances.length - i - 1] = cdistNeg[distances.length - i] + distances[distances.length - i - 1];
				}
			}
			double[] minDistance = new double[distances.length];
			boolean[] useToSkip = new boolean[distances.length];
			int altPoints = 0;
			for (int i = 0; i < distances.length; i++) {
				minDistance[i] = Math.min(cdistNeg[i], cdistPos[i]) * hctx.config.ALT_EXCLUDE_RAD_MULT;
				boolean coveredByPrevious = false;
				for (int j = 0; j < i; j++) {
					if (useToSkip[j] && cdistPos[i] - cdistPos[j] < minDistance[j] * hctx.config.ALT_EXCLUDE_RAD_MULT_IN) {
						coveredByPrevious = true;
						break;
					}
				}
				if(!coveredByPrevious) {
					useToSkip[i] = true;
					altPoints ++;
				} else {
					minDistance[i] = 0; // for printing purpose
				}
			}
			if (DEBUG_VERBOSE_LEVEL >= 1) {
				System.out.printf("Selected %d points for alternatives %s\n", altPoints, Arrays.toString(minDistance));
			}
			if (progress.isCancelled) {
				return;
			}
			for (int i = 0; i < distances.length; i++) {
				if (!useToSkip[i]) {
					continue;
				}
				hctx.clearVisited(stPoints, endPoints);
//				hctx.clearVisited();
				for (NetworkDBPoint pnt : exclude) {
					pnt.rtExclude = false;
				}
				exclude.clear();
				
				LatLon pnt = points.get(i).getPoint();
				List<T> objs = hctx.pointsRect.getClosestObjects(pnt.getLatitude(), pnt.getLongitude(), minDistance[i]);
				for (T p : objs) {
					if (MapUtils.getDistance(p.getPoint(), pnt) <= minDistance[i]) {
						exclude.add(p);
						p.rtExclude = true;
					}
				}
				
				NetworkDBPoint finalPnt = runRoutingPointsToPoints(hctx, stPoints, endPoints);
				if (progress.isCancelled) {
					return;
				}
				if (finalPnt != null) {
					double cost = (finalPnt.rt(false).rtDistanceFromStart + finalPnt.rt(true).rtDistanceFromStart);
					if (DEBUG_VERBOSE_LEVEL == 1) {
						System.out.println("Alternative route cost: " + cost);
					}
					rt = createRouteSegmentFromFinalPoint(hctx, finalPnt);
					route.altRoutes.add(rt);
				} else {
					break;
				}
				
			}
			route.altRoutes.sort(new Comparator<HHNetworkRouteRes>() {

				@Override
				public int compare(HHNetworkRouteRes o1, HHNetworkRouteRes o2) {
					return Double.compare(o1.getHHRoutingTime(), o2.getHHRoutingTime());
				}
			});
			int size = route.altRoutes.size();
			if (size > 0) {
				for(int k = 0; k < route.altRoutes.size(); ) {
					HHNetworkRouteRes altR = route.altRoutes.get(k);
					boolean unique = true;
					for (int j = 0; j <= k; j++) {
						HHNetworkRouteRes cmp = j == k ? route : route.altRoutes.get(j);
						TLongHashSet cp = new TLongHashSet(altR.uniquePoints);
						cp.retainAll(cmp.uniquePoints);
						if (cp.size() >= hctx.config.ALT_NON_UNIQUENESS * altR.uniquePoints.size()) {
							unique = false;
							break;
						}
					}
					if (unique) {
						k++;
					} else {
						route.altRoutes.remove(k);
					}
				}
				if (route.altRoutes.size() > 0) {
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "Cost %.2f - %.2f [%d unique / %d]...", route.altRoutes.get(0).getHHRoutingTime(),
						route.altRoutes.get(route.altRoutes.size() - 1).getHHRoutingTime(), route.altRoutes.size(), size);
				}
				int ind = DEBUG_ALT_ROUTE_SELECTION % (route.altRoutes.size() + 1);
				if (ind > 0) {
					HHNetworkRouteRes rts = route.altRoutes.get(ind - 1);
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, DEBUG_ALT_ROUTE_SELECTION + " select %.2f ", rts.getHHRoutingTime());
					route.detailed = rts.detailed;
					route.segments = rts.segments;
					route.altRoutes = Collections.singletonList(rts);
				}
			}
		} finally {
			for (NetworkDBPoint pnt : exclude) {
				pnt.rtExclude = false;
			}
		}
				
	}

	protected HHRoutingContext<T> initHCtx(HHRoutingConfig c, LatLon start, LatLon end) throws SQLException, IOException {
		HHRoutingContext<T> hctx = this.currentCtx;
		RouteCalculationProgress progress = hctx.rctx.calculationProgress;
		if (predefinedRegions == null) {
			progress.hhIteration(HHIteration.SELECT_REGIONS);
			hctx = selectBestRoutingFiles(start, end, hctx, c.STRICT_BEST_GROUP_MAPS);
		}
		if (hctx == null) {
			System.out.println("No files found for routing");
			return hctx;
		}
		if (HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
			System.out.println("Selected files: " + (hctx == null ? " NULL " : hctx.getRoutingInfo()));
		}
		hctx.stats = new RoutingStats();
		hctx.config = c;
		hctx.setStartEnd(start, end);
		hctx.clearVisited();
		if (hctx.initialized) {
			return hctx;
		}
		
		long time = System.nanoTime();
		progress.hhIteration(HHIteration.LOAD_POINTS);
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "Loading points... ");
		hctx.pointsById = hctx.loadNetworkPoints(pointClass);
		hctx.boundaries = new TLongObjectHashMap<RouteSegment>();
		hctx.pointsByGeo = new TLongObjectHashMap<T>();
		if (c.PRELOAD_SEGMENTS) {
			time = System.nanoTime();
			printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "Loading segments...");
			int cntEdges = hctx.loadNetworkSegments(hctx.pointsById.valueCollection());
			hctx.stats.loadEdgesTime = (System.nanoTime() - time) / 1e6;
			printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, " %,d - %.2fms\n", cntEdges, hctx.stats.loadEdgesTime);
			hctx.stats.loadEdgesCnt = cntEdges;
		} else {
			for (NetworkDBPoint p : hctx.pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
		}
		hctx.clusterOutPoints = groupByClusters(hctx.pointsById, true);
		hctx.clusterInPoints  = groupByClusters(hctx.pointsById, false);
		for (T pnt : hctx.pointsById.valueCollection()) {
			long pos = calculateRoutePointInternalId(pnt.roadId, pnt.start, pnt.end);
			LatLon latlon = pnt.getPoint();
			hctx.pointsRect.registerObject(latlon.getLatitude(), latlon.getLongitude(), pnt);
			if (pos != pnt.getGeoPntId()) {
				throw new IllegalStateException(pnt + " " + pos + " != "+ pnt.getGeoPntId());
			}
			hctx.boundaries.put(pos, null);
			hctx.pointsByGeo.put(pos, pnt);
			hctx.regions.get(pnt.mapId).pntsByFileId.put(pnt.fileId, pnt);
		}		
		if (DEBUG_VERBOSE_LEVEL > 0) {
			hctx.pointsRect.printStatsDistribution("  Points distributed");
		}
		hctx.initialized = true;
		hctx.stats.loadPointsTime = (System.nanoTime() - time) / 1e6;
		printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, " %,d - %.2fms\n", hctx.pointsById.size(), hctx.stats.loadPointsTime);
		return hctx;
	}

	private static class HHRouteRegionsGroup<T extends NetworkDBPoint> {
		List<HHRouteRegion> regions = new ArrayList<>();
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		final long edition;
		final String profileParams;
		
		public int extraParam = 0;
		public int matchParam = 0;
		public boolean containsStartEnd;
		public double sumIntersects;
		
		public HHRouteRegionsGroup(long edition, String params) {
			this.profileParams = params;
			this.edition = edition;
		}
		
		public static <T extends NetworkDBPoint> void appendToGroups(HHRouteRegion r, BinaryMapIndexReader rdr,
				List<HHRouteRegionsGroup<T>> groups, double iou) {
			for (String params : r.profileParams) {
				HHRouteRegionsGroup<T> matchGroup = null;
				for (HHRouteRegionsGroup<T> g : groups) {
					if (g.edition == r.edition && params.equals(g.profileParams)) {
						matchGroup = g;
						break;
					}
				}
				if (matchGroup == null) {
					matchGroup = new HHRouteRegionsGroup<T>(r.edition, params);
					groups.add(matchGroup);
				}
				matchGroup.regions.add(r);
				matchGroup.readers.add(rdr);
				matchGroup.sumIntersects += iou;
			}
		}

		public boolean contains(LatLon p) throws IOException {
			int x31 = MapUtils.get31TileNumberX(p.getLongitude());
			int y31 = MapUtils.get31TileNumberY(p.getLatitude());
			Set<String> checked = new HashSet<>();
			for (int i = 0; i < regions.size(); i++) {
				BinaryMapIndexReader rd = readers.get(i);
				if (rd.containsRouteData()) {
					if (rd.containsActualRouteData(x31, y31, checked)) {
						return true;
					}
				} else {
					HHRouteRegion reg = regions.get(i);
					if (reg.top.contains(x31, y31)) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean containsStartEndRegion(String[] regionsCoveringStartAndTargets) {
			if (regionsCoveringStartAndTargets.length == 0) {
				return true;
			}
			for (BinaryMapIndexReader reader : readers) {
				for (RouteRegion index : reader.getRoutingIndexes()) {
					for (String region : regionsCoveringStartAndTargets) {
						if (region.equalsIgnoreCase(index.getName())) {
							return true;
						}
					}

				}
			}
			return false;
		}
	}

	private HHRoutingContext<T> selectBestRoutingFiles(LatLon start, LatLon end, HHRoutingContext<T> hctx,
	                                                   boolean strictBestGroupMaps) throws IOException {
		List<HHRouteRegionsGroup<T>> groups = new ArrayList<>();
	
		GeneralRouter router = hctx.rctx.config.router;
//		String profile = router.getProfileName();
		String profile = router.getProfile().toString().toLowerCase(); // use base profile
		List<String> ls = router.serializeParameterValues(router.getParameterValues());
		QuadRect qr = new QuadRect(Math.min(start.getLongitude(), end.getLongitude()),
				Math.max(start.getLatitude(), end.getLatitude()),
				Math.max(start.getLongitude(), end.getLongitude()),
				Math.min(start.getLatitude(), end.getLatitude()));
		
		for (BinaryMapIndexReader r : hctx.rctx.map.keySet()) {
			for (HHRouteRegion hhregion : r.getHHRoutingIndexes()) {
				if (hhregion.profile.equals(profile) && QuadRect.intersects(hhregion.getLatLonBbox(), qr)) {
					double intersect = QuadRect.intersectionArea(hhregion.getLatLonBbox(), qr);
					HHRouteRegionsGroup.appendToGroups(hhregion, r, groups, intersect);
				}
			}
		}
		for (HHRouteRegionsGroup<T> g : groups) {
			g.containsStartEnd = g.contains(start) && g.contains(end)
					&& g.containsStartEndRegion(hctx.rctx.regionsCoveringStartAndTargets);
			String[] params = g.profileParams.split(",");
			for (String p : params) {
				if (p.trim().length() == 0) {
					continue;
				}
				if (!ls.contains(p)) {
					g.extraParam++;
				} else {
					g.matchParam++;
				}
			}
		}
		Collections.sort(groups, new Comparator<HHRouteRegionsGroup<T>>() {

			@Override
			public int compare(HHRouteRegionsGroup<T> o1, HHRouteRegionsGroup<T> o2) {
				if (o1.containsStartEnd != o2.containsStartEnd) {
					return o1.containsStartEnd ? -1 : 1;
				} else if (o1.edition != o2.edition) {
					return o1.edition > o2.edition ? -1 : 1;
				} else if (o1.extraParam != o2.extraParam) {
					return o1.extraParam < o2.extraParam ? -1 : 1;
				} else if (o1.matchParam != o2.matchParam) {
					return o1.matchParam > o2.matchParam ? -1 : 1;
				}
				return -Double.compare(o1.sumIntersects, o2.sumIntersects); // higher is better
			}
			
		});
		if (groups.size() == 0) {
			return null;
		}
		HHRouteRegionsGroup<T> bestGroup = groups.get(0);
		List<HHRouteRegionPointsCtx<T>> regions = new ArrayList<>();
		for(short mapId = 0; mapId < bestGroup.regions.size(); mapId++) {
			HHRouteRegionPointsCtx<T> reg = new HHRouteRegionPointsCtx<T>(mapId, bestGroup.regions.get(mapId),
					bestGroup.readers.get(mapId), bestGroup.regions.get(mapId).profileParams.indexOf(bestGroup.profileParams));
			regions.add(reg);
			
		}
		boolean allMatched = true;
		for (HHRouteRegionPointsCtx<T> r : regions) {
			boolean match = false;
			for (HHRouteRegionPointsCtx<T> p : currentCtx.regions) {
				if (p.file == r.file && p.fileRegion == r.fileRegion && p.routingProfile == r.routingProfile) {
					match = true;
					break;
				}
			}
			if (!match) {
				allMatched = false;
				break;
			}
		}
		if (allMatched) {
			return currentCtx;
		}
		if (strictBestGroupMaps && groups.size() > 1) {
			hctx.rctx.mapIndexReaderFilter = new HashSet<>();
			for (HHRouteRegionPointsCtx<T> reg : regions) {
				hctx.rctx.mapIndexReaderFilter.add(reg.file);
			}
		}
		return initNewContext(hctx.rctx, regions);
	}

	public static <T extends NetworkDBPoint> TIntObjectHashMap<List<T>> groupByClusters(TLongObjectHashMap<T> pointsById, boolean out) {
		TIntObjectHashMap<List<T>> res = new TIntObjectHashMap<>();
		for (T p : pointsById.valueCollection()) {
			int cid = out ? p.clusterId : p.dualPoint.clusterId;
			if (!res.containsKey(cid)) {
				res.put(cid, new ArrayList<T>());
			}
			res.get(cid).add(p);
		}
		for (List<T> l : res.valueCollection()) {
			l.sort(new Comparator<NetworkDBPoint>() {

				@Override
				public int compare(NetworkDBPoint o1, NetworkDBPoint o2) {
					return Integer.compare(o1.index, o2.index);
				}
			});
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	private TLongObjectHashMap<T> initStart(HHRoutingContext<T> hctx, RouteSegmentPoint s, boolean reverse, TLongObjectHashMap<T> pnts) throws IOException, InterruptedException {
		if (!hctx.config.ROUTE_LAST_MILE) {
			// simple method to calculate without detailed maps
			double startLat = MapUtils.get31LatitudeY(!reverse? hctx.startY : hctx.endY);
			double startLon = MapUtils.get31LongitudeX(!reverse? hctx.startX : hctx.endX);
			double rad = 10000;
			float spd = hctx.rctx.getRouter().getMinSpeed();
			while (rad < 300000 && pnts.isEmpty()) {
				rad = rad * 2;
				List<T> pntSelect = hctx.pointsRect.getClosestObjects(startLat, startLon, rad);
				// limit by cluster
				int cid = pntSelect.get(0).clusterId;
				for (T pSelect : pntSelect) {
					if (pSelect.clusterId != cid) {
						continue;
					}
					T pnt  = reverse ? (T) pSelect.dualPoint : pSelect;
					double cost = MapUtils.getDistance(pnt.getPoint(), startLat, startLon) / spd;
					pnt.setCostParentRt(reverse, cost + hctx.distanceToEnd(reverse, pnt), null, cost);
					pnts.put(pnt.index, pnt);
				}
			}
			return pnts;
		}
		if (s == null) {
			return pnts;
		}
		T finitePnt = hctx.pointsByGeo.get(calcUniDirRoutePointInternalId(s));
		if (finitePnt != null) {
			// start / end point is directly on a network point
			double plusCost = 0, negCost = 0;
			if (hctx.rctx.config.initialDirection != null) {
				double diff = s.getRoad().directionRoute(s.getSegmentStart(), s.isPositive()) - hctx.rctx.config.initialDirection;
				if (Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
					plusCost += hctx.rctx.config.penaltyForReverseDirection;
				}
				diff = s.getRoad().directionRoute(s.getSegmentEnd(), !s.isPositive()) - hctx.rctx.config.initialDirection;
				if (Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
					negCost += hctx.rctx.config.penaltyForReverseDirection;
				}
			}
			finitePnt.setDistanceToEnd(reverse, hctx.distanceToEnd(reverse, finitePnt));
			finitePnt.setCostParentRt(reverse, plusCost, null, plusCost);
			pnts.put(finitePnt.index, finitePnt);

			T dualPoint = (T) finitePnt.dualPoint;
			dualPoint.setDistanceToEnd(reverse, hctx.distanceToEnd(reverse, dualPoint));
			dualPoint.setCostParentRt(reverse, negCost, null, negCost);
			pnts.put(dualPoint.index, dualPoint);
			return pnts;
		}
		int savedMaxVisited = hctx.rctx.config.MAX_VISITED;
		int savedPlanRoadDirectrion = hctx.rctx.config.planRoadDirection;
		float savedHeuristicCoefficient = hctx.rctx.config.heuristicCoefficient;
		hctx.rctx.config.MAX_VISITED = MAX_POINTS_CLUSTER_ROUTING;
		hctx.rctx.config.planRoadDirection = reverse ? -1 : 1;
		hctx.rctx.config.heuristicCoefficient = 0; // dijkstra
		hctx.rctx.unloadAllData(); // needed for proper multidijsktra work
		// hctx.rctx.calculationProgress = new RouteCalculationProgress(); // reuse same progress
		BinaryRoutePlanner planner = new BinaryRoutePlanner();
		MultiFinalRouteSegment frs = (MultiFinalRouteSegment) planner.searchRouteInternal(hctx.rctx,
				reverse ? null : s, reverse ? s : null, hctx.boundaries);
		hctx.rctx.config.heuristicCoefficient = savedHeuristicCoefficient;
		hctx.rctx.config.planRoadDirection = savedPlanRoadDirectrion;
		hctx.rctx.config.MAX_VISITED = savedMaxVisited;
		if (HHRoutingConfig.STATS_VERBOSE_LEVEL > 0) {
			System.out.println("  " + hctx.rctx.calculationProgress.getInfo(null));
		}
		if (frs != null) {
			TLongSet set = new TLongHashSet();
			for (FinalRouteSegment o : frs.all) {
				// duplicates are possible as alternative routes
				int startSegment = reverse ? o.getSegmentEnd() : o.getSegmentStart();
				int endSegment = reverse ? o.getSegmentStart() : o.getSegmentEnd();
				long pntId = calculateRoutePointInternalId(o.getRoad().getId(), startSegment, endSegment);
				if (set.add(pntId)) {
					T pnt = hctx.pointsByGeo.get(pntId);
					if (pnt == null) {
						if (pnts.contains(PNT_SHORT_ROUTE_START_END)) {
							continue;
						}
						try {
							pnt = pointClass.getDeclaredConstructor().newInstance();
						} catch (Exception e) {
							throw new IllegalStateException(e);
						}
						pnt.index = PNT_SHORT_ROUTE_START_END;
						pnt.roadId = o.getRoad().getId();
						pnt.start = o.getSegmentStart();
						pnt.end = o.getSegmentEnd();
						pnt.startX = o.getStartPointX();
						pnt.endX = o.getEndPointX();
						pnt.startY = o.getStartPointY();
						pnt.endY = o.getEndPointY();
						int preciseY = reverse? hctx.startY : hctx.endY;
						int preciseX = reverse? hctx.startX : hctx.endX;
						o.distanceFromStart += planner.calculatePreciseStartTime(hctx.rctx, preciseX, preciseY, o);
					} else {
						float obstacle = hctx.rctx.getRouter().defineRoutingObstacle(
								o.getRoad(), o.getSegmentStart(), o.getSegmentStart() > o.getSegmentEnd());
						if (obstacle < 0) {
							continue;
						}
						o.distanceFromStart += planner.calcRoutingSegmentTimeOnlyDist(hctx.rctx.getRouter(), o) / 2 + obstacle;
					}
					if (pnt.rt(reverse).rtCost != 0) {
						throw new IllegalStateException();
					}
					pnt.setDistanceToEnd(reverse, hctx.distanceToEnd(reverse, pnt));
					pnt.setDetailedParentRt(reverse, o);
					pnts.put(pnt.index, pnt);
				}
			}

		}
		if (hctx.config.USE_GC_MORE_OFTEN) {
			hctx.rctx.unloadAllData();
			printGCInformation(true);
		}
		return pnts;
	}

	
	protected T runRoutingPointToPoint(HHRoutingContext<T> hctx, T start, T end) throws SQLException, IOException {
		if (start != null) {
			addPointToQueue(hctx, hctx.queue(false), false, start, null, 0, MINIMAL_COST);
		}
		if (end != null) {
			addPointToQueue(hctx, hctx.queue(true), true, end, null, 0, MINIMAL_COST);
		}
		return runRoutingWithInitQueue(hctx);

	}

	protected T runRoutingPointsToPoints(HHRoutingContext<T> hctx, TLongObjectHashMap<T> stPoints,
	                                     TLongObjectHashMap<T> endPoints) throws SQLException, IOException {
		for (T start : stPoints.valueCollection()) {
			if (start.rtExclude) {
				continue;
			}
			double cost = start.rt(false).rtCost;
			addPointToQueue(hctx, hctx.queue(false), false, start, null, start.rt(false).rtDistanceFromStart,
					cost <= 0 ? MINIMAL_COST : cost);
		}
		for (T end : endPoints.valueCollection()) {
			if (end.rtExclude) {
				continue;
			}
			double cost = end.rt(true).rtCost;
			addPointToQueue(hctx, hctx.queue(true), true, end, null, end.rt(true).rtDistanceFromStart,
					cost <= 0 ? MINIMAL_COST : cost);
		}
		T t = runRoutingWithInitQueue(hctx);
//		int i = 0;
//		for(T p : hctx.pointsById.valueCollection()) {
//			if (p.rtPos == null && p.dualPoint != null && p.dualPoint.rtPos == null) {
//				System.out.println(i++ + "  " + p);
//			}
//		}
		return t;
	}
	
	private T runRoutingWithInitQueue(HHRoutingContext<T> hctx) throws SQLException, IOException {
		float DIR_CONFIG = hctx.config.DIJKSTRA_DIRECTION;
		RouteCalculationProgress progress = hctx.rctx == null ? null : hctx.rctx.calculationProgress;
		double straightStartEndCost = squareRootDist31(hctx.startX, hctx.startY, hctx.endX, hctx.endY) /
				hctx.rctx.getRouter().getMaxSpeed();
		while (true) {
			Queue<NetworkDBPointCost<T>> queue;
			if (HHRoutingContext.USE_GLOBAL_QUEUE) {
				queue = hctx.queue(false);
				if (queue.isEmpty()) {
					break;
				}
			} else {
				Queue<NetworkDBPointCost<T>> pos = hctx.queue(false);
				Queue<NetworkDBPointCost<T>> rev = hctx.queue(true);
				if (hctx.config.DIJKSTRA_DIRECTION == 0 || (!rev.isEmpty() && !pos.isEmpty())) {
					if (rev.isEmpty() || pos.isEmpty()) {
						break;
					}
					queue = pos.peek().cost < rev.peek().cost ? pos : rev;
				} else {
					queue = hctx.config.DIJKSTRA_DIRECTION > 0 ? pos : rev;
					if (queue.isEmpty()) {
						break;
					}
				}
			}
			if (progress != null && progress.isCancelled) {
				return null;
			}
			long tm = System.nanoTime();
			NetworkDBPointCost<T> pointCost = queue.poll();
			T point = pointCost.point;
			boolean rev = pointCost.rev;
			hctx.stats.pollQueueTime += (System.nanoTime() - tm) / 1e6;
			hctx.stats.visitedVertices++;
			if (point.rt(!rev).rtVisited) {
				if (hctx.stats.firstRouteVisitedVertices == 0) {
					hctx.stats.firstRouteVisitedVertices = hctx.stats.visitedVertices;
					if (DIR_CONFIG == 0 && hctx.config.HEURISTIC_COEFFICIENT != 0) {
						// focus on 1 direction as it slightly faster
						DIR_CONFIG = rev ? -1 : 1;
					}
				}
				if (hctx.config.HEURISTIC_COEFFICIENT == 0 && hctx.config.DIJKSTRA_DIRECTION == 0) {
					// Valid only HC=0, Dijkstra as we run Many-to-Many - Test( Lat 49.12691 Lon 9.213685 -> Lat 49.155483 Lon 9.2140045)
					T finalPoint = point;
					finalPoint = scanFinalPoint(finalPoint, hctx.visited);
					finalPoint = scanFinalPoint(finalPoint, hctx.visitedRev);
					return finalPoint;
				} else {
					double rcost = point.rt(true).rtDistanceFromStart + point.rt(false).rtDistanceFromStart;
					if (rcost <= pointCost.cost) {
						// Universal condition to stop: works for any algorithm - cost equals to route length
						return point;
					} else {
						queue.add(new NetworkDBPointCost<T>(point, rcost, rev));
						point.markVisited(rev);
						continue;
					}
				}
			}
			if (point.rt(rev).rtVisited) {
				continue;
			}
			hctx.stats.uniqueVisitedVertices++;
			point.markVisited(rev);
			hctx.visited.add(point);
			(rev ? hctx.visited : hctx.visitedRev).add(point);
			printPoint(point, rev);
			if (progress != null && straightStartEndCost > 0) {
				final double STRAIGHT_TO_ROUTE_COST = 1.25; // approximate, tested on car/bike
				// correlation between straight-cost and route-cost (enough for the progress bar)
				double k = (pointCost.cost - straightStartEndCost) / straightStartEndCost * STRAIGHT_TO_ROUTE_COST;
				progress.hhIterationProgress(k);
			}
			if (hctx.config.MAX_COST > 0 && pointCost.cost > hctx.config.MAX_COST) {
				break;
			}
			if (hctx.config.MAX_SETTLE_POINTS > 0 && (rev ? hctx.visitedRev : hctx.visited).size() > hctx.config.MAX_SETTLE_POINTS) {
				break;
			}
			
			boolean directionAllowed = (DIR_CONFIG <= 0 && rev) || (DIR_CONFIG >= 0 && !rev);
			if (directionAllowed) {
				addConnectedToQueue(hctx, queue, point, rev);
			}
		}			
		return null;
	}

	private T scanFinalPoint(T finalPoint, List<T> lt) {
		for (T p : lt) {
			if (p.rt(true).rtDistanceFromStart == 0 || p.rt(false).rtDistanceFromStart == 0) {
				continue;
			}
			if (p.rt(true).rtDistanceFromStart + p.rt(false).rtDistanceFromStart 
					< finalPoint.rt(true).rtDistanceFromStart + finalPoint.rt(false).rtDistanceFromStart) {
				finalPoint = p;
			}
		}
		return finalPoint;
	}
	
	@SuppressWarnings("unchecked")
	private void addConnectedToQueue(HHRoutingContext<T> hctx, Queue<NetworkDBPointCost<T>> queue, T point, boolean reverse) throws SQLException, IOException {
		int depth = hctx.config.USE_MIDPOINT || hctx.config.MAX_DEPTH > 0 ? point.rt(reverse).getDepth(reverse) : 0;
		if (hctx.config.MAX_DEPTH > 0 && depth >= hctx.config.MAX_DEPTH) {
			return;
		}
		long tm = System.nanoTime();
		int cnt = hctx.loadNetworkSegmentPoint(point, reverse);
		hctx.stats.loadEdgesCnt += cnt;
		hctx.stats.loadEdgesTime += (System.nanoTime() - tm) / 1e6;
		for (NetworkDBSegment connected : point.connected(reverse)) {
			T nextPoint = (T) (reverse ? connected.start : connected.end);
			if (!hctx.config.USE_CH && !hctx.config.USE_CH_SHORTCUTS && connected.shortcut) {
				continue;
			}
			if (nextPoint.rtExclude) {
				continue;
			}
			// modify CH to not compute all top points
			if (hctx.config.USE_CH && (nextPoint.chInd() > 0 && nextPoint.chInd() < point.chInd())) {
				continue;
			}
			if (hctx.config.USE_MIDPOINT && Math.min(depth, hctx.config.MIDPOINT_MAX_DEPTH) > nextPoint.midPntDepth() + hctx.config.MIDPOINT_ERROR) {
				continue;
			}
			if (connected.dist < 0) {
				// disabled segment
				continue;
			}
			if (ASSERT_AND_CORRECT_DIST_SMALLER && hctx.config.HEURISTIC_COEFFICIENT > 0
					&& smallestSegmentCost(hctx, point, nextPoint) - connected.dist >  1) {
				double smallestSegmentCost = smallestSegmentCost(hctx, point, nextPoint);
				System.err.printf("Incorrect distance %s -> %s: db = %.2f > fastest %.2f \n", point, nextPoint, connected.dist, smallestSegmentCost);
				connected.dist = smallestSegmentCost;
			}
			double cost = point.rt(reverse).rtDistanceFromStart  + connected.dist + hctx.distanceToEnd(reverse, nextPoint);
			if (ASSERT_COST_INCREASING && point.rt(reverse).rtCost - cost > 1) {
				String msg = String.format("%s (cost %.2f) -> %s (cost %.2f) st=%.2f-> + %.2f, toend=%.2f->%.2f: ",
						point, point.rt(reverse).rtCost, nextPoint, cost, point.rt(reverse).rtDistanceFromStart,
						connected.dist, point.rt(reverse).rtDistanceToEnd, hctx.distanceToEnd(reverse, nextPoint));
				throw new IllegalStateException(msg);
			}
			double exCost = nextPoint.rt(reverse).rtCost;
			if ((exCost == 0 && !nextPoint.rt(reverse).rtVisited) || cost < exCost) {
				addPointToQueue(hctx, queue, reverse, nextPoint, point, connected.dist, cost);
			}
		}
	}

	private void addPointToQueue(HHRoutingContext<T> hctx, Queue<NetworkDBPointCost<T>> queue,
			boolean reverse, T point, T parent, double segmentDist, double cost) {
		long tm = System.nanoTime();
		if (DEBUG_VERBOSE_LEVEL > 2) {
			System.out.printf("Add  %s to visit - cost %.2f (%.2f prev, %.2f dist) > prev cost %.2f \n", point, 
					cost, parent == null ? 0 : parent.rt(reverse).rtDistanceFromStart, segmentDist, point.rt(reverse).rtCost);
		}
		if (point.rt(reverse).rtVisited) {
			throw new IllegalStateException(String.format("%s visited - cost %.2f > prev cost %.2f", point, cost, 
					point.rt(reverse).rtCost));
		}
		point.setCostParentRt(reverse, cost, parent, segmentDist);
		hctx.queueAdded.add(point);
		queue.add(new NetworkDBPointCost<T>(point, cost, reverse)); // we need to add new object to not  remove / rebalance priority queue
		hctx.stats.addQueueTime += (System.nanoTime() - tm) / 1e6;
		hctx.stats.addedVertices++;
	}
	
	
	private double smallestSegmentCost(HHRoutingContext<T> hctx, T st, T end) {
		double dist = squareRootDist31(st.midX(), st.midY(), end.midX(), end.midY());
		return dist / hctx.rctx.getRouter().getMaxSpeed();
	}

		
	private void printPoint(T p, boolean rev) {
		if (DEBUG_VERBOSE_LEVEL > 1) {
			int pind = 0; long pchInd = 0;
			if (p.rt(rev).rtRouteToPoint != null) {
				pind = p.rt(rev).rtRouteToPoint.index;
				pchInd = p.rt(rev).rtRouteToPoint.chInd();
			}
			String symbol = String.format("%s %d [%d] (from %d [%d])", rev ? "<-" : "->", p.index, p.chInd(), pind, pchInd);
			System.out.printf("Visit Point %s (cost %.1f s) %.5f/%.5f - %d\n", symbol, p.rt(rev).rtCost,
					MapUtils.get31LatitudeY(p.startY), MapUtils.get31LongitudeX(p.startX), p.roadId / 64);
		}
	}
	

	
	private FinalRouteSegment runDetailedRouting(HHRoutingContext<T> hctx, NetworkDBPoint startS, NetworkDBPoint endS, boolean useBoundaries)
			throws InterruptedException, IOException {
		BinaryRoutePlanner planner = new BinaryRoutePlanner();
		hctx.rctx.config.planRoadDirection = 0; // A* bidirectional
		hctx.rctx.config.heuristicCoefficient = 1;
		// SPEEDUP: Speed up by just clearing visited
		hctx.rctx.unloadAllData(); // needed for proper multidijsktra work
		// if (c.USE_GC_MORE_OFTEN) {
		// printGCInformation();
		// }
		RouteSegmentPoint start = loadPoint(hctx.rctx, startS);
		RouteSegmentPoint end = loadPoint(hctx.rctx, endS);
		if (start == null) {
			return null; // no logging it's same as end of previos segment
		} else if (end == null) {
			System.out.println(String.format("End point is not present in detailed maps: %s", endS));
			return null;
		}
		double oldP = hctx.rctx.config.penaltyForReverseDirection;
		hctx.rctx.config.penaltyForReverseDirection *= 4; // probably we should try -1 (to fully avoid roundabout) but we don't have use cases yet
		hctx.rctx.config.initialDirection = start.getRoad().directionRoute(start.getSegmentStart(), start.isPositive());
		hctx.rctx.config.targetDirection = end.getRoad().directionRoute(end.getSegmentEnd(), !end.isPositive());
		hctx.rctx.config.MAX_VISITED = useBoundaries ? -1 : MAX_POINTS_CLUSTER_ROUTING * 2;
		// boundaries help to reduce max visited (helpful for long ferries)
		TLongObjectMap<RouteSegment> bounds = null;
		if (useBoundaries) {
			long ps = calcRPId(start, start.getSegmentEnd(), start.getSegmentStart());
			long pe = calcRPId(end, end.getSegmentStart(), end.getSegmentEnd());
			bounds = new ExcludeTLongObjectMap<>(hctx.boundaries, ps, pe);
		}
		FinalRouteSegment f = planner.searchRouteInternal(hctx.rctx, start, end, bounds);
		if (f == null) {
			System.out.printf("No route found between %s -> %s \n", start, end);
		}
		hctx.rctx.config.MAX_VISITED = -1;
		// clean up
		hctx.rctx.config.initialDirection = null;
		hctx.rctx.config.targetDirection = null;
		hctx.rctx.config.penaltyForReverseDirection = oldP;
		return f;
		
	}
	
	private boolean retrieveSegmentsGeometry(HHRoutingContext<T> hctx, RouteResultPreparation rrp, HHNetworkRouteRes route,
			boolean routeSegments, RouteCalculationProgress progress) throws SQLException, InterruptedException, IOException {
		for (int i = 0; i < route.segments.size(); i++) {
			progress.hhIterationProgress((double) i / route.segments.size());

			HHNetworkSegmentRes s = route.segments.get(i);
			if (s.segment == null) {
				// start / end points
				if(i > 0 && i < route.segments.size() -1 ) {
					throw new IllegalStateException(String.format("Segment ind %d is null.", i));
				}
				continue;
			}
			
			if (routeSegments) {
				if (progress.isCancelled) {
					return false;
				}
				FinalRouteSegment f = runDetailedRouting(hctx, s.segment.start, s.segment.end, true);
				if (f == null) {
					boolean full = hctx.config.FULL_DIJKSTRA_NETWORK_RECALC-- > 0;
					System.out.printf("Route not found (%srecalc) %s -> %s\n", full ? "dijkstra+" : "",s.segment.start, s.segment.end);
					if (full) {
						recalculateNetworkCluster(hctx, s.segment.start);
					}
					s.segment.dist = -1;
					return true;
				}
				if ((f.distanceFromStart + MAX_INC_COST_CORR) > (s.segment.dist + MAX_INC_COST_CORR) * hctx.config.MAX_INC_COST_CF) {
					if (DEBUG_VERBOSE_LEVEL > 0) {
						System.out.printf("Route cost increased (%.2f > %.2f) between %s -> %s: recalculate route\n",
								f.distanceFromStart, s.segment.dist, s.segment.start, s.segment.end);
					}
					s.segment.dist = f.distanceFromStart;
					return true;
				}
				s.rtTimeDetailed = f.distanceFromStart;
				s.list = rrp.convertFinalSegmentToResults(hctx.rctx, f);
			} else {
				// load segment geometry from db
				if (!hctx.loadGeometry(s.segment, false)) {
					s.segment.getGeometry().clear();
					s.segment.getGeometry().add(s.segment.start.getPoint());
					s.segment.getGeometry().add(s.segment.end.getPoint());
				}
			}
		}
		return false;
	}
	
	private void recalculateNetworkCluster(HHRoutingContext<T> hctx, NetworkDBPoint start) throws InterruptedException, IOException {
		BinaryRoutePlanner plan = new BinaryRoutePlanner();
		hctx.rctx.config.planRoadDirection = 1; 
		hctx.rctx.config.heuristicCoefficient = 0;
		// SPEEDUP: Speed up by just clearing visited
		hctx.rctx.unloadAllData(); // needed for proper multidijsktra work
		RouteSegmentPoint s = loadPoint(hctx.rctx, start);
		if (s == null) {
			System.err.printf("HH recalculateNetworkCluster: loadPoint() is NULL for %s\n", start);
			return;
		}
		// hctx.rctx.calculationProgress = new RouteCalculationProgress(); // we should reuse same progress for cancellation
		hctx.rctx.config.MAX_VISITED = MAX_POINTS_CLUSTER_ROUTING * 2;
		long ps = calcRPId(s, s.getSegmentStart(), s.getSegmentEnd());
		long ps2 = calcRPId(s, s.getSegmentEnd(), s.getSegmentStart());
		ExcludeTLongObjectMap<RouteSegment> bounds = new ExcludeTLongObjectMap<>(hctx.boundaries, ps, ps2);
		MultiFinalRouteSegment frs = (MultiFinalRouteSegment) plan.searchRouteInternal(hctx.rctx, s, null, bounds);
		hctx.rctx.config.MAX_VISITED = -1;
		TLongObjectHashMap<RouteSegment> resUnique = new TLongObjectHashMap<>();
		if (frs != null) {
			for (FinalRouteSegment o : frs.all) {
				long pntId = calculateRoutePointInternalId(o.getRoad().getId(), o.getSegmentStart(), o.getSegmentEnd());
				if (resUnique.containsKey(pntId)) {
					if (resUnique.get(pntId).getDistanceFromStart() > o.getDistanceFromStart()) {
						System.err.println(resUnique.get(pntId) + " > " + o + " - " + s);
					}
				} else {
					resUnique.put(pntId, o);
					NetworkDBPoint p = hctx.pointsByGeo.get(calcRPId(o, o.getSegmentStart(), o.getSegmentEnd()));
					if (p == null) {
						System.err.println("Error calculations new final boundary not found");
						continue;
					}
					p.startX = o.getStartPointX();
					p.startY = o.getStartPointY();
					p.endX = o.getEndPointX();
					p.endY = o.getEndPointY();
					float routeTime = o.getDistanceFromStart()
							+ plan.calcRoutingSegmentTimeOnlyDist(hctx.rctx.getRouter(), o) / 2 + 1;
					NetworkDBSegment c = start.getSegment(p, true);
					if (c != null) {
						// System.out.printf("Corrected dist %.2f -> %.2f\n", c.dist, routeTime);
						c.dist = routeTime;
					} else {
						start.connected.add(new NetworkDBSegment(start, p, routeTime, true, false));
					}
					NetworkDBSegment co = p.getSegment(start, false);
					if (co != null) {
						co.dist = routeTime;
					} else if (p.connectedReverse != null) {
						p.connectedReverse.add(new NetworkDBSegment(start, p, routeTime, false, false));
					}
				}
			}
		}
		for (NetworkDBSegment c : start.connected) {
			if (!resUnique.containsKey(c.end.getGeoPntId())) {
//				System.out.printf("Remove connection %s -> %s\n", start, c.end); // to debug later if all correct
				c.dist = -1; // disable as not found
				NetworkDBSegment co = c.end.getSegment(start, false);
				if (co != null) {
					co.dist = -1;
				}
			}
		}
	}

	private HHNetworkRouteRes prepareRouteResults(HHRoutingContext<T> hctx, HHNetworkRouteRes route, LatLon start, LatLon end, 
			RouteResultPreparation rrp) throws SQLException, InterruptedException, IOException {
		hctx.rctx.routingTime = 0;
		route.stats = hctx.stats;
		RouteSegmentResult straightLine = null;
		for(int routeSegmentInd = 0; routeSegmentInd < route.segments.size(); routeSegmentInd++ ) {
			HHNetworkSegmentRes routeSegment = route.segments.get(routeSegmentInd);
			NetworkDBSegment s = routeSegment.segment;
			if (routeSegment.list != null && routeSegment.list.size() > 0) {
				if (straightLine != null) {
					route.detailed.add(straightLine);
					straightLine = null;
				}
				if (routeSegmentInd > 0) {
					RouteSegmentResult p = routeSegment.list.get(0);
					if (Math.abs(p.getStartPointIndex() - p.getEndPointIndex()) <= 1) {
						routeSegment.list.remove(0);
					} else {
						p.setStartPointIndex(p.getStartPointIndex() + (p.isForwardDirection() ? +1 : -1));
					}
				}
				route.detailed.addAll(routeSegment.list);
			} else {
				RouteRegion reg = new RouteRegion();
				reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
				RouteDataObject rdo = new RouteDataObject(reg);
				rdo.types = new int[] { 0 };
				rdo.pointsX = new int[] { s.start.startX, s.end.startX };
				rdo.pointsY = new int[] { s.start.startY, s.end.startY };
				RouteDataObject sh = new RouteDataObject(reg);
				sh.types = new int[] { 0 };
				sh.pointsX = new int[] { s.end.startX, s.end.endX };
				sh.pointsY = new int[] { s.end.startY, s.end.endY };
				straightLine = new RouteSegmentResult(sh, 0, 1);
				route.detailed.add(new RouteSegmentResult(rdo, 0, 1));
			}
			hctx.rctx.routingTime += routeSegment.rtTimeDetailed;

			if (DEBUG_VERBOSE_LEVEL >= 1) {
				int segments = routeSegment.list == null ? 0 : routeSegment.list.size();
				if (s == null) {
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "First / last segment - %d segments, %.2fs \n", segments,
							routeSegment.rtTimeDetailed);
				} else {
					printf(HHRoutingConfig.STATS_VERBOSE_LEVEL > 0, "\nRoute %d [%d] -> %d [%d] %s - hh dist %.2f s, detail %.2f s (%.1f%%) segments %d ( end %.5f/%.5f - %d ) ",
							s.start.index, s.start.chInd(), s.end.index, s.end.chInd(), s.shortcut ? "sh" : "bs",
							s.dist, routeSegment.rtTimeDetailed, 100 * (1 - routeSegment.rtTimeDetailed / s.dist),
							segments, MapUtils.get31LatitudeY(s.end.startY), MapUtils.get31LongitudeX(s.end.startX),
							s.end.roadId / 64);
				}
			}
		}
		return route;
	}


	private HHNetworkRouteRes createRouteSegmentFromFinalPoint(HHRoutingContext<T> hctx, NetworkDBPoint pnt) {
		HHNetworkRouteRes route = new HHNetworkRouteRes();
		if (pnt != null) {
			NetworkDBPoint itPnt = pnt;
			route.uniquePoints.add(itPnt.index);
			while (itPnt.rt(true).rtRouteToPoint != null) {
				NetworkDBPoint nextPnt = itPnt.rt(true).rtRouteToPoint;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, false);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				res.rtTimeDetailed = res.rtTimeHHSegments = segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rt(true).rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, itPnt.rt(true).rtDetailedRoute);
				res.rtTimeDetailed = res.rtTimeHHSegments = itPnt.rt(true).rtDetailedRoute.distanceFromStart;
				route.segments.add(res);
			}
			Collections.reverse(route.segments);
			itPnt = pnt;
			while (itPnt.rt(false).rtRouteToPoint != null) {
				NetworkDBPoint nextPnt = itPnt.rt(false).rtRouteToPoint;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, true);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				res.rtTimeDetailed = res.rtTimeHHSegments = segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rt(false).rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, itPnt.rt(false).rtDetailedRoute);
				res.rtTimeDetailed = res.rtTimeHHSegments = itPnt.rt(false).rtDetailedRoute.distanceFromStart;
				route.segments.add(res);
			}
			Collections.reverse(route.segments);
		}
		return route;

	}

	/// Utilities
	static final int ROUTE_POINTS = 11;
	public static RouteSegmentPoint loadPoint(RoutingContext ctx, NetworkDBPoint pnt) {
		RouteSegment s = ctx.loadRouteSegment(pnt.startX, pnt.startY, ctx.config.memoryLimitation);
		while (s != null) {
			if (s.getRoad().getId() == pnt.roadId && s.getSegmentStart() == pnt.start) {
				if (s.getSegmentEnd() != pnt.end) {
					s = s.initRouteSegment(!s.isPositive());
				}
				break;
			}
			s = s.getNext();
		}
		if (s == null || s.getSegmentStart() != pnt.start || s.getSegmentEnd() != pnt.end || s.getRoad().getId() != pnt.roadId) {
//			throw new IllegalStateException("Error on segment " + pnt.roadId / 64);
			return null;
		}
		return new RouteSegmentPoint(s.getRoad(), s.getSegmentStart(), s.getSegmentEnd(), 0);
	}

	static long calculateRoutePointInternalId(final RouteDataObject road, int pntId, int nextPntId) {
		int positive = nextPntId - pntId;
		int pntLen = road.getPointsLength();
		if (positive < 0) {
			throw new IllegalStateException("Check only positive segments are in calculation");
		}
		if (pntId < 0 || nextPntId < 0 || pntId >= pntLen || nextPntId >= pntLen || (positive != -1 && positive != 1) ||
				pntLen > (1 << ROUTE_POINTS)) {
			// should be assert
			throw new IllegalStateException("Assert failed");
		}
		return (road.getId() << ROUTE_POINTS) + (pntId << 1) + (positive > 0 ? 1 : 0);
	}
	
	static long calculateRoutePointInternalId(long id, int pntId, int nextPntId) {
		int positive = nextPntId - pntId;
		return (id << ROUTE_POINTS) + (pntId << 1) + (positive > 0 ? 1 : 0);
	}
	
	static long calcRPId(RouteSegment p, int pntId, int nextPntId) {
		return calculateRoutePointInternalId(p.getRoad().getId(), pntId, nextPntId);
	}

	static long calcUniDirRoutePointInternalId(RouteSegment segm) {
		if (segm.getSegmentStart() < segm.getSegmentEnd()) {
			return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), segm.getSegmentEnd());
		} else {
			return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentEnd(), segm.getSegmentStart());
		}
	}
	
	public static void printGCInformation(boolean gc) {
		if (gc) {
			System.gc();
		}
		long MEMORY_LAST_USED_MB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
		System.out.printf("***** Memory used %d MB *****\n", MEMORY_LAST_USED_MB);		
	}
}
