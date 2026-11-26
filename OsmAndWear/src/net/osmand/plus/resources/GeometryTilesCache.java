package net.osmand.plus.resources;

import net.osmand.binary.BinaryVectorTileReader;
import net.osmand.data.GeometryTile;
import net.osmand.map.ITileSource;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;

import static net.osmand.map.TileSourceManager.MAPILLARY_VECTOR_TILE_EXT;

public class GeometryTilesCache extends TilesCache<GeometryTile> {

	private static final int MAPILLARY_SEQUENCE_LAYER_CACHE_SIZE = 16;
	private static final int MAPILLARY_IMAGE_LAYER_CACHE_SIZE = 4;

	public GeometryTilesCache(AsyncLoadingThread asyncLoadingThread) {
		super(asyncLoadingThread);
		this.maxCacheSize = 4;
	}

	public void useForMapillarySequenceLayer() {
		changeMapillaryLayerToCache(MAPILLARY_SEQUENCE_LAYER_CACHE_SIZE);
	}

	public void useForMapillaryImageLayer() {
		changeMapillaryLayerToCache(MAPILLARY_IMAGE_LAYER_CACHE_SIZE);
	}

	private void changeMapillaryLayerToCache(int maxCacheSize) {
		if (this.maxCacheSize != maxCacheSize) {
			setMaxCacheSize(maxCacheSize);
		}
	}

	@Override
	public void setMaxCacheSize(int maxCacheSize) {
		super.setMaxCacheSize(maxCacheSize);
		cache.clear();
	}

	@Override
	public boolean isTileSourceSupported(ITileSource tileSource) {
		return MAPILLARY_VECTOR_TILE_EXT.equals(tileSource.getTileFormat());
	}

	@Override
	protected GeometryTile getTileObject(@NonNull TileLoadDownloadRequest req) {
		GeometryTile tile = null;
		File en = new File(req.dirWithTiles, req.tileId);
		if (en.exists()) {
			try {
				tile = BinaryVectorTileReader.readTile(en);
				downloadIfExpired(req, en.lastModified());
			} catch (IOException e) {
				log.error("Cannot read tile", e);
			} catch (OutOfMemoryError e) {
				log.error("Out of memory error", e);
				clearTiles();
			}
		}
		return tile;
	}
}