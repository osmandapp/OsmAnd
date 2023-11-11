package net.osmand.router;



import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.MultiFinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.HHRouteDataStructure.*;
import net.osmand.router.HHRoutingDB.NetworkDBPoint;
import net.osmand.router.HHRoutingDB.NetworkDBSegment;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

public class HHRoutePlanner<T extends NetworkDBPoint> {
	static int DEBUG_VERBOSE_LEVEL = 0;
	static int DEBUG_ALT_ROUTE_SELECTION = -1;
	static final double MINIMAL_COST = 0.01;
	
	HHRoutingContext<T> cacheHctx;
	private int routingProfile = 0;
	private final Class<T> pointClass;
	
	public static HHRoutePlanner<NetworkDBPoint> create(RoutingContext ctx, HHRoutingDB networkDB) {
		return new HHRoutePlanner<NetworkDBPoint>(ctx, networkDB, NetworkDBPoint.class);
	}
	
	public HHRoutePlanner(RoutingContext ctx, HHRoutingDB networkDB, Class<T> cl) {
		this.pointClass = cl;
		initEmptyContext(ctx, networkDB);
	}
	
	private HHRoutingContext<T> initEmptyContext(RoutingContext ctx, HHRoutingDB networkDB) {
		cacheHctx = new HHRoutingContext<T>();
		cacheHctx.rctx = ctx;
		cacheHctx.networkDB = networkDB;
		return cacheHctx;
	}

