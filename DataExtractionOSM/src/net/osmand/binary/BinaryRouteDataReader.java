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
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("d:\\android\\data\\Minsk.obf"), "r"); //$NON-NLS-1$ //$NON-NLS-2$
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		BinaryRouteDataReader route = new BinaryRouteDataReader(reader);
		
//		int sleft = MapUtils.get31TileNumberX(27.596);
//		int sright = MapUtils.get31TileNumberX(27.599);
//		int stop = MapUtils.get31TileNumberY(53.921);
//		int sbottom = MapUtils.get31TileNumberY(53.919);
		
		double tileX = MapUtils.getTileNumberX(15, 27.596);
		double tileY = MapUtils.getTileNumberY(15, 53.919);
		RoutingContext ctx = new RoutingContext();
		
		long ms = System.currentTimeMillis();
		route.searchRoutes(ctx, (int) tileX, (int) tileY);
		Collection<BinaryMapDataObject> res = ctx.values();  
		System.out.println(res.size() + " objects for " + (System.currentTimeMillis() - ms) + " ms");
		for(BinaryMapDataObject r : res){
			TagValuePair pair = r.mapIndex.decodeType(r.getTypes()[0]);
			if(r.name != null){
				System.out.println(pair.tag + " " + pair.value + " " + r.name);
			} else {
				System.out.println(pair.tag + " " + pair.value);
			}
		}
	}
}
