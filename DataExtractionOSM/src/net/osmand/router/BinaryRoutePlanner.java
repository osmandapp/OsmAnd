package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

public class BinaryRoutePlanner {
	
	private final static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	private final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	private final BinaryMapIndexReader[] map;
	
	
	
	private static final Log log = LogUtil.getLog(BinaryRoutePlanner.class);
	
	public BinaryRoutePlanner(BinaryMapIndexReader... map){
		this.map = map;
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
	
	

   
	public void loadRoutes(final RoutingContext ctx, int tileX, int tileY) throws IOException {
		int tileC = (tileX << ctx.getZoomToLoadTileWithRoads()) + tileY;
		if(ctx.loadedTiles.contains(tileC)){
			return;
		}
		long now = System.nanoTime();
		
		int zoomToLoad = 31 - ctx.getZoomToLoadTileWithRoads();
		SearchFilter searchFilter = new BinaryMapIndexReader.SearchFilter(){
			@Override
			public boolean accept(TIntArrayList types, MapIndex index) {
				for (int j = 0; j < types.size(); j++) {
					int wholeType = types.get(j);
					TagValuePair pair = index.decodeType(wholeType);
					if (pair != null) {
						int t = wholeType & 3;
						if(t == MapRenderingTypes.POINT_TYPE){
							if(ctx.getRouter().acceptPoint(pair)){
								return true;
							}
						} else if(t == MapRenderingTypes.POLYLINE_TYPE){
							if(ctx.getRouter().acceptLine(pair)){
								return true;
							}
						}
					}
				}
				return false;
			}
		};
		SearchRequest<BinaryMapDataObject> request = BinaryMapIndexReader.buildSearchRequest(tileX << zoomToLoad,
				(tileX + 1) << zoomToLoad, tileY << zoomToLoad, 
				(tileY + 1) << zoomToLoad, 15, searchFilter);
		for (BinaryMapIndexReader r : map) {
			r.searchMapIndex(request);
			for (BinaryMapDataObject o : request.getSearchResults()) {
				BinaryMapDataObject old = ctx.idObjects.get(o.getId());
				// sometimes way are presented only partially in one index
				if (old != null && old.getPointsLength() >= o.getPointsLength()) {
					continue;
				}
				ctx.idObjects.put(o.getId(), o);
				for (int j = 0; j < o.getPointsLength(); j++) {
					long l = (((long) o.getPoint31XTile(j)) << 31) + (long) o.getPoint31YTile(j);
					RouteSegment segment = new RouteSegment();
					segment.road = o;
					segment.segmentEnd = segment.segmentStart = j;
					if (ctx.routes.get(l) != null) {
						segment.next = ctx.routes.get(l);
					}
					ctx.routes.put(l, segment);
				}
			}
			ctx.loadedTiles.add(tileC);
			ctx.timeToLoad += (System.nanoTime() - now);
		}
	}
	
	// calculate distance from C to AB (distnace doesn't calculate 
	private static double calculateDistance(int xA, int yA, int xB, int yB, int xC, int yC, double distAB){
	    	// Scalar multiplication between (AB', AC)
	    	double multiple = (-convert31YToMeters(yB, yA)) * convert31XToMeters(xC,xA) + convert31XToMeters(xB,xA) * convert31YToMeters(yC, yA);
	    	return multiple / distAB;
	}
    
	    // calculate square distance from C to AB (distance doesn't calculate 
	private static double calculatesquareDistance(int xA, int yA, int xB, int yB, int xC, int yC, double distAB) {
    	// Scalar multiplication between (AB', AC)
		double multiple = (-convert31YToMeters(yB, yA)) * convert31XToMeters(xC,xA) + convert31XToMeters(xB,xA) * convert31YToMeters(yC, yA);
		return (multiple * multiple) / (distAB*distAB);
	}
	
	private static double calculateProjection(int xA, int yA, int xB, int yB, int xC, int yC, double distAB) {
		// Scalar multiplication between (AB, AC)
		double multiple = convert31XToMeters(xB, xA) * convert31XToMeters(xC, xA) + convert31YToMeters(yB, yA) * convert31YToMeters(yC, yA);
		return multiple / distAB;
	}
	
	
	public RouteSegment findRouteSegment(double lat, double lon, RoutingContext ctx) throws IOException {
		double tileX = MapUtils.getTileNumberX(ctx.getZoomToLoadTileWithRoads(), lon);
		double tileY = MapUtils.getTileNumberY(ctx.getZoomToLoadTileWithRoads(), lat);
		loadRoutes(ctx, (int) tileX , (int) tileY);
		
		RouteSegment road = null;
		double sdist = 0; 
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		for(BinaryMapDataObject r : ctx.values()){
			if(r.getPointsLength() > 1){
				double priority = ctx.getRouter().getRoadPriorityToCalculateRoute(r);
				for (int j = 1; j < r.getPointsLength(); j++) {
					double mDist = squareRootDist(r.getPoint31XTile(j), r.getPoint31YTile(j), r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1));
					double projection = calculateProjection(r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j),
							px, py, mDist);
					double currentsDist;
					if(projection < 0){//TODO: first 2 and last 2 points of a route should be only near and not based on road priority (I.E. a motorway road node unreachable near my house)
						currentsDist = squareDist(r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1), px, py) / (priority * priority);
					} else if(projection > mDist){
						currentsDist = squareDist(r.getPoint31XTile(j), r.getPoint31YTile(j), px, py) / (priority * priority);
					} else {
						currentsDist = calculatesquareDistance(r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j),
								px, py, mDist) / (priority * priority);
					}
					
					if (road == null || currentsDist < sdist) {
						road = new RouteSegment();
						road.road = r;
						road.segmentStart = j - 1;//TODO: first 2 and last 2 segments should be based on projection. my start/finish point S/F, fake point P between j-1 & j -> SP, PJ; should end at finish point: JP,PF

						road.segmentEnd = j;
						sdist = currentsDist;
					}
				}
			}
		}
		return road;
	}
	
	
	
	// TODO write unit tests
	// TODO add information about turns (?) - probably calculate additional information to show on map
	// TODO think about u-turn
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
		long startNanoTime = System.nanoTime();

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
		
		int targetEndX = end.road.getPoint31XTile(end.segmentStart);
		int targetEndY = end.road.getPoint31YTile(end.segmentStart);
		int startX = start.road.getPoint31XTile(start.segmentStart);
		int startY = start.road.getPoint31YTile(start.segmentStart);
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		start.distanceToEnd = h(ctx, targetEndX, targetEndY, startX, startY);
		end.distanceToEnd = start.distanceToEnd;
		
		// because first point of the start is not visited do the same as in cycle but only for one point
		// it matters when start point is intersection of different roads
		// add start segment to priority queue
		visitAllStartSegments(ctx, start, graphDirectSegments, visitedDirectSegments, startX, startY);
		visitAllStartSegments(ctx, end, graphReverseSegments, visitedOppositeSegments, targetEndX, targetEndY);
		
		// final segment before end
		RouteSegment finalDirectRoute = null;
		RouteSegment finalReverseRoute = null;
		
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		boolean inverse = false;
		
		PriorityQueue<RouteSegment>  graphSegments = inverse ? graphReverseSegments : graphDirectSegments;
		while(!graphSegments.isEmpty()){
			RouteSegment segment = graphSegments.poll();
			
			ctx.visitedSegments ++;
			// for debug purposes
			if (ctx.visitor != null) {
				ctx.visitor.visitSegment(segment);
			}
			if (!inverse) {
				RoutePair pair = processRouteSegment(ctx, end, false, graphDirectSegments, visitedDirectSegments, targetEndX,
						targetEndY, segment, visitedOppositeSegments);
				if (pair != null) {
					finalDirectRoute = pair.a;
					finalReverseRoute = pair.b;
					break;
				}
			} else {
				RoutePair pair = processRouteSegment(ctx, start, true, graphReverseSegments, visitedOppositeSegments, startX,
						startY, segment, visitedDirectSegments);
				if (pair != null) {
					finalReverseRoute = pair.a;
					finalDirectRoute = pair.b;
					break;
				}
			}
			if(graphReverseSegments.isEmpty() || graphDirectSegments.isEmpty()){
				break;
			}
			if(ctx.planRouteIn2Directions()){
				inverse = nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) > 0;
				if (graphDirectSegments.size() * 1.3 > graphReverseSegments.size()) {
					inverse = true;
				} else 	if (graphDirectSegments.size() < 1.3 * graphReverseSegments.size()) {
					inverse = false;
				}
				// make it more simmetrical with dynamic prioritizing it makes big sense
//				inverse = !inverse;
			} else {
				// different strategy : use onedirectional graph
				inverse = !ctx.getPlanRoadDirection().booleanValue();
			}
			graphSegments = inverse ? graphReverseSegments : graphDirectSegments;
		}
		
		
		// 4. Route is found : collect all segments and prepare result
		return prepareResult(ctx, start, end, startNanoTime, finalDirectRoute, finalReverseRoute);
		
	}


	private double h(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		//TODO add possible turn time and barrier according the distance
		return distance / ctx.getRouter().getMaxDefaultSpeed();
	}
	
	protected static double h(RoutingContext ctx, double distToFinalPoint, RouteSegment actual, RouteSegment next) {
		double result = distToFinalPoint / ctx.getRouter().getMaxDefaultSpeed();
		if(ctx.isUseDynamicRoadPrioritising() && next != null){
			double priority = ctx.getRouter().getRoadPriorityToCalculateRoute(next.road);
			result /= priority;
		}
		//TODO add possible turn time and barrier according the distance
		return result; 
	}
	
	private double g(RoutingContext ctx, double distOnRoadToPass,
			RouteSegment segment, int segmentEnd, double obstaclesTime,
			RouteSegment next, double speed) {
		double result = segment.distanceFromStart + distOnRoadToPass / speed;
		// calculate turn time
		result += ctx.getRouter().calculateTurnTime(segment, next, segmentEnd);
		// add obstacles time
		result += obstaclesTime;
		return result;
	}

	private void visitAllStartSegments(final RoutingContext ctx, RouteSegment start, PriorityQueue<RouteSegment> graphDirectSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, int startX, int startY) throws IOException {
		// mark as visited code seems to be duplicated
		long nt = (start.road.getId() << 8l) + start.segmentStart;
		visitedSegments.put(nt, start);
		graphDirectSegments.add(start);
		
		loadRoutes(ctx, (startX >> (31 - ctx.getZoomToLoadTileWithRoads())), (startY >> (31 - ctx.getZoomToLoadTileWithRoads())));
		long ls = (((long) startX) << 31) + (long) startY;
		RouteSegment startNbs = ctx.routes.get(ls);
		while(startNbs != null) { // startNbs.road.id >> 1, start.road.id >> 1
			if(startNbs.road.getId() != start.road.getId()){
				startNbs.parentRoute = start;
				startNbs.parentSegmentEnd = start.segmentStart;
				startNbs.distanceToEnd = start.distanceToEnd;

				// duplicated to be sure start is added
				nt = (startNbs.road.getId() << 8l) + startNbs.segmentStart;
				visitedSegments.put(nt, startNbs);
				graphDirectSegments.add(startNbs);
			}
			startNbs = startNbs.next;
		}
	}

	private RoutePair processRouteSegment(final RoutingContext ctx, RouteSegment end, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, int targetEndX, int targetEndY,
            RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments) throws IOException {
		// Always start from segmentStart (!), not from segmentEnd
		// It makes difference only for the first start segment
		// Middle point will always be skipped from observation considering already visited
		final BinaryMapDataObject road = segment.road;
		final int middle = segment.segmentStart;
		int middlex = road.getPoint31XTile(middle);
		int middley = road.getPoint31YTile(middle);

		// 0. mark route segment as visited
		long nt = (road.getId() << 8l) + middle;
		// avoid empty segments to connect but mark the point as visited
		visitedSegments.put(nt, null);
		if (oppositeSegments.contains(nt) && oppositeSegments.get(nt) != null) {
			segment.segmentEnd = middle;
			RouteSegment opposite = oppositeSegments.get(nt);
			opposite.segmentEnd = middle;
			return new RoutePair(segment, opposite);
		}

		boolean oneway = ctx.getRouter().isOneWay(road);
		boolean minusAllowed = !oneway || reverseWaySearch;
		boolean plusAllowed = !oneway || !reverseWaySearch;

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
			if (oppositeSegments.contains(nts) && oppositeSegments.get(nt) != null) {
				segment.segmentEnd = segmentEnd;
				RouteSegment opposite = oppositeSegments.get(nts);
				opposite.segmentEnd = segmentEnd;
				return new RoutePair(segment, opposite);
			}
			visitedSegments.put(nts, segment);

			// 2. calculate point and try to load neighbor ways if they are not loaded
			int x = road.getPoint31XTile(segmentEnd);
			int y = road.getPoint31YTile(segmentEnd);
			loadRoutes(ctx, (x >> (31 - ctx.getZoomToLoadTileWithRoads())), (y >> (31 - ctx.getZoomToLoadTileWithRoads())));
			long l = (((long) x) << 31) + (long) y;
			RouteSegment next = ctx.routes.get(l);

			// 3. get intersected ways
			if (next != null) {
				double distOnRoadToPass = squareRootDist(x, y, middlex, middley);
				double distToFinalPoint = squareRootDist(x, y, targetEndX, targetEndY);
				RouteSegment foundIntersection = processIntersectionsWithWays(ctx, graphSegments, visitedSegments, oppositeSegments,
						distOnRoadToPass, distToFinalPoint, segment, road,
						d == 0, segmentEnd, next, reverseWaySearch);
				if(foundIntersection != null){
					segment.segmentEnd = segmentEnd;
					return new RoutePair(segment, foundIntersection);
				}
			}
		}
		return null;
	}
	
	


	private RouteSegment processIntersectionsWithWays(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, TLongObjectHashMap<RouteSegment> oppositeSegments,  
			double distOnRoadToPass, double distToFinalPoint, 
			RouteSegment segment, BinaryMapDataObject road, boolean firstOfSegment, int segmentEnd, RouteSegment inputNext,
			boolean reverseWay) {

		// This variables can be in routing context
		// initialize temporary lists to calculate not forbidden ways at way intersections
		ArrayList<RouteSegment> segmentsToVisitPrescripted = new ArrayList<RouteSegment>(5);
		ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<RouteSegment>(5);
		// collect time for obstacles
		double obstaclesTime = 0;
		boolean exclusiveRestriction = false;

		// 3.1 calculate time for obstacles (bumps, traffic_signals, level_crossing)
		if (firstOfSegment) {
			RouteSegment possibleObstacle = inputNext;
			while (possibleObstacle != null) {
				obstaclesTime += ctx.getRouter().defineObstacle(possibleObstacle.road, possibleObstacle.segmentStart);
				possibleObstacle = possibleObstacle.next;
			}
		}

		// 3.2 calculate possible ways to put into priority queue
		// for debug next.road.getId() >> 1 == 33911427 && road.getId() >> 1 == 33911442
		RouteSegment next = inputNext;
		while (next != null) {
			long nts = (next.road.getId() << 8l) + next.segmentStart;
			boolean oppositeConnectionFound = oppositeSegments.containsKey(nts) && oppositeSegments.get(nts) != null;
			
			boolean processRoad = true;
			if (ctx.isUseStrategyOfIncreasingRoadPriorities()) {
				double roadPriority = ctx.getRouter().getRoadPriorityHeuristicToIncrease(segment.road);
				double nextRoadPriority = ctx.getRouter().getRoadPriorityHeuristicToIncrease(segment.road);
				if (nextRoadPriority < roadPriority) {
					processRoad = false;
				}
			} 
			
			/* next.road.getId() >> 1 (3) != road.getId() >> 1 (3) - used that line for debug with osm map */
			// road.id could be equal on roundabout, but we should accept them
			boolean alreadyVisited = visitedSegments.contains(nts);
			if ((!alreadyVisited && processRoad) || oppositeConnectionFound) {
				int type = -1;
				if (!reverseWay) {
					for (int i = 0; i < getRestrictionCount(road); i++) {
						if (getRestriction(road, i) == next.road.getId()) {
							type = getRestrictionType(road, i);
							break;
						}
					}
				} else {
					for (int i = 0; i < getRestrictionCount(next.road); i++) {
						if (getRestriction(next.road, i) == road.getId()) {
							type = getRestrictionType(next.road, i);
							break;
						}
						// Check if there is restriction only to the current road
						if (getRestrictionType(next.road, i) == MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN
								|| getRestrictionType(next.road, i) == MapRenderingTypes.RESTRICTION_ONLY_LEFT_TURN
								|| getRestrictionType(next.road, i) == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
							// check if that restriction applies to considered junk
							RouteSegment foundNext = inputNext;
							while(foundNext != null && foundNext.getRoad().getId() != getRestriction(next.road, i)){
								foundNext = foundNext.next;
							}
							if(foundNext != null) {
								type = REVERSE_WAY_RESTRICTION_ONLY; // special constant
							}
						}
					}
				}
				if(type == REVERSE_WAY_RESTRICTION_ONLY){
					// next = next.next; continue;
				} else if (type == -1 && exclusiveRestriction) {
					// next = next.next; continue;
				} else if (type == MapRenderingTypes.RESTRICTION_NO_LEFT_TURN || type == MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN
						|| type == MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON || type == MapRenderingTypes.RESTRICTION_NO_U_TURN) {
					// next = next.next; continue;
				} else {
					// no restriction can go out
					if(oppositeConnectionFound){
						RouteSegment oppSegment = oppositeSegments.get(nts);
						oppSegment.segmentEnd = next.segmentStart;
						return oppSegment;
					}
					
					double distanceToEnd = h(ctx, distToFinalPoint, segment, next);

					// Using A* routing algorithm
					// g(x) - calculate distance to that point and calculate time
					double speed = ctx.getRouter().defineSpeed(road);
					if (speed == 0) {
						speed = ctx.getRouter().getMinDefaultSpeed();
					}

					double distanceFromStart = g(ctx, distOnRoadToPass, segment, segmentEnd, obstaclesTime, next, speed);

					// segment.getRoad().getId() >> 1
					if (next.parentRoute == null
							|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, distanceFromStart, distanceToEnd) > 0) {
						next.distanceFromStart = distanceFromStart;
						next.distanceToEnd = distanceToEnd;
						if (next.parentRoute != null) {
							// already in queue remove it
							graphSegments.remove(next);
						}
						// put additional information to recover whole route after
						next.parentRoute = segment;
						next.parentSegmentEnd = segmentEnd;
						if (type == -1) {
							// case no restriction
							segmentsToVisitNotForbidden.add(next);
						} else {
							// case exclusive restriction (only_right, only_straight, ...)
							// 1. in case we are going backward we should not consider only_restriction
							// as exclusive because we have main "in" roads and one "out"
							// 2. in case we are going forward we have one "in" and many "out"
							if (!reverseWay) {
								exclusiveRestriction = true;
								segmentsToVisitNotForbidden.clear();
								segmentsToVisitPrescripted.add(next);
							} else {
								segmentsToVisitNotForbidden.add(next);
							}
						}
					}

				}
			} else if (alreadyVisited) {
				//the segment was already visited! We need to follow better route.
				if (segment.distanceFromStart < next.distanceFromStart) {
					// Using A* routing algorithm
					// g(x) - calculate distance to that point and calculate time
					double speed = ctx.getRouter().defineSpeed(road);
					if (speed == 0) {
						speed = ctx.getRouter().getMinDefaultSpeed();
					}
					next.distanceFromStart = g(ctx, distOnRoadToPass, segment, segmentEnd, obstaclesTime, next, speed);
					//TODO calculate also the H heuristic, if this segment is in priority queue
					final RouteSegment findAndReplace = next.parentRoute;
					final RouteSegment actual = segment;
					final int theend = next.parentSegmentEnd;
					next.parentRoute = segment;
					next.parentSegmentEnd = segment.road.getPointsLength()-1; //TODO I don't understand yet the segments correctly, this might be not correct
					//REPLACE all that are branches of the next.parentRoute, because better way was found.
					//TODO check which segments are in priority queue and update it. Probably, it can currently confuse the queue implementation!
					//TODO all leaves of branches that exists from the updateSegment should be updated and leaves also updated in the priority queue
					// --- this will speed up a little because the branches should be 'faster'
					visitedSegments.forEachValue(new TObjectProcedure<BinaryRoutePlanner.RouteSegment>() {
						@Override
						public boolean execute(RouteSegment updateSegment) {
							if (updateSegment != null && updateSegment.parentRoute == findAndReplace && updateSegment.parentSegmentEnd == theend && updateSegment != actual) {
								updateSegment.parentRoute = actual;
								updateSegment.parentSegmentEnd = actual.road.getPointsLength()-1; //TODO I don't understand yet the segments correctly, this might be not correct
							}
							return false;
						}
					});
				}
			}
			next = next.next;
		}

		// add all allowed route segments to priority queue
		for (RouteSegment s : segmentsToVisitNotForbidden) {
			graphSegments.add(s);
		}
		for (RouteSegment s : segmentsToVisitPrescripted) {
			graphSegments.add(s);
		}
		return null;
	}


	
	private int getRestrictionType(BinaryMapDataObject road, int i) {
		throw new UnsupportedOperationException();
	}


	private long getRestriction(BinaryMapDataObject road, int i) {
		throw new UnsupportedOperationException();
	}


	private int getRestrictionCount(BinaryMapDataObject road) {
		throw new UnsupportedOperationException();
	}


	private List<RouteSegmentResult> prepareResult(RoutingContext ctx, RouteSegment start, RouteSegment end, long startNanoTime,
			RouteSegment finalDirectRoute, RouteSegment finalReverseRoute) {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		
		RouteSegment segment = finalReverseRoute;
		int parentSegmentStart = segment == null ? 0 : segment.segmentEnd; 
		while(segment != null){
			RouteSegmentResult res = new RouteSegmentResult();
			res.object = segment.road;
			res.endPointIndex = segment.segmentStart;
			res.startPointIndex = parentSegmentStart;
			parentSegmentStart = segment.parentSegmentEnd;
			segment = segment.parentRoute;
			// reverse start and end point for start if needed
			// rely that point.segmentStart <= point.segmentEnd for end, start
			if(segment == null && res.startPointIndex >= res.endPointIndex && 
					res.endPointIndex < res.object.getPointsLength() - 1){
				res.endPointIndex ++;
			}
			// do not add segments consists from 1 point
			if(res.startPointIndex != res.endPointIndex) {
				result.add(res);
			}
			res.startPoint = convertPoint(res.object, res.startPointIndex);
			res.endPoint = convertPoint(res.object, res.endPointIndex);
		}
		Collections.reverse(result);
		
		segment = finalDirectRoute;
		int parentSegmentEnd = segment == null ? 0 : segment.segmentEnd;
		while(segment != null){
			RouteSegmentResult res = new RouteSegmentResult();
			res.object = segment.road;
			res.endPointIndex = parentSegmentEnd;
			res.startPointIndex = segment.segmentStart;
			parentSegmentEnd = segment.parentSegmentEnd;
			
			segment = segment.parentRoute;
			// reverse start and end point for start if needed
			// rely that point.segmentStart <= point.segmentEnd for end, start
			if(segment == null && res.startPointIndex < res.endPointIndex){
				res.startPointIndex ++;
			}
			// do not add segments consists from 1 point
			if(res.startPointIndex != res.endPointIndex) {
				result.add(res);
			}
			res.startPoint = convertPoint(res.object, res.startPointIndex);
			res.endPoint = convertPoint(res.object, res.endPointIndex);
		}
		Collections.reverse(result);
		
		
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
			System.out.println("ROUTE : ");
			double startLat = MapUtils.get31LatitudeY(start.road.getPoint31YTile(start.segmentEnd));
			double startLon = MapUtils.get31LongitudeX(start.road.getPoint31XTile(start.segmentEnd));
			double endLat = MapUtils.get31LatitudeY(end.road.getPoint31YTile(end.segmentStart));
			double endLon = MapUtils.get31LongitudeX(end.road.getPoint31XTile(end.segmentEnd));
			System.out.println(MessageFormat.format("<test regions=\"\" description=\"\" best_percent=\"\" vehicle=\"\" \n" +
					"    start_lat=\"{0}\" start_lon=\"{1}\" target_lat=\"{2}\" target_lon=\"{3}\">", 
					startLat+"", startLon+"", endLat+"", endLon+""));
			for (RouteSegmentResult res : result) {
				String name = res.object.getName();
				String ref = res.object.getNameByType(res.object.getMapIndex().refEncodingType);
				if(ref != null) {
					name += " " + ref;
				}
				// (res.object.getId() >> 1)
				System.out.println(MessageFormat.format("\t<segment id=\"{0}\" start=\"{1}\" end=\"{2}\" name=\"{3}\"/>", 
						(res.object.getId() >> 1)+"", res.startPointIndex, res.endPointIndex, name));
			}
			System.out.println("</test>");
		}
		
		ctx.timeToCalculate = (System.nanoTime() - startNanoTime);
		log.info("Time to calculate : " + ctx.timeToCalculate / 1e6 +", time to load : " + ctx.timeToLoad / 1e6	 + ", loaded tiles : " + ctx.loadedTiles.size() + 
				", visited segments " + ctx.visitedSegments );
		return result;
	}
	
	private LatLon convertPoint(BinaryMapDataObject o, int ind){
		return new LatLon(MapUtils.get31LatitudeY(o.getPoint31YTile(ind)), MapUtils.get31LongitudeX(o.getPoint31XTile(ind)));
	}
	
	
	public interface RouteSegmentVisitor {
		
		public void visitSegment(RouteSegment segment);
	}
	
	
	
	
	/*public */static int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient ) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, 
				o2DistanceFromStart + heuristicCoefficient *  o2DistanceToEnd);
	}

	public static class RouteSegment {
		int segmentStart = 0;
		int segmentEnd = 0;
		BinaryMapDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		
		// search context (needed for searching route)
		// Initially it should be null (!) because it checks was it segment visited before
		RouteSegment parentRoute = null;
		int parentSegmentEnd = 0;
		
		// distance measured in time (seconds)
		double distanceFromStart = 0;
		double distanceToEnd = 0;
		
		public RouteSegment getNext() {
			return next;
		}
		
		public int getSegmentStart() {
			return segmentStart;
		}
		
		public BinaryMapDataObject getRoad() {
			return road;
		}
	}

	private static class RoutePair {
		RouteSegment a;
		RouteSegment b;
		public RoutePair(RouteSegment a, RouteSegment b) {
			super();
			this.a = a;
			this.b = b;
		}
		
		
	}

	
}
