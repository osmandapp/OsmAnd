package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

public class BinaryRouteDataReader {
	
	private final static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	private final BinaryMapIndexReader[] map;
	private int HEURISTIC_COEFFICIENT = 3;
	
	private static final Log log = LogUtil.getLog(BinaryRouteDataReader.class);
	
	public BinaryRouteDataReader(BinaryMapIndexReader... map){
		this.map = map;
	}

	
	private static class CarRouter {
		// no distinguish for speed in city/outside city (for now)
		private Map<String, Double> autoNotDefinedValues = new LinkedHashMap<String, Double>();
		private Map<String, Double> autoPriorityValues = new LinkedHashMap<String, Double>();
		{
			autoNotDefinedValues.put("motorway", 110d);
			autoNotDefinedValues.put("motorway_link", 80d);
			autoNotDefinedValues.put("trunk", 100d);
			autoNotDefinedValues.put("trunk_link", 80d);
			autoNotDefinedValues.put("primary", 65d);
			autoNotDefinedValues.put("primary_link", 45d);
			autoNotDefinedValues.put("secondary", 50d);
			autoNotDefinedValues.put("secondary_link", 40d);
			autoNotDefinedValues.put("tertiary", 35d);
			autoNotDefinedValues.put("tertiary_link", 30d);
			autoNotDefinedValues.put("residential", 30d);
			autoNotDefinedValues.put("road", 30d);
			autoNotDefinedValues.put("service", 20d);
			autoNotDefinedValues.put("unclassified", 20d);
			autoNotDefinedValues.put("track", 20d);
			autoNotDefinedValues.put("path", 20d);
			autoNotDefinedValues.put("living_street", 20d);
			
			autoPriorityValues.put("motorway", 1.5);
			autoPriorityValues.put("motorway_link", 1.0);
			autoPriorityValues.put("trunk", 1.5);
			autoPriorityValues.put("trunk_link", 1d);
			autoPriorityValues.put("primary", 1.3d);
			autoPriorityValues.put("primary_link", 1d);
			autoPriorityValues.put("secondary", 1.0d);
			autoPriorityValues.put("secondary_link", 1.0d);
			autoPriorityValues.put("tertiary", 1.0d);
			autoPriorityValues.put("tertiary_link", 1.0d);
			autoPriorityValues.put("residential", 0.8d);
			autoPriorityValues.put("service", 0.6d);
			autoPriorityValues.put("unclassified", 0.4d);
			autoPriorityValues.put("road", 0.4d);
			autoPriorityValues.put("track", 0.1d);
			autoPriorityValues.put("path", 0.1d);
			autoPriorityValues.put("living_street", 0.5d);
		}
		
		private boolean acceptLine(TagValuePair pair){
			if(pair.tag.equals("highway")){
				return autoNotDefinedValues.containsKey(pair.value);
			}
			return false;
		}
		
		private boolean acceptPoint(TagValuePair pair){
			if(pair.tag.equals("traffic_calming")){
				return true;
			} else if(pair.tag.equals("highway") && pair.value.equals("traffic_signals")){
				return true;
			} else if(pair.tag.equals("highway") && pair.value.equals("speed_camera")){
				return true;
			} else if(pair.tag.equals("railway") && pair.value.equals("crossing")){
				return true;
			} else if(pair.tag.equals("railway") && pair.value.equals("level_crossing")){
				return true;
			}
			return false;
		}
		
		public boolean isOneWay(int highwayAttributes){
			return MapRenderingTypes.isOneWayWay(highwayAttributes) || 
					MapRenderingTypes.isRoundabout(highwayAttributes);
		}

		/**
		 * return delay in seconds
		 */
		public double defineObstacle(BinaryMapDataObject road, int point) {
			if ((road.getTypes()[0] & 3) == MapRenderingTypes.POINT_TYPE) {
				// possibly not only first type needed ?
				TagValuePair pair = road.getTagValue(0);
				if (pair != null) {
					if(pair.tag.equals("highway") && pair.value.equals("traffic_signals")){
						return 20;
					} else if(pair.tag.equals("railway") && pair.value.equals("crossing")){
						return 25;
					} else if(pair.tag.equals("railway") && pair.value.equals("level_crossing")){
						return 25;
					}
				}
			}
			return 0;
		}
		
