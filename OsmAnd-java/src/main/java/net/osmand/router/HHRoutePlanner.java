package net.osmand.router;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.MultiFinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.HHRoutingDB.NetworkDBPoint;
import net.osmand.router.HHRoutingDB.NetworkDBSegment;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

public class HHRoutePlanner {
	static int DEBUG_VERBOSE_LEVEL = 0;
	static int DEBUG_ALT_ROUTE_SELECTION = -1;
	
	// TODO 1.8 HHRoutePlanner encapsulate HHRoutingPreparationDB, RoutingContext -> HHRoutingContext
	private HHRoutingDB networkDB;
	private RoutingContext ctx;
	private HHRoutingContext cacheHctx;
	private int routingProfile = 0;
	
	public HHRoutePlanner(RoutingContext ctx, HHRoutingDB networkDB) {
		this.ctx = ctx;
		this.networkDB = networkDB;
	}
	
	public void close() throws SQLException {
		networkDB.close();
	}
	
	public static class HHRoutingConfig {
		float HEURISTIC_COEFFICIENT = 0; // A* - 1, Dijkstra - 0
		float DIJKSTRA_DIRECTION = 0; // 0 - 2 directions, 1 - positive, -1 - reverse
		
		boolean ROUTE_LAST_MILE = false;
		boolean ROUTE_ALL_SEGMENTS = false;
		boolean ROUTE_ALL_ALT_SEGMENTS = false;
		boolean PRELOAD_SEGMENTS = false;
		
		boolean CALC_ALTERNATIVES = false;
		boolean USE_GC_MORE_OFTEN = false;
		// TODO 3.1 HHRoutePlanner Alternative routes - could use distributions like 50% route (2 alt), 25%/75% route (1 alt)
		double ALT_EXCLUDE_RAD_MULT = 0.3; // radius multiplier to exclude points
		double ALT_EXCLUDE_RAD_MULT_IN = 3; // skip some points to speed up calculation
		double ALT_NON_UNIQUENESS = 0.7; // 0.7 - 30% of points must be unique

		
		double MAX_COST;
		int MAX_DEPTH = -1; // max depth to go to
		int MAX_SETTLE_POINTS = -1; // max points to settle
		
		boolean USE_CH;
		boolean USE_CH_SHORTCUTS;

		boolean USE_MIDPOINT;
		int MIDPOINT_ERROR = 3;
		int MIDPOINT_MAX_DEPTH = 20 + MIDPOINT_ERROR;
		
		
		public static HHRoutingConfig dijkstra(int direction) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static HHRoutingConfig astar(int direction) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 1;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static HHRoutingConfig ch() {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.USE_CH = true;
			df.USE_CH_SHORTCUTS = true;
			df.DIJKSTRA_DIRECTION = 0;
			return df;
		}
		
		public static HHRoutingConfig midPoints(boolean astar, int dir) {
			HHRoutingConfig df = new HHRoutingConfig();
			df.HEURISTIC_COEFFICIENT = astar ? 1 : 0;
			df.USE_MIDPOINT = true;
			df.DIJKSTRA_DIRECTION = dir;
			return df;
		}
		
		public HHRoutingConfig preloadSegments() {
			this.PRELOAD_SEGMENTS = true;
			return this;
		}
		
		public HHRoutingConfig calcAlternative() {
			this.CALC_ALTERNATIVES = true;
			return this;
		}
		
		public HHRoutingConfig calcDetailed(int segments) {
			this.ROUTE_LAST_MILE = true;
			this.ROUTE_ALL_SEGMENTS = segments >= 1;
			this.ROUTE_ALL_ALT_SEGMENTS = segments >= 2;
			return this;
		}

		
		public HHRoutingConfig useShortcuts() {
			USE_CH_SHORTCUTS = true;
			return this;
		}
		
		public HHRoutingConfig gc() {
			USE_GC_MORE_OFTEN = true;
			return this;
		}
		
		public HHRoutingConfig maxCost(double cost) {
			MAX_COST = cost;
			return this;
		}
		
		public HHRoutingConfig maxDepth(int depth) {
			MAX_DEPTH = depth;
			return this;
		}
		
		public HHRoutingConfig maxSettlePoints(int maxPoints) {
			MAX_SETTLE_POINTS = maxPoints;
			return this;
		}

		@Override
		public String toString() {
			return toString(null, null);
		}
		
		public String toString(LatLon start, LatLon end) {
			return String.format("Routing %s -> %s (HC %d, dir %d)", start == null ? "?" : start.toString(),
					end == null ? "?" : end.toString(), (int) HEURISTIC_COEFFICIENT, (int) DIJKSTRA_DIRECTION);
		}
	}
	
