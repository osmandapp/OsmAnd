package net.osmand.plus.server.endpoints;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MetaTileFileSystemCache {
	private static final Log LOG = PlatformUtil.getLog(TileEndpoint.class);
	private static final String TILES_FOLDER = "webtiles";
	static final int TILE_SIZE_PX = 256;
	static final int TILE_DENSITY = 2;
	static final int METATILE_SIZE = 4;
	private static final int MAX_IN_MEMORY_CACHE_SIZE = 16 / METATILE_SIZE;
	private static final int MAX_CACHE_SIZE = 128;

	private final ConcurrentLinkedQueue<MetaTileCache> inMemoryCache = new ConcurrentLinkedQueue<>();
	private final File externalCacheDir;

	public MetaTileFileSystemCache(OsmandApplication application) {
		externalCacheDir = application.getAppPath(TILES_FOLDER);
		if (!externalCacheDir.exists()) {
			externalCacheDir.mkdir();
		}
	}

	public void put(MetaTileCache tile) {
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
			tile.bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		if (reserveMemSlot()) {
			inMemoryCache.add(tile);
		}
	}

	private boolean reserveMemSlot() {
		while (MAX_IN_MEMORY_CACHE_SIZE > 0 && inMemoryCache.size() >= MAX_IN_MEMORY_CACHE_SIZE) {
			inMemoryCache.poll();
		}
		return MAX_IN_MEMORY_CACHE_SIZE > 0;
	}

	public MetaTileCache get(int zoom, int x, int y) {
		int mx = (x / METATILE_SIZE) * METATILE_SIZE;
		int my = (y / METATILE_SIZE) * METATILE_SIZE;
		for (MetaTileCache r : inMemoryCache) {
			if (r.zoom == zoom && r.ex >= x && r.ey >= y && r.sx <= x && r.sy <= y) {
				return r;
			}
		}
		File file = new File(externalCacheDir, zoom + "_" + METATILE_SIZE + "_" + mx + "_" + my);
		if (file.exists()) {
			MetaTileCache tile = new MetaTileCache(
					BitmapFactory.decodeFile(file.getAbsolutePath()),
					mx, my, mx + METATILE_SIZE - 1, my + METATILE_SIZE - 1, zoom);
			if (reserveMemSlot()) {
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

	public MetaTileCache createMetaTile(int zoom, int x, int y) {
		int mx = (x / METATILE_SIZE) * METATILE_SIZE;
		int my = (y / METATILE_SIZE) * METATILE_SIZE;
		double lat = MapUtils.getLatitudeFromTile(zoom, my + 0.5 * METATILE_SIZE);
		double lon = MapUtils.getLongitudeFromTile(zoom, mx + 0.5 * METATILE_SIZE);
		MetaTileCache res = new MetaTileCache();
		res.bbox = new RotatedTileBox.RotatedTileBoxBuilder()
				.setLocation(lat, lon)
				.setMapDensity(TILE_DENSITY).density(TILE_DENSITY)
				.setZoom(zoom)
				.setPixelDimensions(TILE_SIZE_PX * TILE_DENSITY * METATILE_SIZE,
						TILE_SIZE_PX * TILE_DENSITY * METATILE_SIZE, 0.5f, 0.5f).build();
		res.sx = mx;
		res.ex = mx + METATILE_SIZE - 1;
		res.sy = my;
		res.ey = my + METATILE_SIZE - 1;
		res.zoom = zoom;
		return res;
	}


	public static class MetaTileCache {
		Bitmap bmp;
		int sx;
		int sy;
		int ex;
		int ey;
		int zoom;
		public RotatedTileBox bbox;

		public MetaTileCache() {

		}

		public MetaTileCache(Bitmap bmp, int sx, int sy, int ex, int ey, int zoom) {
			this.bmp = bmp;
			this.sx = sx;
			this.sy = sy;
			this.ex = ex;
			this.ey = ey;
			this.zoom = zoom;
		}

		// to be used in file name
		public String getTileId() {
			return zoom + "_" + METATILE_SIZE + "_" + sx + "_" + sy;
		}

		public Bitmap getBitmap() {
			return bmp;
		}

		public Bitmap getSubtile(int x, int y) {
			return Bitmap.createBitmap(bmp,
					(x - sx) * TILE_SIZE_PX * TILE_DENSITY,
					(y - sy) * TILE_SIZE_PX * TILE_DENSITY,
					TILE_SIZE_PX * TILE_DENSITY, TILE_SIZE_PX * TILE_DENSITY);
		}
	}

}
