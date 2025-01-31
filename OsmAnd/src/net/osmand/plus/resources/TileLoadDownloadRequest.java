package net.osmand.plus.resources;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TileLoadDownloadRequest extends DownloadRequest {

	private static final Log log = PlatformUtil.getLog(TileLoadDownloadRequest.class);

	public final File dirWithTiles;
	public final ITileSource tileSource;

	public final long timestamp;

	public TileLoadDownloadRequest(File dirWithTiles, String url, File fileToSave, String tileId,
			ITileSource source, int tileX, int tileY, int zoom, long timestamp) {
		super(url, fileToSave, tileId, tileX, tileY, zoom);
		this.dirWithTiles = dirWithTiles;
		this.tileSource = source;
		this.timestamp = timestamp;
	}

	public TileLoadDownloadRequest(File dirWithTiles, String url, File fileToSave, String tileId,
			ITileSource source, int tileX,
			int tileY, int zoom, long timestamp, String referer, String userAgent) {
		this(dirWithTiles, url, fileToSave, tileId, source, tileX, tileY, zoom, timestamp);
		this.referer = referer;
		this.userAgent = userAgent;
	}

	public void saveTile(InputStream inputStream) throws IOException {
		if (tileSource instanceof SQLiteTileSource) {
			ByteArrayOutputStream stream = null;
			try {
				stream = new ByteArrayOutputStream(inputStream.available());
				Algorithms.streamCopy(inputStream, stream);
				stream.flush();

				try {
					((SQLiteTileSource) tileSource).insertImage(xTile, yTile, zoom, stream.toByteArray());
				} catch (IOException e) {
					log.warn("Tile x=" + xTile + " y=" + yTile + " z=" + zoom + " couldn't be read", e);
				}
			} finally {
				Algorithms.closeStream(inputStream);
				Algorithms.closeStream(stream);
			}
		} else {
			super.saveTile(inputStream);
		}
	}
}
