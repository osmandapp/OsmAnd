package net.osmand.plus.resources;


import static net.osmand.IndexConstants.MODEL_3D_DIR;
import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_PROVIDER_SUFFIX;
import static net.osmand.plus.AppInitEvents.ASSETS_COPIED;
import static net.osmand.plus.AppInitEvents.MAPS_INITIALIZED;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.HandlerThread;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GeoidAltitudeCorrection;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.binary.CachedOsmandIndexes;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadOsmandIndexesHelper;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.AsyncLoadingThread.MapLoadRequest;
import net.osmand.plus.resources.AsyncLoadingThread.OnMapLoadedListener;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.router.TransportStopsRouteReader;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resource manager is responsible to work with all resources
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 * can't be loaded fully into memory & clear them on request.
 */
public class ResourceManager {

	private static final String INDEXES_CACHE = "ind.cache";
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

	private static final Log log = PlatformUtil.getLog(ResourceManager.class);

	protected static ResourceManager manager;

	protected File dirWithTiles;

	private final List<TilesCache<?>> tilesCacheList = new ArrayList<>();
	private final BitmapTilesCache bitmapTilesCache;
	private final GeometryTilesCache mapillaryVectorTilesCache;
	private List<MapTileLayerSize> mapTileLayerSizes = new ArrayList<>();

	private final OsmandApplication context;
	private final List<ResourceListener> resourceListeners = new ArrayList<>();

	private boolean reloadingIndexes;

	public interface ResourceListener {
		default void onMapsIndexed() {
		}

		default void onReaderIndexed(BinaryMapIndexReader reader) {
		}

		default void onReaderClosed(BinaryMapIndexReader reader) {
		}

		default void onMapClosed(String fileName) {
		}
	}

	// Indexes
	public enum BinaryMapReaderResourceType {
		POI,
		REVERSE_GEOCODING,
		STREET_LOOKUP,
		TRANSPORT,
		ADDRESS,
		QUICK_SEARCH,
		ROUTING,
		TRANSPORT_ROUTING
	}

	public static class MapTileLayerSize {
		final MapTileLayer layer;
		Long markToGCTimestamp;
		long activeTimestamp;
		int tiles;

		public MapTileLayerSize(MapTileLayer layer, int tiles, long activeTimestamp) {
			this.layer = layer;
			this.tiles = tiles;
			this.activeTimestamp = activeTimestamp;
		}
	}

	public static class BinaryMapReaderResource {
		private BinaryMapIndexReader initialReader;
		private final File filename;
		private final List<BinaryMapIndexReader> readers = new ArrayList<>(BinaryMapReaderResourceType.values().length);
		private boolean useForRouting;
		private boolean useForPublicTransport;

		public BinaryMapReaderResource(File f, BinaryMapIndexReader initialReader) {
			this.filename = f;
			this.initialReader = initialReader;
			while (readers.size() < BinaryMapReaderResourceType.values().length) {
				readers.add(null);
			}
		}

		@Nullable
		public BinaryMapIndexReader getReader(BinaryMapReaderResourceType type) {
			BinaryMapIndexReader r = readers.get(type.ordinal());
			BinaryMapIndexReader initialReader = this.initialReader;
			if (r == null && initialReader != null) {
				try {
					RandomAccessFile raf = new RandomAccessFile(filename, "r");
					r = new BinaryMapIndexReader(raf, initialReader);
					readers.set(type.ordinal(), r);
				} catch (IOException e) {
					log.error("Fail to initialize " + filename.getName(), e);
				}
			}
			return r;
		}

		public String getFileName() {
			return filename.getName();
		}

		public long getFileLastModified() {
			return filename.lastModified();
		}

		// should not use methods to read from file!
		@Nullable
		public BinaryMapIndexReader getShallowReader() {
			return initialReader;
		}

		public void close() {
			close(initialReader);
			for (BinaryMapIndexReader rr : readers) {
				if (rr != null) {
					close(rr);
				}
			}
			initialReader = null;
		}

		public boolean isClosed() {
			return initialReader == null;
		}

		private void close(BinaryMapIndexReader r) {
			try {
				r.close();
			} catch (IOException e) {
				log.error("Fail to close " + filename.getName(), e);
			}
		}

		public void setUseForRouting(boolean useForRouting) {
			this.useForRouting = useForRouting;
		}

		public boolean isUseForRouting() {
			return useForRouting;
		}

		public boolean isUseForPublicTransport() {
			return useForPublicTransport;
		}

		public void setUseForPublicTransport(boolean useForPublicTransport) {
			this.useForPublicTransport = useForPublicTransport;
		}
	}

	protected final Map<String, BinaryMapReaderResource> fileReaders = new ConcurrentHashMap<>();

	protected final Map<String, RegionAddressRepository> addressMap = new ConcurrentHashMap<>();
	protected final Map<String, AmenityIndexRepository> amenityRepositories = new ConcurrentHashMap<>();
	//	protected final Map<String, BinaryMapIndexReader> routingMapFiles = new ConcurrentHashMap<>();
	protected final Map<String, BinaryMapReaderResource> transportRepositories = new ConcurrentHashMap<>();
	protected final Map<String, BinaryMapReaderResource> travelRepositories = new ConcurrentHashMap<>();
	protected final Map<String, String> indexFileNames = new ConcurrentHashMap<>();
	protected final Map<String, File> indexFiles = new ConcurrentHashMap<>();
	protected final Map<String, String> basemapFileNames = new ConcurrentHashMap<>();
	private final Map<String, String> backupedFileNames = new ConcurrentHashMap<>();

	protected final IncrementalChangesManager changesManager = new IncrementalChangesManager(this);

	protected final MapRenderRepositories renderer;

	protected final MapTileDownloader tileDownloader;

	public final AsyncLoadingThread asyncLoadingThread = new AsyncLoadingThread(this);

	private final HandlerThread renderingBufferImageThread;

	protected boolean internetIsNotAccessible;
	private boolean depthContours;
	private boolean indexesLoadedOnStart;

