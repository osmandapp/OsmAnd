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
	private static final int MAX_IN_MEMORY_CACHE_SIZE = 128;
	private static final int MAX_CACHE_SIZE = 64;
	private final ConcurrentLinkedQueue<TileEndpoint.MetaTileCache> inMemoryCache = new ConcurrentLinkedQueue<>();
	private final File externalCacheDir;
	public boolean inMemoryCacheEnabled = true;

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
		int mx = (x / METATILE_SIZE) * METATILE_SIZE;
		int my = (y / METATILE_SIZE) * METATILE_SIZE;
		if (inMemoryCacheEnabled) {
			for (TileEndpoint.MetaTileCache r : inMemoryCache) {
				if (r.getZoom() == zoom && r.getEx() >= x && r.getEy() >= y && r.getSx() <= x && r.getSy() <= y) {
					return r;
				}
			}
		}
		File file = new File(externalCacheDir, zoom + "_" + METATILE_SIZE + "_" + mx + "_" + my);
		if (file.exists()) {
			TileEndpoint.MetaTileCache tile = new TileEndpoint.MetaTileCache(
					BitmapFactory.decodeFile(file.getAbsolutePath()),
					mx, my, mx + METATILE_SIZE - 1, my + METATILE_SIZE - 1, zoom);
			if (inMemoryCacheEnabled) {
				inMemoryCache.add(tile);
			}
			return tile;
		}
		return null;
	}

	public void clearCache() {
		clearInMemoryCache();
		clearFileCache();
	}

	private void clearFileCache() {
		for (int i = 0; i < externalCacheDir.listFiles().length; i++) {
			externalCacheDir.listFiles()[i].delete();
		}
	}

	private void clearInMemoryCache() {
		inMemoryCache.clear();
	}
}
