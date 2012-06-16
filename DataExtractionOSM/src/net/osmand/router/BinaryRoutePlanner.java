package net.osmand.router;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
	
	private final static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
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
	// TODO TO-DO ADD-INFO 


	// TODO write unit tests
	// TODO add information about turns (?) - probably calculate additional information to show on map
	// TODO fix roundabout (?) - probably calculate additional information to show on map
	// TODO access
	// TODO fastest/shortest way
	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, RouteSegment start, RouteSegment end) throws IOException {
		// measure time
		ctx.timeToLoad = 0;
		ctx.visitedSegments = 0;
		ctx.timeToCalculate = System.nanoTime();

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
			System.out.println(segment.getRoad().getId() + " " + segment.segmentStart);
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
			if(ctx.runRelaxingStrategy()) {
				relaxNotNeededSegments(ctx, graphDirectSegments, true);
				relaxNotNeededSegments(ctx, graphReverseSegments, false);
			}
		}
		printDebugMemoryInformation(ctx, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
		
		// 4. Route is found : collect all segments and prepare result
		return prepareResult(ctx, start, end);
		
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
		double d = mine * ctx.config.ITERATIONS_TO_RELAX_NODES;
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
		if (tile.isLoaded()) {
			tile.access++;
			return tile;
		}
		loadTileData(ctx, tile, null);
		return tile;
	}


	private void loadTileData(final RoutingContext ctx, final RoutingTile tile, final List<RouteDataObject> toFillIn) {
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
		if (!reverseWaySearch) {
			minusAllowed = oneway <= 0;
			plusAllowed = oneway >= 0;
		} else {
			minusAllowed = oneway >= 0;
			plusAllowed = oneway <= 0;
		}

		// +/- diff from middle point
		int d = plusAllowed ? 1 : -1;
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
				double distanceFromStart = segment.distanceFromStart + distOnRoadToPass / speed;
				distanceFromStart += d > 0? obstaclePlusTime : obstacleMinusTime;
				
				double distToFinalPoint = squareRootDist(x, y, targetEndX, targetEndY);
				
				boolean routeFound = processIntersections(ctx, graphSegments, visitedSegments, oppositeSegments,
						distanceFromStart, distToFinalPoint, segment, segmentEnd, next, reverseWaySearch);
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
		if (!reverseWay && road.getRestrictionLength() > 0) {
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
				RouteSegment opposite = oppositeSegments.get(nts);
				if(reverseWay){
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
			// Calculate complete distance from start
			double gDistFromStart = distFromStart + ctx.getRouter().calculateTurnTime(segment, next, segmentEnd);
			
			// road.id could be equal on roundabout, but we should accept them
			boolean alreadyVisited = visitedSegments.contains(nts);
			if (!alreadyVisited) {
				double distanceToEnd = h(ctx, distToFinalPoint, next);
				if (next.parentRoute == null
						|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, gDistFromStart, distanceToEnd) > 0) {
					next.distanceFromStart = gDistFromStart;
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
				if (gDistFromStart < next.distanceFromStart) {
					// That code is incorrect (when segment is processed itself,
					// then it tries to make wrong u-turn) - 
					// this situation should be very carefully checked in future
//					next.distanceFromStart = gDistFromStart;
//					next.parentRoute = segment;
//					next.parentSegmentEnd = segmentEnd;
//					if (ctx.visitor != null) {
//						ctx.visitor.visitSegment(next, false);
//					}
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
	private List<RouteSegmentResult> prepareResult(RoutingContext ctx, RouteSegment start, RouteSegment end) {
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
			for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
				next = plus ? j + 1 : j - 1;
				double d = squareRootDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next),
						road.getPoint31YTile(next));
				distOnRoadToPass += d / speed + ctx.getRouter().defineObstacle(road, j);
			}
			// last point turn time can be added
			// if(i + 1 < result.size()) { distOnRoadToPass += ctx.getRouter().calculateTurnTime(); }
			rr.setSegmentTime((float) distOnRoadToPass);
			completeTime += distOnRoadToPass;
		}
		
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
			println("ROUTE : ");
			double startLat = MapUtils.get31LatitudeY(start.road.getPoint31YTile(start.segmentStart));
			double startLon = MapUtils.get31LongitudeX(start.road.getPoint31XTile(start.segmentStart));
			double endLat = MapUtils.get31LatitudeY(end.road.getPoint31YTile(end.segmentStart));
			double endLon = MapUtils.get31LongitudeX(end.road.getPoint31XTile(end.segmentStart));
			println(MessageFormat.format("<test regions=\"\" description=\"\" best_percent=\"\" vehicle=\"{5}\" \n" +
					"    start_lat=\"{0}\" start_lon=\"{1}\" target_lat=\"{2}\" target_lon=\"{3}\" complete_time=\"{4}\">", 
					startLat+"", startLon+"", endLat+"", endLon+"", completeTime+"", ctx.config.routerName));
			for (RouteSegmentResult res : result) {
				String name = "Unknown";//res.object.getName();
				String ref = "";//res.object.getNameByType(res.object.getMapIndex().refEncodingType);
				if(ref != null) {
					name += " " + ref;
				}
				println(MessageFormat.format("\t<segment id=\"{0}\" start=\"{1}\" end=\"{2}\" time=\"{4}\" name=\"{3}\"/>", 
						(res.getObject().getId())+"", res.getStartPointIndex()+"", res.getEndPointIndex()+"", name, res.getSegmentTime()));
			}
			println("</test>");
		}
		return result;
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