		/**
		 * return speed in m/s
		 */
		public double defineSpeed(BinaryMapDataObject road) {
			TagValuePair pair = road.getTagValue(0);
			double speed = MapRenderingTypes.getMaxSpeedIfDefined(road.getHighwayAttributes()) / 3.6d;
			boolean highway = "highway".equals(pair.tag);
			double priority = highway && autoPriorityValues.containsKey(pair.value) ? autoPriorityValues.get(pair.value) : 1d;
			if(speed == 0 && highway) {
				Double value = autoNotDefinedValues.get(pair.value);
				if(value == null){
					value = 50d;
				}
				speed =  value / 3.6d;
			}
			return speed * priority;
		}
		

		/**
		 * Used for A* routing to calculate g(x)
		 * @return minimal speed at road
		 */
		public double getMinDefaultSpeed() {
			return 9;
		}
		
		/**
		 * Used for A* routing to predict h(x) : it should be < (!) any g(x) 
		 * @return maximum speed to calculate shortest distance
		 */
		public double getMaxDefaultSpeed() {
			return 30;
		}


		public double calculateTurnTime(int middley, int middlex, int x, int y, RouteSegment segment, RouteSegment next, int j) {
			boolean lineAreNotConnected = j < segment.road.getPointsLength() - 1 || next.segmentStart != 0;
			if(lineAreNotConnected){
				return 25;
			} else {
				if (next.road.getPointsLength() > 1) {
					double a1 = Math.atan2(y - middley, x - middlex);
					double a2 = Math.atan2(y - next.road.getPoint31YTile(1), x - next.road.getPoint31XTile(1));
					double diff = Math.abs(a1 - a2);
					if (diff > Math.PI / 2 && diff < 3 * Math.PI / 2) {
						return 25;
					}
				}
			}
			return 0;
		}
		
	}
	
	public interface RouteSegmentVisitor {
		public void visitSegment(RouteSegment segment);
	}
	
	public static class RoutingContext {
		TLongObjectMap<BinaryMapDataObject> idObjects = new TLongObjectHashMap<BinaryMapDataObject>();
		TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		CarRouter router = new CarRouter();
		
		TIntSet loadedTiles = new TIntHashSet();
		// set collection to not null to monitor visited ways
		public RouteSegmentVisitor visitor = null;
		
		long timeToLoad = 0;
		long timeToCalculate = 0;
		int visitedSegments = 0;
		
		public Collection<BinaryMapDataObject> values(){
			return idObjects.valueCollection();
		}
	}
	// 12 doesn't give result on the phone (?)
	private final static int ZOOM_LOAD_TILES = 13;
	
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
		
		public BinaryMapDataObject getRoad() {
			return road;
		}
	}
	
	public static class RouteSegmentResult {
		public LatLon startPoint;
		public LatLon endPoint;
		public BinaryMapDataObject object;
		public int startPointIndex;
		public int endPointIndex;
	}
	
	private static double squareRootDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = (y1 - y2) * 0.01863d;
		double dx = (x1 - x2) * 0.011d;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
