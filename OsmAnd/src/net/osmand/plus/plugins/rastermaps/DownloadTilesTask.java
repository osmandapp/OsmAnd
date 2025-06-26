package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.DownloadType;
import net.osmand.plus.resources.ResourceManager;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
class DownloadTilesTask extends AsyncTask<Void, Void, Boolean> {

	private static final Log log = PlatformUtil.getLog(DownloadTilesTask.class);

	private static final int REQUESTS_LIMIT = 50;
	private static final long HALF_SECOND = 500;

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

	public boolean cancelled;

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

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	private boolean isTileShouldBeCounted(@NonNull DownloadRequest request) {
		int zoom = request.zoom;
		int x = request.xTile;
		int y = request.yTile;
		return minZoom <= zoom && zoom <= maxZoom
				&& DownloadTilesHelper.getTilesBorder(zoom, latLonRect, tileSource.isEllipticYTile()).contains(x, y, x, y);
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
			QuadRect border = DownloadTilesHelper.getTilesBorder(zoom, latLonRect, tileSource.isEllipticYTile());
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
