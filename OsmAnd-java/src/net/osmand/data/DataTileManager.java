package net.osmand.data;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.util.MapUtils;

/**
 * 
 * @param <T> - object to store in that manager
 */
public class DataTileManager<T> {
	
	private final int zoom;
	
	private TLongObjectHashMap<List<T>> objects = new TLongObjectHashMap<List<T>>();
	
	public DataTileManager(){
		zoom = 15;
	}
	
	public DataTileManager(int z){
		zoom = z;
	}
	
	public int getZoom() {
		return zoom;
	}

	
	public boolean isEmpty(){
		return getObjectsCount() == 0;
	}
	
	@SuppressWarnings("rawtypes")
	public int getObjectsCount(){
		int x = 0;
		for(List s : objects.valueCollection()){
			x += s.size();
		}
		return x;
	}
	
	private void putObjects(int tx, int ty, List<T> r){
		if(objects.containsKey(evTile(tx, ty))){
			r.addAll(objects.get(evTile(tx, ty)));
		} 
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<T> getAllObjects(){
		List<T> l = new ArrayList<T>();
		for(List s : objects.valueCollection()){
			l.addAll(s);
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
	
	public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31) {
		List<T> result = new ArrayList<T>();
		return getObjects(leftX31, topY31, rightX31, bottomY31, result);
	}
	
	public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31, List<T> result ) {
		int tileXUp = leftX31 >> (31 - zoom);
		int tileYUp = topY31 >> (31 - zoom);
		int tileXDown = (rightX31 >> (31 - zoom)) + 1;
		int tileYDown = (bottomY31 >> (31 - zoom)) + 1;
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
	
	private long evTile(int tileX, int tileY){
		return ((tileX) << zoom) + tileY;
	}
	
	
	public long evaluateTile(double latitude, double longitude){
		int tileX = (int) MapUtils.getTileNumberX(zoom, longitude);
		int tileY = (int) MapUtils.getTileNumberY(zoom, latitude);
		return evTile(tileX, tileY);
	}
	
	public long evaluateTileXY(int x31, int y31){
		return evTile(x31 >> (31 - zoom), y31 >> (31 - zoom));
	}
	
	public void unregisterObject(double latitude, double longitude, T object){
		long tile = evaluateTile(latitude, longitude);
		removeObject(object, tile);
	}
	
	
	public void unregisterObjectXY(int  x31, int y31, T object){
		long tile = evaluateTileXY(x31, y31);
		removeObject(object, tile);
	}

	private void removeObject(T object, long tile) {
		if(objects.containsKey(tile)){
			objects.get(tile).remove(object);
		}
	}
	
	public long registerObjectXY(int x31, int y31, T object){
		return addObject(object, evTile(x31 >> (31 - zoom), y31 >> (31 - zoom)));
	}
	
	public long registerObject(double latitude, double longitude, T object){
		long tile = evaluateTile(latitude, longitude);
		return addObject(object, tile);
	}

	private long addObject(T object, long tile) {
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
