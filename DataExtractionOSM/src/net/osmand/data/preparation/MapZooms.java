package net.osmand.data.preparation;

import java.util.ArrayList;
import java.util.List;

public class MapZooms {
	
	public static class MapZoomPair {
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
		
	}
	
	private List<MapZoomPair> levels = new ArrayList<MapZoomPair>();
	
	
	public List<MapZoomPair> getLevels() {
		return levels;
	}
	
	public void setLevels(List<MapZoomPair> levels) {
		this.levels = levels;
	}
	/**
	 * @param zooms - could be 5-8;7-10;13-15;15
	 */
	public static MapZooms parseZooms(String zooms) throws IllegalArgumentException {
		String[] split = zooms.split(";");
		
		int zeroLevel = 15;
		List<MapZoomPair> list = new ArrayList<MapZoomPair>();
		for(String s : split){
			s = s.trim();
			int i = s.indexOf('-');
			if(i == -1){
				zeroLevel = Integer.parseInt(s);
			} else {
				list.add(0, new MapZoomPair(Integer.parseInt(s.substring(0, i)), Integer.parseInt(s.substring(i + 1))));
			}
		}
		list.add(0, new MapZoomPair(zeroLevel, 22));
		if(list.size() < 1 || list.size() > 4){
			throw new IllegalArgumentException("Map zooms should have at least 1 level and less than 4 levels");
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
	public static String MAP_ZOOMS_DEFAULT = "5-8;9-11;12-14;15";
	public static MapZooms getDefault(){
		if(DEFAULT == null){
			DEFAULT = parseZooms(MAP_ZOOMS_DEFAULT);
		}
		return DEFAULT;
		
	}

}
