package net.osmand.router;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.LogUtil;
import net.osmand.NativeLibrary;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.router.RoutingContext.RoutingTile;

import org.apache.commons.logging.Log;

public class BinaryRoutePlanner {
	
	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	private final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	private final NativeLibrary nativeLib;
	private final Map<BinaryMapIndexReader, List<RouteSubregion>> map = new LinkedHashMap<BinaryMapIndexReader, List<RouteSubregion>>();
	
	protected static final Log log = LogUtil.getLog(BinaryRoutePlanner.class);
	
	
	public BinaryRoutePlanner(NativeLibrary nativeLib, BinaryMapIndexReader... map) {
		this.nativeLib = nativeLib;
		for (BinaryMapIndexReader mr : map) {
			List<RouteRegion> rr = mr.getRoutingIndexes();
			List<RouteSubregion> subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
			for (RouteRegion r : rr) {
				for (RouteSubregion rs : r.getSubregions()) {
					subregions.add(new RouteSubregion(rs));
				}
			}
			this.map.put(mr, subregions);
		}
	}
	
	
	private static double squareRootDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = convert31YToMeters(y1, y2);
		double dx = convert31XToMeters(x1, x2);
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	private static double measuredDist(int x1, int y1, int x2, int y2) {
		return MapUtils.getDistance(MapUtils.get31LatitudeY(y1), MapUtils.get31LongitudeX(x1), 
				MapUtils.get31LatitudeY(y2), MapUtils.get31LongitudeX(x2));
	}
	
