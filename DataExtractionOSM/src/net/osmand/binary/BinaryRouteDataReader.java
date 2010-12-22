package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;

import com.google.protobuf.CodedInputStreamRAF;

public class BinaryRouteDataReader {
	private CodedInputStreamRAF codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryRouteDataReader(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	protected void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	protected int readInt() throws IOException {
		return map.readInt();
	}
	
	
	private class CarRouter {
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

   
	private static class RoutingContext {
		// TODO replace with lightweight collection
		Map<Long, BinaryMapDataObject> map = new LinkedHashMap<Long, BinaryMapDataObject>();
		
		TIntSet loadedTiles = new TIntHashSet();
		
		public Collection<BinaryMapDataObject> values(){
			return map.values();
		}
	}
	
	private final static int ZOOM_LOAD_TILES = 15;
	
	public void searchRoutes(RoutingContext ctx, int tileX, int tileY) throws IOException {
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
		final CarRouter router = new CarRouter();
		request.setSearchFilter(new BinaryMapIndexReader.SearchFilter(){
			@Override
			public boolean accept(TIntArrayList types, MapIndex index) {
				for (int j = 0; j < types.size(); j++) {
					int wholeType = types.get(j);
					TagValuePair pair = index.decodeType(wholeType);
					if (pair != null) {
						int t = wholeType & 3;
						if(t == MapRenderingTypes.POINT_TYPE){
							if(router.acceptPoint(pair)){
								return true;
							}
						} else if(t == MapRenderingTypes.POLYLINE_TYPE){
							if(router.acceptLine(pair)){
								return true;
							}
						}
					}
				}
				return false;
			}
		});
		map.searchMapIndex(request);
		for(BinaryMapDataObject o : request.searchResults){
			ctx.map.put(o.getId(), o);
		}
		ctx.loadedTiles.add(tileC);
	}
	
	private static class RouteSegment {
		int segment = 0;
		BinaryMapDataObject road;
	}
	
	private static double dist(int x1, int y1, int x2, int y2) {
		return Math.sqrt(((double)x1 - x2) * ((double)x1 - x2) + ((double)y1 - y2) * ((double)y1 - y2));
	}
	
	public RouteSegment findRouteSegment(double lat, double lon, RoutingContext ctx) throws IOException {
		double tileX = MapUtils.getTileNumberX(ZOOM_LOAD_TILES, lon);
		double tileY = MapUtils.getTileNumberY(ZOOM_LOAD_TILES, lat);
		searchRoutes(ctx, (int) tileX , (int) tileY);
		
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
						road.segment = j;
						dist = prevDist + cDist - mDist;
					}
					prevDist = cDist;
				}
			}
		}
		return road;
	}
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("d:\\android\\data\\Belarus.obf"), "r"); //$NON-NLS-1$ //$NON-NLS-2$
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		BinaryRouteDataReader router = new BinaryRouteDataReader(reader);
		
//		int sleft = MapUtils.get31TileNumberX(27.596);
//		int sright = MapUtils.get31TileNumberX(27.599);
//		int stop = MapUtils.get31TileNumberY(53.921);
//		int sbottom = MapUtils.get31TileNumberY(53.919);
		
		double lon = 27.5976;
		double lat = 53.9199;
		
		RoutingContext ctx = new RoutingContext();
		
		long ms = System.currentTimeMillis();
		// find closest way
		RouteSegment routeSegment = router.findRouteSegment(lat, lon, ctx);
		if (routeSegment != null) {
			BinaryMapDataObject road = routeSegment.road;
			TagValuePair pair = road.mapIndex.decodeType(road.getTypes()[0]);
			System.out.println("ROAD TO START " + pair.tag + " " + pair.value + " " + road.name + " " + routeSegment.segment + " "
					+ (road.id >> 3));
		}

		double tileX = Math.round(MapUtils.getTileNumberX(ZOOM_LOAD_TILES, lon));
		double tileY = Math.round(MapUtils.getTileNumberY(ZOOM_LOAD_TILES, lat));
		// preload neighboors
		router.searchRoutes(ctx, (int) tileX, (int) tileY);
		router.searchRoutes(ctx, (int) tileX - 1, (int) tileY);
		router.searchRoutes(ctx, (int) tileX, (int) tileY - 1);
		router.searchRoutes(ctx, (int) tileX - 1, (int) tileY - 1);
		Collection<BinaryMapDataObject> res = ctx.values();  
		System.out.println(res.size() + " objects for " + (System.currentTimeMillis() - ms) + " ms");
		for(BinaryMapDataObject r : res){
			TagValuePair pair = r.mapIndex.decodeType(r.getTypes()[0]);
//			if(r.name != null){
//				System.out.println(pair.tag + " " + pair.value + " " + r.name );
//			} else {
//				System.out.println(pair.tag + " " + pair.value + " " + (r.id >> 3));
//			}
		}
	}

	
}
