package net.osmand.plus.resources;

import net.osmand.binary.BinaryVectorTileReader;
import net.osmand.data.GeometryTile;
import net.osmand.map.ITileSource;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;

import static net.osmand.map.TileSourceManager.MAPILLARY_VECTOR_TILE_EXT;

public class GeometryTilesCache extends TilesCache<GeometryTile> {

	private static final int SEQUENCE_LAYER_CACHE_SIZE = 16;
	// Mapillary overscales zoom 14 tiles; Panoramax fetches zoom 17 tiles one per screen tile.
	private static final int MAPILLARY_IMAGE_LAYER_CACHE_SIZE = 4;
	private static final int PANORAMAX_IMAGE_LAYER_CACHE_SIZE = 24;
	private static final int COMBINED_IMAGE_LAYER_CACHE_SIZE = 28;

	private boolean imageLayerMode;
	private boolean mapillaryActive;
	private boolean panoramaxActive;

	public GeometryTilesCache(AsyncLoadingThread asyncLoadingThread) {
		super(asyncLoadingThread);
		// Must match imageLayerMode, which starts false: the mode setter is a no-op until it changes.
		this.maxCacheSize = SEQUENCE_LAYER_CACHE_SIZE;
	}

	public void useForMapillarySequenceLayer() {
		setImageLayerMode(false);
	}

	public void useForMapillaryImageLayer() {
		setImageLayerMode(true);
	}

	public synchronized void setMapillaryActive(boolean active) {
		if (mapillaryActive != active) {
			mapillaryActive = active;
			updateMaxCacheSize();
		}
	}

	public synchronized void setPanoramaxActive(boolean active) {
		if (panoramaxActive != active) {
			panoramaxActive = active;
			updateMaxCacheSize();
		}
	}

	private synchronized void setImageLayerMode(boolean imageLayerMode) {
		if (this.imageLayerMode != imageLayerMode) {
			this.imageLayerMode = imageLayerMode;
			// Sequence and image modes use different source zooms.
			clearAllTiles();
			updateMaxCacheSize();
		}
	}

	private void updateMaxCacheSize() {
		setMaxCacheSize(imageLayerMode ? getImageLayerCacheSize() : SEQUENCE_LAYER_CACHE_SIZE);
	}

	private int getImageLayerCacheSize() {
		if (mapillaryActive && panoramaxActive) {
			return COMBINED_IMAGE_LAYER_CACHE_SIZE;
		} else if (panoramaxActive) {
			return PANORAMAX_IMAGE_LAYER_CACHE_SIZE;
		}
		return MAPILLARY_IMAGE_LAYER_CACHE_SIZE;
	}

	@Override
	public synchronized void setMaxCacheSize(int maxCacheSize) {
		super.setMaxCacheSize(maxCacheSize);
		// clearTiles() drops about half, so repeat until the cache is within the new limit.
		while (cache.size() > maxCacheSize && cache.size() > 1) {
			clearTiles();
		}
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