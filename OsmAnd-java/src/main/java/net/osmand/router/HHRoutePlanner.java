package net.osmand.router;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.MultiFinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.HHRoutingPreparationDB.NetworkDBPoint;
import net.osmand.router.HHRoutingPreparationDB.NetworkDBSegment;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

public class HHRoutePlanner {
	static String ROUTING_PROFILE = "car";
	static int DEBUG_VERBOSE_LEVEL = 0;
	
	static boolean USE_LAST_MILE_ROUTING = true;
	static boolean CALCULATE_GEOMETRY = true;
	static boolean PRELOAD_SEGMENTS = false;

	static final int PROC_ROUTING = 0;
	static int PROCESS = PROC_ROUTING;

	
	HHRoutingPreparationDB networkDB;
	private RoutingContext ctx;
	private TLongObjectHashMap<NetworkDBPoint> cachePoints;
	private TLongObjectHashMap<NetworkDBPoint> cachePointsByGeo;
	private TLongObjectHashMap<RouteSegment> cacheBoundaries; 
	
	public HHRoutePlanner(RoutingContext ctx, HHRoutingPreparationDB networkDB) {
		this.ctx = ctx;
		this.networkDB = networkDB;
	}
	
	public static class DijkstraConfig {
		float HEURISTIC_COEFFICIENT = 0; // A* - 1, Dijkstra - 0
		float DIJKSTRA_DIRECTION = 0; // 0 - 2 directions, 1 - positive, -1 - reverse
		
		double MAX_COST;
		int MAX_DEPTH = -1; // max depth to go to
		int MAX_SETTLE_POINTS = -1; // max points to settle
		
		boolean USE_CH;
		boolean USE_CH_SHORTCUTS;

		boolean USE_MIDPOINT;
		int MIDPOINT_ERROR = 3;
		int MIDPOINT_MAX_DEPTH = 20 + MIDPOINT_ERROR;
		
		public List<NetworkDBPoint> visited = new ArrayList<>();
		public List<NetworkDBPoint> visitedRev = new ArrayList<>();
		
