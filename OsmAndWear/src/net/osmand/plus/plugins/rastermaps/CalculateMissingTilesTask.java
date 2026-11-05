package net.osmand.plus.plugins.rastermaps;

import android.os.AsyncTask;

import net.osmand.CallbackWithObject;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.rastermaps.CalculateMissingTilesTask.MissingTilesInfo;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.ResourceManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import androidx.annotation.NonNull;

import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.BYTES_TO_MB;
import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.getNearestZoomTileSize;
import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.getTilesBorder;
import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.getTilesNumber;

@SuppressWarnings("deprecation")
class CalculateMissingTilesTask extends AsyncTask<Void, Void, MissingTilesInfo> {

	public static final int TOTAL_TILES_TO_CHECK = 1000;

	private final ResourceManager resourceManager;
	private final ITileSource tileSource;
	private final CallbackWithObject<MissingTilesInfo> listener;

	private final int minZoom;
	private final int maxZoom;
	private final QuadRect latLonRect;

	private boolean cancelled;

	public CalculateMissingTilesTask(@NonNull OsmandApplication app,
	                                 @NonNull ITileSource tileSource,
	                                 int minZoom,
	                                 int maxZoom,
	                                 @NonNull QuadRect latLonRect,
	                                 @NonNull CallbackWithObject<MissingTilesInfo> listener) {
		this.resourceManager = app.getResourceManager();
		this.tileSource = tileSource;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.latLonRect = latLonRect;
		this.listener = listener;
	}

	public void cancel() {
		cancelled = true;
		cancel(false);
	}

	@NonNull
	@Override
	protected MissingTilesInfo doInBackground(Void... voids) {
		long tilesNumber = getTilesNumber(minZoom, maxZoom, latLonRect, tileSource.isEllipticYTile());
		return tilesNumber > TOTAL_TILES_TO_CHECK
				? getApproxMissingTilesInfo()
				: getPreciseMissingTilesInfo();
	}

	@NonNull
	private MissingTilesInfo getApproxMissingTilesInfo() {
		MissingTilesInfo missingTilesInfo = new MissingTilesInfo(tileSource, resourceManager.getBitmapTilesCache(), true);

		boolean ellipticYTile = tileSource.isEllipticYTile();
		long tilesNumber = getTilesNumber(minZoom, maxZoom, latLonRect, ellipticYTile);

		for (int zoom = minZoom; zoom <= maxZoom && !cancelled; zoom++) {
			long tilesForZoom = getTilesNumber(zoom, zoom, latLonRect, ellipticYTile);
			float ratio = (float) tilesForZoom / tilesNumber;
			int tilesToCheck = (int) (ratio * TOTAL_TILES_TO_CHECK);

			QuadRect border = getTilesBorder(zoom, latLonRect, ellipticYTile);
			int left = (int) border.left;
			int top = (int) border.top;
			int right = (int) border.right;
			int bottom = (int) border.bottom;
			int width = right - left + 1;
			int height = bottom - top + 1;

			long missingTiles = 0;

			for (int i = 0; i < tilesToCheck && !cancelled; i++) {
				int randomX = new Random().nextInt(width);
				int randomY = new Random().nextInt(height);
				int x = left + randomX;
				int y = top + randomY;
				String tileId = resourceManager.calculateTileId(tileSource, x, y, zoom);
				if (!resourceManager.isTileDownloaded(tileId, tileSource, x, y, zoom)) {
					missingTiles++;
				}
			}

			float approxMissingRatio = (float) missingTiles / tilesToCheck;
			long approxMissingTilesForZoom = (long) (tilesForZoom * approxMissingRatio);

			missingTilesInfo.addMissingTilesForZoom(zoom, approxMissingTilesForZoom);
		}

		return missingTilesInfo;
	}

	@NonNull
	private MissingTilesInfo getPreciseMissingTilesInfo() {
		MissingTilesInfo missingTilesInfo = new MissingTilesInfo(tileSource, resourceManager.getBitmapTilesCache(), false);

		for (int zoom = minZoom; zoom <= maxZoom && !cancelled; zoom++) {
			QuadRect border = DownloadTilesHelper.getTilesBorder(zoom, latLonRect, tileSource.isEllipticYTile());
			int left = (int) border.left;
			int top = (int) border.top;
			int right = (int) border.right;
			int bottom = (int) border.bottom;

			int missingTilesForZoom = 0;

			for (int x = left; x <= right && !cancelled; x++) {
				for (int y = top; y <= bottom && !cancelled; y++) {
					String tileId = resourceManager.calculateTileId(tileSource, x, y, zoom);
					if (!resourceManager.isTileDownloaded(tileId, tileSource, x, y, zoom)) {
						missingTilesForZoom++;
					}
				}
			}

			missingTilesInfo.addMissingTilesForZoom(zoom, missingTilesForZoom);
		}

		return missingTilesInfo;
	}

	@Override
	protected void onPostExecute(@NonNull MissingTilesInfo missingTilesInfo) {
		if (!cancelled) {
			listener.processResult(missingTilesInfo);
		}
	}

	static class MissingTilesInfo {

		private final ITileSource tileSource;
		private final BitmapTilesCache bitmapTilesCache;

		private final boolean approximate;
		private final Map<Integer, Long> missingTilesByZooms = new LinkedHashMap<>();

		public MissingTilesInfo(@NonNull ITileSource tileSource, @NonNull BitmapTilesCache bitmapTilesCache, boolean approximate) {
			this.tileSource = tileSource;
			this.bitmapTilesCache = bitmapTilesCache;
			this.approximate = approximate;
		}

		public boolean isApproximate() {
			return approximate;
		}

		private void addMissingTilesForZoom(int zoom, long missingTiles) {
			missingTilesByZooms.put(zoom, missingTiles);
		}

		public long getMissingTiles() {
			long missingTiles = 0;
			for (long tiles : missingTilesByZooms.values()) {
				missingTiles += tiles;
			}
			return missingTiles;
		}

		public float getApproxMissingSizeMb() {
			float approxSize = 0;
			for (Entry<Integer, Long> zoomInfo : missingTilesByZooms.entrySet()) {
				int zoom = zoomInfo.getKey();
				long missingTiles = zoomInfo.getValue();
				approxSize += getNearestZoomTileSize(zoom, tileSource, bitmapTilesCache) * missingTiles;
			}
			return approxSize / BYTES_TO_MB;
		}
	}
}