	public static class HHRoutingContext {
		TLongObjectHashMap<NetworkDBPoint> pointsById;
		TLongObjectHashMap<NetworkDBPoint> pointsByGeo;
		TIntObjectHashMap<List<NetworkDBPoint>> clusterInPoints;
		TIntObjectHashMap<List<NetworkDBPoint>> clusterOutPoints;

		DataTileManager<NetworkDBPoint> pointsRect = new DataTileManager<>(11); // 20km tile
		TLongObjectHashMap<RouteSegment> boundaries; 
		
		List<NetworkDBPoint> queueAdded = new ArrayList<>();
		List<NetworkDBPoint> visited = new ArrayList<>();
		List<NetworkDBPoint> visitedRev = new ArrayList<>();
		Queue<NetworkDBPointCost> queue = new PriorityQueue<>(new Comparator<NetworkDBPointCost>() {
			@Override
			public int compare(NetworkDBPointCost o1, NetworkDBPointCost o2) {
				return Double.compare(o1.cost, o2.cost);
			}
		});
		
		public void clearVisited() {
			queue.clear();
			for (NetworkDBPoint p : queueAdded) {
				p.clearRouting();
			}
			queueAdded.clear();
			visited.clear();
			visitedRev.clear();
		}

		public void clearVisited(TLongObjectHashMap<NetworkDBPoint> stPoints, TLongObjectHashMap<NetworkDBPoint> endPoints) {
			queue.clear();
			Iterator<NetworkDBPoint> it = queueAdded.iterator();
			while (it.hasNext()) {
				NetworkDBPoint p = it.next();
				if (stPoints.containsKey(p.index)) {
					p.setDetailedParentRt(false, p.rtDetailedRoute);
				} else if (endPoints.containsKey(p.index)) {
					p.setDetailedParentRt(true, p.rtDetailedRouteRev);
				} else {
					p.clearRouting();
				}
				it.remove();
			}
			visited.clear();
			visitedRev.clear();
		}

		public void unloadAllConnections() {
			for (NetworkDBPoint p : pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
		}
		
	}
	
	static class RoutingStats {
		int visitedVertices = 0;
		int uniqueVisitedVertices = 0;
		int addedVertices = 0;

		double loadPointsTime = 0;
		int loadEdgesCnt;
		double loadEdgesTime = 0;
		double altRoutingTime;
		double routingTime = 0;
		double searchPointsTime = 0;
		double addQueueTime = 0;
		double pollQueueTime = 0;
		double prepTime = 0;
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