//	private static int absDist(int x1, int y1, int x2, int y2) {
//		return Math.abs(x1 - x2) + Math.abs(y1 - y2);
//	}
   
	public void loadRoutes(final RoutingContext ctx, int tileX, int tileY) throws IOException {
		int tileC = (tileX << ZOOM_LOAD_TILES) + tileY;
		if(ctx.loadedTiles.contains(tileC)){
			return;
		}
		long now = System.nanoTime();
		
		SearchRequest<BinaryMapDataObject> request = new SearchRequest<BinaryMapDataObject>();
		request.left = tileX << (31 - ZOOM_LOAD_TILES);
		request.right = (tileX + 1) << (31 - ZOOM_LOAD_TILES);
		request.top = tileY << (31 - ZOOM_LOAD_TILES);
		request.bottom = (tileY + 1) << (31 - ZOOM_LOAD_TILES);
		request.zoom = 15;
		request.setSearchFilter(new BinaryMapIndexReader.SearchFilter(){
			@Override
			public boolean accept(TIntArrayList types, MapIndex index) {
				for (int j = 0; j < types.size(); j++) {
					int wholeType = types.get(j);
					TagValuePair pair = index.decodeType(wholeType);
					if (pair != null) {
						int t = wholeType & 3;
						if(t == MapRenderingTypes.POINT_TYPE){
							if(ctx.router.acceptPoint(pair)){
								return true;
							}
						} else if(t == MapRenderingTypes.POLYLINE_TYPE){
							if(ctx.router.acceptLine(pair)){
								return true;
							}
						}
					}
				}
				return false;
			}
		});
		for (BinaryMapIndexReader r : map) {
			r.searchMapIndex(request);
			for (BinaryMapDataObject o : request.searchResults) {
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
	

	
	public RouteSegment findRouteSegment(double lat, double lon, RoutingContext ctx) throws IOException {
		double tileX = MapUtils.getTileNumberX(ZOOM_LOAD_TILES, lon);
		double tileY = MapUtils.getTileNumberY(ZOOM_LOAD_TILES, lat);
		loadRoutes(ctx, (int) tileX , (int) tileY);
		
		RouteSegment road = null;
		double dist = 0; 
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		for(BinaryMapDataObject r : ctx.values()){
			if(r.getPointsLength() > 1){
				double prevDist = squareRootDist(r.getPoint31XTile(0), r.getPoint31YTile(0), px, py);
				for (int j = 1; j < r.getPointsLength(); j++) {
					double cDist = squareRootDist(r.getPoint31XTile(j), r.getPoint31YTile(j), px, py);
					double mDist = squareRootDist(r.getPoint31XTile(j), r.getPoint31YTile(j), r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1));
					if (road == null || prevDist + cDist - mDist < dist) {
						road = new RouteSegment();
						road.road = r;
						road.segmentStart = j - 1;
						road.segmentEnd = j;
						dist = prevDist + cDist - mDist;
					}
					prevDist = cDist;
				}
			}
		}
		return road;
	}
	
	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + HEURISTIC_COEFFICIENT * o1DistanceToEnd, 
				o2DistanceFromStart + HEURISTIC_COEFFICIENT *  o2DistanceToEnd);
	}
	
	
	// TODO write unit tests
	// TODO add information about turns
	// TODO think about u-turn
	// TODO fix roundabout
	// TODO access
	// TODO bicycle router (?)
	// TODO fastest/shortest way
	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	public List<RouteSegmentResult> searchRoute(RoutingContext ctx, RouteSegment start, RouteSegment end) throws IOException {
		
		// measure time
		ctx.timeToLoad = 0;
		ctx.visitedSegments = 0;
		long startNanoTime = System.nanoTime();

		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> segmentsComparator = new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd);
			}
		};
		PriorityQueue<RouteSegment> graphSegments = new PriorityQueue<RouteSegment>(50, segmentsComparator);
		// initialize temporary lists to calculate not forbidden ways at way intersections 
		ArrayList<RouteSegment> segmentsToVisitPrescricted = new ArrayList<RouteSegment>(5);
		ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<RouteSegment>(5);
		
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongHashSet visitedSegments = new TLongHashSet();
		
		
		int endX = end.road.getPoint31XTile(end.segmentEnd);
		int endY = end.road.getPoint31YTile(end.segmentEnd);
		int startX = start.road.getPoint31XTile(start.segmentStart);
		int startY = start.road.getPoint31YTile(start.segmentStart);
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		start.distanceToEnd = squareRootDist(startX, startY, endX, endY) / ctx.router.getMaxDefaultSpeed();
		
		// add start segment to priority queue
		graphSegments.add(start);
		
		// because first point of the start is not visited do the same as in cycle but only for one point
		long ls = (((long) startX) << 31) + (long) startY;
		loadRoutes(ctx, (startX >> (31 - ZOOM_LOAD_TILES)), (startY >> (31 - ZOOM_LOAD_TILES)));
		RouteSegment startNbs = ctx.routes.get(ls);
		while(startNbs != null) { // startNbs.road.id >> 1, start.road.id >> 1
			if(startNbs.road.id != start.road.id){
				startNbs.parentRoute = start;
				startNbs.parentSegmentEnd = start.segmentStart;
				startNbs.distanceToEnd = start.distanceToEnd;
				long nt = (startNbs.road.id << 8l) + startNbs.segmentStart;
				visitedSegments.add(nt);
				graphSegments.add(startNbs);
			}
			startNbs = startNbs.next;
		}
		
		// final segment before end
		RouteSegment finalRoute = null;
		
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found 
		while(!graphSegments.isEmpty() && finalRoute == null){
			RouteSegment segment = graphSegments.poll();
			BinaryMapDataObject road = segment.road;
			
			ctx.visitedSegments ++;
			// for debug purposes
			if (ctx.visitor != null) {
				ctx.visitor.visitSegment(segment);
			}
			
			// Always start from segmentStart (!), not from segmentEnd
			// It makes difference only for the first start segment
			// Middle point will always be skipped from observation considering already visited
			int middle = segment.segmentStart;
			int middlex = road.getPoint31XTile(middle);
			int middley = road.getPoint31YTile(middle);
			// +/- diff from middle point
			int d = 1;
			
			boolean oneway = ctx.router.isOneWay(road.getHighwayAttributes());
			boolean minus = true;
			boolean plus = true;
			
			if(end.road.id == road.id && end.segmentStart == middle){
				finalRoute = segment;
			}
			// collect time for obstacles 
			double obstaclesTime = 0;
			
			// Go through all point of the way and find ways to continue
			while(finalRoute == null && ((!oneway && minus) || plus)) {
				// 1. calculate point not equal to middle
				//	  (algorithm should visit all point on way if it is not oneway)
				int j = middle + d;
				if(oneway){
					d++;
				} else {
					if(d <= 0){
						d = -d + 1;
					} else {
						d = -d;
					}
				}
				if(j < 0){
					minus = false;
					continue;
				}
				if(j >= road.getPointsLength()){
					plus = false;
					continue;
				}

				// if we found end point break cycle
				if(end.road.id == road.id && end.segmentStart == j){
					finalRoute = segment;
					break;
				}
				
				// 2. calculate point and try to load neighbor ways if they are not loaded
				long l = (((long) road.getPoint31XTile(j)) << 31) + (long) road.getPoint31YTile(j);
				loadRoutes(ctx, (road.getPoint31XTile(j) >> (31 - ZOOM_LOAD_TILES)), (road.getPoint31YTile(j) >> (31 - ZOOM_LOAD_TILES)));
				long nt = (road.id << 8l) + segment.segmentStart;
				visitedSegments.add(nt);
				
				// 3. get intersected ways
				RouteSegment next = ctx.routes.get(l);
				if (next != null) {
					
					segmentsToVisitPrescricted.clear();
					segmentsToVisitNotForbidden.clear();
					boolean exclusiveRestriction = false;
					
					// 3.1 calculate time for obstacles (bumps, traffic_signals, level_crossing)
					if (d != 0) {
						RouteSegment possibleObstacle = next;
						while (possibleObstacle != null) {
							ctx.router.defineObstacle(possibleObstacle.road, possibleObstacle.segmentStart);
							possibleObstacle = possibleObstacle.next;
						}
					}
					
					// 3.2 calculate possible ways to put into priority queue 
					while(next != null){
						long nts = (next.road.id << 8l) + next.segmentStart;
						/* next.road.id >> 1 != road.id >> 1 - used that line for debug with osm map */
						// road.id could be equal on roundabout, but we should accept them
						if(!visitedSegments.contains(nts)){
							int type = -1;
							for(int i = 0; i< road.getRestrictionCount(); i++){
								if(road.getRestriction(i) == next.road.id){
									type = road.getRestrictionType(i);
									break;
								}
							}
							if(type == -1 && exclusiveRestriction){
								// next = next.next; continue; 
							} else if(type == MapRenderingTypes.RESTRICTION_NO_LEFT_TURN ||
									type == MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN ||
									type == MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON ||
									type == MapRenderingTypes.RESTRICTION_NO_U_TURN){
								// next = next.next; continue; 
							} else {
								
								int x = road.getPoint31XTile(j);
								int y = road.getPoint31YTile(j);
								
								// Using A* routing algorithm
								// g(x) - calculate distance to that point and calculate time
								double speed = ctx.router.defineSpeed(road);
								if(speed == 0){
									speed = ctx.router.getMinDefaultSpeed();
								}
								
								double distanceFromStart = segment.distanceFromStart + squareRootDist(x, y, middlex, middley) / speed; 
								// calculate turn time 
								distanceFromStart += ctx.router.calculateTurnTime(middley, middlex, x, y, segment, next, j); 
								// add obstacles time
								distanceFromStart += obstaclesTime;
								
								
								double distanceToEnd = squareRootDist(x, y, endX, endY) / ctx.router.getMaxDefaultSpeed();
								
								if(next.parentRoute == null || 
										roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, 
												distanceFromStart, distanceToEnd) > 0){
									next.distanceFromStart = distanceFromStart;
									next.distanceToEnd = distanceToEnd;
									if(next.parentRoute != null){
										// already in queue remove it
										graphSegments.remove(next);
									}
									// put additional information to recover whole route after 
									next.parentRoute = segment;
									next.parentSegmentEnd = j;
									if(type == -1){
										// case no restriction
										segmentsToVisitNotForbidden.add(next);
									} else {
										// case exclusive restriction (only_right, only_straight, ...)
										exclusiveRestriction = true;
										segmentsToVisitNotForbidden.clear();
										segmentsToVisitPrescricted.add(next);
									}
								}
									
								
							}
						}
						next = next.next;
					}
					
					// add all allowed route segments to priority queue
					for(RouteSegment s : segmentsToVisitNotForbidden){
						graphSegments.add(s);
					}
					for(RouteSegment s : segmentsToVisitPrescricted){
						graphSegments.add(s);
					}
				}
			}
		}
		
		
		// 4. Route is found : collect all segments and prepare result
		return prepareResult(ctx, start, end, startNanoTime, finalRoute);
	}

	
	
	private List<RouteSegmentResult> prepareResult(RoutingContext ctx, RouteSegment start, RouteSegment end, long startNanoTime,
			RouteSegment finalRoute) {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		// Try to define direction of last movement and reverse start and end point for end if needed
		int parentSegmentEnd = finalRoute != null && finalRoute.segmentStart <= end.segmentStart ? 
				end.segmentEnd : end.segmentStart;
		RouteSegment segment = finalRoute;
		
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
			System.out.println("ROUTE : ");
			System.out.println("Start lat=" + MapUtils.get31LatitudeY(start.road.getPoint31YTile(start.segmentEnd)) + " lon="
					+ MapUtils.get31LongitudeX(start.road.getPoint31XTile(start.segmentEnd)));
			System.out.println("END lat=" + MapUtils.get31LatitudeY(end.road.getPoint31YTile(end.segmentStart)) + " lon="
					+ MapUtils.get31LongitudeX(end.road.getPoint31XTile(end.segmentStart)));
		}
		
		while(segment != null){
			RouteSegmentResult res = new RouteSegmentResult();
			res.object = segment.road;
			res.endPointIndex = parentSegmentEnd;
			res.startPointIndex = segment.segmentStart;
			parentSegmentEnd = segment.parentSegmentEnd;
			if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
				//			System.out.println(segment.road.name + " time to go " + 
				//	(segment.distanceFromStart  / 60) + " estimate time " + (segment.distanceToEnd  / 60));
			}
			
			segment = segment.parentRoute;
			// reverse start and end point for start if needed
			if(segment == null && res.startPointIndex >= res.endPointIndex){
				res.startPointIndex = start.segmentEnd;
			}
			// do not add segments consists from 1 point
			if(res.startPointIndex != res.endPointIndex) {
				if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
					System.out.println("id=" + (res.object.id >> 1) + " start=" + res.startPointIndex + " end=" + res.endPointIndex);
				}
				
				result.add(0, res);
			}
			res.startPoint = convertPoint(res.object, res.startPointIndex);
			res.endPoint = convertPoint(res.object, res.endPointIndex);
		}
		
		
		ctx.timeToCalculate = (System.nanoTime() - startNanoTime);
		log.info("Time to calculate : " + ctx.timeToCalculate / 1e6 +", time to load : " + ctx.timeToLoad / 1e6	 + ", loaded tiles : " + ctx.loadedTiles.size() + 
				", visited segments " + ctx.visitedSegments );
		return result;
	}
	
	private LatLon convertPoint(BinaryMapDataObject o, int ind){
		return new LatLon(MapUtils.get31LatitudeY(o.getPoint31YTile(ind)), MapUtils.get31LongitudeX(o.getPoint31XTile(ind)));
	}
	
	
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("d:\\android\\data\\Belarus.obf"), "r"); //$NON-NLS-1$ //$NON-NLS-2$
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		BinaryRouteDataReader router = new BinaryRouteDataReader(reader);
		
		//double lon = 27.5967;
		//double lat = 53.9204;
		// akad
