package com.osmand.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.osmand.NodeUtil;

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
	
	public int getZoom() {
		return zoom;
	}
	
	public void setZoom(int zoom) {
		// TODO !!! it is required to reindex all stored objects
		if(!objects.isEmpty()){
			throw new UnsupportedOperationException();
		}
		this.zoom = zoom;
	}
	
	private void putObjects(int tx, int ty, List<T> r){
		if(objects.containsKey(evTile(tx, ty))){
			r.addAll(objects.get(evTile(tx, ty)));
		} 
	}
	
	public List<T> getObjects(double latitudeUp, double longitudeUp, double latitudeDown, double longitudeDown) {
		int tileXUp = (int) NodeUtil.getTileNumberX(zoom, longitudeUp);
		int tileYUp = (int) NodeUtil.getTileNumberY(zoom, latitudeUp);
		int tileXDown = (int) NodeUtil.getTileNumberX(zoom, longitudeDown);
		int tileYDown = (int) NodeUtil.getTileNumberY(zoom, latitudeDown);
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
	public List<T> getClosestObjects(double latitude, double longitude, int depth){
		int tileX = (int) NodeUtil.getTileNumberX(zoom, longitude);
		int tileY = (int) NodeUtil.getTileNumberY(zoom, latitude);
		List<T> result = new ArrayList<T>();
		
		
		putObjects(tileX, tileY, result);
		
		// that's very difficult way visiting node : 
		// similar to visit by spiral
		// however the simplest way could be to visit row by row & after sort tiles by distance (that's less efficient) 
		
		// go through circle
		for (int i = 1; i <= depth; i++) {

			// goes à
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
		return tileX +"_"+tileY;
	}
	
	
	public String evaluateTile(double latitude, double longitude){
		int tileX = (int) NodeUtil.getTileNumberX(zoom, longitude);
		int tileY = (int) NodeUtil.getTileNumberY(zoom, latitude);
		return evTile(tileX, tileY);
	}
	
	public String registerObject(double latitude, double longitude, T object){
		String tile = evaluateTile(latitude, longitude);
		if(!objects.containsKey(tile)){
			objects.put(tile, new ArrayList<T>());
		}
		objects.get(tile).add(object);
		return tile;
	}
	
	
	
	// testing way to search
	public static void print(int x, int y){
		System.out.println(x + (y-1)*5);
	}
	
	public static void main(String[] args) {
		int tileX = 3; 
		int tileY = 3;
		int depth = 3;
		for(int i=1; i<=depth; i++){
			
			// goes à
			for(int j=0; j<=i; j++){
				// left & right
				int dx = j==0 ? 0 : -1;
				for(; dx < 1 || (j < i && dx == 1); dx +=2){
					// north
					print(tileX + dx * j, tileY + i);
					// east
					print(tileX + i, tileY - dx *j);
					// south
					print(tileX - dx * j, tileY - i);
					// west
					print(tileX - i, tileY + dx *j);
				}
			}
		}
	}
	
	
	

}
