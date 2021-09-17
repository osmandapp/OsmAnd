package net.osmand.plus.resources;

import net.osmand.binary.BinaryVectorTileReader;
import net.osmand.data.GeometryTile;
import net.osmand.map.ITileSource;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;

import java.io.File;
import java.io.IOException;

import static net.osmand.map.TileSourceManager.MAPILLARY_VECTOR_TILE_EXT;

public class GeometryTilesCache extends TilesCache<GeometryTile> {

	private final int tileZoom;

	public GeometryTilesCache(AsyncLoadingThread asyncLoadingThread, int tileZoom) {
		super(asyncLoadingThread);
		this.maxCacheSize = 4;
		this.tileZoom = tileZoom;
	}

	@Override
	public boolean isTileSourceSupported(ITileSource tileSource) {
		return MAPILLARY_VECTOR_TILE_EXT.equals(tileSource.getTileFormat());
	}

	@Override
	public boolean isTileZoomCorrect(int tileZoom) {
		return this.tileZoom == tileZoom;
	}

	@Override
	protected GeometryTile getTileObject(TileLoadDownloadRequest req) {
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
