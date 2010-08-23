package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.osm.MapUtils;


/**
 * 
 * @param <T> - object to store in that manager
 */
public class DataTileManager<T> {
	
	private int zoom = 15;
	
	/**
	 * map for objects stores as 'xTile_yTile' -> List<T>
	 */
	private Map<String, List<T>> objects = new HashMap<String, List<T>>();
	
	public DataTileManager(){
		zoom = 15;
	}
	
	public DataTileManager(int z){
		zoom = z;
	}
	
	public int getZoom() {
		return zoom;
	}
	
	public void setZoom(int zoom) {
		// it is required to reindex all stored objects
		if(!isEmpty()){
			throw new UnsupportedOperationException();
		}
		this.zoom = zoom;
	}
	
	public boolean isEmpty(){
		for(String s : objects.keySet()){
			if(!objects.get(s).isEmpty()){
				return false;
			}
		}
		return true;
	}
	
	public int getObjectsCount(){
		int x = 0;
		for(String s : objects.keySet()){
			x += objects.get(s).size();
		}
		
		return x;
	}
	
	private void putObjects(int tx, int ty, List<T> r){
		if(objects.containsKey(evTile(tx, ty))){
			r.addAll(objects.get(evTile(tx, ty)));
		} 
	}
	
	public List<T> getAllObjects(){
		List<T> l = new ArrayList<T>();
		for(String s : objects.keySet()){
			l.addAll(objects.get(s));
		}
		return l;
	}
	
	public List<T> getObjects(double latitudeUp, double longitudeUp, double latitudeDown, double longitudeDown) {
		int tileXUp = (int) MapUtils.getTileNumberX(zoom, longitudeUp);
		int tileYUp = (int) MapUtils.getTileNumberY(zoom, latitudeUp);
		int tileXDown = (int) MapUtils.getTileNumberX(zoom, longitudeDown) + 1;
		int tileYDown = (int) MapUtils.getTileNumberY(zoom, latitudeDown) + 1;
		List<T> result = new ArrayList<T>();
		for (int i = tileXUp; i <= tileXDown; i++) {
			for (int j = tileYUp; j <= tileYDown; j++) {
				putObjects(i, j, result);
			}
		}
		return result;
	}
	
	/**
	 * @depth of the neighbor tile to visit
	 * returns not exactly sorted list, 
	 * however the first objects are from closer tile than last
	 */
	public List<T> getClosestObjects(double latitude, double longitude, int defaultStep){
		if(isEmpty()){
			return Collections.emptyList();
		}
		int dp = 0;
		List<T> l = null;
		while (l == null || l.isEmpty()) {
			l = getClosestObjects(latitude, longitude, dp, dp + defaultStep);
			dp += defaultStep;
		}
		return l;
	}
	
	public List<T> getClosestObjects(double latitude, double longitude){
		return getClosestObjects(latitude, longitude, 3);
	}
		
	public List<T> getClosestObjects(double latitude, double longitude, int startDepth, int depth){
		int tileX = (int) MapUtils.getTileNumberX(zoom, longitude);
		int tileY = (int) MapUtils.getTileNumberY(zoom, latitude);
		List<T> result = new ArrayList<T>();
		
		if(startDepth <= 0){
			putObjects(tileX, tileY, result);
			startDepth = 1;
		}
		
		// that's very difficult way visiting node : 
		// similar to visit by spiral
		// however the simplest way could be to visit row by row & after sort tiles by distance (that's less efficient) 
		
		// go through circle
		for (int i = startDepth; i <= depth; i++) {

			// goes 
			for (int j = 0; j <= i; j++) {
				// left & right
				int dx = j == 0 ? 0 : -1;
				for (; dx < 1 || (j < i && dx == 1); dx += 2) {
					// north
					putObjects(tileX + dx * j, tileY + i, result);
					// east
					putObjects(tileX + i, tileY - dx * j, result);
					// south
					putObjects(tileX - dx * j, tileY - i, result);
					// west
					putObjects(tileX - i, tileY + dx * j, result);
				}
			}
		}
		return result;
	}
	
	private String evTile(int tileX, int tileY){
		return tileX +"_"+tileY; //$NON-NLS-1$
	}
	
	
	public String evaluateTile(double latitude, double longitude){
		int tileX = (int) MapUtils.getTileNumberX(zoom, longitude);
		int tileY = (int) MapUtils.getTileNumberY(zoom, latitude);
		return evTile(tileX, tileY);
	}
	
	public void unregisterObject(double latitude, double longitude, T object){
		String tile = evaluateTile(latitude, longitude);
		if(objects.containsKey(tile)){
			objects.get(tile).remove(object);
		}
		
	}
	
	public String registerObject(double latitude, double longitude, T object){
		String tile = evaluateTile(latitude, longitude);
		if(!objects.containsKey(tile)){
			objects.put(tile, new ArrayList<T>());
		}
		objects.get(tile).add(object);
		return tile;
	}

	
	public void clear(){
		objects.clear();
	}
	

}