		public static DijkstraConfig dijkstra(int direction) {
			DijkstraConfig df = new DijkstraConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static DijkstraConfig astar(int direction) {
			DijkstraConfig df = new DijkstraConfig();
			df.HEURISTIC_COEFFICIENT = 1;
			df.DIJKSTRA_DIRECTION = direction;
			return df;
		}
		
		public static DijkstraConfig ch() {
			DijkstraConfig df = new DijkstraConfig();
			df.HEURISTIC_COEFFICIENT = 0;
			df.USE_CH = true;
			df.USE_CH_SHORTCUTS = true;
			df.DIJKSTRA_DIRECTION = 0;
			return df;
		}
		
		public static DijkstraConfig midPoints(boolean astar, int dir) {
			DijkstraConfig df = new DijkstraConfig();
			df.HEURISTIC_COEFFICIENT = astar ? 1 : 0;
			df.USE_MIDPOINT = true;
			df.DIJKSTRA_DIRECTION = dir;
			return df;
		}
		
		public DijkstraConfig useShortcuts() {
			USE_CH_SHORTCUTS = true;
			return this;
		}
		
		public DijkstraConfig maxCost(double cost) {
			MAX_COST = cost;
			return this;
		}
		
		public DijkstraConfig maxDepth(int depth) {
			MAX_DEPTH = depth;
			return this;
		}
		
		public DijkstraConfig maxSettlePoints(int maxPoints) {
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
	
	static class RoutingStats {
		int visitedVertices = 0;
		int uniqueVisitedVertices = 0;
		int addedVertices = 0;

		double loadPointsTime = 0;
		int loadEdgesCnt;
		double loadEdgesTime = 0;
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

	public HHNetworkRouteRes runRouting(LatLon start, LatLon end, DijkstraConfig c) throws SQLException, IOException, InterruptedException {
		RoutingStats stats = new RoutingStats();
		long time = System.nanoTime(), startTime = System.nanoTime();
		if (c == null) {
			c = new DijkstraConfig();
			// test data for debug swap
//			c = DijkstraConfig.dijkstra(0);
			c = DijkstraConfig.astar(1);
//			c = DijkstraConfig.ch();
			PRELOAD_SEGMENTS = false;
			CALCULATE_GEOMETRY = true;
			USE_LAST_MILE_ROUTING = true;
			DEBUG_VERBOSE_LEVEL = 1;
		}
		System.out.println(c.toString(start, end));
		System.out.print("Loading points... ");
		if (cachePoints == null) {
			cachePoints = networkDB.getNetworkPoints(false);
			cacheBoundaries = new TLongObjectHashMap<RouteSegment>();
			cachePointsByGeo = new TLongObjectHashMap<NetworkDBPoint>();
			stats.loadPointsTime = (System.nanoTime() - time) / 1e6;
			System.out.printf(" %,d - %.2fms\n", cachePoints.size(), stats.loadPointsTime);
			if (PRELOAD_SEGMENTS) {
				time = System.nanoTime();
				System.out.printf("Loading segments...");
				int cntEdges = networkDB.loadNetworkSegments(cachePoints.valueCollection());
				stats.loadEdgesTime = (System.nanoTime() - time) / 1e6;
				System.out.printf(" %,d - %.2fms\n", cntEdges, stats.loadEdgesTime);
				stats.loadEdgesCnt = cntEdges;
			} else {
				for (NetworkDBPoint p : cachePoints.valueCollection()) {
					p.markSegmentsNotLoaded();
				}
			}
			for (NetworkDBPoint pnt : cachePoints.valueCollection()) {
				long pos = calculateRoutePointInternalId(pnt.roadId, pnt.start, pnt.end);
				long neg = calculateRoutePointInternalId(pnt.roadId, pnt.end, pnt.start);
				if (pos != pnt.pntGeoId && neg != pnt.pntGeoId) {
					throw new IllegalStateException();
				}
				cacheBoundaries.put(pos, null);
				cacheBoundaries.put(neg, null);
				cachePointsByGeo.put(pos, pnt);
				cachePointsByGeo.put(neg, pnt);
			}
		}
		for (NetworkDBPoint pnt : cachePoints.valueCollection()) {
			pnt.clearRouting();
		}
		System.out.printf("Looking for route %s -> %s \n", start, end);
		System.out.print("Finding first / last segments...");
		List<NetworkDBPoint> stPoints = initStart(c, start, end, !USE_LAST_MILE_ROUTING, false);
		List<NetworkDBPoint> endPoints = initStart(c, end, start, !USE_LAST_MILE_ROUTING, true);

		stats.searchPointsTime = (System.nanoTime() - time) / 1e6;
		time = System.nanoTime();
		System.out.printf("%.2f ms\nRouting...", stats.searchPointsTime);
		NetworkDBPoint pnt = runDijkstraNetworkRouting(stPoints, endPoints, start, end, c, stats);
		stats.routingTime = (System.nanoTime() - time) / 1e6;
		
		time = System.nanoTime();
		System.out.printf("%.2f ms\nPreparation...", stats.routingTime);
		HHNetworkRouteRes route = prepareRoutingResults(networkDB, pnt, stats);
		stats.prepTime = (System.nanoTime() - time) / 1e6;
		System.out.printf("%.2f ms\n", (System.nanoTime() - time) / 1e6);
		System.out.println(String.format("Found final route - cost %.2f (%.2f + start %.2f), %d depth ( visited %,d (%,d unique) of %,d added vertices )", 
				route.routingTimeSegments, route.routingTimeHHDetailed, route.routingTimeDetailed,
				route.segments.size(), stats.visitedVertices, stats.uniqueVisitedVertices, stats.addedVertices));
		
		time = System.nanoTime();
		System.out.println(c.toString(start, end));
		System.out.printf("Calculate turns...");
		if (CALCULATE_GEOMETRY && route.detailed != null) {
			route.detailed = new RouteResultPreparation().prepareResult(ctx, route.detailed, false);
			System.out.printf("%.2f ms\n", (System.nanoTime() - time) / 1e6);
//			RouteResultPreparation.printResults(ctx, start, end, route.detailed);
		}
		System.out.printf("Routing finished all %.1f ms: last mile %.1f ms, load data %.1f ms (%,d edges), routing %.1f ms (queue  - %.1f add ms + %.1f poll ms), prep result %.1f ms\n",
				(System.nanoTime() - startTime) / 1e6, stats.searchPointsTime,
				stats.loadEdgesTime + stats.loadPointsTime, stats.loadEdgesCnt, stats.routingTime, stats.addQueueTime,
				stats.pollQueueTime, stats.prepTime);
		printGCInformation();
		return route;
	}

	private List<NetworkDBPoint> initStart(DijkstraConfig c, LatLon p, LatLon e, boolean simple, boolean reverse)
			throws IOException, InterruptedException {
		if (simple) {
			NetworkDBPoint st = null;
			for (NetworkDBPoint pnt : cachePoints.valueCollection()) {
				if (st == null) {
					st = pnt;
				}
				if (MapUtils.getDistance(p, pnt.getPoint()) < MapUtils.getDistance(p, st.getPoint())) {
					st = pnt;
				}
			}
			if (st == null) {
				Collections.emptyList();
			}
			return Collections.singletonList(st);
		}
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		RouteSegmentPoint s = planner.findRouteSegment(p.getLatitude(), p.getLongitude(), ctx, null);
		if (s == null) {
			return Collections.emptyList();
		}
		ctx.config.planRoadDirection = reverse ? -1 : 1;
		ctx.config.heuristicCoefficient = 0; // dijkstra
		ctx.unloadAllData(); // needed for proper multidijsktra work
		ctx.calculationProgress = new RouteCalculationProgress();
		if (reverse) {
			ctx.targetX = s.getRoad().getPoint31XTile(s.getSegmentStart(), s.getSegmentEnd());
			ctx.targetY = s.getRoad().getPoint31XTile(s.getSegmentStart(), s.getSegmentEnd());
		} else {
			ctx.startX = s.getRoad().getPoint31XTile(s.getSegmentStart(), s.getSegmentEnd());
			ctx.startY = s.getRoad().getPoint31YTile(s.getSegmentStart(), s.getSegmentEnd());
		}
		
		MultiFinalRouteSegment frs = (MultiFinalRouteSegment) new BinaryRoutePlanner().searchRouteInternal(ctx,
				reverse ? null : s, reverse ? s : null, null, cacheBoundaries);
		List<NetworkDBPoint> pnts = new ArrayList<>();
		if (frs != null) {
			TLongSet set = new TLongHashSet();
			for (FinalRouteSegment o : frs.all) {
				// duplicates are possible as alternative routes
				long pntId = calculateRoutePointInternalId(o);
				if (set.add(pntId)) {
					NetworkDBPoint pnt = cachePointsByGeo.get(pntId);
					pnt.setCostParentRt(reverse, o.getDistanceFromStart() + distanceToEnd(c, reverse, pnt, e), null,
							o.getDistanceFromStart());
					pnt.setCostDetailedParentRt(reverse, o);
					pnts.add(pnt);
				}
			}
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
	
	protected NetworkDBPoint runDijkstraNetworkRouting(NetworkDBPoint start, NetworkDBPoint end, DijkstraConfig c,
			RoutingStats stats) throws SQLException {
		List<NetworkDBPoint> starts = start == null ? Collections.emptyList() : Collections.singletonList(start);
		List<NetworkDBPoint> ends = end == null ? Collections.emptyList() : Collections.singletonList(end);
		return runDijkstraNetworkRouting(starts, ends, start == null ? null : start.getPoint(),
				end == null ? null : end.getPoint(), c, stats);
	}
	
	protected NetworkDBPoint runDijkstraNetworkRouting(List<NetworkDBPoint> starts, List<NetworkDBPoint> ends,
			LatLon startLatLon, LatLon endLatLon, DijkstraConfig c,
			RoutingStats stats) throws SQLException {
		Queue<NetworkDBPointCost> queue = new PriorityQueue<>(new Comparator<NetworkDBPointCost>() {
			@Override
			public int compare(NetworkDBPointCost o1, NetworkDBPointCost o2) {
				return Double.compare(o1.cost, o2.cost);
			}
		});
		// TODO revert 2 queues to fail fast in 1 direction
		for (NetworkDBPoint start : starts) {
			if (start.rtCost(false) <= 0) {
				start.setCostParentRt(false, distanceToEnd(c, false, start, endLatLon), null, 0);
			}
			queue.add(new NetworkDBPointCost(start, start.rtCost(false), false));
		}
		for (NetworkDBPoint end : ends) {
			if (end.rtCost(false) <= 0) {
				end.setCostParentRt(true, distanceToEnd(c, true, end, startLatLon), null, 0);
			}
			queue.add(new NetworkDBPointCost(end, end.rtCost(true), true));
		}

		while (!queue.isEmpty()) {
			long tm = System.nanoTime();
			NetworkDBPointCost pointCost = queue.poll();
			NetworkDBPoint point = pointCost.point;
			boolean rev = pointCost.rev;
			stats.pollQueueTime += (System.nanoTime() - tm) / 1e6;
			stats.visitedVertices++;
			if (point.visited(!rev)) {
				if (c.HEURISTIC_COEFFICIENT == 1 && c.DIJKSTRA_DIRECTION == 0) {
					// TODO could be improved while adding vertices ? too slow
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
						finalPoint = scanFinalPoint(finalPoint, c.visited);
						finalPoint = scanFinalPoint(finalPoint, c.visitedRev);
					}
					return finalPoint;
				}
			}
			if (point.visited(rev)) {
				continue;
			}
			stats.uniqueVisitedVertices++;
			point.markVisited(rev);
			(rev ? c.visitedRev : c.visited).add(point);
			printPoint(point, rev);
			if (c.MAX_COST > 0 && pointCost.cost > c.MAX_COST) {
				break;
			}
			if (c.MAX_SETTLE_POINTS > 0 && (rev ? c.visitedRev : c.visited).size() > c.MAX_SETTLE_POINTS) {
				break;
			}
			boolean directionAllowed = (c.DIJKSTRA_DIRECTION <= 0 && rev) || (c.DIJKSTRA_DIRECTION >= 0 && !rev);
			if (directionAllowed) {
				addToQueue(queue, point, rev ? startLatLon : endLatLon, rev, c, stats);
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
			DijkstraConfig c, RoutingStats stats) throws SQLException {
		int depth = c.USE_MIDPOINT || c.MAX_DEPTH > 0 ? point.getDepth(!reverse) : 0;
		if (c.MAX_DEPTH > 0 && depth >= c.MAX_DEPTH) {
			return;
		}
		long tm = System.nanoTime();
		int cnt = networkDB.loadNetworkSegmentPoint(cachePoints, point, reverse);
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
				queue.add(new NetworkDBPointCost(nextPoint, cost, reverse)); // we need to add new object to not  remove / rebalance priority queue
				stats.addQueueTime += (System.nanoTime() - tm) / 1e6;
				if (DEBUG_VERBOSE_LEVEL > 2) {
					System.out.printf("Add  %s to visit - cost %.2f > prev cost %.2f \n", nextPoint, cost, exCost);
				}
				stats.addedVertices++;
			}
		}
	}

	private double distanceToEnd(DijkstraConfig c, boolean reverse,  NetworkDBPoint nextPoint, LatLon target) {
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
		public List<HHNetworkSegmentRes> segments = new ArrayList<>();
		public List<RouteSegmentResult> detailed = new ArrayList<>();
		public float routingTimeHHDetailed;
		public float routingTimeDetailed;
		public double routingTimeSegments;
	}

	
	private HHNetworkSegmentRes runDetailedRouting(HHNetworkSegmentRes res) throws InterruptedException, IOException {
		BinaryRoutePlanner planner = new BinaryRoutePlanner();
		NetworkDBSegment segment = res.segment;
		ctx.config.planRoadDirection = 0; // A* bidirectional
		ctx.config.heuristicCoefficient = 1; 
		ctx.unloadAllData(); // needed for proper multidijsktra work
		RouteSegmentPoint start = loadPoint(ctx, segment.start);
		RouteSegmentPoint end = loadPoint(ctx, segment.end);
		ctx.startX = start.getRoad().getPoint31XTile(start.getSegmentStart(), start.getSegmentEnd());
		ctx.startY = start.getRoad().getPoint31YTile(start.getSegmentStart(), start.getSegmentEnd());
		ctx.targetX = end.getRoad().getPoint31XTile(end.getSegmentStart(), end.getSegmentEnd());
		ctx.targetY = end.getRoad().getPoint31YTile(end.getSegmentStart(), end.getSegmentEnd());
		// TODO use cache boundaries to speed up
		FinalRouteSegment f = planner.searchRouteInternal(ctx, start, end, null, null);
		res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, f);
		return res;
	}
	
	
	private HHNetworkRouteRes prepareRoutingResults(HHRoutingPreparationDB networkDB, NetworkDBPoint pnt, RoutingStats stats) throws SQLException, InterruptedException, IOException {
		HHNetworkRouteRes route = calculateSegmentsFromFinalPoint(networkDB, pnt);
		ctx.routingTime = 0;
		boolean prevEmpty = true;
		for (HHNetworkSegmentRes res : route.segments) {
			double rt = 0;
			int detList = 0;
			NetworkDBSegment s = res.segment;
			if (res.list != null && res.list.size() > 0) {
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
			route.routingTimeHHDetailed += rt;
			route.routingTimeSegments += s.dist;
			if (DEBUG_VERBOSE_LEVEL >= 1) {
				System.out.printf("\nRoute %d [%d] -> %d [%d] %s - hh dist %.2f s, detail %.2f s segments %d ( end %.5f/%.5f - %d ) ", 
					s.start.index, s.start.chInd, s.end.index,s.end.chInd, s.shortcut ? "sh" : "bs",
					s.dist, rt, detList,
					MapUtils.get31LatitudeY(s.end.startY), MapUtils.get31LongitudeX(s.end.startX), s.end.roadId / 64);
			}
		}
		return route;
	}



	private HHNetworkRouteRes calculateSegmentsFromFinalPoint(HHRoutingPreparationDB networkDB, NetworkDBPoint pnt)
			throws SQLException, InterruptedException, IOException {
		HHNetworkRouteRes route = new HHNetworkRouteRes();
		if (pnt != null) {
			NetworkDBPoint itPnt = pnt;
			if (itPnt.rtDetailedRouteRev != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, itPnt.rtDetailedRouteRev);
				route.segments.add(res);
			}
			while (itPnt.rtRouteToPointRev != null) {
				NetworkDBPoint nextPnt = itPnt.rtRouteToPointRev;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, false);
				networkDB.loadGeometry(segment, false);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				if (CALCULATE_GEOMETRY && segment.geometry.size() <= 2) {
					runDetailedRouting(res);
				}
				itPnt = nextPnt;
			}
			Collections.reverse(route.segments);
			itPnt = pnt;
			while (itPnt.rtRouteToPoint != null) {
				NetworkDBPoint nextPnt = itPnt.rtRouteToPoint;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, true);
				networkDB.loadGeometry(segment, false);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				if (CALCULATE_GEOMETRY && segment.geometry.size() <= 2) {
					runDetailedRouting(res);
				}
				itPnt = nextPnt;
			}
			if (itPnt.rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(ctx, itPnt.rtDetailedRoute);
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
		while (s != null && (s.getRoad().getId() != pnt.roadId || s.getSegmentStart() != pnt.start
				|| s.getSegmentEnd() != pnt.end)) {
			s = s.getNext();
		}
		if (s == null) {
			throw new IllegalStateException("Error on segment " + pnt.roadId / 64);
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

	static long calculateRoutePointInternalId(RouteSegment segm) {
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
