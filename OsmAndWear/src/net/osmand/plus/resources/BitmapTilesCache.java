package net.osmand.plus.resources;

import static net.osmand.map.TileSourceManager.MAPILLARY_VECTOR_TILE_EXT;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.ITileSource;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BitmapTilesCache extends TilesCache<Bitmap> {

	private final Map<String, Map<Integer, Long>> mapsTilesSizes = new HashMap<>();

	public BitmapTilesCache(AsyncLoadingThread asyncLoadingThread) {
		super(asyncLoadingThread);
		// it is not good investigated but no more than 64 (satellite images)
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		maxCacheSize = 28;
	}

	public synchronized long getTileSize(@NonNull ITileSource tileSource, int zoom) {
		Map<Integer, Long> tilesSizes = mapsTilesSizes.get(tileSource.getName());
		if (tilesSizes == null) {
			return 0;
		}
		Long size = tilesSizes.get(zoom);
		return size == null ? 0 : size;
	}

	@Override
	public boolean isTileSourceSupported(ITileSource tileSource) {
		return !MAPILLARY_VECTOR_TILE_EXT.equals(tileSource.getTileFormat());
	}

	@Override
	protected Bitmap getTileObject(@NonNull TileLoadDownloadRequest req) {
		return req.tileSource instanceof SQLiteTileSource
				? getBitmapFromDb(((SQLiteTileSource) req.tileSource), req)
				: getBitmapFromFile(req);
	}

	@Nullable
	private Bitmap getBitmapFromDb(@NonNull SQLiteTileSource tileSource, @NonNull TileLoadDownloadRequest request) {
		Bitmap bitmap = null;
		try {
			long[] timeHolder = new long[1];
			byte[] blob = tileSource.getBytes(request.xTile, request.yTile, request.zoom,null, timeHolder);
			if (blob != null) {
				String[] params = tileSource.getTileDbParams(request.xTile, request.yTile, request.zoom);
				bitmap = tileSource.getImage(blob, params);
			}
			if (bitmap != null) {
				updateTilesSizes(tileSource.getName(), request.zoom, blob.length);
			}
			if (timeHolder[0] != 0) {
				downloadIfExpired(request, timeHolder[0]);
			}
		} catch (OutOfMemoryError e) {
			log.error("Out of memory error", e);
			clearTiles();
		} catch (IOException e) {
			log.error("Failed to get tile bytes", e);
		}
		return bitmap;
	}

	@Nullable
	private Bitmap getBitmapFromFile(@NonNull TileLoadDownloadRequest request) {
		Bitmap bitmap = null;
		File en = new File(request.dirWithTiles, request.tileId);
		if (en.exists()) {
			try {
				bitmap = BitmapFactory.decodeFile(en.getAbsolutePath());
				downloadIfExpired(request, en.lastModified());
				if (bitmap != null) {
					updateTilesSizes(request.tileSource.getName(), request.zoom, en.length());
				}
			} catch (OutOfMemoryError e) {
				log.error("Out of memory error", e);
				clearTiles();
			} catch (SecurityException e) {
				log.error("No access to file", e);
			}
		}
		return bitmap;
	}

	private synchronized void updateTilesSizes(@NonNull String mapName, int zoom, long tileSize) {
		Map<Integer, Long> tilesSizes = mapsTilesSizes.get(mapName);
		if (tilesSizes == null) {
			tilesSizes = new HashMap<>();
			mapsTilesSizes.put(mapName, tilesSizes);
		}

		Long storedTileSize = tilesSizes.get(zoom);
		if (storedTileSize == null || storedTileSize < tileSize) {
			tilesSizes.put(zoom, tileSize);
		}
	}
}