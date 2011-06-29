package net.osmand.data.preparation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapZooms {
	
	public static class MapZoomPair {
		public static int MAX_ALLOWED_ZOOM = 22;
		private int minZoom;
		private int maxZoom;
		
		public MapZoomPair(int minZoom, int maxZoom) {
			this.maxZoom = maxZoom;
			this.minZoom = minZoom;
		}

		public int getMinZoom() {
			return minZoom;
		}
		
		public int getMaxZoom() {
			return maxZoom;
		}
		
		@Override
		public String toString() {
			return "MapZoomPair : " + minZoom + " - "+ maxZoom;
		}
	}
	
	private List<MapZoomPair> levels = new ArrayList<MapZoomPair>();
	
	
	public List<MapZoomPair> getLevels() {
		return levels;
	}
	
	public void setLevels(List<MapZoomPair> levels) {
		this.levels = levels;
		Collections.sort(levels, new Comparator<MapZoomPair>() {

			@Override
			public int compare(MapZoomPair o1, MapZoomPair o2) {
				return -new Integer(o1.getMaxZoom()).compareTo(o2.getMaxZoom());
			}
		});
	}
	/**
	 * @param zooms - could be 5-8;7-10;11-14;15-
	 */
	public static MapZooms parseZooms(String zooms) throws IllegalArgumentException {
		String[] split = zooms.split(";");
		
		int zeroLevel = 15;
		List<MapZoomPair> list = new ArrayList<MapZoomPair>();
		for(String s : split){
			s = s.trim();
			int i = s.indexOf('-');
			if (i == -1) {
				zeroLevel = Integer.parseInt(s);
				list.add(0, new MapZoomPair(zeroLevel, zeroLevel));
			} else if(s.endsWith("-")){
				list.add(0, new MapZoomPair(Integer.parseInt(s.substring(0, i)), MapZoomPair.MAX_ALLOWED_ZOOM));
			} else {
				list.add(0, new MapZoomPair(Integer.parseInt(s.substring(0, i)), Integer.parseInt(s.substring(i + 1))));
			}
		}
		if(list.size() < 1 || list.size() > 8){
			throw new IllegalArgumentException("Map zooms should have at least 1 level and less than 8 levels");
		}
		MapZooms mapZooms = new MapZooms();
		mapZooms.setLevels(list);
		return mapZooms;
	}
	
	public int size(){
		return levels.size();
	}
	
	public MapZoomPair getLevel(int level){
		return levels.get(level);
	}
	
	private static MapZooms DEFAULT = null;
	public static String MAP_ZOOMS_DEFAULT = "7;8;9;10;11;12;13-14;15-";
	public static MapZooms getDefault(){
		if(DEFAULT == null){
			DEFAULT = parseZooms(MAP_ZOOMS_DEFAULT);
		}
		return DEFAULT;
		
	}

}