	private static double squareDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = convert31YToMeters(y1, y2);
		double dx = convert31XToMeters(x1, x2);
		return dx * dx + dy * dy;
	}
	
	private static double convert31YToMeters(int y1, int y2) {
		// translate into meters 
		return (y1 - y2) * 0.01863d;
	}
	
	private static double convert31XToMeters(int x1, int x2) {
		// translate into meters 
		return (x1 - x2) * 0.011d;
	}
   
	
	private static double calculateProjection(int xA, int yA, int xB, int yB, int xC, int yC) {
		// Scalar multiplication between (AB, AC)
		double multiple = convert31XToMeters(xB, xA) * convert31XToMeters(xC, xA) + convert31YToMeters(yB, yA) * convert31YToMeters(yC, yA);
		return multiple;
	}
	
	
	
	public RouteSegment findRouteSegment(double lat, double lon, RoutingContext ctx) throws IOException {
		int zoomAround = 15;
		int coordinatesShift = (1 << (31 - zoomAround));
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		// put in map to avoid duplicate map loading
		TIntObjectHashMap<RoutingTile> ts = new TIntObjectHashMap<RoutingContext.RoutingTile>();
		// calculate box to load neighbor tiles
		RoutingTile rt = ctx.getRoutingTile(px - coordinatesShift, py - coordinatesShift);
		ts.put(rt.getId(), rt);
		rt = ctx.getRoutingTile(px + coordinatesShift, py - coordinatesShift);
		ts.put(rt.getId(), rt);
		rt = ctx.getRoutingTile(px - coordinatesShift, py + coordinatesShift);
		ts.put(rt.getId(), rt);
		rt = ctx.getRoutingTile(px + coordinatesShift, py + coordinatesShift);
		ts.put(rt.getId(), rt);
		
		List<RouteDataObject> dataObjects = new ArrayList<RouteDataObject>();
		Iterator<RoutingTile> it = ts.valueCollection().iterator();
		while(it.hasNext()){
			loadTileData(ctx, it.next(), dataObjects);
		}
		RouteSegment road = null;
		double sdist = 0; 
		int foundProjX = 0;
		int foundProjY = 0;
		
		for(RouteDataObject r : dataObjects){
			if(r.getPointsLength() > 1){
				for (int j = 1; j < r.getPointsLength(); j++) {
					double mDist = squareRootDist(r.getPoint31XTile(j), r.getPoint31YTile(j), r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1));
					int prx = r.getPoint31XTile(j);
					int pry = r.getPoint31YTile(j);
					double projection = calculateProjection(r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j),
							px, py);
					if(projection < 0){
						prx = r.getPoint31XTile(j - 1);
						pry = r.getPoint31YTile(j - 1);
					} else if(projection >= mDist * mDist){
						prx = r.getPoint31XTile(j);
						pry = r.getPoint31YTile(j);
					} else {
						prx = (int) (r.getPoint31XTile(j - 1) + (r.getPoint31XTile(j) - r.getPoint31XTile(j - 1))* (projection / (mDist *mDist)));
						pry = (int) (r.getPoint31YTile(j - 1) + (r.getPoint31YTile(j) - r.getPoint31YTile(j - 1)) * (projection / (mDist *mDist)));
					}
					double currentsDist = squareDist(prx, pry, px, py);
					if (road == null || currentsDist < sdist) {
						RouteDataObject ro = new RouteDataObject(r);
						road = new RouteSegment(ro, j);
						ro.insert(j, prx, pry);
						sdist = currentsDist;
						foundProjX = prx;
						foundProjY = pry;
					}
				}
			}
		}
		if(road != null) {
			// re-register the best road because one more point was inserted
			registerRouteDataObject(ctx, road.getRoad(), ctx.getRoutingTile(foundProjX, foundProjY));
		}
		return road;
	}
	
	
	
	// TODO TO-DO U-TURN
	// TODO fastest/shortest way
	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, RouteSegment start, RouteSegment end, boolean leftSideNavigation) throws IOException {
		// measure time
		ctx.timeToLoad = 0;
		ctx.visitedSegments = 0;
		ctx.timeToCalculate = System.nanoTime();
		if(ctx.config.initialDirection != null) {
			ctx.firstRoadId = (start.getRoad().id << 8) + start.getSegmentStart();
			double plusDir = start.getRoad().directionRoute(start.segmentStart, true);
			double diff	 = plusDir - ctx.config.initialDirection;
			if(Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
				ctx.firstRoadDirection = 1;
			} else if(Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
				ctx.firstRoadDirection = -1;
			}
			
		}

		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> segmentsComparator = new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return ctx.roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd);
			}
		};
		
		Comparator<RouteSegment> nonHeuristicSegmentsComparator = new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd, 0.5);
			}
		};

		PriorityQueue<RouteSegment> graphDirectSegments = new PriorityQueue<RouteSegment>(50, segmentsComparator);
		PriorityQueue<RouteSegment> graphReverseSegments = new PriorityQueue<RouteSegment>(50, segmentsComparator);
		
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<RouteSegment> visitedDirectSegments = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteSegment> visitedOppositeSegments = new TLongObjectHashMap<RouteSegment>();
		
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		int targetEndX = end.road.getPoint31XTile(end.segmentStart);
		int targetEndY = end.road.getPoint31YTile(end.segmentStart);
		int startX = start.road.getPoint31XTile(start.segmentStart);
		int startY = start.road.getPoint31YTile(start.segmentStart);
		float estimatedDistance = (float) h(ctx, targetEndX, targetEndY, startX, startY);
		end.distanceToEnd = start.distanceToEnd	= estimatedDistance;
		
		graphDirectSegments.add(start);
		graphReverseSegments.add(end);
		
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		boolean inverse = false;
		boolean init = false;
		
		PriorityQueue<RouteSegment>  graphSegments;
		if(inverse) {
			graphSegments = graphReverseSegments;
		} else {
			graphSegments = graphDirectSegments;
		}
		while (!graphSegments.isEmpty()) {
			RouteSegment segment = graphSegments.poll();
			ctx.visitedSegments++;
			// for debug purposes
			if (ctx.visitor != null) {
				ctx.visitor.visitSegment(segment, true);
			}
			boolean routeFound = false;
			if (!inverse) {
				routeFound = processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, targetEndX, targetEndY,
						segment, visitedOppositeSegments);
			} else {
				routeFound = processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, startX, startY, segment,
						visitedDirectSegments);
			}
			if (graphReverseSegments.isEmpty() || graphDirectSegments.isEmpty() || routeFound) {
				break;
			}
			if (!init) {
				inverse = !inverse;
				init = true;
			} else if (ctx.planRouteIn2Directions()) {
				inverse = nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) > 0;
				if (graphDirectSegments.size() * 1.3 > graphReverseSegments.size()) {
					inverse = true;
				} else if (graphDirectSegments.size() < 1.3 * graphReverseSegments.size()) {
					inverse = false;
				}
			} else {
				// different strategy : use onedirectional graph
				inverse = ctx.getPlanRoadDirection() < 0;
			}
			if (inverse) {
				graphSegments = graphReverseSegments;
			} else {
				graphSegments = graphDirectSegments;
			}

			if(ctx.runTilesGC()) {
				unloadUnusedTiles(ctx, ctx.config.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY);
			}
			if(ctx.runRelaxingStrategy() ) {
				relaxNotNeededSegments(ctx, graphDirectSegments, true);
				relaxNotNeededSegments(ctx, graphReverseSegments, false);
			}
		}
		printDebugMemoryInformation(ctx, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
		
		// 4. Route is found : collect all segments and prepare result
		return prepareResult(ctx, start, end, leftSideNavigation);
		
	}
	
	private void unloadUnusedTiles(RoutingContext ctx, int desirableSize) {
		// now delete all
		List<RoutingTile> list = new ArrayList<RoutingContext.RoutingTile>();
		TIntObjectIterator<RoutingTile> it = ctx.tiles.iterator();
		int loaded = 0;
		while(it.hasNext()) {
			it.advance();
			RoutingTile t = it.value();
			if(t.isLoaded()) {
				list.add(t);
				loaded++;
			}
			
		}
		ctx.maxLoadedTiles = Math.max(ctx.maxLoadedTiles, ctx.getCurrentlyLoadedTiles());
		Collections.sort(list, new Comparator<RoutingTile>() {
			private int pow(int base, int pw) {
				int r = 1;
				for (int i = 0; i < pw; i++) {
					r *= base;
				}
				return r;
			}
			@Override
			public int compare(RoutingTile o1, RoutingTile o2) {
				int v1 = (o1.access + 1) * pow(10, o1.getUnloadCont() -1);
				int v2 = (o2.access + 1) * pow(10, o2.getUnloadCont() -1);
				return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
			}
		});
		int toUnload = Math.max(loaded / 5, loaded - desirableSize);
		for (int i = 0; i < loaded; i++) {
			list.get(i).access = 0;
			if (i < toUnload) {
				ctx.unloadTile(list.get(i), true);
			}
		}
	}
	

	private void relaxNotNeededSegments(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments, boolean inverse) {
		RouteSegment next = graphSegments.peek();
		double mine = next.distanceToEnd;
//		int before = graphSegments.size();
//		SegmentStat statStart = new SegmentStat("Distance from start (" + inverse + ") ");
//		SegmentStat statEnd = new SegmentStat("Distance to end (" + inverse + ") ");
		Iterator<RouteSegment> iterator = graphSegments.iterator();
		while (iterator.hasNext()) {
			RouteSegment s = iterator.next();
//			statStart.addNumber((float) s.distanceFromStart);
//			statEnd.addNumber((float) s.distanceToEnd);
			if (s.distanceToEnd < mine) {
				mine = s.distanceToEnd;
			}
		}
		double d = mine * ctx.config.RELAX_NODES_IF_START_DIST_COEF;
		iterator = graphSegments.iterator();
		while (iterator.hasNext()) {
			RouteSegment s = iterator.next();
			if (s.distanceToEnd > d) {
				ctx.relaxedSegments++;
				iterator.remove();
			}
		}
//		int after = graphSegments.size();
//		println(statStart.toString());
//		println(statEnd.toString());
//		println("Relaxing : before " + before + " after " + after + " maxdiststart " + ((float) maxd) + " minend " + ((float) mine));
	}

	private double h(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		return distance / ctx.getRouter().getMaxDefaultSpeed();
	}
	
	protected static double h(RoutingContext ctx, double distToFinalPoint, RouteSegment next) {
		double result = distToFinalPoint / ctx.getRouter().getMaxDefaultSpeed();
		if(ctx.isUseDynamicRoadPrioritising() && next != null){
			double priority = ctx.getRouter().getFutureRoadPriority(next.road);
			result /= priority;
			int dist = ctx.getDynamicRoadPriorityDistance();
			// only firts 500 m count by dynamic priority
			if(distToFinalPoint > dist && dist != 0){
				result = (distToFinalPoint - dist) / ctx.getRouter().getMaxDefaultSpeed() + 
						dist / (ctx.getRouter().getMaxDefaultSpeed() * priority);
			}
		}
		return result; 
	}
	
	private void registerRouteDataObject(final RoutingContext ctx, RouteDataObject o, RoutingTile suggestedTile ) {
		if(!ctx.getRouter().acceptLine(o)){
			return;
		}
		RoutingTile tl = suggestedTile;
		RouteDataObject old = tl.idObjects.get(o.id);
		// sometimes way is present only partially in one index
		if (old != null && old.getPointsLength() >= o.getPointsLength()) {
			return;
		};
		for (int j = 0; j < o.getPointsLength(); j++) {
			int x = o.getPoint31XTile(j);
			int y = o.getPoint31YTile(j);
			if(!tl.checkContains(x, y)){
				// don't register in different tiles
				// in order to throw out tile object easily
				continue;
			}
			long l = (((long) x) << 31) + (long) y;
			RouteSegment segment = new RouteSegment(o , j);
			RouteSegment prev = tl.routes.get(l);
			boolean i = true;
			if (prev != null) {
				if (old == null) {
					segment.next = prev;
				} else if (prev.road == old) {
					segment.next = prev.next;
				} else {
					// segment somewhere in the middle replace element in linked list
					RouteSegment rr = prev;
					while (rr != null) {
						if (rr.road == old) {
							prev.next = segment;
							segment.next = rr.next;
							break;
						}
						prev = rr;
						rr = rr.next;
					}
					i = false;
				}
			}
			if (i) {
				tl.routes.put(l, segment);
				tl.idObjects.put(o.id, o);
			}
		}
	}
	
	private static void println(String logMsg) {
//		log.info(logMsg);
		System.out.println(logMsg);
	}
	
	public void printDebugMemoryInformation(RoutingContext ctx, PriorityQueue<RouteSegment> graphDirectSegments, PriorityQueue<RouteSegment> graphReverseSegments, 
			TLongObjectHashMap<RouteSegment> visitedDirectSegments,TLongObjectHashMap<RouteSegment> visitedOppositeSegments) {
		println("Time to calculate : " + (System.nanoTime() - ctx.timeToCalculate) / 1e6 + ", time to load : " + ctx.timeToLoad / 1e6);
		println("Current loaded tiles : " + ctx.getCurrentlyLoadedTiles() + ", maximum loaded tiles " + ctx.maxLoadedTiles);
		println("Loaded tiles : " + ctx.loadedTiles + ", unloaded tiles " + ctx.unloadedTiles + 
				" (distinct " + ctx.distinctUnloadedTiles.size()+") "+ ", loaded \"unloaded\" tiles "
				+ ctx.loadedPrevUnloadedTiles );
		println("Visited roads, " + ctx.visitedSegments + ", relaxed roads " + ctx.relaxedSegments);
		if (graphDirectSegments != null && graphReverseSegments != null) {
			println("Priority queues sizes : " + graphDirectSegments.size() + "/" + graphReverseSegments.size());
		}
		if (visitedDirectSegments != null && visitedOppositeSegments != null) {
			println("Visited segments sizes: " + visitedDirectSegments.size() + "/" + visitedOppositeSegments.size());
		}

	}
	
	public RoutingTile loadRoutes(final RoutingContext ctx, int tile31X, int tile31Y) {
		final RoutingTile tile = ctx.getRoutingTile(tile31X, tile31Y);
		loadTileData(ctx, tile, null);
		return tile;
	}


	private void loadTileData(final RoutingContext ctx, final RoutingTile tile, final List<RouteDataObject> toFillIn) {
		if (tile.isLoaded()) {
			tile.access++;
			if(toFillIn != null){
				for(RouteDataObject ro : tile.idObjects.valueCollection()){
					toFillIn.add(ro);
				}
			}
			return;
		}
		long now = System.nanoTime();
		ResultMatcher<RouteDataObject> matcher = new ResultMatcher<RouteDataObject>() {
			@Override
			public boolean publish(RouteDataObject o) {
				if (toFillIn != null) {
					if (ctx.getRouter().acceptLine(o)) {
						toFillIn.add(o);
					}
				}
				registerRouteDataObject(ctx, o, tile);
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		int zoomToLoad = 31 - tile.getZoom();
		int tileX = tile.getTileX();
		int tileY = tile.getTileY();
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(tileX << zoomToLoad,
				(tileX + 1) << zoomToLoad, tileY << zoomToLoad, (tileY + 1) << zoomToLoad, matcher);
		for (Entry<BinaryMapIndexReader, List<RouteSubregion>> r : map.entrySet()) {
			if(nativeLib != null) {
				for(RouteRegion reg : r.getKey().getRoutingIndexes()) {
					nativeLoadRegion(request, reg);
				}
			} else {
				try {
					r.getKey().searchRouteIndex(request, r.getValue());
				} catch (IOException e) {
					throw new RuntimeException("Loading data exception", e);
				}
			}
		}
		ctx.loadedTiles++;
		if (tile.isUnloaded()) {
			ctx.loadedPrevUnloadedTiles++;
		}
		tile.setLoaded();
		ctx.timeToLoad += (System.nanoTime() - now);
	}

	


	private void nativeLoadRegion(SearchRequest<RouteDataObject> request, RouteRegion reg) {
		boolean intersects = false;
		for(RouteSubregion sub : reg.getSubregions()) {
			if(request.intersects(sub.left, sub.top, sub.right, sub.bottom)) {
				intersects = true;
				break;
			}
		}
		if(intersects) {
			RouteDataObject[] res = nativeLib.loadRouteRegion(reg, request.getLeft(), request.getRight(), request.getTop(), request.getBottom());
			if(res != null){
				for(RouteDataObject ro : res) {
					if(ro != null) {
						request.publish(ro);
					}
				}
			}
		}
	}


	private boolean processRouteSegment(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, int targetEndX, int targetEndY,
            RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments) throws IOException {
		// Always start from segmentStart (!), not from segmentEnd
		// It makes difference only for the first start segment
		// Middle point will always be skipped from observation considering already visited
		final RouteDataObject road = segment.road;
		final int middle = segment.segmentStart;
		int middlex = road.getPoint31XTile(middle);
		int middley = road.getPoint31YTile(middle);
		double obstaclePlusTime = 0;
		double obstacleMinusTime = 0;

		// 0. mark route segment as visited
		long nt = (road.getId() << 8l) + middle;
		// avoid empty segments to connect but mark the point as visited
		visitedSegments.put(nt, null);

		int oneway = ctx.getRouter().isOneWay(road);
		boolean minusAllowed;
		boolean plusAllowed; 
		if(ctx.firstRoadId == nt) {
			minusAllowed = ctx.firstRoadDirection <= 0;
			plusAllowed = ctx.firstRoadDirection >= 0;
		} else if (!reverseWaySearch) {
			minusAllowed = oneway <= 0;
			plusAllowed = oneway >= 0;
		} else {
			minusAllowed = oneway >= 0;
			plusAllowed = oneway <= 0;
		}

		// +/- diff from middle point
		int d = plusAllowed ? 1 : -1;
		if(segment.parentRoute != null) {
			if(plusAllowed && middle < segment.getRoad().getPointsLength() - 1) {
				obstaclePlusTime = ctx.getRouter().calculateTurnTime(segment, segment.getRoad().getPointsLength() - 1,  
					segment.parentRoute, segment.parentSegmentEnd);
			}
			if(minusAllowed && middle > 0) {
				obstacleMinusTime = ctx.getRouter().calculateTurnTime(segment, 0, 
						segment.parentRoute, segment.parentSegmentEnd);
			}
		}
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on way (it doesn't take into account)
		while (minusAllowed || plusAllowed) {
			// 1. calculate point not equal to middle
			// (algorithm should visit all point on way if it is not oneway)
			int segmentEnd = middle + d;
			if (!minusAllowed && d > 0) {
				d++;
			} else if (!plusAllowed && d < 0) {
				d--;
			} else {
				if (d <= 0) {
					d = -d + 1;
				} else {
					d = -d;
				}
			}
			if (segmentEnd < 0) {
				minusAllowed = false;
				continue;
			}
			if (segmentEnd >= road.getPointsLength()) {
				plusAllowed = false;
				continue;
			}

			// if we found end point break cycle
			long nts = (road.getId() << 8l) + segmentEnd;
			visitedSegments.put(nts, segment);

			// 2. calculate point and try to load neighbor ways if they are not loaded
			int x = road.getPoint31XTile(segmentEnd);
			int y = road.getPoint31YTile(segmentEnd);
			RoutingTile tile = loadRoutes(ctx, x, y);
			
			// 2.1 calculate possible obstacle plus time
			if(d > 0){
				obstaclePlusTime +=  ctx.getRouter().defineObstacle(road, segmentEnd);
			} else if(d < 0) {
				obstacleMinusTime +=  ctx.getRouter().defineObstacle(road, segmentEnd);
			}
			
			
			long l = (((long) x) << 31) + (long) y;
			RouteSegment next = tile.routes.get(l);
			// 3. get intersected ways
			if (next != null) {
				// TO-DO U-Turn
				if((next == segment || next.road.id == road.id) && next.next == null) {
					// simplification if there is no real intersection
					continue;
				}
				// Using A* routing algorithm
				// g(x) - calculate distance to that point and calculate time
				double distOnRoadToPass = squareRootDist(x, y, middlex, middley);
				double priority = ctx.getRouter().defineSpeedPriority(road);
				double speed = ctx.getRouter().defineSpeed(road) * priority;
				if (speed == 0) {
					speed = ctx.getRouter().getMinDefaultSpeed() * priority;
				}
				
				double distStartObstacles = segment.distanceFromStart + ( d > 0? obstaclePlusTime : obstacleMinusTime) +
						 distOnRoadToPass / speed;
				
				double distToFinalPoint = squareRootDist(x, y, targetEndX, targetEndY);
				
				boolean routeFound = processIntersections(ctx, graphSegments, visitedSegments, oppositeSegments,
						distStartObstacles, distToFinalPoint, segment, segmentEnd, next, reverseWaySearch);
				if(routeFound){
					return routeFound;
				}
				
			}
		}
		return false;
	}


	private boolean proccessRestrictions(RoutingContext ctx, RouteDataObject road, RouteSegment inputNext, boolean reverseWay) {
		ctx.segmentsToVisitPrescripted.clear();
		ctx.segmentsToVisitNotForbidden.clear();
		boolean exclusiveRestriction = false;
		RouteSegment next = inputNext;
		if (!reverseWay && road.getRestrictionLength() == 0) {
			return false;
		}
		if(!ctx.getRouter().restrictionsAwayre()) {
			return false;
		}
		while (next != null) {
			int type = -1;
			if (!reverseWay) {
				for (int i = 0; i < road.getRestrictionLength(); i++) {
					if (road.getRestrictionId(i) == next.road.id) {
						type = road.getRestrictionType(i);
						break;
					}
				}
			} else {
				for (int i = 0; i < next.road.getRestrictionLength(); i++) {
					int rt = next.road.getRestrictionType(i);
					long restrictedTo = next.road.getRestrictionId(i);
					if (restrictedTo == road.id) {
						type = rt;
						break;
					}

					// Check if there is restriction only to the other than current road
					if (rt == MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN || rt == MapRenderingTypes.RESTRICTION_ONLY_LEFT_TURN
							|| rt == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
						// check if that restriction applies to considered junk
						RouteSegment foundNext = inputNext;
						while (foundNext != null) {
							if (foundNext.getRoad().id == restrictedTo) {
								break;
							}
							foundNext = foundNext.next;
						}
						if (foundNext != null) {
							type = REVERSE_WAY_RESTRICTION_ONLY; // special constant
						}
					}
				}
			}
			if (type == REVERSE_WAY_RESTRICTION_ONLY) {
				// next = next.next; continue;
			} else if (type == -1 && exclusiveRestriction) {
				// next = next.next; continue;
			} else if (type == MapRenderingTypes.RESTRICTION_NO_LEFT_TURN || type == MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN
					|| type == MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON || type == MapRenderingTypes.RESTRICTION_NO_U_TURN) {
				// next = next.next; continue;
			} else if (type == -1) {
				// case no restriction
				ctx.segmentsToVisitNotForbidden.add(next);
			} else {
				// case exclusive restriction (only_right, only_straight, ...)
				// 1. in case we are going backward we should not consider only_restriction
				// as exclusive because we have many "in" roads and one "out"
				// 2. in case we are going forward we have one "in" and many "out"
				if (!reverseWay) {
					exclusiveRestriction = true;
					ctx.segmentsToVisitNotForbidden.clear();
					ctx.segmentsToVisitPrescripted.add(next);
				} else {
					ctx.segmentsToVisitNotForbidden.add(next);
				}
			}
			next = next.next;
		}
		ctx.segmentsToVisitPrescripted.addAll(ctx.segmentsToVisitNotForbidden);
		return true;
	}
	
	


	private boolean processIntersections(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, TLongObjectHashMap<RouteSegment> oppositeSegments,  
			double distFromStart, double distToFinalPoint, 
			RouteSegment segment, int segmentEnd, RouteSegment inputNext,
			boolean reverseWay) {

		boolean thereAreRestrictions = proccessRestrictions(ctx, segment.road, inputNext, reverseWay);
		Iterator<RouteSegment> nextIterator = null;
		if (thereAreRestrictions) {
			nextIterator = ctx.segmentsToVisitPrescripted.iterator();
		}
		// Calculate possible ways to put into priority queue
		RouteSegment next = inputNext;
		boolean hasNext = nextIterator == null || nextIterator.hasNext();
		while (hasNext) {
			if(nextIterator != null) {
				next = nextIterator.next();
			}
			long nts = (next.road.getId() << 8l) + next.segmentStart;
			
			// 1. Check if opposite segment found so we can stop calculations
			if (oppositeSegments.contains(nts) && oppositeSegments.get(nts) != null) {
				// check restrictions
				RouteSegment opposite = oppositeSegments.get(nts);
				if (reverseWay) {
					ctx.finalReverseEndSegment = segmentEnd;
					ctx.finalReverseRoute = segment;
					ctx.finalDirectEndSegment = next.segmentStart;
					ctx.finalDirectRoute = opposite;
				} else {
					ctx.finalDirectEndSegment = segmentEnd;
					ctx.finalDirectRoute = segment;
					ctx.finalReverseEndSegment = next.segmentStart;
					ctx.finalReverseRoute = opposite;
				}
				return true;
			}
			// road.id could be equal on roundabout, but we should accept them
			boolean alreadyVisited = visitedSegments.contains(nts);
			if (!alreadyVisited) {
				double distanceToEnd = h(ctx, distToFinalPoint, next);
				if (next.parentRoute == null
						|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, distFromStart, distanceToEnd) > 0) {
					next.distanceFromStart = distFromStart;
					next.distanceToEnd = distanceToEnd;
					if (next.parentRoute != null) {
						// already in queue remove it
						graphSegments.remove(next);
					}
					// put additional information to recover whole route after
					next.parentRoute = segment;
					next.parentSegmentEnd = segmentEnd;
					graphSegments.add(next);
				}
				if (ctx.visitor != null) {
					ctx.visitor.visitSegment(next, false);
				}
			} else {
				// the segment was already visited! We need to follow better route if it exists
				// that is very strange situation and almost exception (it can happen when we underestimate distnceToEnd)
				if (distFromStart < next.distanceFromStart && next.road.id != segment.road.id) {
					// That code is incorrect (when segment is processed itself,
					// then it tries to make wrong u-turn) - 
					// this situation should be very carefully checked in future (seems to be fixed)
//					System.out.println(segment.getRoad().getName() + " " + next.getRoad().getName());
//					System.out.println(next.distanceFromStart + " ! " + distFromStart);
					next.distanceFromStart = distFromStart;
					next.parentRoute = segment;
					next.parentSegmentEnd = segmentEnd;
					if (ctx.visitor != null) {
						ctx.visitor.visitSegment(next, false);
					}
				}
			}
			
			// iterate to next road
			if(nextIterator == null) {
				next = next.next;
				hasNext = next != null;
			} else {
				hasNext = nextIterator.hasNext();
			}
		}
		return false;
	}
	
	/**
	 * Helper method to prepare final result 
	 */
	private List<RouteSegmentResult> prepareResult(RoutingContext ctx, RouteSegment start, RouteSegment end, boolean leftside) {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();

		RouteSegment segment = ctx.finalReverseRoute;
		int parentSegmentStart = ctx.finalReverseEndSegment;
		while (segment != null) {
			RouteSegmentResult res = new RouteSegmentResult(segment.road, parentSegmentStart, segment.segmentStart);
			parentSegmentStart = segment.parentSegmentEnd;
			segment = segment.parentRoute;
			result.add(res);
		}
		Collections.reverse(result);

		segment = ctx.finalDirectRoute;
		int parentSegmentEnd = ctx.finalDirectEndSegment;
		while (segment != null) {
			RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.segmentStart, parentSegmentEnd);
			parentSegmentEnd = segment.parentSegmentEnd;
			segment = segment.parentRoute;
			result.add(res);
		}
		Collections.reverse(result);

		// calculate time
		float completeTime = 0;
		float completeDistance = 0;
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteDataObject road = rr.getObject();
			double distOnRoadToPass = 0;
			double speed = ctx.getRouter().defineSpeed(road);
			if (speed == 0) {
				speed = ctx.getRouter().getMinDefaultSpeed();
			}
			boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int next;
			double distance = 0;
			for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
				next = plus ? j + 1 : j - 1;
				if(j == rr.getStartPointIndex()) {
					attachRoadSegments(ctx, result, i, j, plus);
				}
				if(next != rr.getEndPointIndex()) {
					attachRoadSegments(ctx, result, i, next, plus);
				}
				
				double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next),
						road.getPoint31YTile(next));
				distance += d;
				distOnRoadToPass += d / speed + ctx.getRouter().defineObstacle(road, j);
				
				List<RouteSegmentResult> attachedRoutes = rr.getAttachedRoutes(next);
				if (next != rr.getEndPointIndex() && !rr.getObject().roundabout() && attachedRoutes != null) {
					float before = rr.getBearing(next, !plus);
					float after = rr.getBearing(next, plus);
					boolean straight = Math.abs(MapUtils.degreesDiff(before + 180, after)) < 70;
					boolean split = false;
					// split if needed
					for (RouteSegmentResult rs : attachedRoutes) {
						double diff = MapUtils.degreesDiff(before + 180, rs.getBearingBegin());
						if (Math.abs(diff) < 50) {
							split = true;
						} else if (!straight && Math.abs(diff) < 100) {
							split = true;
						}
					}
					if (split) {
						int endPointIndex = rr.getEndPointIndex();
						RouteSegmentResult splitted = new RouteSegmentResult(rr.getObject(), next, endPointIndex);
						rr.setSegmentTime((float) distOnRoadToPass);
						rr.setSegmentSpeed((float) speed);
						rr.setDistance((float) distance);
						rr.setEndPointIndex(next);

						result.add(i + 1, splitted);
						completeTime += distOnRoadToPass;
						completeDistance += distance;
						// switch current segment to the splitted
						rr = splitted;
						distOnRoadToPass = 0;
						distance = 0;
					}
				}
			}
			// last point turn time can be added
			// if(i + 1 < result.size()) { distOnRoadToPass += ctx.getRouter().calculateTurnTime(); }
			rr.setSegmentTime((float) distOnRoadToPass);
			rr.setSegmentSpeed((float) speed);
			rr.setDistance((float) distance);

			completeTime += distOnRoadToPass;
			completeDistance += distance;
		}
		addTurnInfo(leftside, result);

		println("ROUTE : ");
		double startLat = MapUtils.get31LatitudeY(start.road.getPoint31YTile(start.segmentStart));
		double startLon = MapUtils.get31LongitudeX(start.road.getPoint31XTile(start.segmentStart));
		double endLat = MapUtils.get31LatitudeY(end.road.getPoint31YTile(end.segmentStart));
		double endLon = MapUtils.get31LongitudeX(end.road.getPoint31XTile(end.segmentStart));
		StringBuilder add = new StringBuilder();
		add.append("loadedTiles = \"").append(ctx.loadedTiles).append("\" ");
		add.append("visitedSegments = \"").append(ctx.visitedSegments).append("\" ");
		add.append("complete_distance = \"").append(completeDistance).append("\" ");
		println(MessageFormat.format("<test regions=\"\" description=\"\" best_percent=\"\" vehicle=\"{5}\" \n"
				+ "    start_lat=\"{0}\" start_lon=\"{1}\" target_lat=\"{2}\" target_lon=\"{3}\" complete_time=\"{4}\" {6} >", startLat
				+ "", startLon + "", endLat + "", endLon + "", completeTime + "", ctx.config.routerName, add.toString()));
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
			for (RouteSegmentResult res : result) {
				String name = res.getObject().getName();
				String ref = res.getObject().getRef();
				if (name == null) {
					name = "";
				}
				if (ref != null) {
					name += " (" + ref + ") ";
				}
				StringBuilder additional = new StringBuilder();
				additional.append("time = \"").append(res.getSegmentTime()).append("\" ");
				additional.append("name = \"").append(name).append("\" ");
				additional.append("distance = \"").append(res.getDistance()).append("\" ");
				if (res.getTurnType() != null) {
					additional.append("turn = \"").append(res.getTurnType()).append("\" ");
					if (res.getTurnType().getLanes() != null) {
						additional.append("lanes = \"").append(Arrays.toString(res.getTurnType().getLanes())).append("\" ");
					}
				}
				additional.append("start_bearing = \"").append(res.getBearingBegin()).append("\" ");
				additional.append("end_bearing = \"").append(res.getBearingEnd()).append("\" ");
				additional.append("description = \"").append(res.getDescription()).append("\" ");
				println(MessageFormat.format("\t<segment id=\"{0}\" start=\"{1}\" end=\"{2}\" {3}/>", (res.getObject().getId()) + "",
						res.getStartPointIndex() + "", res.getEndPointIndex() + "", additional.toString()));
			}
		}
		println("</test>");
		return result;
	}


	private void addTurnInfo(boolean leftside, List<RouteSegmentResult> result) {
		int prevSegment = -1;
		float dist = 0;
		for (int i = 0; i <= result.size(); i++) {
			TurnType t = null;
			if (i < result.size()) {
				t = getTurnInfo(result, i, leftside);
				result.get(i).setTurnType(t);
			}
			if (t != null || i == result.size()) {
				if (prevSegment >= 0) {
					String turn = result.get(prevSegment).getTurnType().toString();
					if (result.get(prevSegment).getTurnType().getLanes() != null) {
						turn += Arrays.toString(result.get(prevSegment).getTurnType().getLanes());
					}
					result.get(prevSegment).setDescription(turn + String.format(" and go %.2f meters", dist));
					if(result.get(prevSegment).getTurnType().isSkipToSpeak()) {
						result.get(prevSegment).setDescription(result.get(prevSegment).getDescription() +" (*)");
					}
				}
				prevSegment = i;
				dist = 0;
			}
			if (i < result.size()) {
				dist += result.get(i).getDistance();
			}
		}
	}
	
	private boolean highwayLowEnd(String highway) {
		if (highway == null || highway.endsWith("_link") || highway.endsWith("services") || highway.endsWith("service")
				|| highway.endsWith("unclassified") || highway.endsWith("road")) {
			return true;
		}
		return false;
	}


	private TurnType getTurnInfo(List<RouteSegmentResult> result, int i, boolean leftSide) {
		if (i == 0) {
			return TurnType.valueOf(TurnType.C, false);
		}
		RouteSegmentResult prev = result.get(i - 1) ;
		if(prev.getObject().roundabout()) {
			return null;
		}
		RouteSegmentResult rr = result.get(i);
		if (rr.getObject().roundabout()) {
			int exit = 1;
			RouteSegmentResult last = rr;
			for (int j = i; j < result.size(); j++) {
				RouteSegmentResult rnext = result.get(j);
				if (rnext.getObject().roundabout()) {
					last = rnext;
					boolean plus = rnext.getStartPointIndex() < rnext.getEndPointIndex();
					int k = rnext.getStartPointIndex();
					if (j == i) {
						k = plus ? k + 1 : k - 1;
					}
					while (k != rnext.getEndPointIndex()) {
						if (rnext.getAttachedRoutes(k).size() > 0) {
							exit++;
						}
						k = plus ? k + 1 : k - 1;
					}
				} else {
					break;
				}
			}
			// combine all roundabouts
			TurnType t = TurnType.valueOf("EXIT"+exit, leftSide);
			t.setTurnAngle((float) MapUtils.degreesDiff(last.getBearingBegin(), prev.getBearingEnd()));
			return t;
		}
		TurnType t = null;
		if (prev != null) {
			// add description about turn
			double mpi = MapUtils.degreesDiff(prev.getBearingEnd(), rr.getBearingBegin());
			int[] lanes =  null;
			if (mpi >= 50) {
				if (mpi < 60) {
					t = TurnType.valueOf(TurnType.TSLL, leftSide);
				} else if (mpi < 120) {
					t = TurnType.valueOf(TurnType.TL, leftSide);
				} else if (mpi < 135) {
					t = TurnType.valueOf(TurnType.TSHL, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TU, leftSide);
				}
			} else if (mpi < -50) {
				if (mpi > -60) {
					t = TurnType.valueOf(TurnType.TSLR, leftSide);
				} else if (mpi > -120) {
					t = TurnType.valueOf(TurnType.TR, leftSide);
				} else if (mpi > -135) {
					t = TurnType.valueOf(TurnType.TSHR, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TU, leftSide);
				}
			} else {
				// keep left/right
				boolean kl = false;
				boolean kr = false;
				List<RouteSegmentResult> attachedRoutes = rr.getAttachedRoutes(rr.getStartPointIndex());
				int ls = prev.getObject().getLanes();
				int left = 0;
				int right = 0;
				boolean speak = highwayLowEnd(prev.getObject().getHighway()) || highwayLowEnd(rr.getObject().getHighway());
				if(attachedRoutes != null){
					for(RouteSegmentResult rs : attachedRoutes){
						double ex = MapUtils.degreesDiff(rs.getBearingBegin(), rr.getBearingBegin());
						if(ex < 60 && ex >= 0) {
							kl = true;
							int lns = rs.getObject().getLanes();
							if (lns > 0) {
								right += lns;
							}
							speak = speak  || !highwayLowEnd(rs.getObject().getHighway());
						} else if(ex > -60 && ex <= 0) {
							kr = true;
							int lns = rs.getObject().getLanes();
							if (lns > 0) {
								left += lns;
							}
							speak = speak  || !highwayLowEnd(rs.getObject().getHighway());
						}
					}
				}
				if(kr && left == 0) {
					left = 1;
				} else if(kl && right == 0) {
					right = 1;
				}
				int current = rr.getObject().getLanes();
				if (current <= 0) {
					current = 1;
				}
				if(ls >= 0 /*&& current + left + right >= ls*/){
					lanes = new int[current + left + right];
					ls = current + left + right;
					for(int it=0; it< ls; it++) {
						if(it < left || it >= left + current) {
							lanes[it] = 0;
						} else {
							lanes[it] = 1;
						}
					}
				}
				
				if (kl) {
					t = TurnType.valueOf(TurnType.KL, leftSide);
					t.setSkipToSpeak(!speak);
				} else if(kr){
					t = TurnType.valueOf(TurnType.KR, leftSide);
					t.setSkipToSpeak(!speak);
				}
			}
			if(t != null) {
				t.setTurnAngle((float) -mpi);
				
				if (lanes != null) {
					t.setLanes(lanes);
				}
			}
			
		}
		return t;
	}


	private void attachRoadSegments(RoutingContext ctx, List<RouteSegmentResult> result, int routeInd, int pointInd, boolean plus) {
		RouteSegmentResult rr = result.get(routeInd);
		RouteDataObject road = rr.getObject();
		RoutingTile tl = loadRoutes(ctx, road.getPoint31XTile(pointInd), road.getPoint31YTile(pointInd));
		long l = (((long) road.getPoint31XTile(pointInd)) << 31) + (long) road.getPoint31YTile(pointInd);
		
		// attach additional roads to represent more information about the route
		RouteSegmentResult previousResult = null;
		
		// by default make same as this road id
		long previousRoadId = road.getId();
		if (pointInd == rr.getStartPointIndex() && routeInd > 0) {
			previousResult = result.get(routeInd - 1);
			previousRoadId = previousResult.getObject().getId();
			if (previousRoadId != road.getId()) {
				if (previousResult.getStartPointIndex() < previousResult.getEndPointIndex()
						&& previousResult.getEndPointIndex() < previousResult.getObject().getPointsLength() - 1) {
					rr.attachRoute(pointInd, new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(),
							previousResult.getObject().getPointsLength() - 1));
				} else if (previousResult.getStartPointIndex() > previousResult.getEndPointIndex() 
						&& previousResult.getEndPointIndex() > 0) {
					rr.attachRoute(pointInd, new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(), 0));
				}
			}
		}
		RouteSegment routeSegment = tl.getLoadedRoutes().get(l);
		// try to attach all segments except with current id
		while (routeSegment != null) {
			if (routeSegment.road.getId() != road.getId() && routeSegment.road.getId() != previousRoadId) {
				RouteDataObject addRoad = routeSegment.road;
				// TODO restrictions can be considered as well
				int oneWay = ctx.getRouter().isOneWay(addRoad);
				if (oneWay >= 0 && routeSegment.segmentStart < addRoad.getPointsLength() - 1) {
					rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.segmentStart, addRoad.getPointsLength() - 1));
				}
				if (oneWay <= 0 && routeSegment.segmentStart > 0) {
					rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.segmentStart, 0));
				}
			}
			routeSegment = routeSegment.next;
		}
	}
	
	
	/*public */static int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient ) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, 
				o2DistanceFromStart + heuristicCoefficient *  o2DistanceToEnd);
	}

	
	public interface RouteSegmentVisitor {
		
		public void visitSegment(RouteSegment segment, boolean poll);
	}
	
	public static class RouteSegment {
		final int segmentStart;
		final RouteDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		
		// search context (needed for searching route)
		// Initially it should be null (!) because it checks was it segment visited before
		RouteSegment parentRoute = null;
		int parentSegmentEnd = 0;
		
		// distance measured in time (seconds)
		double distanceFromStart = 0;
		double distanceToEnd = 0;
		
		public RouteSegment(RouteDataObject road, int segmentStart) {
			this.road = road;
			this.segmentStart = segmentStart;
		}
		
		public RouteSegment getNext() {
			return next;
		}
		
		public int getSegmentStart() {
			return segmentStart;
		}
		
		public RouteDataObject getRoad() {
			return road;
		}
		
		public String getTestName(){
			return String.format("s%.2f e%.2f", ((float)distanceFromStart), ((float)distanceToEnd));
		}
		
	}
	

	public static class SegmentStat {
		String name;
		Set<Float> set = new TreeSet<Float>();

		public SegmentStat(String name) {
			this.name = name;
		}

		void addNumber(float v) {
			set.add(v);
		}

		@Override
		public String toString() {
			int segmentation = 7;
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(" (").append(set.size()).append(") : ");
			float s = set.size() / ((float) segmentation);
			int k = 0, number = 0;
			float limit = 0, value = 0;
			Iterator<Float> it = set.iterator();
			while (it.hasNext()) {
				k++;
				number++;
				value += it.next();
				if (k >= limit) {
					limit += s;
					sb.append(value / number).append(" ");
					number = 0;
					value = 0;
				}
			}
			if(number > 0) {
				sb.append(value / number).append(" ");
			}
			return sb.toString();
		}

	}
	
}
