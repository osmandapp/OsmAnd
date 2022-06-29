package net.osmand.core.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.IMapTiledDataProvider;
import net.osmand.core.jni.ImageMapLayerProvider;
import net.osmand.core.jni.MapStubStyle;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_ImageMapLayerProvider;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;


public class TileSourceProxyProvider extends interface_ImageMapLayerProvider {

	private static final int IMAGE_LOAD_TIMEOUT = 30000;

	private final String dirWithTiles;
	private final ResourceManager rm;
	private final BitmapTilesCache tilesCache;
	private final ITileSource tileSource;
	
	public TileSourceProxyProvider(OsmandApplication app, ITileSource tileSource) {
		this.dirWithTiles = app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath();
		this.rm = app.getResourceManager();
		this.tilesCache = rm.getBitmapTilesCache();
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
	public SWIGTYPE_p_sk_spT_SkImage_const_t obtainImage(IMapTiledDataProvider.Request request) {
		long requestTimestamp = System.currentTimeMillis();
		int zoom = request.getZoom().swigValue();
		int tileX = request.getTileId().getX();
		int tileY = request.getTileId().getY();
		float tileSize = tileSource.getTileSize();
		double offsetY = 0;
		if (tileSource.isEllipticYTile()) {
			double latitude = MapUtils.getLatitudeFromTile(zoom, tileY);
			double[] tileOffset = MapUtils.getTileEllipsoidNumberAndOffsetY(zoom, latitude, tileSource.getTileSize());
			tileY = (int) tileOffset[0];
			offsetY = tileOffset[1];
		}
		boolean shiftedTile = offsetY > 0;
		byte[] firstTileData;
		byte[] secondTileData;
		firstTileData = getTileBytes(tileX, tileY, zoom, requestTimestamp);
		if (firstTileData == null) {
			return SwigUtilities.nullSkImage();
		}

		Bitmap firstTileBitmap = BitmapFactory.decodeByteArray(firstTileData, 0, firstTileData.length);
		if (firstTileBitmap == null) {
			return SwigUtilities.nullSkImage();
		}
		if (shiftedTile) {
			Bitmap resultTileBitmap = Bitmap.createBitmap((int)tileSize, (int)tileSize, Bitmap.Config.ARGB_8888);
			Paint paint = new Paint();
			Canvas canvas = new Canvas(resultTileBitmap);
			canvas.translate(0, (float)-offsetY);
			canvas.drawBitmap(firstTileBitmap, 0, 0, paint);
			secondTileData = getTileBytes(tileX, tileY + 1, zoom, requestTimestamp);
			if (secondTileData != null) {
				Bitmap secondTileBitmap = BitmapFactory.decodeByteArray(secondTileData, 0, secondTileData.length);
				if (secondTileBitmap == null) {
					return SwigUtilities.nullSkImage();
				}
				canvas.translate(0, tileSize);
				canvas.drawBitmap(secondTileBitmap, 0, 0, paint);
			}
			return NativeUtilities.createSkImageFromBitmap(resultTileBitmap);
		} else {
			return NativeUtilities.createSkImageFromBitmap(firstTileBitmap);
		}

	}

	@Override
	public boolean supportsObtainImage() {
		return true;
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

	@Nullable
	private byte[] getTileBytes(int tileX, int tileY, int zoom, long requestTimestamp) {
		byte[] bytes = null;
		try {
			String tileFilename = rm.calculateTileId(tileSource, tileX, tileY, zoom);
			if (tileSource.couldBeDownloadedFromInternet()) {
				final TileReadyCallback tileReadyCallback = new TileReadyCallback(tileSource, tileX, tileY, zoom);
				rm.getMapTileDownloader().addDownloaderCallback(tileReadyCallback);
				try {
					while (tilesCache.getTileForMapSync(tileFilename, tileSource, tileX, tileY, zoom, true,
							requestTimestamp) == null && System.currentTimeMillis() - requestTimestamp < IMAGE_LOAD_TIMEOUT) {
						synchronized (tileReadyCallback.getSync()) {
							if (tileReadyCallback.isReady()) {
								break;
							}
							try {
								tileReadyCallback.getSync().wait(50);
							} catch (InterruptedException ignored) {
							}
						}
					}
				} finally {
					rm.getMapTileDownloader().removeDownloaderCallback(tileReadyCallback);
				}
			} else {
				tilesCache.get(tileFilename, requestTimestamp);
			}
			bytes = tileSource.getBytes(tileX, tileY, zoom, dirWithTiles);
		} catch (Exception ignore) {

		}
		return bytes;
	}
}
