package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DownloadTilesHelper implements TilesDownloadListener {

	private static final Log log = PlatformUtil.getLog(DownloadTilesHelper.class);

	private static final int BYTES_TO_MB = 1024 * 1024;
	private static final float DEFAULT_TILE_SIZE_MB = 0.012f;

	private static final long HALF_SECOND = 500;

	private final OsmandApplication app;

	private DownloadTilesTask downloadTilesTask;
	private TilesDownloadListener listener;

	public DownloadTilesHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isDownloadStarted() {
		return downloadTilesTask != null;
	}

	public boolean isDownloadFinished() {
		return downloadTilesTask != null && downloadTilesTask.getStatus() == Status.FINISHED;
	}

	public void clearDownload() {
		downloadTilesTask.cancelled = true;
		downloadTilesTask = null;
	}

	public void downloadTiles(int minZoom,
	                          int maxZoom,
	                          @NonNull QuadRect latLonRect,
	                          @NonNull ITileSource tileSource) {
		downloadTilesTask = new DownloadTilesTask(app, minZoom, maxZoom, latLonRect, tileSource, this);
		downloadTilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void setListener(@Nullable TilesDownloadListener listener) {
		this.listener = listener;
	}

	@Override
	public void onTileDownloaded(long tileNumber, long cumulativeTilesSize) {
		if (listener != null) {
			listener.onTileDownloaded(tileNumber, cumulativeTilesSize);
		}
	}

	@Override
	public void onDownloadFailed() {
		if (listener != null) {
			listener.onDownloadFailed();
		}
	}

	@SuppressWarnings("deprecation")
	private static class DownloadTilesTask extends AsyncTask<Void, Void, Void> {

		private static final int REQUESTS_LIMIT = 50;

		private final OsmandApplication app;

		private final int minZoom;
		private final int maxZoom;
		private final QuadRect latLonRect;
		private final ITileSource tileSource;

		private final TilesDownloadListener listener;

		private final ResourceManager resourceManager;
		private final MapTileDownloader tileDownloader;
		private final IMapDownloaderCallback tileDownloadCallback;

		private int activeRequests = 0;
		private long downloadedTiles = 0;
		private long totalTilesBytes = 0;

		private boolean cancelled = false;

		public DownloadTilesTask(@NonNull OsmandApplication app,
		                         int minZoom,
		                         int maxZoom,
		                         @NonNull QuadRect latLonRect,
		                         @NonNull ITileSource tileSource,
		                         @NonNull TilesDownloadListener listener) {
			this.app = app;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
			this.latLonRect = latLonRect;
			this.tileSource = tileSource;
			this.listener = listener;

			resourceManager = app.getResourceManager();
			tileDownloader = MapTileDownloader.getInstance(Version.getAppVersion(app));
			tileDownloadCallback = request -> {
				if (request != null) {
					long tileSize = request.fileToSave.length();
					totalTilesBytes += tileSize;
					app.runInUIThread(() -> listener.onTileDownloaded(downloadedTiles++, totalTilesBytes));
				}
			};
		}

		@Override
		protected Void doInBackground(Void... voids) {
			List<IMapDownloaderCallback> previousCallbacks = tileDownloader.getDownloaderCallbacks();
			tileDownloader.clearCallbacks();
			tileDownloader.addDownloaderCallback(tileDownloadCallback);
			try {
				downloadTiles();
				if (cancelled) {
					tileDownloader.refuseAllPreviousRequests();
				} else {
					waitForDownloads(true);
				}
			} catch (InterruptedException e) {
				log.error("Failed to download tiles", e);
				tileDownloader.refuseAllPreviousRequests();
				app.runInUIThread(listener::onDownloadFailed);
			} finally {
				tileDownloader.clearCallbacks();
				for (IMapDownloaderCallback callback : previousCallbacks) {
					tileDownloader.addDownloaderCallback(callback);
				}
				resourceManager.reloadTilesFromFS();
			}
			return null;
		}

		private void downloadTiles() throws InterruptedException {
			boolean ellipticYTile = tileSource.isEllipticYTile();

			for (int zoom = minZoom; zoom <= maxZoom && !cancelled; zoom++) {
				int leftX = (int) MapUtils.getTileNumberX(zoom, latLonRect.left);
				int rightX = (int) MapUtils.getTileNumberX(zoom, latLonRect.right);
				int topY;
				int bottomY;
				if (ellipticYTile) {
					topY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.top);
					bottomY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.bottom);
				} else {
					topY = (int) MapUtils.getTileNumberY(zoom, latLonRect.top);
					bottomY = (int) MapUtils.getTileNumberY(zoom, latLonRect.bottom);
				}
				downloadTilesForZoom(zoom, leftX, rightX, topY, bottomY);
			}
		}

		private void downloadTilesForZoom(int zoom, int leftX, int rightY, int topY, int bottomY)
				throws InterruptedException {
			for (int x = leftX; x <= rightY && !cancelled; x++) {
				for (int y = topY; y <= bottomY && !cancelled; y++) {
					waitOutDownloadErrors();
					downloadTile(zoom, x, y);
					waitForDownloads(false);
				}
			}
		}

		private void waitOutDownloadErrors() throws InterruptedException {
			while (!cancelled && tileDownloader.shouldSkipRequests()) {
				Thread.sleep(HALF_SECOND);
			}
		}

		private void downloadTile(int zoom, int x, int y) {
			if (!cancelled) {
				String tileId = resourceManager.calculateTileId(tileSource, x, y, zoom);
				if (resourceManager.isTileDownloaded(tileId, tileSource, x, y, zoom)) {
					int tileSize = resourceManager.getTileBytesSizeOnFileSystem(tileId, tileSource, x, y, zoom);
					totalTilesBytes += tileSize;
					app.runInUIThread(() -> listener.onTileDownloaded(downloadedTiles++, totalTilesBytes));
				} else {
					long time = System.currentTimeMillis();
					resourceManager.getTileForMapSync(tileId, tileSource, x, y, zoom, true, time);
					activeRequests++;
				}
			}
		}

		private void waitForDownloads(boolean waitForAll) throws InterruptedException {
			if (!cancelled) {
				if (waitForAll) {
					while (tileDownloader.isSomethingBeingDownloaded()) {
						Thread.sleep(HALF_SECOND);
					}
				} else if (activeRequests >= REQUESTS_LIMIT) {
					activeRequests = 0;
					while (tileDownloader.isSomethingBeingDownloaded()) {
						Thread.sleep(HALF_SECOND);
					}
				}
			}
		}
	}

	public static float getApproxTilesSizeMb(int minZoom, int maxZoom, @NonNull QuadRect latLonRect,
	                                         @NonNull ITileSource tileSource,
	                                         @NonNull BitmapTilesCache bitmapTilesCache) {
		float sizeMb = 0;
		for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
			long tilesNumber = getTilesNumber(zoom, latLonRect, tileSource.isEllipticYTile());
			long tileSize = getNearestZoomTileSize(zoom, tileSource, bitmapTilesCache);
			sizeMb += (float) tilesNumber * tileSize / BYTES_TO_MB;
		}
		return sizeMb > 0
				? sizeMb
				: getTilesNumber(minZoom, maxZoom, latLonRect, tileSource.isEllipticYTile()) * DEFAULT_TILE_SIZE_MB;
	}

	private static long getNearestZoomTileSize(int zoom, @NonNull ITileSource tileSource, @NonNull BitmapTilesCache cache) {
		long size = cache.getTileSize(tileSource, zoom);
		if (size > 0) {
			return size;
		}
		int minZoom = tileSource.getMinimumZoomSupported();
		int maxZoom = tileSource.getMaximumZoomSupported();
		int diff = 1;
		while (true) {
			boolean outOfZoomBounds = Math.min(zoom + diff, zoom - diff) < minZoom
					|| Math.max(zoom + diff, zoom - diff) > maxZoom;
			if (outOfZoomBounds) {
				return 0;
			}

			if (zoom - diff >= minZoom && zoom - diff <= maxZoom) {
				size = cache.getTileSize(tileSource, zoom - diff);
			}
			if (size <= 0 && zoom + diff >= minZoom && zoom + diff <= maxZoom) {
				size = cache.getTileSize(tileSource, zoom + diff);
			}

			if (size > 0) {
				return size;
			}

			diff++;
		}
	}

	public static long getTilesNumber(int minZoom, int maxZoom, @NonNull QuadRect latLonRect, boolean ellipticYTile) {
		long tilesNumber = 0;
		for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
			tilesNumber += getTilesNumber(zoom, latLonRect, ellipticYTile);
		}
		return tilesNumber;
	}

	private static long getTilesNumber(int zoom, @NonNull QuadRect latLonRect, boolean ellipticYTile) {
		int leftTileX = (int) MapUtils.getTileNumberX(zoom, latLonRect.left);
		int rightTileX = (int) MapUtils.getTileNumberX(zoom, latLonRect.right);
		int topTileY;
		int bottomTileY;
		if (ellipticYTile) {
			topTileY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.top);
			bottomTileY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.bottom);
		} else {
			topTileY = (int) MapUtils.getTileNumberY(zoom, latLonRect.top);
			bottomTileY = (int) MapUtils.getTileNumberY(zoom, latLonRect.bottom);
		}
		return (rightTileX - leftTileX + 1L) * (bottomTileY - topTileY + 1);
	}
}