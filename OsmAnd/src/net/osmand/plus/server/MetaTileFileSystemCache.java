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
	private static final String TILES_FOLDER = "webtiles";
	private static final int MAX_IN_MEMORY_CACHE_SIZE = 16;
	private static final int MAX_CACHE_SIZE = 64;
	private final ConcurrentLinkedQueue<TileEndpoint.MetaTileCache> inMemoryCache = new ConcurrentLinkedQueue<>();
	private final File externalCacheDir;
	public boolean inMemoryCacheEnabled = true;

	public MetaTileFileSystemCache(OsmandApplication application) {
		externalCacheDir = application.getAppPath(TILES_FOLDER);
		new File(application.getExternalCacheDir(), TILES_FOLDER)
		if (!externalCacheDir.exists()) {
			externalCacheDir.mkdir();
		}
	}

	public void put(TileEndpoint.MetaTileCache tile) {
		while (inMemoryCache.size() > MAX_IN_MEMORY_CACHE_SIZE) {
			inMemoryCache.poll();
		}
		// TODO list files too slow, better to have local variable to monitor or local list
		while (externalCacheDir.listFiles().length > MAX_CACHE_SIZE) {

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

	public TileEndpoint.MetaTileCache get(int zoom, int metaTileSize, int x, int y) {
		int mx = (x / metaTileSize) * metaTileSize;
		int my = (y / metaTileSize) * metaTileSize;
		if (inMemoryCacheEnabled) {
			for (TileEndpoint.MetaTileCache r : inMemoryCache) {
				if (r.getZoom() == zoom && r.getEx() >= x && r.getEy() >= y && r.getSx() <= x && r.getSy() <= y) {
					return r;
				}
			}
		}
		File file = new File(externalCacheDir, zoom + "_" + metaTileSize + "_" + mx + "_" + my);
		if (file.exists()) {
			TileEndpoint.MetaTileCache tile = new TileEndpoint.MetaTileCache(
					BitmapFactory.decodeFile(file.getAbsolutePath()),
					mx, my, mx + metaTileSize - 1, my + metaTileSize - 1, zoom);
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
