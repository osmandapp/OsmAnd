package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapUtils;

public class BinaryMapIndexFilter {

	private final BinaryMapIndexReader reader;

	public BinaryMapIndexFilter(File file) throws IOException{
		reader = new BinaryMapIndexReader(new RandomAccessFile(file.getPath(), "r"), file);
	}
	
	
	private static class Stat {
		int pointCount = 0;
		int totalCount = 0;
		int wayCount = 0;
		int polygonCount = 0;
		int polygonBigSize = 0;
		
		@Override
		public String toString() {
			return " ways " + wayCount + " polygons " + polygonCount + " points " + pointCount + " total " + totalCount +"\n"+
				" polygons big size " + polygonBigSize;
		}
	}

	private double calculateArea(BinaryMapDataObject o, int zoom){
		double sum = 0;
		for(int i=0; i< o.getPointsLength(); i++){
			double x = MapUtils.getTileNumberX(zoom + 8, MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
			int prev = i == 0 ? o.getPointsLength() - 1 : i -1;
			int next = i == o.getPointsLength() - 1 ? 0 : i + 1;
			double y1 = MapUtils.getTileNumberY(zoom + 8, MapUtils.get31LatitudeY(o.getPoint31YTile(prev)));
			double y2 = MapUtils.getTileNumberY(zoom + 8, MapUtils.get31LatitudeY(o.getPoint31YTile(next)));
			sum += x * (y1 - y2);
		}
		return Math.abs(sum);
	}
	
	private double calculateLength(BinaryMapDataObject o, int zoom){
		double sum = 0;
		for(int i=1; i< o.getPointsLength(); i++){
			double x = MapUtils.getTileNumberX(zoom + 8, MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
			double y = MapUtils.getTileNumberY(zoom + 8, MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
			double x2 = MapUtils.getTileNumberX(zoom + 8, MapUtils.get31LongitudeX(o.getPoint31XTile(i - 1)));
			double y2 = MapUtils.getTileNumberY(zoom + 8, MapUtils.get31LatitudeY(o.getPoint31YTile(i - 1)));
			sum += Math.sqrt((x - x2) * (x - x2)  +  (y - y2) * (y - y2));
		}
		return Math.abs(sum);
	}
	
	private int tilesCovers(BinaryMapDataObject o, int zoom, TIntHashSet set){
		set.clear();
		for(int i=0; i< o.getPointsLength(); i++){
			int x = (int) MapUtils.getTileNumberX(zoom, MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
			int y = (int) MapUtils.getTileNumberY(zoom, MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
			int val = ((x << 16) | y);
			set.add(val);
		}
		return set.size();
	}
	
	private Stat process(final int zoom) throws IOException {
		final Stat stat = new Stat();
		final Map<TagValuePair, Integer> map = new HashMap<TagValuePair, Integer>();
		SearchFilter sf = new SearchFilter() {
			@Override
			public boolean accept(TIntArrayList types, MapIndex index) {
				boolean polygon = false;
				boolean polyline = false;
				for (int j = 0; j < types.size(); j++) {
					int wholeType = types.get(j);
					TagValuePair pair = index.decodeType(wholeType);
					if (pair != null) {
						int t = wholeType & 3;
						if (t == RenderingRulesStorage.POINT_RULES) {
							stat.pointCount++;
						} else if (t == RenderingRulesStorage.LINE_RULES) {
							stat.wayCount++;
							polyline = true;
						} else {
							polygon = true;
							stat.polygonCount++;
							if (!map.containsKey(pair)) {
								map.put(pair, 0);
							}
							map.put(pair, map.get(pair) + 1);
						}
					}
				}
				stat.totalCount++;
				return polyline;
			}
		};
		ResultMatcher<BinaryMapDataObject> matcher = new ResultMatcher<BinaryMapDataObject>() {
			TIntHashSet set = new TIntHashSet();
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean publish(BinaryMapDataObject object) {
//				double area = calculateArea(object, zoom);
				double len = calculateLength(object, zoom);
				if(/*tilesCovers(object, zoom, set) >= 2  && */ len > 100){
					stat.polygonBigSize ++;
					if(stat.polygonBigSize % 10000 == 0){
						return true;
					}
				}
				
				return false;
			}
		};
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom,
				sf, matcher);
		List<BinaryMapDataObject> result = reader.searchMapIndex(req);
		
		ArrayList<TagValuePair> list = new ArrayList<TagValuePair>(map.keySet());
		Collections.sort(list, new Comparator<TagValuePair>() {
			@Override
			public int compare(TagValuePair o1, TagValuePair o2) {
				return -map.get(o1) + map.get(o2);
			}

		});
		for(TagValuePair tp : list){
			Integer i = map.get(tp);
			if(i > 10){
//				System.out.println(tp.toString() + " " + i);
			}
		}
		
		for(BinaryMapDataObject obj : result){
			System.out.println("id " + (obj.getId() >> 3) + " " + calculateArea(obj, zoom));
		}
		return stat;
	}
	
	public static void main(String[] iargs) throws IOException {
		BinaryMapIndexFilter filter = new BinaryMapIndexFilter(new File(""));
		for (int i = 10; i <= 14; i++) {
			Stat st = filter.process(i);
			System.out.println(i + " zoom -> " + st);
		}
		
	}

	
}
