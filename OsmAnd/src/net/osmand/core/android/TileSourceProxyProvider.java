package net.osmand.core.android;

import net.osmand.IndexConstants;
import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.IMapTiledDataProvider;
import net.osmand.core.jni.ImageMapLayerProvider;
import net.osmand.core.jni.MapStubStyle;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_ImageMapLayerProvider;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.ResourceManager;

public class TileSourceProxyProvider extends interface_ImageMapLayerProvider {

	private final ITileSource tileSource;
	private final String dirWithTiles;
	private final ResourceManager rm;
	private final BitmapTilesCache tilesCache;
	
	public TileSourceProxyProvider(OsmandApplication app, ITileSource tileSource) {
		this.tileSource = tileSource;
		this.dirWithTiles = app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath();
		this.rm = app.getResourceManager();
		this.tilesCache = rm.getBitmapTilesCache();
	}

	@Override
	public MapStubStyle getDesiredStubsStyle() {
		return MapStubStyle.Unspecified;
	}
	
	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMinimumZoomSupported());
	}
	
	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMaximumZoomSupported());
	}

	@Override
	public boolean supportsNaturalObtainData() {
		return true;
	}

	@Override
	public SWIGTYPE_p_QByteArray obtainImageData(IMapTiledDataProvider.Request request) {
		byte[] image;
		long requestTimestamp = System.currentTimeMillis();
		int zoom = request.getZoom().swigValue();
		int tileX = request.getTileId().getX();
		int tileY = request.getTileId().getY();
		String tileFilename = rm.calculateTileId(tileSource, tileX, tileY, zoom);
		try {
			if (tileSource.couldBeDownloadedFromInternet()) {
				final TileReadyCallback tileReadyCallback = new TileReadyCallback(tileSource, tileX, tileY, zoom);
				rm.getMapTileDownloader().addDownloaderCallback(tileReadyCallback);
				boolean error = false;
				while (tilesCache.getTileForMapSync(tileFilename, tileSource, tileX, tileY,
						zoom, true, requestTimestamp) == null) {
					synchronized (tileReadyCallback.getSync()) {
						error = tileReadyCallback.isError();
						if (tileReadyCallback.isReady()) {
							break;
						}
						try {
							tileReadyCallback.getSync().wait(50);
						} catch (InterruptedException ignored) {
						}
					}
				}
				rm.getMapTileDownloader().removeDownloaderCallback(tileReadyCallback);
				if (error) {
					return SwigUtilities.emptyQByteArray();
				}
			} else {
				tilesCache.get(tileFilename, requestTimestamp);
			}
			image = tileSource.getBytes(tileX, tileY, zoom, dirWithTiles);
		} catch (Exception e) {
			return SwigUtilities.emptyQByteArray();
		}
		if (image == null) {
			return SwigUtilities.emptyQByteArray();
		}
		return SwigUtilities.createQByteArrayAsCopyOf(image);
	}

	@Override
	public boolean supportsNaturalObtainDataAsync() {
		return true;
	}

	@Override
	public void obtainImageAsync(IMapTiledDataProvider.Request request, ImageMapLayerProvider.AsyncImageData asyncImage) {
	}
	
	@Override
	public long getTileSize() {
		return tileSource.getTileSize();
	}
	
	@Override
	public float getTileDensityFactor() {
		return 1.0f;
	}
	
	@Override
	public AlphaChannelPresence getAlphaChannelPresence() {
		return AlphaChannelPresence.Unknown;
	}

	private static class TileReadyCallback implements MapTileDownloader.IMapDownloaderCallback {
		private final ITileSource tileSource;
		private final int x;
		private final int y;
		private final int zoom;
		private boolean ready = false;
		private boolean error = false;
		private final Object sync = new Object();

		public TileReadyCallback(ITileSource tileSource, int x, int y, int zoom) {
			this.tileSource = tileSource;
			this.x = x;
			this.y = y;
			this.zoom = zoom;
		}

		public boolean isReady() {
			return ready;
		}

		public boolean isError() {
			return error;
		}

		public Object getSync() {
			return sync;
		}

		@Override
		public void tileDownloaded(DownloadRequest request) {
			if (!(request instanceof TileLoadDownloadRequest)) {
				return;
			}
			TileLoadDownloadRequest tileLoadRequest = (TileLoadDownloadRequest)request;
			if (tileSource != tileLoadRequest.tileSource ||
					x != tileLoadRequest.xTile ||
					y != tileLoadRequest.yTile ||
					zoom != tileLoadRequest.zoom) {
				return;
			}
			synchronized (sync) {
				ready = true;
				error = request.error;
				sync.notifyAll();
			}
		}
	}
}