	public void close() throws SQLException {
		cacheHctx.networkDB.close();
		initEmptyContext(cacheHctx.rctx, null);
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
	
	private HHRoutingConfig prepareDefaultRoutingConfig(HHRoutingConfig c) {
		if (c == null) {
			c = new HHRoutingConfig();
			// test data for debug swap
			c = HHRoutingConfig.dijkstra(0); 
//			c = HHRoutingConfig.astar(1);
//			c = HHRoutingConfig.ch();
//			c.preloadSegments();
			c.ROUTE_LAST_MILE = true;
//			c.calcDetailed(2);
//			c.calcAlternative();
//			c.gc();
			DEBUG_VERBOSE_LEVEL = 0;
//			DEBUG_ALT_ROUTE_SELECTION++;
			c.ALT_EXCLUDE_RAD_MULT_IN = 1;
			c.ALT_EXCLUDE_RAD_MULT = 0.05;
//			routingProfile = (routingProfile + 1) % networkDB.getRoutingProfiles().size();
		}
		return c;
	}

	public HHNetworkRouteRes runRouting(LatLon start, LatLon end, HHRoutingConfig config) throws SQLException, IOException, InterruptedException {
		long startTime = System.nanoTime();
		config = prepareDefaultRoutingConfig(config);
		HHRoutingContext<T> hctx = initHCtx(config);
		hctx.setStartEnd(start, end);
		hctx.clearVisited();
		if (hctx.config.USE_GC_MORE_OFTEN) {
			printGCInformation();
		}
		long time = System.nanoTime();
		System.out.println(config.toString(start, end));
		System.out.println("Finding first / last segments...");
		TLongObjectHashMap<T> stPoints = initStart(hctx, false);
		TLongObjectHashMap<T> endPoints = initStart(hctx, true);
		hctx.stats.searchPointsTime = (System.nanoTime() - time) / 1e6;
		System.out.printf("Finding first / last segments...%.2f ms\n", hctx.stats.searchPointsTime);

		time = System.nanoTime();
		System.out.printf("Routing...");
		NetworkDBPoint finalPnt = runRoutingPointsToPoints(hctx, stPoints, endPoints);
		HHNetworkRouteRes route = createRouteSegmentFromFinalPoint(hctx, finalPnt);
		hctx.stats.routingTime = (System.nanoTime() - time) / 1e6;
		
		System.out.printf("%d segments, cost %.2f, %.2f ms\n", route.segments.size(),
				route.routingTimeSegments, hctx.stats.routingTime);
		
		if (hctx.config.CALC_ALTERNATIVES) {
			System.out.printf("Alternative routes...");
			time = System.nanoTime();
			calcAlternativeRoute(hctx, route, stPoints, endPoints);
			hctx.stats.altRoutingTime = (System.nanoTime() - time) / 1e6;
			hctx.stats.routingTime += hctx.stats.altRoutingTime;
			System.out.printf("%d %.2f ms\n", route.altRoutes.size(), hctx.stats.altRoutingTime);
		}

		if (hctx.config.USE_GC_MORE_OFTEN) {
			hctx.unloadAllConnections();
			printGCInformation();
		}
		
		System.out.printf("Prepare detailed route segments...");		
		time = System.nanoTime();
		prepareDetailedRoutingResults(hctx, route);
		route.stats = hctx.stats;
		hctx.stats.prepTime = (System.nanoTime() - time) / 1e6;
		System.out.printf("%.2f ms\n", hctx.stats.prepTime);
		
		System.out.println(String.format("Found final route - cost %.2f (detailed %.2f), %d depth ( visited %,d (%,d unique) of %,d added vertices )", 
				route.routingTimeSegments, route.routingTimeDetailed,
				route.segments.size(), hctx.stats.visitedVertices, hctx.stats.uniqueVisitedVertices, hctx.stats.addedVertices));
		
		time = System.nanoTime();
		System.out.println(hctx.config.toString(start, end));
		System.out.printf("Calculate turns...");
		if (hctx.config.ROUTE_ALL_SEGMENTS && route.detailed != null) {
			route.detailed = new RouteResultPreparation().prepareResult(hctx.rctx, route.detailed);
		}
		System.out.printf("%.2f ms\n", (System.nanoTime() - time) / 1e6);
//			RouteResultPreparation.printResults(ctx, start, end, route.detailed);
		
		System.out.printf("Routing finished all %.1f ms: last mile %.1f ms, load data %.1f ms (%,d edges), routing %.1f ms (queue  - %.1f add ms + %.1f poll ms), prep result %.1f ms\n",
				(System.nanoTime() - startTime) / 1e6, hctx.stats.searchPointsTime,
				hctx.stats.loadEdgesTime + hctx.stats.loadPointsTime, hctx.stats.loadEdgesCnt, hctx.stats.routingTime, 
				hctx.stats.addQueueTime, hctx.stats.pollQueueTime, hctx.stats.prepTime);
		printGCInformation();
		return route;
	}

	private void calcAlternativeRoute(HHRoutingContext<T> hctx, HHNetworkRouteRes route, TLongObjectHashMap<T> stPoints,
			TLongObjectHashMap<T> endPoints) throws SQLException {
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
				if (finalPnt != null) {
					double cost = (finalPnt.rt(false).rtDistanceFromStart + finalPnt.rt(true).rtDistanceFromStart);
					if (DEBUG_VERBOSE_LEVEL == 1) {
						System.out.println("Alternative route cost: " + cost);
					}
					rt = createRouteSegmentFromFinalPoint(hctx, finalPnt);
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

	protected HHRoutingContext<T> initHCtx(HHRoutingConfig c) throws SQLException {
		HHRoutingContext<T> hctx = this.cacheHctx;
		if (hctx.networkDB.getRoutingProfile() != routingProfile) {
			hctx.networkDB.selectRoutingProfile(routingProfile);
			hctx = initEmptyContext(hctx.rctx, hctx.networkDB);
		}
		System.out.println("Routing profile: " + hctx.networkDB.getRoutingProfiles().get(routingProfile));
		hctx.stats = new RoutingStats();
		hctx.config = c;
		if (hctx.pointsById != null) {
			return hctx;
		}
		long time = System.nanoTime();
		System.out.print("Loading points... ");
		hctx.pointsById = hctx.networkDB.loadNetworkPoints(pointClass);
		hctx.boundaries = new TLongObjectHashMap<RouteSegment>();
		hctx.pointsByGeo = new TLongObjectHashMap<T>();
		hctx.stats.loadPointsTime = (System.nanoTime() - time) / 1e6;
		System.out.printf(" %,d - %.2fms\n", hctx.pointsById.size(), hctx.stats.loadPointsTime);
		if (c.PRELOAD_SEGMENTS) {
			time = System.nanoTime();
			System.out.printf("Loading segments...");
			int cntEdges = hctx.networkDB.loadNetworkSegments(hctx.pointsById.valueCollection());
			hctx.stats.loadEdgesTime = (System.nanoTime() - time) / 1e6;
			System.out.printf(" %,d - %.2fms\n", cntEdges, hctx.stats.loadEdgesTime);
			hctx.stats.loadEdgesCnt = cntEdges;
		} else {
			for (NetworkDBPoint p : hctx.pointsById.valueCollection()) {
				p.markSegmentsNotLoaded();
			}
		}
		hctx.clusterOutPoints = hctx.networkDB.groupByClusters(hctx.pointsById, true);
		hctx.clusterInPoints = hctx.networkDB.groupByClusters(hctx.pointsById, false);
		for (T pnt : hctx.pointsById.valueCollection()) {
			long pos = calculateRoutePointInternalId(pnt.roadId, pnt.start, pnt.end);
			LatLon latlon = pnt.getPoint();
			hctx.pointsRect.registerObject(latlon.getLatitude(), latlon.getLongitude(), pnt);
			if (pos != pnt.pntGeoId) {
				throw new IllegalStateException(pnt + " " + pos + " != "+ pnt.pntGeoId);
			}
			hctx.boundaries.put(pos, null);
			hctx.pointsByGeo.put(pos, pnt);
		}		
		hctx.pointsRect.printStatsDistribution("Points distributed");
		return hctx;
	}

	@SuppressWarnings("unchecked")
	private TLongObjectHashMap<T> initStart(HHRoutingContext<T> hctx, boolean reverse) throws IOException, InterruptedException {
		TLongObjectHashMap<T> pnts = new TLongObjectHashMap<>();
		double startLat = MapUtils.get31LatitudeY(!reverse? hctx.startY : hctx.endY);
		double startLon = MapUtils.get31LongitudeX(!reverse? hctx.startX : hctx.endX);
		if (!hctx.config.ROUTE_LAST_MILE) {
			double rad = 10000;
			float spd = hctx.rctx.getRouter().getMinSpeed();
			while (rad < 300000 && pnts.isEmpty()) {
				rad = rad * 2;
				
				List<T> pntSelect = hctx.pointsRect.getClosestObjects(startLat, startLon, rad);
				// limit by cluster
				int cid = pntSelect.get(0).clusterId;
				for (T pSelect : pntSelect) {
					if(pSelect.clusterId != cid) {
						continue;
					}
					T pnt  = reverse ? (T) pSelect.dualPoint : pSelect;
					double cost = MapUtils.getDistance(pnt.getPoint(), startLat, startLon) / spd;
					pnt.setCostParentRt(reverse, cost + distanceToEnd(hctx, reverse, pnt), null, cost);
					pnts.put(pnt.index, pnt);
				}
			}
			return pnts;
		}
		RoutePlannerFrontEnd planner = new RoutePlannerFrontEnd();
		RouteSegmentPoint s = planner.findRouteSegment(startLat, startLon, hctx.rctx, null);
		if (s == null) {
			return pnts;
		}
		hctx.rctx.config.planRoadDirection = reverse ? -1 : 1;
		hctx.rctx.config.heuristicCoefficient = 0; // dijkstra
		hctx.rctx.unloadAllData(); // needed for proper multidijsktra work
		hctx.rctx.calculationProgress = new RouteCalculationProgress();
		MultiFinalRouteSegment frs = (MultiFinalRouteSegment) new BinaryRoutePlanner().searchRouteInternal(hctx.rctx,
				reverse ? null : s, reverse ? s : null, hctx.boundaries);
		System.out.println(hctx.rctx.calculationProgress.getInfo(null));		
		if (frs != null) {
			TLongSet set = new TLongHashSet();
			for (FinalRouteSegment o : frs.all) {
				// duplicates are possible as alternative routes
				long pntId = calculateRoutePointInternalId(o.getRoad().getId(),
						reverse ? o.getSegmentEnd() : o.getSegmentStart(),
						reverse ? o.getSegmentStart() : o.getSegmentEnd());
				if (set.add(pntId)) {
					T pnt = hctx.pointsByGeo.get(pntId);
					pnt.setDistanceToEnd(reverse, distanceToEnd(hctx, reverse, pnt));
					pnt.setDetailedParentRt(reverse, o);
					pnts.put(pnt.index, pnt);
				}
			}

		}
		if (hctx.config.USE_GC_MORE_OFTEN) {
			hctx.rctx.unloadAllData();
			printGCInformation();
		}
		return pnts;
	}

	
	protected T runRoutingPointToPoint(HHRoutingContext<T> hctx, T start, T end) throws SQLException {
		if (start != null) {
			addPointToQueue(hctx, hctx.queue, false, start, null, 0, MINIMAL_COST);
		}
		if (end != null) {
			addPointToQueue(hctx, hctx.queue, true, end, null, 0, MINIMAL_COST);
		}
		return runRoutingWithInitQueue(hctx);

	}
	
	protected T runRoutingPointsToPoints(HHRoutingContext<T> hctx, TLongObjectHashMap<T> stPoints,
			TLongObjectHashMap<T> endPoints) throws SQLException {
		for (T start : stPoints.valueCollection()) {
			if (start.rtExclude) {
				continue;
			}
			double cost = start.rt(false).rtCost;
			addPointToQueue(hctx, hctx.queue, false, start, null, start.rt(false).rtDistanceFromStart,
					cost <= 0 ? MINIMAL_COST : cost);
		}
		for (T end : endPoints.valueCollection()) {
			if (end.rtExclude) {
				continue;
			}
			double cost = end.rt(true).rtCost;
			addPointToQueue(hctx, hctx.queue, true, end, null, end.rt(true).rtDistanceFromStart,
					cost <= 0 ? MINIMAL_COST : cost);
		}
		return runRoutingWithInitQueue(hctx);
	}
	
	private T runRoutingWithInitQueue(HHRoutingContext<T> hctx) throws SQLException {
		Queue<NetworkDBPointCost<T>> queue = hctx.queue;
		while (!queue.isEmpty()) {
			long tm = System.nanoTime();
			NetworkDBPointCost<T> pointCost = queue.poll();
			T point = pointCost.point;
			boolean rev = pointCost.rev;
			hctx.stats.pollQueueTime += (System.nanoTime() - tm) / 1e6;
			hctx.stats.visitedVertices++;
			if (point.rt(!rev).rtVisited) {
				if (hctx.config.HEURISTIC_COEFFICIENT == 1 && hctx.config.DIJKSTRA_DIRECTION == 0) {
					// TODO 2.1 HHRoutePlanner Improve / Review A* finish condition
					double rcost = point.rt(true).rtDistanceFromStart + point.rt(false).rtDistanceFromStart;
					if (rcost <= pointCost.cost) {
						return point;
					} else {
						queue.add(new NetworkDBPointCost<T>(point, rcost, rev));
						point.markVisited(rev);
						continue;
					}
				} else {
					T finalPoint = point;
					if (hctx.config.DIJKSTRA_DIRECTION == 0) {
						finalPoint = scanFinalPoint(finalPoint, hctx.visited);
						finalPoint = scanFinalPoint(finalPoint, hctx.visitedRev);
					}
					return finalPoint;
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
			if (hctx.config.MAX_COST > 0 && pointCost.cost > hctx.config.MAX_COST) {
				break;
			}
			if (hctx.config.MAX_SETTLE_POINTS > 0 && (rev ? hctx.visitedRev : hctx.visited).size() > hctx.config.MAX_SETTLE_POINTS) {
				break;
			}
			boolean directionAllowed = (hctx.config.DIJKSTRA_DIRECTION <= 0 && rev) || (hctx.config.DIJKSTRA_DIRECTION >= 0 && !rev);
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
	private void addConnectedToQueue(HHRoutingContext<T> hctx, Queue<NetworkDBPointCost<T>> queue, T point, boolean reverse) throws SQLException {
		int depth = hctx.config.USE_MIDPOINT || hctx.config.MAX_DEPTH > 0 ? point.rt(reverse).getDepth(reverse) : 0;
		if (hctx.config.MAX_DEPTH > 0 && depth >= hctx.config.MAX_DEPTH) {
			return;
		}
		long tm = System.nanoTime();
		int cnt = hctx.networkDB.loadNetworkSegmentPoint(hctx.pointsById, hctx.clusterInPoints, hctx.clusterOutPoints,  point, reverse);
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
			double cost = point.rt(reverse).rtDistanceFromStart  + connected.dist + distanceToEnd(hctx, reverse, nextPoint) ;
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

	private double distanceToEnd(HHRoutingContext<T> hctx, boolean reverse,  NetworkDBPoint nextPoint) {
		if (hctx.config.HEURISTIC_COEFFICIENT > 0) {
			double distanceToEnd = nextPoint.rt(reverse).rtDistanceToEnd;
			if (distanceToEnd == 0) {
				double dist = squareRootDist31(reverse ? hctx.startX : hctx.endX, reverse ? hctx.startY : hctx.endY, 
						nextPoint.midX(), nextPoint.midY());
				distanceToEnd = hctx.config.HEURISTIC_COEFFICIENT * dist / hctx.rctx.getRouter().getMaxSpeed();
				nextPoint.setDistanceToEnd(reverse, distanceToEnd);
			}
			return distanceToEnd;
		}
		return 0;
	}
		
	private void printPoint(T p, boolean rev) {
		if (DEBUG_VERBOSE_LEVEL > 1) {
			int pind = 0; long pchInd = 0;
			if (p.rt(rev).rtRouteToPoint != null) {
				pind = p.rt(rev).rtRouteToPoint.index;
				pchInd = p.rt(rev).rtRouteToPoint.chInd();
			}
			String symbol = String.format("%s %d [%d] (from %d [%d])", rev ? "<-" : "->", p.index, p.chInd(), pind, pchInd);
			System.out.printf("Visit Point %s (cost %.1f s) %.5f/%.5f - %d\n", symbol, p.rt(rev),
					MapUtils.get31LatitudeY(p.startY), MapUtils.get31LongitudeX(p.startX), p.roadId / 64);
		}
	}
	

	
	private HHNetworkSegmentRes runDetailedRouting(HHRoutingContext<T> hctx, HHNetworkSegmentRes res)
			throws InterruptedException, IOException {
		BinaryRoutePlanner planner = new BinaryRoutePlanner();
		NetworkDBSegment segment = res.segment;
		hctx.rctx.config.planRoadDirection = 0; // A* bidirectional
		hctx.rctx.config.heuristicCoefficient = 1;
		// TODO 2.0.4 should be speed up by just clearing visited
		hctx.rctx.unloadAllData(); // needed for proper multidijsktra work
		// if (c.USE_GC_MORE_OFTEN) {
		// printGCInformation();
		// }
		RouteSegmentPoint start = loadPoint(hctx.rctx, segment.start);
		RouteSegmentPoint end = loadPoint(hctx.rctx, segment.end);
		if (start == null || end == null) {
			// TODO 2.2 in Future
			throw new IllegalStateException(String.format("Points are not present in detailed maps: %s",
					start == null ? segment.start : segment.end));
		}
		// TODO 2.0.2 HHRoutePlanner use cache boundaries to speed up
		FinalRouteSegment f = planner.searchRouteInternal(hctx.rctx, start, end, null);
		res.list = new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, f);
		return res;
	}
	
	private HHNetworkRouteRes prepareDetailedRoutingResults(HHRoutingContext<T> hctx, HHNetworkRouteRes route) 
			throws SQLException, InterruptedException, IOException {
		for (int i = 0; i < route.segments.size(); i++) {
			HHNetworkSegmentRes s = route.segments.get(i);
			if (s.segment != null) {
				hctx.networkDB.loadGeometry(s.segment, false);
				if (hctx.config.ROUTE_ALL_SEGMENTS && s.segment.getGeometry().size() <= 2) {
					runDetailedRouting(hctx, s);
				}
			}
		}
		for (HHNetworkRouteRes alt : route.altRoutes) {
			for (int i = 0; i < alt.segments.size(); i++) {
				HHNetworkSegmentRes s = alt.segments.get(i);
				if (s.segment != null) {
					hctx.networkDB.loadGeometry(s.segment, false);
					if (hctx.config.ROUTE_ALL_ALT_SEGMENTS && s.segment.getGeometry().size() <= 2) {
						runDetailedRouting(hctx, s);
					}
				}
			}
		}
		
		hctx.rctx.routingTime = 0;
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
			hctx.rctx.routingTime += rt;
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
					s.start.index, s.start.chInd(), s.end.index,s.end.chInd(), s.shortcut ? "sh" : "bs",
					s.dist, rt, detList,
					MapUtils.get31LatitudeY(s.end.startY), MapUtils.get31LongitudeX(s.end.startX), s.end.roadId / 64);
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
				route.routingTimeSegments += segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rt(true).rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, itPnt.rt(true).rtDetailedRoute);
				route.routingTimeSegments += itPnt.rt(true).rtDetailedRoute.distanceFromStart;
				route.segments.add(res);
			}
			Collections.reverse(route.segments);
			itPnt = pnt;
			while (itPnt.rt(false).rtRouteToPoint != null) {
				NetworkDBPoint nextPnt = itPnt.rt(false).rtRouteToPoint;
				NetworkDBSegment segment = nextPnt.getSegment(itPnt, true);
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(segment);
				route.segments.add(res);
				route.routingTimeSegments += segment.dist;
				itPnt = nextPnt;
				route.uniquePoints.add(itPnt.index);
			}
			if (itPnt.rt(false).rtDetailedRoute != null) {
				HHNetworkSegmentRes res = new HHNetworkSegmentRes(null);
				res.list = new RouteResultPreparation().convertFinalSegmentToResults(hctx.rctx, itPnt.rt(false).rtDetailedRoute);
				route.routingTimeSegments += itPnt.rt(false).rtDetailedRoute.distanceFromStart;
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
