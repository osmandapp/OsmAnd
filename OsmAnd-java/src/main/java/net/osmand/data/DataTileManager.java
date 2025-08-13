package net.osmand.data;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.util.MapUtils;

import java.util.*;

/**
 * @param <T> - object to store in that manager
 */
public class DataTileManager<T> {

	private final int zoom;
	private final TLongObjectHashMap<List<T>> objects = new TLongObjectHashMap<>();

	public DataTileManager() {
		zoom = 15;
	}

	public DataTileManager(int z) {
		zoom = z;
	}

	public int getZoom() {
		return zoom;
	}

	public boolean isEmpty() {
		return getObjectsCount() == 0;
	}

	public int getObjectsCount() {
		int x = 0;
		for (List<T> s : objects.valueCollection()) {
			x += s.size();
		}
		return x;
	}
	
	public void printStatsDistribution(String name) {
		int min = -1, max = -1, total = 0;
		for (List<T> l : objects.valueCollection()) {
			if (min == -1) {
				max = min = l.size();
			} else {
				min = Math.min(min, l.size());
				max = Math.max(max, l.size());
			}
			total += l.size();
		}
		System.out.printf("%s tiles stores %d in %d tiles. Tile size min %d, max %d, avg %.2f.\n ", name, total, objects.size(), min, max, 
				total / (objects.size() + 0.1));
	}

	private void putObjects(long t, List<T> r) {
		if (objects.containsKey(t)) {
			r.addAll(objects.get(t));
		}
	}

	public List<T> getAllObjects() {
		List<T> l = new ArrayList<>();
		for (List<T> s : getAllEditObjects()) {
			l.addAll(s);
		}
		return l;
	}

	public List<List<T>> getAllEditObjects() {
		return new ArrayList<>(objects.valueCollection());
	}

	public List<T> getObjects(double latitudeUp, double longitudeUp, double latitudeDown, double longitudeDown) {
		int tileXUp = (int) MapUtils.getTileNumberX(zoom, longitudeUp);
		int tileYUp = (int) MapUtils.getTileNumberY(zoom, latitudeUp);
		int tileXDown = (int) MapUtils.getTileNumberX(zoom, longitudeDown) + 1;
		int tileYDown = (int) MapUtils.getTileNumberY(zoom, latitudeDown) + 1;
		List<T> result = new ArrayList<>();
		if (tileXUp > tileXDown) {
			tileXDown = tileXUp;
			tileXUp = 0;
		}
		if (tileYUp > tileYDown) {
			tileYDown = tileYUp;
			tileXUp = 0;
		}
		for (int i = tileXUp; i <= tileXDown; i++) {
			for (int j = tileYUp; j <= tileYDown; j++) {
				putObjects(evTile(i, j), result);
			}
		}
		return result;
	}

	public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31) {
		List<T> result = new ArrayList<>();
		return getObjects(leftX31, topY31, rightX31, bottomY31, result);
	}

	public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31, List<T> result) {
		int tileXUp = leftX31 >> (31 - zoom);
		int tileYUp = topY31 >> (31 - zoom);
		int tileXDown = (rightX31 >> (31 - zoom)) + 1;
		int tileYDown = (bottomY31 >> (31 - zoom)) + 1;
		for (int i = tileXUp; i <= tileXDown; i++) {
			for (int j = tileYUp; j <= tileYDown; j++) {
				putObjects(evTile(i, j), result);
			}
		}
		return result;
	}

	/**
	 * returns not exactly sorted list (but sorted by tiles)
	 */
	public List<T> getClosestObjects(double latitude, double longitude, double radius) {
		if (isEmpty()) {
			return new ArrayList<>();
		}
		double tileDist = radius / MapUtils.getTileDistanceWidth(latitude, zoom);
		int tileDistInt = (int) Math.ceil(tileDist);
		double px = MapUtils.getTileNumberX(zoom, longitude);
		double py = MapUtils.getTileNumberY(zoom, latitude);
		int stTileX = (int) px;
		int stTileY = (int) py;
		final Map<Long, Double> tiles = new HashMap<>();
		for (int xTile = -tileDistInt; xTile <= tileDistInt; xTile++) {
			for (int yTile = -tileDistInt; yTile <= tileDistInt; yTile++) {
				double dx = xTile + 0.5 - (px - stTileX);
				double dy = yTile + 0.5 - (py - stTileY);
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist <= tileDist) {
					tiles.put(evTile(stTileX + xTile, stTileY + yTile), dist);
				}
			}
		}
		List<Long> keys = new ArrayList<>(tiles.keySet());
		Collections.sort(keys, new Comparator<Long>() {
			@Override
			public int compare(Long o1, Long o2) {
				return Double.compare(tiles.get(o1), tiles.get(o2));
			}
		});
		List<T> result = new ArrayList<>();
		for (Long key : keys) {
			putObjects(key, result);
		}
		return result;
	}

	protected List<T> getClosestObjectsBySpiral(double latitude, double longitude, int startDepth, int maxDepth) {
		List<T> result = new ArrayList<>();
		int tileX = (int) MapUtils.getTileNumberX(zoom, longitude);
		int tileY = (int) MapUtils.getTileNumberY(zoom, latitude);
		if (startDepth <= 0) {
			putObjects(evTile(tileX, tileY), result);
			startDepth = 1;
		}

		// that's very difficult way visiting node : similar to visit by spiral
		// however the simplest way could be to visit row by row & after sort tiles by
		// distance (that's less efficient)

		// go through by different depth
		for (int depth = startDepth; depth <= maxDepth; depth++) {
			for (int j = 0; j <= depth; j++) {
				// left & right
				int dx = j == 0 ? 0 : -1;
				for (; dx < 1 || (j < depth && dx == 1); dx += 2) {
					// north
					putObjects(evTile(tileX + dx * j, tileY + depth), result);
					// east
					putObjects(evTile(tileX + depth, tileY - dx * j), result);
					// south
					putObjects(evTile(tileX - dx * j, tileY - depth), result);
					// west
					putObjects(evTile(tileX - depth, tileY + dx * j), result);
				}
			}
		}
		return result;
	}

	private long evTile(int tileX, int tileY) {
		return ((long) (tileX) << zoom) + tileY;
	}

	public long evaluateTile(double latitude, double longitude) {
		int tileX = (int) MapUtils.getTileNumberX(zoom, longitude);
		int tileY = (int) MapUtils.getTileNumberY(zoom, latitude);
		return evTile(tileX, tileY);
	}

	public long evaluateTileXY(int x31, int y31) {
		return evTile(x31 >> (31 - zoom), y31 >> (31 - zoom));
	}

	public void unregisterObject(double latitude, double longitude, T object) {
		long tile = evaluateTile(latitude, longitude);
		removeObject(object, tile);
	}

	public void unregisterObjectXY(int x31, int y31, T object) {
		long tile = evaluateTileXY(x31, y31);
		removeObject(object, tile);
	}

	private void removeObject(T object, long tile) {
		if (objects.containsKey(tile)) {
			objects.get(tile).remove(object);
		}
	}

	public long registerObjectXY(int x31, int y31, T object) {
		return addObject(object, evTile(x31 >> (31 - zoom), y31 >> (31 - zoom)));
	}

	public long registerObject(double latitude, double longitude, T object) {
		long tile = evaluateTile(latitude, longitude);
		return addObject(object, tile);
	}

	private long addObject(T object, long tile) {
		if (!objects.containsKey(tile)) {
			objects.put(tile, new ArrayList<T>());
		}
		objects.get(tile).add(object);
		return tile;
	}

	public void clear() {
		objects.clear();
	}

}