	public ResourceManager(@NonNull OsmandApplication context) {
		this.context = context;
		this.renderer = new MapRenderRepositories(context);

		bitmapTilesCache = new BitmapTilesCache(asyncLoadingThread);
		mapillaryVectorTilesCache = new GeometryTilesCache(asyncLoadingThread);
		tilesCacheList.add(bitmapTilesCache);
		tilesCacheList.add(mapillaryVectorTilesCache);

		asyncLoadingThread.start();
		renderingBufferImageThread = new HandlerThread("RenderingBaseImage");
		renderingBufferImageThread.start();

		tileDownloader = MapTileDownloader.getInstance(Version.getFullVersion(context));
		resetStoreDirectory();

		DisplayMetrics dm = new DisplayMetrics();
		AndroidUtils.getDisplay(context).getMetrics(dm);
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		float tiles = (dm.widthPixels / 256 + 2) * (dm.heightPixels / 256 + 2) * 3;
		log.info("Bitmap tiles to load in memory : " + tiles);
		bitmapTilesCache.setMaxCacheSize((int) (tiles));

		File path = context.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (!path.exists()) {
			path.mkdir();
		}
	}

	public BitmapTilesCache getBitmapTilesCache() {
		return bitmapTilesCache;
	}

	public GeometryTilesCache getMapillaryVectorTilesCache() {
		return mapillaryVectorTilesCache;
	}

	public MapTileDownloader getMapTileDownloader() {
		return tileDownloader;
	}

	public HandlerThread getRenderingBufferImageThread() {
		return renderingBufferImageThread;
	}

	public boolean isIndexesLoadedOnStart() {
		return indexesLoadedOnStart;
	}

	public void addResourceListener(ResourceListener listener) {
		if (!resourceListeners.contains(listener)) {
			resourceListeners.add(listener);
		}
	}

	public void removeResourceListener(ResourceListener listener) {
		resourceListeners.remove(listener);
	}

	public boolean checkIfObjectDownloaded(String downloadName) {
		String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return indexFileNames.containsKey(regionName) || indexFileNames.containsKey(roadsRegionName);
	}

	public boolean checkIfObjectBackuped(String downloadName) {
		String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return backupedFileNames.containsKey(regionName) || backupedFileNames.containsKey(roadsRegionName);
	}

	public List<MapTileLayerSize> getMapTileLayerSizes() {
		return mapTileLayerSizes;
	}

	public void setMapTileLayerSizes(MapTileLayer layer, int tiles) {
		MapTileLayerSize layerSize = getMapTileLayerSize(layer);
		if (layerSize != null) {
			if (layerSize.markToGCTimestamp != null) {
				layerSize.markToGCTimestamp = null;
				layerSize.activeTimestamp = System.currentTimeMillis();
			}
			layerSize.tiles = tiles;
		} else {
			List<MapTileLayerSize> layerSizes = new ArrayList<>(mapTileLayerSizes);
			layerSizes.add(new MapTileLayerSize(layer, tiles, System.currentTimeMillis()));
			mapTileLayerSizes = layerSizes;
		}
	}

	public void removeMapTileLayerSize(MapTileLayer layer) {
		MapTileLayerSize layerSize = getMapTileLayerSize(layer);
		if (layerSize != null) {
			List<MapTileLayerSize> layerSizes = new ArrayList<>(mapTileLayerSizes);
			layerSizes.remove(layerSize);
			mapTileLayerSizes = layerSizes;
		}
	}

	@Nullable
	private MapTileLayerSize getMapTileLayerSize(MapTileLayer layer) {
		for (MapTileLayerSize layerSize : mapTileLayerSizes) {
			if (layerSize.layer == layer) {
				return layerSize;
			}
		}
		return null;
	}

	public void resetStoreDirectory() {
		dirWithTiles = context.getAppPath(IndexConstants.TILES_INDEX_DIR);
		dirWithTiles.mkdirs();
		context.getAppPath(IndexConstants.GPX_INDEX_DIR).mkdirs();
		// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery app
		try {
			context.getAppPath(".nomedia").createNewFile();
		} catch (Exception e) {
			// ignore
		}
		for (TilesCache<?> tilesCache : tilesCacheList) {
			tilesCache.setDirWithTiles(dirWithTiles);
		}
	}