//		double lon = 27.5993;
//		double lat = 53.9186;
		double lon = 27.6024;
		double lat = 53.9141;
		
		double elon = 27.6018;
		double elat = 53.9223;
		
		RoutingContext ctx = new RoutingContext();
		
		long ms = System.currentTimeMillis();
		// find closest way
		RouteSegment start = router.findRouteSegment(lat, lon, ctx);
		if (start != null) {
			BinaryMapDataObject road = start.road;
			TagValuePair pair = road.mapIndex.decodeType(road.getTypes()[0]);
			System.out.println("ROAD TO START " + pair.tag + " " + pair.value + " " + road.name + " " + start.segmentStart + " "
					+ (road.id >> 1));
		}
		
		RouteSegment end = router.findRouteSegment(elat, elon, ctx);
		if (end != null) {
			BinaryMapDataObject road = end.road;
			TagValuePair pair = road.mapIndex.decodeType(road.getTypes()[0]);
			System.out.println("ROAD TO END " + pair.tag + " " + pair.value + " " + road.name + " " + end.segmentStart + " "
					+ (road.id >> 1));
		}
		
		

//		double tileX = Math.round(MapUtils.getTileNumberX(ZOOM_LOAD_TILES, lon));
//		double tileY = Math.round(MapUtils.getTileNumberY(ZOOM_LOAD_TILES, lat));
		// preload neighboors
