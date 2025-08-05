package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class DownloadTilesHelper implements TilesDownloadListener {

	private static final Log log = PlatformUtil.getLog(DownloadTilesHelper.class);

	public static final int BYTES_TO_MB = 1024 * 1024;
	private static final float DEFAULT_TILE_SIZE_MB = 0.012f;

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
		if (downloadTilesTask != null) {
			downloadTilesTask.setCancelled(true); // TODO replace with system cancel()
		}
		downloadTilesTask = null;
	}

	public void downloadTiles(int minZoom, int maxZoom, @NonNull QuadRect latLonRect,
			@NonNull ITileSource tileSource, @NonNull DownloadType downloadType) {
		downloadTilesTask = new DownloadTilesTask(app, minZoom, maxZoom, latLonRect, tileSource, downloadType, this);
		OsmAndTaskManager.executeTask(downloadTilesTask);
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

	public static float getApproxTilesSizeMb(int minZoom, int maxZoom, @NonNull QuadRect latLonRect,
			@NonNull ITileSource tileSource, @NonNull BitmapTilesCache bitmapTilesCache) {
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

	public enum DownloadType {
		FORCE_ALL,     // All tiles will be downloaded and counted
		ALL,           // Missing tiles will be downloaded, present tiles won't be downloaded, all tiles will be counted
		ONLY_MISSING   // Only missing tiles will be downloaded and counted
	}
}