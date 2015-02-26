package net.osmand.core.android;

import android.graphics.Bitmap;

import java.io.IOException;

import net.osmand.IndexConstants;
import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.IMapDataProvider;
import net.osmand.core.jni.MapStubStyle;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_ImageMapLayerProvider;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.resources.ResourceManager;

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
	public SWIGTYPE_p_QByteArray obtainImage(TileId tileId, ZoomLevel zoom) {
		byte[] image;
		try {
			ResourceManager rm = app.getResourceManager();
			String tileFilename = rm.calculateTileId(tileSource, tileId.getX(), tileId.getY(),
					zoom.swigValue());

			final TileReadyCallback tileReadyCallback = new TileReadyCallback(tileSource,
					tileId.getX(), tileId.getY(), zoom.swigValue());
			rm.getMapTileDownloader().addDownloaderCallback(tileReadyCallback);
			while (rm.getTileImageForMapAsync(tileFilename, tileSource, tileId.getX(), tileId.getY(),
					zoom.swigValue(), true) == null) {
				synchronized (tileReadyCallback.getSync()) {
					if (tileReadyCallback.isReady()) {
						break;
					}
					try {
						tileReadyCallback.getSync().wait(250);
					} catch (InterruptedException e) {
					}
				}
			}
			rm.getMapTileDownloader().removeDownloaderCallback(tileReadyCallback);

			image = tileSource.getBytes(tileId.getX(), tileId.getY(), zoom.swigValue(),
					app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath());
		} catch(IOException e) {
			return SwigUtilities.emptyQByteArray();
		}
		if (image == null)
			return SwigUtilities.emptyQByteArray();

		return SwigUtilities.createQByteArrayAsCopyOf(image);
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

	@Override
	public IMapDataProvider.SourceType getSourceType() {
		return IMapDataProvider.SourceType.NetworkDirect;
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
