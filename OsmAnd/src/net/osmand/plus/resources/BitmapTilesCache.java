package net.osmand.plus.resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.osmand.map.ITileSource;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

import static net.osmand.map.TileSourceManager.MAPILLARY_VECTOR_TILE_EXT;

public class BitmapTilesCache extends TilesCache<Bitmap> {

	private final Map<String, Map<Integer, Integer>> mapsTilesSizes = new HashMap<>();

	public BitmapTilesCache(AsyncLoadingThread asyncLoadingThread) {
		super(asyncLoadingThread);
		// it is not good investigated but no more than 64 (satellite images)
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		maxCacheSize = 28;
	}

	public int getTileSize(@NonNull ITileSource tileSource, int zoom) {
		Map<Integer, Integer> tilesSizes = mapsTilesSizes.get(tileSource.getName());
		if (tilesSizes == null) {
			return 0;
		}
		Integer size = tilesSizes.get(zoom);
		return size == null ? 0 : size;
	}

	@Override
	public boolean isTileSourceSupported(ITileSource tileSource) {
		return !MAPILLARY_VECTOR_TILE_EXT.equals(tileSource.getTileFormat());
	}

	@Override
	protected Bitmap getTileObject(@NonNull TileLoadDownloadRequest req) {
		Bitmap bmp = null;
		if (req.tileSource instanceof SQLiteTileSource) {
			try {
				long[] tm = new long[1];
				bmp = ((SQLiteTileSource) req.tileSource).getImage(req.xTile, req.yTile, req.zoom, tm);
				if (tm[0] != 0) {
					downloadIfExpired(req, tm[0]);
				}
			} catch (OutOfMemoryError e) {
				log.error("Out of memory error", e);
				clearTiles();
			}
		} else {
			File en = new File(req.dirWithTiles, req.tileId);
			if (en.exists()) {
				try {
					bmp = BitmapFactory.decodeFile(en.getAbsolutePath());
					downloadIfExpired(req, en.lastModified());
				} catch (OutOfMemoryError e) {
					log.error("Out of memory error", e);
					clearTiles();
				}
			}
		}

		if (bmp != null) {
			updateTilesSizes(req);
		}

		return bmp;
	}

	private void updateTilesSizes(@NonNull TileLoadDownloadRequest request) {
		String mapName = request.tileSource.getName();
		Map<Integer, Integer> tilesSizes = mapsTilesSizes.get(mapName);
		if (tilesSizes == null) {
			tilesSizes = new HashMap<>();
			mapsTilesSizes.put(mapName, tilesSizes);
		}

		int tileSize = getTileBytesSizeOnFileSystem(request.tileId, request.tileSource,
				request.xTile, request.yTile, request.zoom);
		Integer storedTileSize = tilesSizes.get(request.zoom);
		if (storedTileSize == null || storedTileSize < tileSize) {
			tilesSizes.put(request.zoom, tileSize);
		}
	}
}