	public HHNetworkRouteRes runRouting(LatLon start, LatLon end, HHRoutingConfig c) throws SQLException, IOException, InterruptedException {
		RoutingStats stats = new RoutingStats();
		long startTime = System.nanoTime();
		if (c == null) {
			c = new HHRoutingConfig();
			
			// test data for debug swap
//			c = HHRoutingConfig.dijkstra(1); 
			c = HHRoutingConfig.astar(1);
//			c = HHRoutingConfig.ch();
//			c.preloadSegments();
//			c.ROUTE_LAST_MILE = false;
			c.calcDetailed(2);
			c.calcAlternative();
			c.gc();
			DEBUG_VERBOSE_LEVEL = 0;
//			DEBUG_ALT_ROUTE_SELECTION++;
			c.ALT_EXCLUDE_RAD_MULT_IN = 1;
			c.ALT_EXCLUDE_RAD_MULT = 0.05;
//			routingProfile = (routingProfile + 1) % networkDB.getRoutingProfiles().size();
			System.out.println("Routing profile: " + networkDB.getRoutingProfiles().get(routingProfile));
		}
		System.out.println(c.toString(start, end));
		HHRoutingContext hctx = this.cacheHctx;
		if (networkDB.getRoutingProfile() != routingProfile) {
			networkDB.selectRoutingProfile(routingProfile);
			hctx = null;
		}
		if (hctx == null) {
			hctx = initHCtx(c, stats);
			cacheHctx = hctx;
		}
		hctx.clearVisited();
		if (c.USE_GC_MORE_OFTEN) {
			printGCInformation();
		}
		long time = System.nanoTime();
		System.out.printf("Looking for route %s -> %s \n", start, end);
		System.out.println("Finding first / last segments...");
		TLongObjectHashMap<NetworkDBPoint> stPoints = initStart(c, hctx, start, end, false);
		TLongObjectHashMap<NetworkDBPoint> endPoints = initStart(c, hctx, end, start, true);
		stats.searchPointsTime = (System.nanoTime() - time) / 1e6;
		System.out.printf("Finding first / last segments...%.2f ms\n", stats.searchPointsTime);

		time = System.nanoTime();
		System.out.printf("Routing...");
		NetworkDBPoint finalPnt = runDijkstraNetworkRouting(stPoints, endPoints, start, end, c, hctx, stats);
		HHNetworkRouteRes route = createRoute(finalPnt);
		stats.routingTime = (System.nanoTime() - time) / 1e6;
		
		System.out.printf("%d segments, cost %.2f, %.2f ms\n", route.segments.size(),
				route.routingTimeSegments, stats.routingTime);
		
		if (c.CALC_ALTERNATIVES) {
			System.out.printf("Alternative routes...");
			time = System.nanoTime();
			calcAlternativeRoute(route, stPoints, endPoints, start, end, c, hctx, stats);
			stats.altRoutingTime = (System.nanoTime() - time) / 1e6;
			stats.routingTime += stats.altRoutingTime;
			System.out.printf("%d %.2f ms\n", route.altRoutes.size(), stats.altRoutingTime);
		}

		if (c.USE_GC_MORE_OFTEN) {
			hctx.unloadAllConnections();
			printGCInformation();
		}
		
		System.out.printf("Prepare detailed route segments...");		
		time = System.nanoTime();
		prepareDetailedRoutingResults(networkDB,c, route, stats);
		route.stats = stats;
		stats.prepTime = (System.nanoTime() - time) / 1e6;
		System.out.printf("%.2f ms\n", stats.prepTime);
		
		System.out.println(String.format("Found final route - cost %.2f (detailed %.2f), %d depth ( visited %,d (%,d unique) of %,d added vertices )", 
				route.routingTimeSegments, route.routingTimeDetailed,
				route.segments.size(), stats.visitedVertices, stats.uniqueVisitedVertices, stats.addedVertices));
		
		time = System.nanoTime();
		System.out.println(c.toString(start, end));
		System.out.printf("Calculate turns...");
		if (c.ROUTE_ALL_SEGMENTS && route.detailed != null) {
			route.detailed = new RouteResultPreparation().prepareResult(ctx, route.detailed);
		}
		System.out.printf("%.2f ms\n", (System.nanoTime() - time) / 1e6);
//			RouteResultPreparation.printResults(ctx, start, end, route.detailed);
		
		System.out.printf("Routing finished all %.1f ms: last mile %.1f ms, load data %.1f ms (%,d edges), routing %.1f ms (queue  - %.1f add ms + %.1f poll ms), prep result %.1f ms\n",
				(System.nanoTime() - startTime) / 1e6, stats.searchPointsTime,
				stats.loadEdgesTime + stats.loadPointsTime, stats.loadEdgesCnt, stats.routingTime, stats.addQueueTime,
				stats.pollQueueTime, stats.prepTime);
		printGCInformation();
		return route;
	}

