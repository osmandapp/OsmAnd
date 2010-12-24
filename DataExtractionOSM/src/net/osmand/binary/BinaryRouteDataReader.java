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

import com.google.protobuf.CodedInputStreamRAF;

public class BinaryRouteDataReader {
	private CodedInputStreamRAF codedIS;
	private final BinaryMapIndexReader map;
	
	public BinaryRouteDataReader(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	protected void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	protected int readInt() throws IOException {
		return map.readInt();
	}
	
	private static class CarRouter {
		// no distinguish for speed in city/outside city (for now)
		private Map<String, Double> autoValues = new LinkedHashMap<String, Double>();
		{
			autoValues.put("motorway", 110d);
			autoValues.put("motorway_link", 110d);
			autoValues.put("trunk", 110d);
			autoValues.put("trunk_link", 110d);
			autoValues.put("primary", 70d);
			autoValues.put("primary_link", 70d);
			autoValues.put("secondary", 60d);
			autoValues.put("secondary_link", 60d);
			autoValues.put("tertiary", 30d);
			autoValues.put("tertiary_link", 30d);
			autoValues.put("residential", 30d);
			autoValues.put("service", 5d);
			autoValues.put("unclassified", 5d);
			autoValues.put("road", 25d);
			autoValues.put("track", 20d);
			autoValues.put("path", 20d);
			autoValues.put("living_street", 20d);
		}
		
		private boolean acceptLine(TagValuePair pair){
			if(pair.tag.equals("highway")){
				return autoValues.containsKey(pair.value);
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
			}
			return false;
		}
		
		public boolean isOneWay(int highwayAttributes){
			// TODO correctly define roundabout!!!
			return MapRenderingTypes.isOneWayWay(highwayAttributes) || 
					MapRenderingTypes.isRoundabout(highwayAttributes);
		}

		/**
		 * return speed in m/s
		 */
		public double defineSpeed(BinaryMapDataObject road) {
			double speed = MapRenderingTypes.getMaxSpeedIfDefined(road.getHighwayAttributes()) / 3.6d;
			if(speed != 0) {
				return speed;
			}
			TagValuePair pair = road.getTagValue(0);
			if("highway".equals(pair.tag) && autoValues.containsKey(pair.value)){
				return autoValues.get(pair.value) / 3.6d;
			}
			return 9;
		}

		public double getMinDefaultSpeed() {
			return 10;
		}
		
	}
	
	public static class RoutingContext {
		TLongObjectMap<BinaryMapDataObject> idObjects = new TLongObjectHashMap<BinaryMapDataObject>();
		TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		CarRouter router = new CarRouter();
		
		TIntSet loadedTiles = new TIntHashSet();
		
		int timeToLoad = 0;
		int timeToCalculate = 0;
		
		public Collection<BinaryMapDataObject> values(){
			return idObjects.valueCollection();
		}
	}
	
	private final static int ZOOM_LOAD_TILES = 13;
	
	public static class RouteSegment {
		int segmentStart = 0;
		int segmentEnd = 0;
		BinaryMapDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		
		// search context (needed for searching route)
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
		double dy = (y1 - y2) * 0.01863d;
		double dx = (x1 - x2) * 0.011d;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	private static int absDist(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2);
	}
   
	public void loadRoutes(final RoutingContext ctx, int tileX, int tileY) throws IOException {
		int tileC = (tileX << ZOOM_LOAD_TILES) + tileY;
		if(ctx.loadedTiles.contains(tileC)){
			return;
		}
		long now = System.currentTimeMillis();
		
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
		map.searchMapIndex(request);
		for (BinaryMapDataObject o : request.searchResults) {
			if(ctx.idObjects.containsKey(o.getId())){
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
		ctx.timeToLoad += (System.currentTimeMillis() - now);
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
	
	
	
	public List<RouteSegmentResult> searchRoute(RoutingContext ctx, RouteSegment start, RouteSegment end) throws IOException {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		long now = System.currentTimeMillis();

		PriorityQueue<RouteSegment> graphSegments = new PriorityQueue<RouteSegment>(50, new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return Double.compare(o1.distanceFromStart + o1.distanceToEnd, o2.distanceFromStart + o2.distanceToEnd);
			}
			
		});
		TLongHashSet visitedPoints = new TLongHashSet();
		graphSegments.add(start);
		RouteSegment endRoute = null;
		int endX = end.road.getPoint31XTile(end.segmentEnd);
		int endY = end.road.getPoint31YTile(end.segmentEnd);
		int startX = start.road.getPoint31XTile(start.segmentStart);
		int startY = start.road.getPoint31YTile(start.segmentStart);
		start.distanceToEnd = squareRootDist(startX, startY, endX, endY) / 10;
		
//		double maxAreaDist = 100 * squareRootDist(startX, startY, endX, endY);
		
		while(!graphSegments.isEmpty() && endRoute == null){
			RouteSegment segment = graphSegments.poll();
			BinaryMapDataObject road = segment.road;
			
			// try to find all ways
			boolean oneway = ctx.router.isOneWay(road.getHighwayAttributes());
			
			int middle = segment.segmentEnd;
			boolean minus = true;
			boolean plus = true;
			int d = 0;
			int middlex = road.getPoint31XTile(middle);
			int middley = road.getPoint31YTile(middle);
			double trafficSignalsTime = 0;
			while(endRoute == null && ((!oneway && minus) || plus)) {
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
				if(end.road.id == road.id && end.segmentStart == j){
					endRoute = segment;
					break;
				}
				
				long l = (((long) road.getPoint31XTile(j)) << 31) + (long) road.getPoint31YTile(j);
				if(visitedPoints.contains(l)){
					continue;
				}
				loadRoutes(ctx, (road.getPoint31XTile(j) >> (31 - ZOOM_LOAD_TILES)), (road.getPoint31YTile(j) >> (31 - ZOOM_LOAD_TILES)));
				
				RouteSegment next = ctx.routes.get(l);
				
				if (next != null) {
					visitedPoints.add(l);
					if (d != 0) {
						RouteSegment trafficSignalsTest = next;
						while (trafficSignalsTest != null) {
							if ((trafficSignalsTest.road.getTypes()[0] & 3) == MapRenderingTypes.POINT_TYPE) {
								TagValuePair pair = trafficSignalsTest.road.getTagValue(0);
								if (pair != null && pair.tag.equals("highway") && pair.value.equals("traffic_signals")) {
									trafficSignalsTime += 25;
								}
							}
							trafficSignalsTest = trafficSignalsTest.next;
						}
					}
				}
				while(next != null){
					// TODO consider restrictions !
					if(next.road.id != road.id){
						next.parentRoute = segment; 
						next.parentSegmentEnd = j;
						int x = road.getPoint31XTile(j);
						int y = road.getPoint31YTile(j);
						
						// Using A* routing algorithm
						// g(x) - calculate distance to that point and calculate time
						
						double speed = ctx.router.defineSpeed(road);
						if(speed == 0){
							speed = ctx.router.getMinDefaultSpeed();
						}
						
						next.distanceFromStart = segment.distanceFromStart + squareRootDist(x, y, middlex, middley) / speed; 
						// for each turn add 45 seconds
						// TODO consider right turn 20 seconds and left turn 45 seconds 
						if (j < road.getPointsLength() - 1) {
							next.distanceFromStart += 30;
						}
						// traffic signals time
						next.distanceFromStart += trafficSignalsTime;
						// h(x) - calculate approximate distance to the end point and divide to 37 km/h = 10 m/s
						// max speed
						next.distanceToEnd = squareRootDist(x, y, endX, endY) / 30;
						
						graphSegments.add(next);
					}
					next = next.next;
				}
				
				
			}
		}
		// reverse start and end point for end if needed
		int parentSegmentEnd = endRoute != null && endRoute.segmentStart <= end.segmentStart ? 
				end.segmentEnd : end.segmentStart;
		RouteSegment segment = endRoute;
		while(segment != null){
			RouteSegmentResult res = new RouteSegmentResult();
			res.object = segment.road;
			res.endPointIndex = parentSegmentEnd;
			res.startPointIndex = segment.segmentStart;
			parentSegmentEnd = segment.parentSegmentEnd;
			System.out.println(segment.road.name + " time to go " + 
					(segment.distanceFromStart  / 60) + " estimate time " + (segment.distanceToEnd  / 60));
			segment = segment.parentRoute;
			if(res.startPointIndex != res.endPointIndex) {
				// skip duplicates
				result.add(0, res);
				// reverse start and end point for start if needed
				if(segment == null && res.startPointIndex > res.endPointIndex){
					res.startPointIndex = start.segmentEnd;
				}
			}
			res.startPoint = convertPoint(res.object, res.startPointIndex);
			res.endPoint = convertPoint(res.object, res.endPointIndex);
			
		}
		ctx.timeToCalculate = (int) (System.currentTimeMillis() - now);
		System.out.println("Time to calculate : " + ctx.timeToCalculate +", time to load : " + ctx.timeToLoad + ", loaded tiles : " + ctx.loadedTiles.size());
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
					+ (road.id >> 3));
		}
		
		RouteSegment end = router.findRouteSegment(elat, elon, ctx);
		if (end != null) {
			BinaryMapDataObject road = end.road;
			TagValuePair pair = road.mapIndex.decodeType(road.getTypes()[0]);
			System.out.println("ROAD TO END " + pair.tag + " " + pair.value + " " + road.name + " " + end.segmentStart + " "
					+ (road.id >> 3));
		}
		
		

		double tileX = Math.round(MapUtils.getTileNumberX(ZOOM_LOAD_TILES, lon));
		double tileY = Math.round(MapUtils.getTileNumberY(ZOOM_LOAD_TILES, lat));
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
//				System.out.println(pair.tag + " " + pair.value + " " + (r.id >> 3));
//			}
//		}
	}

	
}
