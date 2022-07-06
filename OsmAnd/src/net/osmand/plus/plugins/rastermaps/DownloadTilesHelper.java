package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

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

		private final TilesDownloadListener listener;

		private final ResourceManager resourceManager;
		private final MapTileDownloader tileDownloader;
		private final IMapDownloaderCallback tileDownloadCallback;

		private int activeRequests = 0;
		private long availableTiles = 0;
		private long totalTilesBytes = 0;
		private final List<String> recentlyDownloadedTilesIds = new ArrayList<>();
		private final long tilesToProcess;

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

			tilesToProcess = DownloadTilesHelper.getTilesNumber(minZoom, maxZoom, latLonRect, tileSource.isEllipticYTile());

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
			return minZoom <= zoom && zoom <= maxZoom && getConstraints(zoom).contains(x, y, x, y);
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
				QuadRect constraints = getConstraints(zoom);
				for (int x = (int) constraints.left; x <= (int) constraints.right; x++) {
					for (int y = (int) constraints.top; y <= (int) constraints.bottom; y++) {
						waitOutDownloadErrors();
						downloadTile(zoom, x, y);
						waitForDownloads(false);
					}
				}
			}
		}

		private void waitOutDownloadErrors() throws InterruptedException {
			while (!cancelled && tileDownloader.shouldSkipRequests() && availableTiles < tilesToProcess) {
				Thread.sleep(HALF_SECOND);
			}
		}

		private void downloadTile(int zoom, int x, int y) {
			if (!cancelled) {
				String tileId = resourceManager.calculateTileId(tileSource, x, y, zoom);
				if (resourceManager.isTileDownloaded(tileId, tileSource, x, y, zoom)) {
					if (!recentlyDownloadedTilesIds.contains(tileId)) {
						int tileSize = resourceManager.getTileBytesSizeOnFileSystem(tileId, tileSource, x, y, zoom);
						totalTilesBytes += tileSize;
						app.runInUIThread(() -> listener.onTileDownloaded(availableTiles++, totalTilesBytes));
					}
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
					while (tileDownloader.isSomethingBeingDownloaded() && availableTiles < tilesToProcess) {
						Thread.sleep(HALF_SECOND);
					}
				} else if (activeRequests >= REQUESTS_LIMIT) {
					activeRequests = 0;
					while (tileDownloader.isSomethingBeingDownloaded() && availableTiles < tilesToProcess) {
						Thread.sleep(HALF_SECOND);
					}
				}
			}
		}

		@NonNull
		private QuadRect getConstraints(int zoom) {
			int leftX = (int) MapUtils.getTileNumberX(zoom, latLonRect.left);
			int rightX = (int) MapUtils.getTileNumberX(zoom, latLonRect.right);
			int topY = getConstraintY(zoom, true);
			int bottomY = getConstraintY(zoom, false);
			return new QuadRect(leftX, topY, rightX, bottomY);
		}

		private int getConstraintY(int zoom, boolean top) {
			double constraint = top ? latLonRect.top : latLonRect.bottom;
			return tileSource.isEllipticYTile()
					? (int) MapUtils.getTileEllipsoidNumberY(zoom, constraint)
					: (int) MapUtils.getTileNumberY(zoom, constraint);
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