	@NonNull
	public DateFormat getDateFormat() {
		return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US);
	}

	@NonNull
	public OsmandApplication getContext() {
		return context;
	}

	public boolean hasDepthContours() {
		return depthContours;
	}

	////////////////////////////////////////////// Working with tiles ////////////////////////////////////////////////

	@Nullable
	private TilesCache<?> getTilesCache(ITileSource map) {
		for (TilesCache<?> cache : tilesCacheList) {
			if (cache.isTileSourceSupported(map)) {
				return cache;
			}
		}
		return null;
	}

	public synchronized void tileDownloaded(DownloadRequest request) {
		if (request instanceof TileLoadDownloadRequest) {
			TileLoadDownloadRequest req = ((TileLoadDownloadRequest) request);
			TilesCache<?> cache = getTilesCache(req.tileSource);
			if (cache != null) {
				cache.tilesOnFS.put(req.tileId, Boolean.TRUE);
			}
		}
	}

	public synchronized boolean isTileDownloaded(String file, ITileSource map, int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null && cache.isTileDownloaded(file, map, x, y, zoom);
	}

	public synchronized boolean isTileSavedOnFileSystem(@NonNull String tileId, @Nullable ITileSource map,
	                                                    int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null && cache.isTileSavedOnFileSystem(tileId, map, x, y, zoom);
	}

	public synchronized int getTileBytesSizeOnFileSystem(@NonNull String tileId, @NonNull ITileSource map,
	                                                     int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null
				? cache.getTileBytesSizeOnFileSystem(tileId, map, x, y, zoom)
				: 0;
	}

	public void clearTileForMap(String file, ITileSource map, int x, int y, int zoom, long requestTimestamp) {
		TilesCache<?> cache = getTilesCache(map);
		if (cache != null) {
			cache.getTileForMap(file, map, x, y, zoom, true, false, true, requestTimestamp);
		}
	}

	private GeoidAltitudeCorrection geoidAltitudeCorrection;
	private boolean searchAmenitiesInProgress;

	@Nullable
	public synchronized String calculateTileId(ITileSource map, int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		if (cache != null) {
			return cache.calculateTileId(map, x, y, zoom);
		}
		return null;
	}

	protected boolean hasRequestedTile(TileLoadDownloadRequest req) {
		TilesCache<?> cache = getTilesCache(req.tileSource);
		return cache != null && cache.getRequestedTile(req) != null;
	}

	public void getTileForMapSync(String file, ITileSource map, int x, int y, int zoom,
	                              boolean loadFromInternetIfNeeded, long requestTimestamp) {
		TilesCache<?> cache = getTilesCache(map);
		if (cache != null) {
			cache.getTileForMapSync(file, map, x, y, zoom, loadFromInternetIfNeeded, requestTimestamp);
		}
	}

	public void downloadTileForMapSync(@NonNull ITileSource map, int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		if (cache != null) {
			String tileId = calculateTileId(map, x, y, zoom);
			long time = System.currentTimeMillis();
			cache.getTileForMap(tileId, map, x, y, zoom, true, true, true, time);
		}
	}

	public void clearCacheAndTiles(@NonNull ITileSource map) {
		map.deleteTiles(new File(dirWithTiles, map.getName()).getAbsolutePath());
		TilesCache<?> cache = getTilesCache(map);
		if (cache != null) {
			cache.clearAllTiles();
		}
	}

	////////////////////////////////////////////// Working with indexes ////////////////////////////////////////////////

	private final ExecutorService reloadIndexesSingleThreadExecutor = Executors.newSingleThreadExecutor();

	public List<String> reloadIndexesOnStart(@NonNull AppInitializer progress, List<String> warnings) {
		close();
		// check we have some assets to copy to sdcard
		warnings.addAll(checkAssets(progress, false, true));
		progress.notifyEvent(ASSETS_COPIED);
		reloadIndexes(progress, warnings);
		progress.notifyEvent(MAPS_INITIALIZED);
		indexesLoadedOnStart = true;
		return warnings;
	}

	public void reloadIndexesAsync(@Nullable IProgress progress, @Nullable ReloadIndexesListener listener) {
		ReloadIndexesTask reloadIndexesTask = new ReloadIndexesTask(progress, listener);
		reloadIndexesTask.executeOnExecutor(reloadIndexesSingleThreadExecutor);
	}

	public List<String> reloadIndexes(@Nullable IProgress progress, @NonNull List<String> warnings) {
		ReloadIndexesTask task = new ReloadIndexesTask(progress, null);
		try {
			warnings.addAll(task.executeOnExecutor(reloadIndexesSingleThreadExecutor).get());
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return warnings;
	}

	private class ReloadIndexesTask extends AsyncTask<Void, String, List<String>> {

		private final IProgress progress;
		private final ReloadIndexesListener listener;

		public ReloadIndexesTask(@Nullable IProgress progress, @Nullable ReloadIndexesListener listener) {
			this.progress = progress;
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			context.runInUIThread(() -> reloadingIndexes = true);
			if (listener != null) {
				listener.reloadIndexesStarted();
			}
		}

		@Override
		protected List<String> doInBackground(Void... params) {
			geoidAltitudeCorrection = new GeoidAltitudeCorrection(context.getAppPath(null));
			// do it lazy
			// indexingImageTiles(progress);
			List<String> warnings = new ArrayList<>();
			warnings.addAll(indexingMaps(progress));
			warnings.addAll(indexVoiceFiles(progress));
			warnings.addAll(indexFontFiles(progress));
			warnings.addAll(PluginsHelper.onIndexingFiles(progress));
			warnings.addAll(indexAdditionalMaps(progress));

			return warnings;
		}

		@Override
		protected void onPostExecute(List<String> warnings) {
			context.runInUIThread(() -> reloadingIndexes = false);
			if (listener != null) {
				listener.reloadIndexesFinished(warnings);
			}
		}
	}

	public interface ReloadIndexesListener {

		default void reloadIndexesStarted() {

		}

		void reloadIndexesFinished(@NonNull List<String> warnings);
	}

	public List<String> indexAdditionalMaps(@Nullable IProgress progress) {
		return context.getAppCustomization().onIndexingFiles(progress, indexFileNames);
	}


	public List<String> indexVoiceFiles(@Nullable IProgress progress) {
		File voiceDir = context.getAppPath(VOICE_INDEX_DIR);
		voiceDir.mkdirs();
		List<String> warnings = new ArrayList<>();
		if (voiceDir.exists() && voiceDir.canRead()) {
			File[] files = voiceDir.listFiles();
			if (files != null) {
				DateFormat dateFormat = getDateFormat();
				for (File file : files) {
					if (file.isDirectory()) {
						String lang = file.getName().replace(VOICE_PROVIDER_SUFFIX, "");
						File conf = new File(file, lang + "_" + TTSVOICE_INDEX_EXT_JS);
						if (conf.exists()) {
							indexFileNames.put(file.getName(), dateFormat.format(conf.lastModified()));
							indexFiles.put(file.getName(), file);
						}
					}
				}
			}
		}
		return warnings;
	}

	public List<String> indexFontFiles(@Nullable IProgress progress) {
		File fontDir = context.getAppPath(IndexConstants.FONT_INDEX_DIR);
		fontDir.mkdirs();
		List<String> warnings = new ArrayList<>();
		if (fontDir.exists() && fontDir.canRead()) {
			File[] files = fontDir.listFiles();
			if (files != null) {
				DateFormat dateFormat = getDateFormat();
				for (File file : files) {
					if (!file.isDirectory()) {
						indexFileNames.put(file.getName(), dateFormat.format(file.lastModified()));
						indexFiles.put(file.getName(), file);
					}
				}
			}
		}
		return warnings;
	}

	public boolean isReloadingIndexes() {
		return reloadingIndexes;
	}

	public void copyMissingJSAssets() {
		try {
			List<AssetEntry> assets = DownloadOsmandIndexesHelper.getBundledAssets(context.getAssets());
			File appPath = context.getAppPath(null);
			if (appPath.canWrite()) {
				for (AssetEntry asset : assets) {
					File jsFile = new File(appPath, asset.destination);
					if (asset.destination.contains(VOICE_PROVIDER_SUFFIX) && asset.destination
							.endsWith(TTSVOICE_INDEX_EXT_JS)) {
						File oggFile = new File(appPath, asset.destination.replace(
								VOICE_PROVIDER_SUFFIX, ""));
						if (oggFile.getParentFile().exists() && !oggFile.exists()) {
							copyAssets(context.getAssets(), asset.source, oggFile);
						}
					} else if (asset.destination.startsWith(MODEL_3D_DIR) && !jsFile.exists()) {
						copyAssets(context.getAssets(), asset.source, jsFile);
					}
					if (jsFile.getParentFile().exists() && !jsFile.exists()) {
						copyAssets(context.getAssets(), asset.source, jsFile);
					}
				}
			}
		} catch (XmlPullParserException | IOException e) {
			log.error("Error while loading tts files from assets", e);
		}
	}

	private final ExecutorService checkAssetsSingleThreadExecutor = Executors.newSingleThreadExecutor();

	public void checkAssetsAsync(@Nullable IProgress progress, boolean forceUpdate, boolean forceCheck,
	                             @Nullable CheckAssetsListener listener) {
		CheckAssetsTask task = new CheckAssetsTask(progress, forceUpdate, forceCheck, listener);
		task.executeOnExecutor(checkAssetsSingleThreadExecutor);
	}

	public List<String> checkAssets(@Nullable IProgress progress, boolean forceUpdate, boolean forceCheck) {
		List<String> warnings = new ArrayList<>();
		CheckAssetsTask task = new CheckAssetsTask(progress, forceUpdate, forceCheck, null);
		try {
			warnings.addAll(task.executeOnExecutor(checkAssetsSingleThreadExecutor).get());
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return warnings;
	}

	private class CheckAssetsTask extends AsyncTask<Void, String, List<String>> {

		private final IProgress progress;
		private final CheckAssetsListener listener;

		private final boolean forceUpdate;
		private final boolean forceCheck;

		public CheckAssetsTask(@Nullable IProgress progress, boolean forceUpdate, boolean forceCheck,
		                       @Nullable CheckAssetsListener listener) {
			this.progress = progress;
			this.forceUpdate = forceUpdate;
			this.forceCheck = forceCheck;
			this.listener = listener;
		}

		@Override
		protected void onPreExecute() {
			if (listener != null) {
				listener.checkAssetsStarted();
			}
		}

		@Override
		protected List<String> doInBackground(Void... params) {
			return checkAssets(progress, forceUpdate, forceCheck);
		}

		private List<String> checkAssets(IProgress progress, boolean forceUpdate, boolean forceCheck) {
			if (context.getAppInitializer().isAppVersionChanged()) {
				copyMissingJSAssets();
			}
			String fv = Version.getFullVersion(context);
			OsmandSettings settings = context.getSettings();
			boolean versionChanged = !fv.equalsIgnoreCase(settings.PREVIOUS_INSTALLED_VERSION.get());
			boolean overwrite = versionChanged || forceUpdate;
			if (overwrite || forceCheck) {
				File appDataDir = context.getAppPath(null);
				appDataDir.mkdirs();
				if (appDataDir.canWrite()) {
					try {
						progress.startTask(context.getString(R.string.installing_new_resources), -1);
						AssetManager assetManager = context.getAssets();
						boolean firstInstall = !settings.PREVIOUS_INSTALLED_VERSION.isSet();
						unpackBundledAssets(assetManager, appDataDir, firstInstall || forceUpdate, overwrite, forceCheck);
						settings.PREVIOUS_INSTALLED_VERSION.set(fv);
						copyRegionsBoundaries(overwrite);
						// see Issue #3381
						//copyPoiTypes();
						RendererRegistry registry = context.getRendererRegistry();
						for (String internalStyle : registry.getInternalRenderers().keySet()) {
							File file = registry.getFileForInternalStyle(internalStyle);
							if (file.exists() && overwrite) {
								registry.copyFileForInternalStyle(internalStyle);
							}
						}
					} catch (SQLiteException | IOException | XmlPullParserException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			return Collections.emptyList();
		}

		@Override
		protected void onPostExecute(List<String> warnings) {
			if (listener != null) {
				listener.checkAssetsFinished(warnings);
			}
		}
	}

	public interface CheckAssetsListener {

		void checkAssetsStarted();

		void checkAssetsFinished(List<String> warnings);
	}

	private void copyRegionsBoundaries(boolean overwrite) {
		try {
			File file = context.getAppPath("regions.ocbf");
			boolean exists = file.exists();
			if (!exists || overwrite) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void copyPoiTypes(boolean overwrite) {
		try {
			File file = context.getAppPath(IndexConstants.SETTINGS_DIR + "poi_types.xml");
			boolean exists = file.exists();
			if (!exists || overwrite) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(MapPoiTypes.class.getResourceAsStream("poi_types.xml"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static final String ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall = "alwaysCopyOnFirstInstall";
	private static final String ASSET_COPY_MODE__overwriteOnlyIfExists = "overwriteOnlyIfExists";
	private static final String ASSET_COPY_MODE__alwaysOverwriteOrCopy = "alwaysOverwriteOrCopy";
	private static final String ASSET_COPY_MODE__copyOnlyIfDoesNotExist = "copyOnlyIfDoesNotExist";

	private void unpackBundledAssets(@NonNull AssetManager assetManager, @NonNull File appDataDir,
	                                 boolean firstInstall,
	                                 boolean overwrite,
	                                 boolean forceCheck) throws IOException, XmlPullParserException {
		List<AssetEntry> assetEntries = DownloadOsmandIndexesHelper.getBundledAssets(assetManager);
		for (AssetEntry asset : assetEntries) {
			String[] modes = asset.combinedMode.split("\\|");
			if (modes.length == 0) {
				log.error("Mode '" + asset.combinedMode + "' is not valid");
				continue;
			}
			String installMode = null;
			String copyMode = null;
			for (String mode : modes) {
				if (ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall.equals(mode)) {
					installMode = mode;
				} else if (ASSET_COPY_MODE__overwriteOnlyIfExists.equals(mode) ||
						ASSET_COPY_MODE__alwaysOverwriteOrCopy.equals(mode) ||
						ASSET_COPY_MODE__copyOnlyIfDoesNotExist.equals(mode)) {
					copyMode = mode;
				} else {
					log.error("Mode '" + mode + "' is unknown");
				}
			}

			File destinationFile = new File(appDataDir, asset.destination);
			boolean exists = destinationFile.exists();
			boolean shouldCopy = false;
			if (ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall.equals(installMode)) {
				if (firstInstall || (forceCheck && !exists)) {
					shouldCopy = true;
				}
			}
			if (copyMode == null) {
				log.error("No copy mode was defined for " + asset.source);
			}
			if (ASSET_COPY_MODE__alwaysOverwriteOrCopy.equals(copyMode)) {
				if (firstInstall || overwrite) {
					shouldCopy = true;
				} else if (forceCheck && !exists) {
					shouldCopy = true;
				}
			}
			if (ASSET_COPY_MODE__overwriteOnlyIfExists.equals(copyMode) && exists) {
				if (firstInstall || overwrite) {
					shouldCopy = true;
				}
			}
			if (ASSET_COPY_MODE__copyOnlyIfDoesNotExist.equals(copyMode)) {
				if (!exists) {
					shouldCopy = true;
				} else if (asset.version != null &&
						destinationFile.lastModified() < asset.version.getTime()) {
					shouldCopy = true;
				}
			}
			if (shouldCopy) {
				copyAssets(assetManager, asset.source, destinationFile);
			}
		}
	}

	public static void copyAssets(AssetManager assetManager, String assetName, File file) throws IOException {
		if (file.exists()) {
			Algorithms.removeAllFiles(file);
		}
		file.getParentFile().mkdirs();
		InputStream is = assetManager.open(assetName, AssetManager.ACCESS_STREAMING);
		FileOutputStream out = new FileOutputStream(file);
		Algorithms.streamCopy(is, out);
		Algorithms.closeStream(out);
		Algorithms.closeStream(is);
	}

	private List<File> collectFiles(File dir, String ext, List<File> files) {
		if (dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if (lf == null || lf.length == 0) {
				return files;
			}
			for (File f : lf) {
				if (f.getName().endsWith(ext)) {
					files.add(f);
				}
			}
		}
		return files;
	}

	private void renameRoadsFiles(ArrayList<File> files, File roadsPath) {
		Iterator<File> it = files.iterator();
		while (it.hasNext()) {
			File f = it.next();
			if (f.getName().endsWith("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName().replace("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT,
						IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)));
			} else if (f.getName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName()));
			}
		}
	}

	public List<String> indexingMaps(@Nullable IProgress progress) {
		return indexingMaps(progress, Collections.emptyList());
	}

	public List<String> indexingMaps(@Nullable IProgress progress, @NonNull List<File> filesToReindex) {
		long val = System.currentTimeMillis();
		ArrayList<File> files = new ArrayList<>();
		File appPath = context.getAppPath(null);
		File roadsPath = context.getAppPath(IndexConstants.ROADS_INDEX_DIR);
		roadsPath.mkdirs();

		collectFiles(context.getAppInternalPath(IndexConstants.HIDDEN_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
		collectFiles(appPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		renameRoadsFiles(files, roadsPath);
		collectFiles(roadsPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		if (Version.isPaidVersion(context)) {
			collectFiles(context.getAppPath(IndexConstants.WIKI_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
			collectFiles(context.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, files);
		}
		if (PluginsHelper.isActive(SRTMPlugin.class) || InAppPurchaseUtils.isContourLinesAvailable(context)) {
			collectFiles(context.getAppPath(IndexConstants.SRTM_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
		}
		if (PluginsHelper.isActive(NauticalMapsPlugin.class) || InAppPurchaseUtils.isDepthContoursAvailable(context)) {
			collectFiles(context.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR), IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT, files);
		}

		changesManager.collectChangesFiles(context.getAppPath(IndexConstants.LIVE_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);

		Collections.sort(files, Algorithms.getFileVersionComparator());
		List<String> warnings = new ArrayList<>();
		renderer.clearAllResources();
		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = context.getAppPath(INDEXES_CACHE);
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		File liveDir = context.getAppPath(IndexConstants.LIVE_INDEX_DIR);
		depthContours = false;
		File worldBasemapStd = null;
		File worldBasemapDetailed = null;
		File worldBasemapMini = null;
		for (File f : files) {
			if (f.getName().equals("World_basemap.obf")) {
				worldBasemapStd = f;
			}
			if (f.getName().startsWith("World_basemap_mini")) {
				worldBasemapMini = f;
			}
			if (f.getName().startsWith("World_basemap_detailed")) {
				worldBasemapDetailed = f;
			}
		}

		if (worldBasemapDetailed != null) {
			if (worldBasemapStd != null) {
				files.remove(worldBasemapStd);
			}
			if (worldBasemapMini != null) {
				files.remove(worldBasemapMini);
			}

		} else if (worldBasemapStd != null && worldBasemapMini != null) {
			files.remove(worldBasemapMini);
		}

		DateFormat dateFormat = getDateFormat();
		for (File f : files) {
			String fileName = f.getName();
			if (progress != null) {
				progress.startTask(context.getString(R.string.indexing_map) + " " + fileName, -1);
			}
			try {
				BinaryMapIndexReader mapReader = null;
				boolean reindex = filesToReindex.contains(f);
				try {
					mapReader = cachedOsmandIndexes.getReader(f, !reindex);
					if (mapReader.getVersion() != IndexConstants.BINARY_MAP_VERSION) {
						mapReader = null;
					}
				} catch (IOException e) {
					log.error(String.format("File %s could not be read", fileName), e);
				}
				boolean wikiMap = WikipediaPlugin.containsWikipediaExtension(fileName);
				boolean srtmMap = SrtmDownloadItem.containsSrtmExtension(fileName);
				if (mapReader == null || (!Version.isPaidVersion(context) && wikiMap)) {
					warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), fileName)); //$NON-NLS-1$
				} else {
					if (mapReader.isBasemap()) {
						basemapFileNames.put(fileName, fileName);
					}
					long dateCreated = mapReader.getDateCreated();
					if (dateCreated == 0) {
						dateCreated = f.lastModified();
					}
					if (f.getParentFile().getName().equals(liveDir.getName())) {
						boolean toUse = changesManager.index(f, dateCreated, mapReader);
						if (!toUse) {
							try {
								mapReader.close();
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
							continue;
						}
					} else if (!wikiMap && !srtmMap) {
						changesManager.indexMainMap(f, dateCreated);
						if (reindex) {
							for (ResourceListener l : resourceListeners) {
								l.onReaderIndexed(mapReader);
							}
						}
					}
					indexFileNames.put(fileName, dateFormat.format(dateCreated));
					indexFiles.put(fileName, f);
					if (!depthContours && fileName.toLowerCase().startsWith("depth_")) {
						depthContours = true;
					}
					renderer.initializeNewResource(f, mapReader);
					BinaryMapReaderResource resource = new BinaryMapReaderResource(f, mapReader);
					if (mapReader.containsPoiData()) {
						amenityRepositories.put(fileName, new AmenityIndexRepositoryBinary(resource, context));
					}
					fileReaders.put(fileName, resource);
					if (resource.getFileName().endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
						travelRepositories.put(resource.getFileName(), resource);
						// travel files should be indexed separately (so it's possible to turn on / off)
						continue;
					}
					if (!mapReader.getRegionNames().isEmpty()) {
						RegionAddressRepositoryBinary rarb = new RegionAddressRepositoryBinary(this, resource);
						addressMap.put(fileName, rarb);
					}
					if (mapReader.hasTransportData()) {
						transportRepositories.put(fileName, resource);
					}
					// disable osmc for routing temporarily due to some bugs
					if (mapReader.containsRouteData() && (!f.getParentFile().equals(liveDir) ||
							context.getSettings().USE_OSM_LIVE_FOR_ROUTING.get())) {
						resource.setUseForRouting(true);
					}
					if (mapReader.hasTransportData() && (!f.getParentFile().equals(liveDir) ||
							context.getSettings().USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT.get())) {
						resource.setUseForPublicTransport(true);
					}
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e);
				warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), fileName));
			} catch (OutOfMemoryError oome) {
				log.error("Exception reading " + f.getAbsolutePath(), oome);
				warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_big_for_memory), fileName));
			}
		}
		Map<PoiCategory, Map<String, PoiType>> toAddPoiTypes = new HashMap<>();
		for (AmenityIndexRepository repo : amenityRepositories.values()) {
			Map<String, List<String>> categories = ((AmenityIndexRepositoryBinary) repo).getDeltaPoiCategories();
			if (!categories.isEmpty()) {
				for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
					PoiCategory poiCategory = context.getPoiTypes().getPoiCategoryByName(entry.getKey(), true);
					if (!toAddPoiTypes.containsKey(poiCategory)) {
						toAddPoiTypes.put(poiCategory, new TreeMap<>());
					}
					Map<String, PoiType> poiTypes = toAddPoiTypes.get(poiCategory);
					if (poiTypes != null) {
						for (String s : entry.getValue()) {
							PoiType pt = new PoiType(MapPoiTypes.getDefault(), poiCategory, null, s);
							pt.setOsmTag("");
							pt.setOsmValue("");
							pt.setNotEditableOsm(true);
							poiTypes.put(s, pt);
						}
					}
				}
			}
		}
		Iterator<Entry<PoiCategory, Map<String, PoiType>>> it = toAddPoiTypes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<PoiCategory, Map<String, PoiType>> next = it.next();
			PoiCategory category = next.getKey();
			category.addExtraPoiTypes(next.getValue());
		}
		log.debug("All map files initialized " + (System.currentTimeMillis() - val) + " ms");
		if (files.size() > 0 && (!indCache.exists() || indCache.canWrite())) {
			try {
				cachedOsmandIndexes.writeToFile(indCache);
			} catch (Exception e) {
				log.error("Index file could not be written", e);
			}
		}
		backupedFileNames.clear();
		getBackupIndexes(backupedFileNames);
		for (ResourceListener l : resourceListeners) {
			l.onMapsIndexed();
		}
		return warnings;
	}

	public List<String> getTravelRepositoryNames() {
		List<String> fileNames = new ArrayList<>(travelRepositories.keySet());
		Collections.sort(fileNames, Algorithms.getStringVersionComparator());
		return fileNames;
	}

	public List<BinaryMapIndexReader> getTravelMapRepositories() {
		List<BinaryMapIndexReader> res = new ArrayList<>();
		for (String fileName : getTravelRepositoryNames()) {
			BinaryMapReaderResource resource = travelRepositories.get(fileName);
			if (resource != null) {
				BinaryMapIndexReader shallowReader = resource.getShallowReader();
				if (shallowReader != null && shallowReader.containsMapData()) {
					res.add(shallowReader);
				}
			}
		}
		return res;
	}

	public List<BinaryMapIndexReader> getTravelRepositories() {
		List<BinaryMapIndexReader> res = new ArrayList<>();
		for (String fileName : getTravelRepositoryNames()) {
			BinaryMapReaderResource r = travelRepositories.get(fileName);
			if (r != null) {
				res.add(r.getReader(BinaryMapReaderResourceType.POI));
			}
		}
		return res;
	}

	public boolean isTravelGuidesRepositoryEmpty() {
		return getTravelRepositories().isEmpty();
	}

	public void initMapBoundariesCacheNative() {
		File indCache = context.getAppPath(INDEXES_CACHE);
		if (indCache.exists()) {
			NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
			if (nativeLib != null) {
				nativeLib.initCacheMapFile(indCache.getAbsolutePath());
			}
		}
	}

	////////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<AmenityIndexRepository> getAmenityRepositories() {
		return getAmenityRepositories(true);
	}

	public List<AmenityIndexRepository> getAmenityRepositories(boolean includeTravel) {
		List<String> fileNames = new ArrayList<>(amenityRepositories.keySet());
		Collections.sort(fileNames, Algorithms.getStringVersionComparator());
		List<AmenityIndexRepository> res = new ArrayList<>();
		for (String fileName : fileNames) {
			if (fileName.endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
				if (!includeTravel || !context.getTravelRendererHelper().getFileVisibilityProperty(fileName).get()) {
					continue;
				}
			}
			AmenityIndexRepository r = amenityRepositories.get(fileName);
			if (r != null) {
				res.add(r);
			}
		}
		return res;
	}

	@NonNull
	public List<Amenity> searchAmenities(SearchPoiTypeFilter filter, QuadRect rect, boolean includeTravel) {
		return searchAmenities(filter, rect.top, rect.left, rect.bottom, rect.right, -1, includeTravel, null);
	}

	@NonNull
	public List<Amenity> searchAmenities(SearchPoiTypeFilter filter, double topLatitude,
	                                     double leftLongitude, double bottomLatitude,
	                                     double rightLongitude, int zoom, boolean includeTravel,
	                                     ResultMatcher<Amenity> matcher) {
		List<Amenity> amenities = new ArrayList<>();
		searchAmenitiesInProgress = true;
		try {
			if (!filter.isEmpty()) {
				int top31 = MapUtils.get31TileNumberY(topLatitude);
				int left31 = MapUtils.get31TileNumberX(leftLongitude);
				int bottom31 = MapUtils.get31TileNumberY(bottomLatitude);
				int right31 = MapUtils.get31TileNumberX(rightLongitude);
				for (AmenityIndexRepository index : getAmenityRepositories(includeTravel)) {
					if (matcher != null && matcher.isCancelled()) {
						searchAmenitiesInProgress = false;
						break;
					}
					if (index != null && index.checkContainsInt(top31, left31, bottom31, right31)) {
						List<Amenity> r = index.searchAmenities(top31,
								left31, bottom31, right31, zoom, filter, matcher);
						if (r != null) {
							amenities.addAll(r);
						}
					}
				}
			}
		} finally {
			searchAmenitiesInProgress = false;
		}
		return amenities;
	}

	@NonNull
	public List<String> searchPoiSubTypesByPrefix(@NonNull String prefix) {
		Set<String> poiSubTypes = new HashSet<>();
		for (AmenityIndexRepository index : getAmenityRepositories()) {
			if (index instanceof AmenityIndexRepositoryBinary) {
				AmenityIndexRepositoryBinary repository = (AmenityIndexRepositoryBinary) index;
				List<PoiSubType> subTypes = repository.searchPoiSubTypesByPrefix(prefix);
				for (PoiSubType subType : subTypes) {
					poiSubTypes.add(subType.name);
				}
			}
		}
		return new ArrayList<>(poiSubTypes);
	}

	public List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, SearchPoiTypeFilter filter,
	                                              ResultMatcher<Amenity> matcher) {
		searchAmenitiesInProgress = true;
		List<Amenity> amenities = new ArrayList<>();
		try {
			if (locations != null && locations.size() > 0) {
				List<AmenityIndexRepository> repos = new ArrayList<>();
				double topLatitude = locations.get(0).getLatitude();
				double bottomLatitude = locations.get(0).getLatitude();
				double leftLongitude = locations.get(0).getLongitude();
				double rightLongitude = locations.get(0).getLongitude();
				for (Location l : locations) {
					topLatitude = Math.max(topLatitude, l.getLatitude());
					bottomLatitude = Math.min(bottomLatitude, l.getLatitude());
					leftLongitude = Math.min(leftLongitude, l.getLongitude());
					rightLongitude = Math.max(rightLongitude, l.getLongitude());
				}
				if (!filter.isEmpty()) {
					for (AmenityIndexRepository index : getAmenityRepositories()) {
						if (index.checkContainsInt(
								MapUtils.get31TileNumberY(topLatitude),
								MapUtils.get31TileNumberX(leftLongitude),
								MapUtils.get31TileNumberY(bottomLatitude),
								MapUtils.get31TileNumberX(rightLongitude))) {
							repos.add(index);
						}
					}
					if (!repos.isEmpty()) {
						for (AmenityIndexRepository r : repos) {
							List<Amenity> res = r.searchAmenitiesOnThePath(locations, radius, filter, matcher);
							if (res != null) {
								amenities.addAll(res);
							}
						}
					}
				}
			}
		} finally {
			searchAmenitiesInProgress = false;
		}
		return amenities;
	}

	public boolean containsAmenityRepositoryToSearch(boolean searchByName) {
		for (AmenityIndexRepository index : getAmenityRepositories()) {
			if (searchByName) {
				if (index instanceof AmenityIndexRepositoryBinary) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	public List<Amenity> searchAmenitiesByName(String searchQuery,
	                                           double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
	                                           double lat, double lon, ResultMatcher<Amenity> matcher) {
		List<Amenity> amenities = new ArrayList<>();
		List<AmenityIndexRepositoryBinary> list = new ArrayList<>();
		int left = MapUtils.get31TileNumberX(leftLongitude);
		int top = MapUtils.get31TileNumberY(topLatitude);
		int right = MapUtils.get31TileNumberX(rightLongitude);
		int bottom = MapUtils.get31TileNumberY(bottomLatitude);
		for (AmenityIndexRepository index : getAmenityRepositories(false)) {
			if (matcher != null && matcher.isCancelled()) {
				break;
			}
			if (index instanceof AmenityIndexRepositoryBinary) {
				if (index.checkContainsInt(top, left, bottom, right)) {
					if (index.checkContains(lat, lon)) {
						list.add(0, (AmenityIndexRepositoryBinary) index);
					} else {
						list.add((AmenityIndexRepositoryBinary) index);
					}

				}
			}
		}

		// Not using boundaries results in very slow initial search if user has many maps installed
//		int left = 0;
//		int top = 0;
//		int right = Integer.MAX_VALUE;
//		int bottom = Integer.MAX_VALUE;
		for (AmenityIndexRepositoryBinary index : list) {
			if (matcher != null && matcher.isCancelled()) {
				break;
			}
			List<Amenity> result = index.searchAmenitiesByName(MapUtils.get31TileNumberX(lon), MapUtils.get31TileNumberY(lat),
					left, top, right, bottom,
					searchQuery, matcher);
			amenities.addAll(result);
		}

		return amenities;
	}


	public AmenityIndexRepositoryBinary getAmenityRepositoryByFileName(String filename) {
		return (AmenityIndexRepositoryBinary) amenityRepositories.get(filename);
	}

	////////////////////////////////////////////// Working with address ///////////////////////////////////////////

	public RegionAddressRepository getRegionRepository(String name) {
		return addressMap.get(name);
	}

	public Collection<RegionAddressRepository> getAddressRepositories() {
		return addressMap.values();
	}

	public Collection<BinaryMapReaderResource> getFileReaders() {
		List<String> fileNames = new ArrayList<>(fileReaders.keySet());
		Collections.sort(fileNames, Algorithms.getStringVersionComparator());
		List<BinaryMapReaderResource> res = new ArrayList<>();
		for (String fileName : fileNames) {
			BinaryMapReaderResource r = fileReaders.get(fileName);
			if (r != null) {
				res.add(r);
			}
		}
		return res;
	}

	////////////////////////////////////////////// Working with transport ////////////////////////////////////////////////

	private List<BinaryMapIndexReader> getTransportRepositories(double topLat, double leftLon, double bottomLat, double rightLon) {
		List<String> fileNames = new ArrayList<>(transportRepositories.keySet());
		Collections.sort(fileNames, Algorithms.getStringVersionComparator());
		List<BinaryMapIndexReader> res = new ArrayList<>();
		for (String fileName : fileNames) {
			BinaryMapReaderResource r = transportRepositories.get(fileName);
			if (r != null && r.isUseForPublicTransport() &&
					r.getShallowReader().containTransportData(topLat, leftLon, bottomLat, rightLon)) {
				res.add(r.getReader(BinaryMapReaderResourceType.TRANSPORT));
			}
		}
		return res;
	}


	public List<TransportStop> searchTransportSync(double topLat, double leftLon, double bottomLat, double rightLon,
	                                               ResultMatcher<TransportStop> matcher) throws IOException {
		TransportStopsRouteReader readers =
				new TransportStopsRouteReader(getTransportRepositories(topLat, leftLon, bottomLat, rightLon));
		List<TransportStop> stops = new ArrayList<>();
		BinaryMapIndexReader.SearchRequest<TransportStop> req = BinaryMapIndexReader.buildSearchTransportRequest(MapUtils.get31TileNumberX(leftLon),
				MapUtils.get31TileNumberX(rightLon), MapUtils.get31TileNumberY(topLat),
				MapUtils.get31TileNumberY(bottomLat), -1, stops);
		for (TransportStop s : readers.readMergedTransportStops(req)) {
			if (!s.isDeleted() && !s.isMissingStop()) {
				stops.add(s);
			}
		}
		return stops;
	}

	public List<TransportRoute> getRoutesForStop(TransportStop stop) {
		List<TransportRoute> rts = stop.getRoutes();
		if (rts != null) {
			return rts;
		}
		return Collections.emptyList();
	}

	////////////////////////////////////////////// Working with map ////////////////////////////////////////////////
	public boolean updateRenderedMapNeeded(RotatedTileBox rotatedTileBox, DrawSettings drawSettings) {
		return renderer.updateMapIsNeeded(rotatedTileBox, drawSettings);
	}

	public void updateRendererMap(@NonNull RotatedTileBox tileBox) {
		updateRendererMap(tileBox, null, false);
	}

	public void updateRendererMap(@NonNull RotatedTileBox tileBox, @Nullable OnMapLoadedListener listener, boolean forceLoadMap) {
		renderer.interruptLoadingMap();
		asyncLoadingThread.requestToLoadMap(new MapLoadRequest(tileBox, listener, forceLoadMap));
	}

	public void interruptRendering() {
		renderer.interruptLoadingMap();
	}

	public boolean isSearchAmenitiesInProgress() {
		return searchAmenitiesInProgress;
	}

	public MapRenderRepositories getRenderer() {
		return renderer;
	}

	////////////////////////////////////////////// Closing methods ////////////////////////////////////////////////

	public void closeFile(String fileName) {
		amenityRepositories.remove(fileName);
		addressMap.remove(fileName);
		transportRepositories.remove(fileName);
		indexFileNames.remove(fileName);
		backupedFileNames.remove(fileName);
		indexFiles.remove(fileName);
		travelRepositories.remove(fileName);
		renderer.closeConnection(fileName);
		BinaryMapReaderResource resource = fileReaders.remove(fileName);
		if (resource != null) {
			for (ResourceListener l : resourceListeners) {
				l.onReaderClosed(resource.initialReader);
			}
			resource.close();
		}
		for (ResourceListener l : resourceListeners) {
			l.onMapClosed(fileName);
		}
	}

	public synchronized void close() {
		for (TilesCache<?> tc : tilesCacheList) {
			tc.close();
		}
		indexFileNames.clear();
		indexFiles.clear();
		basemapFileNames.clear();
		renderer.clearAllResources();
		transportRepositories.clear();
		travelRepositories.clear();
		addressMap.clear();
		amenityRepositories.clear();
		for (BinaryMapReaderResource res : fileReaders.values()) {
			res.close();
		}
		fileReaders.clear();
	}

	public BinaryMapIndexReader[] getReverseGeocodingMapFiles() {
		Collection<BinaryMapReaderResource> fileReaders = getFileReaders();
		List<BinaryMapIndexReader> readers = new ArrayList<>(fileReaders.size());
		for (BinaryMapReaderResource r : fileReaders) {
			BinaryMapIndexReader reader = r.getReader(BinaryMapReaderResourceType.REVERSE_GEOCODING);
			if (reader != null) {
				readers.add(reader);
			}
		}
		return readers.toArray(new BinaryMapIndexReader[0]);
	}

	public BinaryMapIndexReader[] getRoutingMapFiles() {
		Collection<BinaryMapReaderResource> fileReaders = getFileReaders();
		List<BinaryMapIndexReader> readers = new ArrayList<>(fileReaders.size());
		for (BinaryMapReaderResource r : fileReaders) {
			if (r.isUseForRouting()) {
				BinaryMapIndexReader reader = r.getReader(BinaryMapReaderResourceType.ROUTING);
				if (reader != null) {
					readers.add(reader);
				}
			}
		}
		return readers.toArray(new BinaryMapIndexReader[0]);
	}

	public BinaryMapIndexReader[] getTransportRoutingMapFiles() {
		Collection<BinaryMapReaderResource> fileReaders = getFileReaders();
		List<BinaryMapIndexReader> readers = new ArrayList<>(fileReaders.size());
		for (BinaryMapReaderResource r : fileReaders) {
			if (r.isUseForPublicTransport()) {
				BinaryMapIndexReader reader = r.getReader(BinaryMapReaderResourceType.TRANSPORT_ROUTING);
				if (reader != null) {
					readers.add(reader);
				}
			}
		}
		return readers.toArray(new BinaryMapIndexReader[0]);
	}

	public BinaryMapIndexReader[] getQuickSearchFiles() {
		Collection<BinaryMapReaderResource> fileReaders = getFileReaders();
		List<BinaryMapIndexReader> readers = new ArrayList<>(fileReaders.size());
		for (BinaryMapReaderResource r : fileReaders) {
			BinaryMapIndexReader shallowReader = r.getShallowReader();
			if (shallowReader != null && (shallowReader.containsPoiData() || shallowReader.containsAddressData()) &&
					!r.getFileName().endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
				BinaryMapIndexReader reader = r.getReader(BinaryMapReaderResourceType.QUICK_SEARCH);
				if (reader != null) {
					readers.add(reader);
				}
			}
		}
		return readers.toArray(new BinaryMapIndexReader[0]);
	}

	public Map<String, String> getIndexFileNames() {
		return new LinkedHashMap<>(indexFileNames);
	}

	public Map<String, File> getIndexFiles() {
		return new LinkedHashMap<>(indexFiles);
	}

	public boolean containsBasemap() {
		return !basemapFileNames.isEmpty();
	}

	public boolean isAnyMapInstalled() {
		return isMapsPresentInDirectory(null) || isMapsPresentInDirectory(IndexConstants.ROADS_INDEX_DIR);
	}

	private boolean isMapsPresentInDirectory(@Nullable String path) {
		File dir = context.getAppPath(path);
		File[] maps = dir.listFiles(pathname -> pathname.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) &&
				!pathname.getName().endsWith("World_basemap_mini.obf"));
		return maps != null && maps.length > 0;
	}

	public Map<String, String> getBackupIndexes(Map<String, String> map) {
		File file = context.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		if (file != null && file.isDirectory()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
						map.put(f.getName(), AndroidUtils.formatDate(context, f.lastModified()));
					}
				}
			}
		}
		return map;
	}

	public synchronized void reloadTilesFromFS() {
		for (TilesCache<?> tc : tilesCacheList) {
			tc.tilesOnFS.clear();
		}
	}

	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory");
		clearTiles();
		for (RegionAddressRepository r : addressMap.values()) {
			r.clearCache();
		}
		renderer.clearCache();

		System.gc();
	}

	public GeoidAltitudeCorrection getGeoidAltitudeCorrection() {
		return geoidAltitudeCorrection;
	}

	public OsmandRegions getOsmandRegions() {
		return context.getRegions();
	}

	protected synchronized void clearTiles() {
		log.info("Cleaning tiles...");
		for (TilesCache<?> tc : tilesCacheList) {
			tc.clearTiles();
		}
	}

	public IncrementalChangesManager getChangesManager() {
		return changesManager;
	}
}
