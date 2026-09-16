package net.osmand.router;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.util.MapUtils;

/**
 * Land far from any shore, from the ocean flags of the world basemap: its map boxes are marked ocean or land where
 * they hold no coastline. Regional maps carry no such flags.
 *
 * The flags are coarse - a narrow fjord or the Wadden Sea is land to them - so they only decide where the
 * coastline cannot: a tile counts as land when the basemap marks it land and nothing in it ocean.
 */
public class BasemapLandTiles implements SeaObstacles.FarFromShore {

	/** Fine enough to tell Leiden from the North Sea 8 km away, coarse enough to hit the basemap's flags. */
	private static final int ZOOM = 11;
	private static final SearchFilter NO_OBJECTS = new SearchFilter() {
		@Override
		public boolean accept(TIntArrayList types, MapIndex index) {
			return false;
		}
	};

	private final BinaryMapIndexReader basemap;
	private final Map<Long, Boolean> tiles = new HashMap<>();

	public BasemapLandTiles(BinaryMapIndexReader basemap) {
		this.basemap = basemap;
	}

	@Override
	public boolean isLand(double lat, double lon) {
		int shift = 31 - ZOOM;
		int tileX = MapUtils.get31TileNumberX(lon) >> shift, tileY = MapUtils.get31TileNumberY(lat) >> shift;
		long key = (((long) tileX) << 32) | (tileY & 0xffffffffL);
		Boolean land = tiles.get(key);
		if (land == null) {
			land = queryTile(tileX << shift, tileY << shift, (1 << shift) - 1);
			tiles.put(key, land);
		}
		return land;
	}

	private boolean queryTile(int left, int top, int size) {
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(left, left + size, top,
				top + size, ZOOM, NO_OBJECTS);
		try {
			for (MapIndex index : basemap.getMapIndexes()) {
				basemap.searchMapIndex(req, index);
			}
		} catch (IOException e) {
			return false;
		}
		return req.isLand() && !req.isOcean();
	}
}
