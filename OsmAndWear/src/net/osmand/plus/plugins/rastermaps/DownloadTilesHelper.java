package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadTilesHelper implements TilesDownloadListener {

	private static final Log log = PlatformUtil.getLog(DownloadTilesHelper.class);

	public static final int BYTES_TO_MB = 1024 * 1024;
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
	                          @NonNull ITileSource tileSource,
	                          @NonNull DownloadType downloadType) {
		downloadTilesTask = new DownloadTilesTask(app, minZoom, maxZoom, latLonRect, tileSource, downloadType, this);
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
	public void onSuccessfulFinish() {
		if (listener != null) {
			listener.onSuccessfulFinish();
		}
	}

	@Override
	public void onDownloadFailed() {
		if (listener != null) {
			listener.onDownloadFailed();
		}
	}

	@SuppressWarnings("deprecation")
	private static class DownloadTilesTask extends AsyncTask<Void, Void, Boolean> {

		private static final int REQUESTS_LIMIT = 50;

		private final OsmandApplication app;

		private final int minZoom;
		private final int maxZoom;
		private final QuadRect latLonRect;
		private final ITileSource tileSource;
		private final DownloadType downloadType;

		private final TilesDownloadListener listener;

		private final ResourceManager resourceManager;
		private final MapTileDownloader tileDownloader;
		private final IMapDownloaderCallback tileDownloadCallback;

		private int activeRequests;
		private long availableTiles;
		private long totalTilesBytes;
		private final List<String> recentlyDownloadedTilesIds = Collections.synchronizedList(new ArrayList<>());

		private boolean cancelled;

		public DownloadTilesTask(@NonNull OsmandApplication app,
		                         int minZoom,
		                         int maxZoom,
		                         @NonNull QuadRect latLonRect,
		                         @NonNull ITileSource tileSource,
		                         @NonNull DownloadType downloadType,
		                         @NonNull TilesDownloadListener listener) {
			this.app = app;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
			this.latLonRect = latLonRect;
			this.tileSource = tileSource;
			this.downloadType = downloadType;
			this.listener = listener;

			resourceManager = app.getResourceManager();
			tileDownloader = MapTileDownloader.getInstance(Version.getAppVersion(app));
			tileDownloadCallback = request -> {
				if (request != null && isTileShouldBeCounted(request)) {
					recentlyDownloadedTilesIds.add(request.tileId);
					long tileSize = request.fileToSave.length();
					totalTilesBytes += tileSize;
					app.runInUIThread(() -> listener.onTileDownloaded(availableTiles++, totalTilesBytes));
				}
			};
		}

		private boolean isTileShouldBeCounted(@NonNull DownloadRequest request) {
			int zoom = request.zoom;
			int x = request.xTile;
			int y = request.yTile;
			return minZoom <= zoom
					&& zoom <= maxZoom
					&& getTilesBorder(zoom, latLonRect, tileSource.isEllipticYTile()).contains(x, y, x, y);
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			tileDownloader.refuseAllPreviousRequests();

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
				return false;
			} finally {
				tileDownloader.clearCallbacks();
				for (IMapDownloaderCallback callback : previousCallbacks) {
					tileDownloader.addDownloaderCallback(callback);
				}
				resourceManager.reloadTilesFromFS();
			}
			return true;
		}

		private void downloadTiles() throws InterruptedException {
			for (int zoom = minZoom; zoom <= maxZoom && !cancelled; zoom++) {
				QuadRect border = getTilesBorder(zoom, latLonRect, tileSource.isEllipticYTile());
				int left = (int) border.left;
				int top = (int) border.top;
				int right = (int) border.right;
				int bottom = (int) border.bottom;

				for (int x = left; x <= right && !cancelled; x++) {
					for (int y = top; y <= bottom && !cancelled; y++) {
						handleTile(zoom, x, y);
					}
				}
			}
		}

		private void handleTile(int zoom, int x, int y) throws InterruptedException {
			if (downloadType == DownloadType.FORCE_ALL) {
				downloadTile(zoom, x, y);
			} else {
				String tileId = resourceManager.calculateTileId(tileSource, x, y, zoom);
				boolean downloaded = resourceManager.isTileDownloaded(tileId, tileSource, x, y, zoom);
				if (downloaded && downloadType == DownloadType.ALL) {
					if (!recentlyDownloadedTilesIds.contains(tileId)) {
						int tileSize = resourceManager.getTileBytesSizeOnFileSystem(tileId, tileSource, x, y, zoom);
						totalTilesBytes += tileSize;
						app.runInUIThread(() -> listener.onTileDownloaded(availableTiles++, totalTilesBytes));
					}
				} else if (!downloaded) {
					downloadTile(zoom, x, y);
				}
			}
		}

		private void downloadTile(int zoom, int x, int y) throws InterruptedException {
			waitOutDownloadErrors();
			resourceManager.downloadTileForMapSync(tileSource, x, y, zoom);
			activeRequests++;
			waitForDownloads(false);
		}

		private void waitOutDownloadErrors() throws InterruptedException {
			while (!cancelled && tileDownloader.shouldSkipRequests()) {
				Thread.sleep(HALF_SECOND);
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

		@Override
		protected void onPostExecute(@NonNull Boolean successful) {
			if (successful) {
				app.runInUIThread(listener::onSuccessfulFinish);
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

	public static long getNearestZoomTileSize(int zoom, @NonNull ITileSource tileSource, @NonNull BitmapTilesCache cache) {
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

	@NonNull
	public static QuadRect getTilesBorder(int zoom, @NonNull QuadRect latLonRect, boolean ellipticYTile) {
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
		return new QuadRect(leftX, topY, rightX, bottomY);
	}

	enum DownloadType {

		FORCE_ALL,     // All tiles will be downloaded and counted
		ALL,           // Missing tiles will be downloaded, present tiles won't be downloaded, all tiles will be counted
		ONLY_MISSING   // Only missing tiles will be downloaded and counted
	}
}