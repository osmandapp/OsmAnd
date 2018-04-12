package net.osmand.plus.resources;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.osmand.map.ITileSource;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;

import java.io.File;

public class BitmapTilesCache extends TilesCache<Bitmap> {

	public BitmapTilesCache(AsyncLoadingThread asyncLoadingThread) {
		super(asyncLoadingThread);
		// it is not good investigated but no more than 64 (satellite images)
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		maxCacheSize = 28;
	}

	@Override
	public boolean isTileSourceSupported(ITileSource tileSource) {
		return !".mvt".equals(tileSource.getTileFormat());
	}

	@Override
	protected Bitmap getTileObject(TileLoadDownloadRequest req) {
		Bitmap bmp = null;
		if (req.tileSource instanceof SQLiteTileSource) {
			try {
				long[] tm = new long[1];
				bmp = ((SQLiteTileSource) req.tileSource).getImage(req.xTile, req.yTile, req.zoom, tm);
				if (tm[0] != 0) {
					downloadIfExpired(req, tm[0]);
				}
			} catch (OutOfMemoryError e) {
				log.error("Out of memory error", e); //$NON-NLS-1$
				clearTiles();
			}
		} else {
			File en = new File(req.dirWithTiles, req.tileId);
			if (en.exists()) {
				try {
					bmp = BitmapFactory.decodeFile(en.getAbsolutePath());
					downloadIfExpired(req, en.lastModified());
				} catch (OutOfMemoryError e) {
					log.error("Out of memory error", e); //$NON-NLS-1$
					clearTiles();
				}
			}
		}
		return bmp;
	}
}
