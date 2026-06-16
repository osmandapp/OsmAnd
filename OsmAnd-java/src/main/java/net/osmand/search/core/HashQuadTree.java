package net.osmand.search.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.util.MapUtils;

/**
 * 
 * This is not exact QuadTree implementation -
 * It doesn't support search for containing objects for performance reasons.
 * In order to find all objects inside large polygon, reverse - HashQuadTree could be used
 * as it requires full scanning of objects and another index
 */
public class HashQuadTree<T> {
	private static final int MAX_ZOOM = 20;
	TLongObjectHashMap<List<T>>[] indexByTileId;

	@SuppressWarnings("unchecked")
	public HashQuadTree(int maxZoom) {
		indexByTileId = new TLongObjectHashMap[maxZoom + 1];
	}

	public void put(int z, long tileId, T value) {
		if (indexByTileId[z] == null) {
			indexByTileId[z] = new TLongObjectHashMap<>();
		}
		List<T> list = indexByTileId[z].get(tileId);
		if (list == null) {
			list = new ArrayList<>();
			indexByTileId[z].put(tileId, list);
		}
		list.add(value);
	}

	public void forEachMatchHigherZoom(int startZoom, long tileId, Consumer<List<T>> action) {
		startZoom--;
		tileId >>= 2;
		forEachMatch(startZoom, tileId, action);
	}

	public void forEachMatch(int startZoom, long tileId, Consumer<List<T>> action) {
		forEachMatch(startZoom, 0, tileId, action);
	}

	public void forEachMatch(int startZoom, int endZoom, long tileId, Consumer<List<T>> action) {
		for (int z = startZoom; z >= endZoom; z--) {
			if (indexByTileId[z] != null) {
				List<T> res = indexByTileId[z].get(tileId);
				if (res != null) {
					action.accept(res);
				}
			}
			tileId >>= 2;
		}
	}

	public static long encodeTileId(int z, int x, int y) {
		if (z > MAX_ZOOM) {
			throw new UnsupportedOperationException();
		}
		long il = (MapUtils.interleaveBits(x, y));
		return il;
	}
}