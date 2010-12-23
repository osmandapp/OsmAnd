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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			autoValues.put("secondary", 50d);
			autoValues.put("secondary_link", 50d);
			autoValues.put("tertiary", 50d);
			autoValues.put("tertiary_link", 50d);
			autoValues.put("residential", 50d);
			autoValues.put("service", 40d);
			autoValues.put("unclassified", 40d);
			autoValues.put("road", 40d);
			autoValues.put("track", 30d);
			autoValues.put("path", 30d);
			autoValues.put("living_street", 30d);
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
			} else if(pair.tag.equals("highway") && pair.tag.equals("traffic_signals")){
				return true;
			} else if(pair.tag.equals("highway") && pair.tag.equals("speed_camera")){
				return true;
			}
			return false;
		}
		
	}
	
	public static class RoutingContext {
		TLongObjectMap<BinaryMapDataObject> idObjects = new TLongObjectHashMap<BinaryMapDataObject>();
		TLongObjectMap<RouteSegment> routes = new TLongObjectHashMap<RouteSegment>();
		CarRouter router = new CarRouter();
		
		TIntSet loadedTiles = new TIntHashSet();
		
		public Collection<BinaryMapDataObject> values(){
			return idObjects.valueCollection();
		}
	}
	
	private final static int ZOOM_LOAD_TILES = 15;
	
	public static class RouteSegment {
		int segmentStart = 0;
		int segmentEnd = 0;
		BinaryMapDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		// search context (needed for searching route)
		int parentRoute = 0;
		int parentSegmentEnd = 0;
		
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
	
	private static double dist(int x1, int y1, int x2, int y2) {
		return Math.sqrt(((double)x1 - x2) * ((double)x1 - x2) + ((double)y1 - y2) * ((double)y1 - y2));
	}
   
	public void loadRoutes(final RoutingContext ctx, int tileX, int tileY) throws IOException {
		int tileC = tileX << ZOOM_LOAD_TILES + tileY;
		if(ctx.loadedTiles.contains(tileC)){
			return;
		}
		
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
				double prevDist = dist(r.getPoint31XTile(0), r.getPoint31YTile(0), px, py);
				for (int j = 1; j < r.getPointsLength(); j++) {
					double cDist = dist(r.getPoint31XTile(j), r.getPoint31YTile(j), px, py);
					double mDist = dist(r.getPoint31XTile(j), r.getPoint31YTile(j), r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1));
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
		
		List<RouteSegment> searchList = new ArrayList<RouteSegment>();
		TLongHashSet visitedPoints = new TLongHashSet();
		searchList.add(start);
		int stIndex = 0;
		int endIndex = -1;
		while(stIndex < searchList.size() && endIndex == -1){
			RouteSegment segment = searchList.get(stIndex);
			BinaryMapDataObject road = segment.road;
 
			
			// try to find all ways
			// TODO check for one way (?) and start from segment end
			for (int j = 0; j < road.getPointsLength() && endIndex == -1; j++) {
				long l = (((long) road.getPoint31XTile(j)) << 31) + (long) road.getPoint31YTile(j);
				if(visitedPoints.contains(l)){
					continue;
				}
				// TODO efficient (?)
				loadRoutes(ctx, (road.getPoint31XTile(j) >> (31 - ZOOM_LOAD_TILES)), (road.getPoint31YTile(j) >> (31 - ZOOM_LOAD_TILES)));
				
				RouteSegment next = ctx.routes.get(l);
				if(next != null){
					visitedPoints.add(l);
				}
				while(next != null){
					if(next.road.id != road.id){
						next.parentRoute = stIndex; 
						next.parentSegmentEnd = j;
						searchList.add(next);
						// TODO check that there is way to that point !!!
						if(end.road.id == next.road.id){
							endIndex = searchList.size() - 1;
							break;
						}
					}
					next = next.next;
				}
				
			}
			stIndex++;
		}
		stIndex = endIndex;
		start.parentRoute = -1;
		int parentSegmentEnd = end.segmentEnd;
		while(stIndex != -1){
			RouteSegment segment = searchList.get(stIndex);
			RouteSegmentResult res = new RouteSegmentResult();
			res.object = segment.road;
			res.endPointIndex = parentSegmentEnd;
			res.startPointIndex = segment.segmentStart;
			res.startPoint = convertPoint(res.object, res.startPointIndex);
			res.endPoint = convertPoint(res.object, res.endPointIndex);
			if(res.startPointIndex != res.endPointIndex) {
				// skip duplicates
				result.add(0, res);
			}
			parentSegmentEnd = segment.parentSegmentEnd;
			stIndex = segment.parentRoute;
		}
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
