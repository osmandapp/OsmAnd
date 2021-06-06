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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.resources.ResourceManager;

import java.io.IOException;

public class TileSourceProxyProvider extends interface_ImageMapLayerProvider {

	private final OsmandApplication app;
	private final ITileSource tileSource;
	
	public TileSourceProxyProvider(OsmandApplication app, ITileSource tileSource) {
		this.app = app;
		this.tileSource = tileSource;
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
	public SWIGTYPE_p_QByteArray obtainImage(IMapTiledDataProvider.Request request) {
		byte[] image;
		try {
			long requestTimestamp = System.currentTimeMillis();
			int zoom = request.getZoom().swigValue();
			int tileX = request.getTileId().getX();
			int tileY = request.getTileId().getY();

			ResourceManager rm = app.getResourceManager();
			String tileFilename = rm.calculateTileId(tileSource, tileX, tileY, zoom);

			final TileReadyCallback tileReadyCallback = new TileReadyCallback(tileSource, tileX, tileY, zoom);
			rm.getMapTileDownloader().addDownloaderCallback(tileReadyCallback);

			while (rm.getBitmapTilesCache().getTileForMapAsync(tileFilename, tileSource, tileX, tileY,
					zoom, true, requestTimestamp) == null) {
				synchronized (tileReadyCallback.getSync()) {
					if (tileReadyCallback.isReady()) {
						break;
					}
					try {
						tileReadyCallback.getSync().wait(50);
					} catch (InterruptedException e) {
					}
				}
			}
			rm.getMapTileDownloader().removeDownloaderCallback(tileReadyCallback);

			image = tileSource.getBytes(tileX, tileY, zoom, app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath());
		} catch (IOException e) {
			return SwigUtilities.emptyQByteArray();
		}
		if (image == null)
			return SwigUtilities.emptyQByteArray();

		return SwigUtilities.createQByteArrayAsCopyOf(image);
	}

	@Override
	public boolean supportsNaturalObtainDataAsync() {
		return false;
	}

	@Override
	public void obtainImageAsync(IMapTiledDataProvider.Request request, ImageMapLayerProvider.AsyncImage asyncImage) {
		//TODO: Launch the request via manager and after image is ready (or error is ready)
		// call asyncImage.submit()
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

		public Object getSync() {
			return sync;
		}

		@Override
		public void tileDownloaded(MapTileDownloader.DownloadRequest request) {
			if (!(request instanceof AsyncLoadingThread.TileLoadDownloadRequest)) {
				return;
			}
			AsyncLoadingThread.TileLoadDownloadRequest tileLoadRequest =
					(AsyncLoadingThread.TileLoadDownloadRequest)request;

			if (tileSource != tileLoadRequest.tileSource ||
					x != tileLoadRequest.xTile ||
					y != tileLoadRequest.yTile ||
					zoom != tileLoadRequest.zoom) {
				return;
			}

			synchronized (sync) {
				ready = true;
				sync.notifyAll();
			}
		}
	}
}