//		router.loadRoutes(ctx, (int) tileX, (int) tileY);
//		router.loadRoutes(ctx, (int) tileX - 1, (int) tileY);
//		router.loadRoutes(ctx, (int) tileX, (int) tileY - 1);
//		router.loadRoutes(ctx, (int) tileX - 1, (int) tileY - 1);
		  
		
		
		for(RouteSegmentResult s : router.searchRoute(ctx, start, end)){
			double dist = MapUtils.getDistance(s.startPoint, s.endPoint);
			System.out.println("Street " + s.object.name + " distance " + dist);
		}
		
		Collection<BinaryMapDataObject> res = ctx.values();
		System.out.println(res.size() + " objects for " + (System.currentTimeMillis() - ms) + " ms");
		
		LatLon ls = new LatLon(0, 5);
		LatLon le = new LatLon(1, 5);
		System.out.println("X equator " + MapUtils.getDistance(ls, le) / (MapUtils.get31TileNumberX(ls.getLongitude()) - MapUtils.get31TileNumberX(le.getLongitude())));
		System.out.println("Y equator " + MapUtils.getDistance(ls, le) / (MapUtils.get31TileNumberY(ls.getLatitude()) - MapUtils.get31TileNumberY(le.getLatitude())));
		
//		for(BinaryMapDataObject r : res){
//			TagValuePair pair = r.mapIndex.decodeType(r.getTypes()[0]);
//			if(r.name != null){
//				System.out.println(pair.tag + " " + pair.value + " " + r.name );
//			} else {
//				System.out.println(pair.tag + " " + pair.value + " " + (r.id >>13));
//			}
//		}
	}

	
}
