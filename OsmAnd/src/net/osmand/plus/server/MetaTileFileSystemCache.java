package net.osmand.plus.server;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.server.endpoints.TileEndpoint;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MetaTileFileSystemCache {
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	private static final Object TILES_FOLDER = "tiles";
	private static final int MAX_IN_MEMORY_CACHE_SIZE = 4;
	private static final int MAX_CACHE_SIZE = 4;
	private final ConcurrentLinkedQueue<TileEndpoint.MetaTileCache> inMemoryCache = new ConcurrentLinkedQueue<>();
	private final File externalCacheDir;
	public boolean inMemoryCacheEnabled = false;

	public MetaTileFileSystemCache(OsmandApplication application) {
		externalCacheDir = new File(
				application.getExternalCacheDir().getAbsoluteFile() + File.separator + TILES_FOLDER);
		if (!externalCacheDir.exists()) {
			externalCacheDir.mkdir();
		}
	}

	public void put(TileEndpoint.MetaTileCache tile) {
		while (inMemoryCache.size() > MAX_IN_MEMORY_CACHE_SIZE) {
			inMemoryCache.poll();
		}
		while (externalCacheDir.listFiles().length > MAX_CACHE_SIZE) {
			//remove outdated files
			for (int i = 0; i < externalCacheDir.listFiles().length - MAX_CACHE_SIZE; i++) {
				externalCacheDir.listFiles()[i].delete();
			}
		}
		String fileName = tile.getTileId();
		File file = new File(externalCacheDir, fileName);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			tile.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		if (inMemoryCacheEnabled) {
			inMemoryCache.add(tile);
		}
	}

	public TileEndpoint.MetaTileCache get(int zoom, int METATILE_SIZE, int x, int y) {
		for (int tx = x - METATILE_SIZE + 1; tx < METATILE_SIZE + x - 1; tx++) {
			for (int ty = y - METATILE_SIZE + 1; ty < METATILE_SIZE + y - 1; ty++) {
				File file = new File(externalCacheDir, zoom + "_" + METATILE_SIZE + "_" + tx + "_" + ty);
				if (file.exists()) {
					TileEndpoint.MetaTileCache tile = new TileEndpoint.MetaTileCache(
							BitmapFactory.decodeFile(file.getAbsolutePath()),
							tx, ty, tx + METATILE_SIZE, ty + METATILE_SIZE, zoom
					);
					if (inMemoryCacheEnabled) {
						inMemoryCache.add(tile);
					}
					return tile;
				}
			}
		}
		return null;
	}
}