	private void calcAlternativeRoute(HHNetworkRouteRes route, TLongObjectHashMap<NetworkDBPoint> stPoints,
			TLongObjectHashMap<NetworkDBPoint> endPoints, LatLon start, LatLon end, HHRoutingConfig c,
			HHRoutingContext hctx, RoutingStats stats) throws SQLException {
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
			LatLon prev = null;
			for (int i = 0; i < distances.length; i++) {
				LatLon pnt = points.get(i).getPoint();
				if (i == 0) {
					distances[i] = MapUtils.getDistance(start, pnt);
					prev = pnt;
				} else if (i == distances.length - 1) {
					LatLon last = points.get(i).getPoint();
					distances[i] = MapUtils.getDistance(last, end);
				} else {
					distances[i] = MapUtils.getDistance(prev, pnt);
					prev = pnt;
				}
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
				minDistance[i] = Math.min(cdistNeg[i], cdistPos[i]) * c.ALT_EXCLUDE_RAD_MULT;
				boolean coveredByPrevious = false;
				for (int j = 0; j < i; j++) {
					if (useToSkip[j] && cdistPos[i] - cdistPos[j] < minDistance[j] * c.ALT_EXCLUDE_RAD_MULT_IN) {
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
				List<NetworkDBPoint> objs = hctx.pointsRect.getClosestObjects(pnt.getLatitude(), pnt.getLongitude(), minDistance[i]);
				for(NetworkDBPoint p : objs) {
					if(MapUtils.getDistance(p.getPoint(), pnt) <= minDistance[i]) {
						exclude.add(p);
						p.rtExclude = true;
					}
				}
				
				NetworkDBPoint finalPnt = runDijkstraNetworkRouting(stPoints, endPoints, start, end, c, hctx, stats);
				if (finalPnt != null) {
					double cost = (finalPnt.rtDistanceFromStart + finalPnt.rtDistanceFromStartRev);
					if (DEBUG_VERBOSE_LEVEL == 1) {
						System.out.println("Alternative route cost: " + cost);
					}
					rt = createRoute(finalPnt);
					rt.routingTimeSegments = cost;
					route.altRoutes.add(rt);
				} else {
					break;
				}
				
			}
			route.altRoutes.sort(new Comparator<HHNetworkRouteRes>() {

				@Override
				public int compare(HHNetworkRouteRes o1, HHNetworkRouteRes o2) {
					return Double.compare(o1.routingTimeSegments, o2.routingTimeSegments);
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
						if (cp.size() >= c.ALT_NON_UNIQUENESS * altR.uniquePoints.size()) {
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
					System.out.printf("Cost %.2f - %.2f [%d unique / %d]...", route.altRoutes.get(0).routingTimeSegments,
						route.altRoutes.get(route.altRoutes.size() - 1).routingTimeSegments, route.altRoutes.size(), size);
				}
				if (DEBUG_ALT_ROUTE_SELECTION >= 0) {
					HHNetworkRouteRes rts = route.altRoutes.get(DEBUG_ALT_ROUTE_SELECTION % route.altRoutes.size());
					System.out.printf(DEBUG_ALT_ROUTE_SELECTION + " select %.2f ", rts.routingTimeSegments);
					route.altRoutes = Collections.singletonList(rts);
				}
			}
		} finally {
			for (NetworkDBPoint pnt : exclude) {
				pnt.rtExclude = false;
			}
		}
				
	}

	protected HHRoutingContext initHCtx(HHRoutingConfig c, RoutingStats stats) throws SQLException {
		long time = System.nanoTime();
		HHRoutingContext hctx = new HHRoutingContext();
		System.out.print("Loading points... ");
		hctx.pointsById = networkDB.loadNetworkPoints();
		hctx.boundaries = new TLongObjectHashMap<RouteSegment>();
		hctx.pointsByGeo = new TLongObjectHashMap<NetworkDBPoint>();
		stats.loadPointsTime = (System.nanoTime() - time) / 1e6;
		System.out.printf(" %,d - %.2fms\n", hctx.pointsById.size(), stats.loadPointsTime);
		if (c.PRELOAD_SEGMENTS) {
			time = System.nanoTime();
			System.out.printf("Loading segments...");
			int cntEdges = networkDB.loadNetworkSegments(hctx.pointsById.valueCollection());
			stats.loadEdgesTime = (System.nanoTime() - time) / 1e6;
			System.out.printf(" %,d - %.2fms\n", cntEdges, stats.loadEdgesTime);
			stats.loadEdgesCnt = cntEdges;
		} else {
			for (NetworkDBPoint p : hctx.pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
		}
		hctx.clusterOutPoints = networkDB.groupByClusters(hctx.pointsById, true);
		hctx.clusterInPoints = networkDB.groupByClusters(hctx.pointsById, false);
		for (NetworkDBPoint pnt : hctx.pointsById.valueCollection()) {
			long pos = calculateRoutePointInternalId(pnt.roadId, pnt.start, pnt.end);
			LatLon latlon = pnt.getPoint();
			hctx.pointsRect.registerObject(latlon.getLatitude(), latlon.getLongitude(), pnt);
			if (pos != pnt.pntGeoId) {
				throw new IllegalStateException();
			}
			hctx.boundaries.put(pos, null);
			hctx.pointsByGeo.put(pos, pnt);
		}		
		hctx.pointsRect.printStatsDistribution("Points distributed");
		return hctx;
	}

	private TLongObjectHashMap<NetworkDBPoint> initStart(HHRoutingConfig c, HHRoutingContext hctx, LatLon p, LatLon e, boolean reverse)
			throws IOException, InterruptedException {
		TLongObjectHashMap<NetworkDBPoint> pnts = new TLongObjectHashMap<>();
		if (!c.ROUTE_LAST_MILE) {
			double rad = 10000;
			float spd = ctx.getRouter().getMinSpeed();
			while (rad < 300000 && pnts.isEmpty()) {
				rad = rad * 2;
				List<NetworkDBPoint> pntSelect = hctx.pointsRect.getClosestObjects(p.getLatitude(), p.getLongitude(), rad);
				for (NetworkDBPoint pnt : pntSelect) {
					double cost = MapUtils.getDistance(p, pnt.getPoint()) / spd;
					pnt.setCostParentRt(reverse, cost + distanceToEnd(c, reverse, pnt, e), null, cost);
					pnts.put(pnt.index, pnt);
				}
			}
			return pnts;
		}
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		RouteSegmentPoint s = planner.findRouteSegment(p.getLatitude(), p.getLongitude(), ctx, null);
		if (s == null) {
			return pnts;
		}
		ctx.config.planRoadDirection = reverse ? -1 : 1;
		ctx.config.heuristicCoefficient = 0; // dijkstra
		ctx.unloadAllData(); // needed for proper multidijsktra work
		ctx.calculationProgress = new RouteCalculationProgress();
		MultiFinalRouteSegment frs = (MultiFinalRouteSegment) new BinaryRoutePlanner().searchRouteInternal(ctx,
				reverse ? null : s, reverse ? s : null, hctx.boundaries);
		System.out.println(ctx.calculationProgress.getInfo(null));		
		if (frs != null) {
			TLongSet set = new TLongHashSet();
			for (FinalRouteSegment o : frs.all) {
				// duplicates are possible as alternative routes
				long pntId = calculateRoutePointInternalId(o.getRoad().getId(),
						reverse ? o.getSegmentEnd() : o.getSegmentStart(),
						reverse ? o.getSegmentStart() : o.getSegmentEnd());
				if (set.add(pntId)) {
					NetworkDBPoint pnt = hctx.pointsByGeo.get(pntId);
					distanceToEnd(c, reverse, pnt, e);
					pnt.setDetailedParentRt(reverse, o);
					pnts.put(pnt.index, pnt);
				}
			}

		}
		if (c.USE_GC_MORE_OFTEN) {
			ctx.unloadAllData();
			printGCInformation();
		}
		return pnts;
	}

	
	static class NetworkDBPointCost {
		NetworkDBPoint point;
		double cost;
		boolean rev;
		
		NetworkDBPointCost(NetworkDBPoint p, double cost, boolean rev) {
			point = p;
			this.cost = cost;
			this.rev = rev;
		}
	}
	
	protected NetworkDBPoint runDijkstraNetworkRouting(NetworkDBPoint start, NetworkDBPoint end, HHRoutingConfig c,
			HHRoutingContext hctx, RoutingStats stats) throws SQLException {
		if (start != null) {
			start.setCostParentRt(false, 0, null, 0);
			hctx.queueAdded.add(start);
			hctx.queue.add(new NetworkDBPointCost(start, start.rtCost(false), false));
		}
		
		if (end != null) {
			end.setCostParentRt(true, 0, null, 0);
			hctx.queueAdded.add(end);
			hctx.queue.add(new NetworkDBPointCost(start, start.rtCost(true), true));
		}
		return runDijkstraNetworkRouting(hctx, null, null, c, stats);

	}
	
	protected NetworkDBPoint runDijkstraNetworkRouting(TLongObjectHashMap<NetworkDBPoint> stPoints, TLongObjectHashMap<NetworkDBPoint> endPoints,
			LatLon startLatLon, LatLon endLatLon, HHRoutingConfig c,
			HHRoutingContext hctx, RoutingStats stats) throws SQLException {
		Queue<NetworkDBPointCost> queue = hctx.queue;
		// TODO 1.6 HHRoutePlanner revert 2 queues to fail fast in 1 direction
		for (NetworkDBPoint start : stPoints.valueCollection()) {
			if (start.rtExclude) {
				continue;
			}
			if (start.rtCost(false) <= 0) {
				start.setCostParentRt(false, distanceToEnd(c, false, start, endLatLon), null, 0);
			}
			hctx.queueAdded.add(start);
			queue.add(new NetworkDBPointCost(start, start.rtCost(false), false));
		}
		for (NetworkDBPoint end : endPoints.valueCollection()) {
			if (end.rtExclude) {
				continue;
			}
			if (end.rtCost(false) <= 0) {
				end.setCostParentRt(true, distanceToEnd(c, true, end, startLatLon), null, 0);
			}
			hctx.queueAdded.add(end);
			queue.add(new NetworkDBPointCost(end, end.rtCost(true), true));
		}
		return runDijkstraNetworkRouting(hctx, startLatLon, endLatLon, c, stats);
	}
	
	protected NetworkDBPoint runDijkstraNetworkRouting(HHRoutingContext hctx, 
				LatLon startLatLon, LatLon endLatLon, HHRoutingConfig c, RoutingStats stats) throws SQLException {
		Queue<NetworkDBPointCost> queue = hctx.queue;
		while (!queue.isEmpty()) {
			long tm = System.nanoTime();
			NetworkDBPointCost pointCost = queue.poll();
			NetworkDBPoint point = pointCost.point;
			boolean rev = pointCost.rev;
			stats.pollQueueTime += (System.nanoTime() - tm) / 1e6;
			stats.visitedVertices++;
			if (point.visited(!rev)) {
				if (c.HEURISTIC_COEFFICIENT == 1 && c.DIJKSTRA_DIRECTION == 0) {
					// TODO 2.1 HHRoutePlanner Improve / Review A* finish condition
					double rcost = point.rtDistanceFromStart + point.rtDistanceFromStartRev;
					if (rcost <= pointCost.cost) {
						return point;
					} else {
						queue.add(new NetworkDBPointCost(point, rcost, rev));
						point.markVisited(rev);
						continue;
					}
				} else {
					NetworkDBPoint finalPoint = point;
					if (c.DIJKSTRA_DIRECTION == 0) {
						finalPoint = scanFinalPoint(finalPoint, hctx.visited);
						finalPoint = scanFinalPoint(finalPoint, hctx.visitedRev);
					}
					return finalPoint;
				}
			}
			if (point.visited(rev)) {
				continue;
			}
			stats.uniqueVisitedVertices++;
			point.markVisited(rev);
			hctx.visited.add(point);
			(rev ? hctx.visited : hctx.visitedRev).add(point);
			printPoint(point, rev);
			if (c.MAX_COST > 0 && pointCost.cost > c.MAX_COST) {
				break;
			}
			if (c.MAX_SETTLE_POINTS > 0 && (rev ? hctx.visitedRev : hctx.visited).size() > c.MAX_SETTLE_POINTS) {
				break;
			}
			boolean directionAllowed = (c.DIJKSTRA_DIRECTION <= 0 && rev) || (c.DIJKSTRA_DIRECTION >= 0 && !rev);
			if (directionAllowed) {
				addToQueue(queue, point, rev ? startLatLon : endLatLon, rev, c, hctx, stats);
			}
		}			
		return null;
		
	}

	private NetworkDBPoint scanFinalPoint(NetworkDBPoint finalPoint, List<NetworkDBPoint> lt) {
		for (NetworkDBPoint p : lt) {
			if (p.rtDistanceFromStart == 0 || p.rtDistanceFromStartRev == 0) {
				continue;
			}
			if (p.rtDistanceFromStart + p.rtDistanceFromStartRev < finalPoint.rtDistanceFromStart + finalPoint.rtDistanceFromStartRev) {
				finalPoint = p;
			}
		}
		return finalPoint;
	}
	
	private void addToQueue(Queue<NetworkDBPointCost> queue, NetworkDBPoint point, LatLon target, boolean reverse, 
			HHRoutingConfig c, HHRoutingContext hctx, RoutingStats stats) throws SQLException {
		int depth = c.USE_MIDPOINT || c.MAX_DEPTH > 0 ? point.getDepth(!reverse) : 0;
		if (c.MAX_DEPTH > 0 && depth >= c.MAX_DEPTH) {
			return;
		}
		long tm = System.nanoTime();
		int cnt = networkDB.loadNetworkSegmentPoint(hctx.pointsById, hctx.clusterInPoints, hctx.clusterOutPoints,  point, reverse);
		stats.loadEdgesCnt += cnt;
		stats.loadEdgesTime += (System.nanoTime() - tm) / 1e6;
		for (NetworkDBSegment connected : point.connected(reverse)) {
			NetworkDBPoint nextPoint = reverse ? connected.start : connected.end;
			if (!c.USE_CH && !c.USE_CH_SHORTCUTS && connected.shortcut) {
				continue;
			}
			if (nextPoint.rtExclude) {
				continue;
			}
			// modify CH to not compute all top points
			if (c.USE_CH && (nextPoint.chInd > 0 && nextPoint.chInd < point.chInd)) {
				continue;
			}
			if (c.USE_MIDPOINT && Math.min(depth, c.MIDPOINT_MAX_DEPTH) > nextPoint.rtCnt + c.MIDPOINT_ERROR) {
				continue;
			}
			double cost = point.distanceFromStart(reverse) + connected.dist + distanceToEnd(c, reverse, nextPoint, target);
			double exCost = nextPoint.rtCost(reverse);
			if ((exCost == 0 && !nextPoint.visited(reverse)) || cost < exCost) {
				if (nextPoint.visited(reverse)) {
					throw new IllegalStateException(String.format("%s visited - cost %.2f > prev cost %.2f", nextPoint, cost, exCost));
				}
				nextPoint.setCostParentRt(reverse, cost, point, connected.dist);
				tm = System.nanoTime();
				hctx.queueAdded.add(nextPoint);
				queue.add(new NetworkDBPointCost(nextPoint, cost, reverse)); // we need to add new object to not  remove / rebalance priority queue
				stats.addQueueTime += (System.nanoTime() - tm) / 1e6;
				if (DEBUG_VERBOSE_LEVEL > 2) {
					System.out.printf("Add  %s to visit - cost %.2f > prev cost %.2f \n", nextPoint, cost, exCost);
				}
				stats.addedVertices++;
			}
		}
	}

	private double distanceToEnd(HHRoutingConfig c, boolean reverse,  NetworkDBPoint nextPoint, LatLon target) {
		if (c.HEURISTIC_COEFFICIENT > 0) {
			double distanceToEnd = nextPoint.distanceToEnd(reverse);
			if (distanceToEnd == 0) {
				distanceToEnd = c.HEURISTIC_COEFFICIENT * 
						MapUtils.getDistance(target, nextPoint.getPoint()) / ctx.getRouter().getMaxSpeed();
				nextPoint.setDistanceToEnd(reverse, distanceToEnd);
			}
			return distanceToEnd;
		}
		return 0;
	}
		
	private void printPoint(NetworkDBPoint p, boolean rev) {
		if (DEBUG_VERBOSE_LEVEL > 1) {
			int pind = 0; long pchInd = 0;
			if (rev && p.rtRouteToPointRev != null) {
				pind = p.rtRouteToPointRev.index;
				pchInd = p.rtRouteToPointRev.chInd;
			}
			if (!rev && p.rtRouteToPoint != null) {
				pind = p.rtRouteToPoint.index;
				pchInd = p.rtRouteToPoint.chInd;
			}
			String symbol = String.format("%s %d [%d] (from %d [%d])", rev ? "<-" : "->", p.index, p.chInd, pind, pchInd);
			System.out.printf("Visit Point %s (cost %.1f s) %.5f/%.5f - %d\n", symbol, p.rtCost(rev),
					MapUtils.get31LatitudeY(p.startY), MapUtils.get31LongitudeX(p.startX), p.roadId / 64);
		}
	}
	
	public static class HHNetworkSegmentRes {
		public NetworkDBSegment segment;
		public List<RouteSegmentResult> list = null;
		public HHNetworkSegmentRes(NetworkDBSegment s) {
			segment = s;
		}
	}
	
	public static class HHNetworkRouteRes {
		public RoutingStats stats;
		public List<HHNetworkSegmentRes> segments = new ArrayList<>();
		public List<RouteSegmentResult> detailed = new ArrayList<>();
		public List<HHNetworkRouteRes> altRoutes = new ArrayList<>();
		public TLongHashSet uniquePoints = new TLongHashSet();
		
		public float routingTimeDetailed;
		public double routingTimeSegments;
	}

	
	// TODO 1.3 HHRoutePlanner routing 1/-1/0 FIX routing time 7288 / 7088 / 7188 (43.15274, 19.55169 -> 42.955495, 19.0972263)
	private HHNetworkSegmentRes runDetailedRouting(HHRoutingConfig c, HHNetworkSegmentRes res) throws InterruptedException, IOException {
		
		BinaryRoutePlanner planner = new BinaryRoutePlanner();
		NetworkDBSegment segment = res.segment;
		ctx.config.planRoadDirection = 0; // A* bidirectional
		ctx.config.heuristicCoefficient = 1; 
		ctx.unloadAllData(); // needed for proper multidijsktra work
//		if (c.USE_GC_MORE_OFTEN) {
//			printGCInformation();
//		}
		RouteSegmentPoint start = loadPoint(ctx, segment.start);
		RouteSegmentPoint end = loadPoint(ctx, segment.end);
		if(start == null || end == null) {
			// TODO in Future
			throw new IllegalStateException("Points are not present in detailed maps: segment need to be recalculated");
		}
		// TODO 1.4 HHRoutePlanner use cache boundaries to speed up
		FinalRouteSegment f = planner.searchRouteInternal(ctx, start, end, null);
		res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, f);
		return res;
	}
	
	
	private HHNetworkRouteRes prepareDetailedRoutingResults(HHRoutingDB networkDB, HHRoutingConfig c, HHNetworkRouteRes route, RoutingStats stats) 
			throws SQLException, InterruptedException, IOException {
		for (int i = 0; i < route.segments.size(); i++) {
			HHNetworkSegmentRes s = route.segments.get(i);
			if (s.segment != null) {
				networkDB.loadGeometry(s.segment, false);
				if (c.ROUTE_ALL_SEGMENTS && s.segment.geometry.size() <= 2) {
					runDetailedRouting(c, s);
				}
			}
		}
		for (HHNetworkRouteRes alt : route.altRoutes) {
			for (int i = 0; i < alt.segments.size(); i++) {
				HHNetworkSegmentRes s = alt.segments.get(i);
				if (s.segment != null) {
					networkDB.loadGeometry(s.segment, false);
					if (c.ROUTE_ALL_ALT_SEGMENTS && s.segment.geometry.size() <= 2) {
						runDetailedRouting(c, s);
					}
				}
			}
		}
		
		ctx.routingTime = 0;
		boolean prevEmpty = true;
		RouteSegmentResult shift = null;
		for (HHNetworkSegmentRes res : route.segments) {
			double rt = 0;
			int detList = 0;
			NetworkDBSegment s = res.segment;
			if (res.list != null && res.list.size() > 0) {
				if (shift != null) {
					route.detailed.add(shift);
					shift = null;
				}
				detList = res.list.size();
				if (!prevEmpty && s != null) {
					RouteSegmentResult p = res.list.get(0);
					if (Math.abs(p.getStartPointIndex() - p.getEndPointIndex()) <= 1) {
						res.list.remove(0);
					} else {
						p.setStartPointIndex(p.getStartPointIndex() + (p.isForwardDirection() ? +1 : -1));
					}
				}
				route.detailed.addAll(res.list);
				for (RouteSegmentResult r : res.list) {
//					System.out.println(r);
					rt += r.getRoutingTime();
				}
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
				shift = new RouteSegmentResult(sh, 0, 1);
				route.detailed.add(new RouteSegmentResult(rdo, 0, 1));
			}
			ctx.routingTime += rt;
			route.routingTimeDetailed += rt;
			if (s == null) {
				prevEmpty = true;
				if (DEBUG_VERBOSE_LEVEL >= 1) {
					System.out.printf("First / last segment - %d segments, %.2fs \n",
							res.list == null ? 0 : res.list.size(), rt);
				}
				continue;
			}
			prevEmpty = false;
			if (DEBUG_VERBOSE_LEVEL >= 1) {
				System.out.printf("\nRoute %d [%d] -> %d [%d] %s - hh dist %.2f s, detail %.2f s segments %d ( end %.5f/%.5f - %d ) ", 
					s.start.index, s.start.chInd, s.end.index,s.end.chInd, s.shortcut ? "sh" : "bs",
					s.dist, rt, detList,
					MapUtils.get31LatitudeY(s.end.startY), MapUtils.get31LongitudeX(s.end.startX), s.end.roadId / 64);
			}
		}
		return route;
	}


	private HHNetworkRouteRes createRoute(NetworkDBPoint pnt) {
		HHNetworkRouteRes route = new HHNetworkRouteRes();
		if (pnt != null) {
			NetworkDBPoint itPnt = pnt;
			route.uniquePoints.add(itPnt.index);
			while (itPnt.rtRouteToPointRev != null) {
				NetworkDBPoint nextPnt = itPnt.rtRouteToPointRev;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, false);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				route.routingTimeSegments += segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rtDetailedRouteRev != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, itPnt.rtDetailedRouteRev);
				route.routingTimeSegments += itPnt.rtDetailedRouteRev.distanceFromStart;
				route.segments.add(res);
			}
			Collections.reverse(route.segments);
			itPnt = pnt;
			while (itPnt.rtRouteToPoint != null) {
				NetworkDBPoint nextPnt = itPnt.rtRouteToPoint;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, true);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				route.routingTimeSegments += segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, itPnt.rtDetailedRoute);
				route.routingTimeSegments += itPnt.rtDetailedRoute.distanceFromStart;

				route.segments.add(res);
			}
			Collections.reverse(route.segments);
		}
		return route;

	}

	/// Utilities
	static final int ROUTE_POINTS = 11;
	public static RouteSegmentPoint loadPoint(RoutingContext ctx, NetworkDBPoint pnt) {
		RouteSegment s;
		s = ctx.loadRouteSegment(pnt.startX, pnt.startY, ctx.config.memoryLimitation);
		while (s != null) {
			if(s.getRoad().getId() == pnt.roadId&& s.getSegmentStart() == pnt.start) {
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

	static long calcUniDirRoutePointInternalId(RouteSegment segm) {
		if (segm.getSegmentStart() < segm.getSegmentEnd()) {
			return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), segm.getSegmentEnd());
		} else {
			return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentEnd(), segm.getSegmentStart());
		}
	}
	
	public static void printGCInformation() {
		System.gc();
		long MEMORY_LAST_USED_MB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
		System.out.printf("***** Memory used %d MB *****\n", MEMORY_LAST_USED_MB);		
	}

}
