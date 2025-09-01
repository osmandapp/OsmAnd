package net.osmand.plus.resources;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Stack;

/**
 * Thread to load map objects (POI, transport stops) async
 */
public class AsyncLoadingThread extends Thread {

	private static final int CACHE_LAYER_SIZE_EXPIRE_TIME_MS = 30 * 1000;

	private static final Log log = PlatformUtil.getLog(AsyncLoadingThread.class);

	private final Stack<Object> requests = new Stack<>();
	private final ResourceManager resourceManger;

	public AsyncLoadingThread(ResourceManager resourceManger) {
		super("Loader map objects (synchronizer)");
		this.resourceManger = resourceManger;
	}

	@Override
	public void run() {
		int cacheCounter = 0;
		long lastRequestTimestamp = 0;
		while (true) {
			try {
				if (lastRequestTimestamp != 0 && System.currentTimeMillis() - lastRequestTimestamp > 750) {
					lastRequestTimestamp = 0;
					updateBitmapTilesCache();
				}
				boolean tileLoaded = false;
				boolean mapLoaded = false;
				while (!requests.isEmpty()) {
					cacheCounter++;
					Object req = requests.pop();
					if (req instanceof TileLoadDownloadRequest request) {
						tileLoaded |= resourceManger.hasRequestedTile(request);
					} else if (req instanceof MapLoadRequest request) {
						if (!mapLoaded || request.forceLoadMap) {
							resourceManger.getRenderer().loadMap(request.tileBox, resourceManger.getMapTileDownloader());
							mapLoaded = !resourceManger.getRenderer().wasInterrupted();

							if (request.listener != null) {
								request.listener.onMapLoaded(!mapLoaded);
							}
						}
					}
					if (cacheCounter == 10) {
						cacheCounter = 0;
						updateBitmapTilesCache();
					}
					lastRequestTimestamp = System.currentTimeMillis();
				}
				if (tileLoaded || mapLoaded) {
					// use downloader callback
					resourceManger.getMapTileDownloader().fireLoadCallback(null);
				}
				sleep(50);
			} catch (InterruptedException | RuntimeException e) {
				log.error(e, e);
			}
		}
	}

	public void requestToLoadTile(TileLoadDownloadRequest req) {
		requests.push(req);
	}

	public void requestToLoadMap(MapLoadRequest req) {
		requests.push(req);
	}

	public boolean isFilePendingToDownload(File fileToSave) {
		return resourceManger.getMapTileDownloader().isFilePendingToDownload(fileToSave);
	}
	
	public boolean isFileCurrentlyDownloaded(File fileToSave) {
		return resourceManger.getMapTileDownloader().isFileCurrentlyDownloaded(fileToSave);
	}

	public void requestToDownload(TileLoadDownloadRequest req) {
		resourceManger.getMapTileDownloader().requestToDownload(req);
	}

	private void updateBitmapTilesCache() {
		int maxCacheSize = 0;
		long currentTime = System.currentTimeMillis();
		for (MapTileLayerSize layerSize : resourceManger.getMapTileLayerSizes()) {
			if (layerSize.markToGCTimestamp != null && currentTime - layerSize.markToGCTimestamp > CACHE_LAYER_SIZE_EXPIRE_TIME_MS) {
				resourceManger.removeMapTileLayerSize(layerSize.layer);
			} else if (currentTime - layerSize.activeTimestamp > CACHE_LAYER_SIZE_EXPIRE_TIME_MS) {
				layerSize.markToGCTimestamp = currentTime + CACHE_LAYER_SIZE_EXPIRE_TIME_MS;
			} else if (layerSize.markToGCTimestamp == null) {
				maxCacheSize += layerSize.tiles;
			}
		}
		BitmapTilesCache bitmapTilesCache = resourceManger.getBitmapTilesCache();
		int oldCacheSize = bitmapTilesCache.getMaxCacheSize();
		if (maxCacheSize != 0 && maxCacheSize * 1.2 < oldCacheSize || maxCacheSize > oldCacheSize) {
			if (maxCacheSize / 2.5 > oldCacheSize) {
				bitmapTilesCache.clearTiles();
			}
			log.info("Bitmap tiles to load in memory : " + maxCacheSize);
			bitmapTilesCache.setMaxCacheSize(maxCacheSize);
		}
	}

	public interface OnMapLoadedListener {
		void onMapLoaded(boolean interrupted);
	}

	protected static class MapLoadRequest {

		public final RotatedTileBox tileBox;
		public final OnMapLoadedListener listener;
		public final boolean forceLoadMap;

		public MapLoadRequest(@NonNull RotatedTileBox tileBox) {
			this(tileBox, null, false);
		}

		public MapLoadRequest(@NonNull RotatedTileBox tileBox, @Nullable OnMapLoadedListener listener, boolean forceLoadMap) {
			this.tileBox = tileBox;
			this.listener = listener;
			this.forceLoadMap = forceLoadMap;
		}
	}
}