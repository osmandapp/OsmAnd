package net.osmand.plus.resources;


import static net.osmand.IndexConstants.*;
import static net.osmand.plus.AppInitEvents.ASSETS_COPIED;
import static net.osmand.plus.AppInitEvents.MAPS_INITIALIZED;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.HandlerThread;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import net.osmand.GeoidAltitudeCorrection;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.binary.CachedOsmandIndexes;
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
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.resources.AsyncLoadingThread.MapLoadRequest;
import net.osmand.plus.resources.AsyncLoadingThread.OnMapLoadedListener;
import net.osmand.plus.resources.CheckAssetsTask.CheckAssetsListener;
import net.osmand.plus.resources.ReloadIndexesTask.ReloadIndexesListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.router.TransportStopsRouteReader;
import net.osmand.search.AmenitySearcher;
import net.osmand.search.core.AmenityIndexRepository;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
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
	private AssetsCollection assetsCollection;

	private final OsmandApplication app;
	private final List<ResourceListener> resourceListeners = new ArrayList<>();


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

	protected final Map<String, BinaryMapReaderResource> fileReaders = new ConcurrentHashMap<>();

	protected final Map<String, RegionAddressRepository> addressMap = new ConcurrentHashMap<>();
	protected final Map<String, BinaryMapReaderResource> transportRepositories = new ConcurrentHashMap<>();
	protected final Map<String, AmenityIndexRepository> travelRepositories = new ConcurrentHashMap<>();
	protected final Map<String, String> indexFileNames = new ConcurrentHashMap<>();
	protected final Map<String, File> indexFiles = new ConcurrentHashMap<>();
	protected final Map<String, String> basemapFileNames = new ConcurrentHashMap<>();
	private final Map<String, String> backupedFileNames = new ConcurrentHashMap<>();

	private Set<String> standardPoiTypesKeyNames = null;

	protected final IncrementalChangesManager changesManager = new IncrementalChangesManager(this);

	protected final MapRenderRepositories renderer;

	protected final MapTileDownloader tileDownloader;

	public final AsyncLoadingThread asyncLoadingThread = new AsyncLoadingThread(this);

	private final HandlerThread renderingBufferImageThread;

	private ReloadIndexesTask reloadIndexesTask;

	private boolean depthContours;
	private boolean indexesLoadedOnStart;

	private final AmenitySearcher amenitySearcher;

	public ResourceManager(@NonNull OsmandApplication app) {
		this.app = app;
		this.renderer = new MapRenderRepositories(app);

		bitmapTilesCache = new BitmapTilesCache(asyncLoadingThread);
		mapillaryVectorTilesCache = new GeometryTilesCache(asyncLoadingThread);
		tilesCacheList.add(bitmapTilesCache);
		tilesCacheList.add(mapillaryVectorTilesCache);

		asyncLoadingThread.start();
		renderingBufferImageThread = new HandlerThread("RenderingBaseImage");
		renderingBufferImageThread.start();

		tileDownloader = MapTileDownloader.getInstance(Version.getFullVersion(app));
		resetStoreDirectory();

		DisplayMetrics dm = new DisplayMetrics();
		AndroidUtils.getDisplay(app).getMetrics(dm);
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		float tiles = (dm.widthPixels / 256 + 2) * (dm.heightPixels / 256 + 2) * 3;
		log.info("Bitmap tiles to load in memory : " + tiles);
		bitmapTilesCache.setMaxCacheSize((int) (tiles));

		File path = app.getAppPath(ROUTING_PROFILES_DIR);
		if (!path.exists()) {
			path.mkdir();
		}

		amenitySearcher = new AmenitySearcher(app.getPoiTypes());
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
		String regionName = getMapFileName(downloadName);
		String roadsRegionName = getRoadMapFileName(downloadName);
		return indexFileNames.containsKey(regionName) || indexFileNames.containsKey(roadsRegionName);
	}

	public boolean checkIfObjectBackuped(String downloadName) {
		String regionName = getMapFileName(downloadName);
		String roadsRegionName = getRoadMapFileName(downloadName);
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
		dirWithTiles = app.getAppPath(TILES_INDEX_DIR);
		dirWithTiles.mkdirs();
		app.getAppPath(GPX_INDEX_DIR).mkdirs();
		// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery app
		try {
			app.getAppPath(".nomedia").createNewFile();
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
	public OsmandApplication getApp() {
		return app;
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

	public synchronized boolean isTileDownloaded(String file, ITileSource map, int x, int y,
			int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null && cache.isTileDownloaded(file, map, x, y, zoom);
	}

	public synchronized boolean isTileSavedOnFileSystem(@NonNull String tileId,
			@Nullable ITileSource map,
			int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null && cache.isTileSavedOnFileSystem(tileId, map, x, y, zoom);
	}

	public synchronized int getTileBytesSizeOnFileSystem(@NonNull String tileId,
			@NonNull ITileSource map,
			int x, int y, int zoom) {
		TilesCache<?> cache = getTilesCache(map);
		return cache != null
				? cache.getTileBytesSizeOnFileSystem(tileId, map, x, y, zoom)
				: 0;
	}

	private GeoidAltitudeCorrection geoidAltitudeCorrection;

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

	public List<String> reloadIndexesOnStart(@NonNull AppInitializer progress,
			List<String> warnings) {
		close();
		// check we have some assets to copy to sdcard
		warnings.addAll(checkAssets(progress, false, true));
		progress.notifyEvent(ASSETS_COPIED);
		reloadIndexes(progress, warnings);
		progress.notifyEvent(MAPS_INITIALIZED);
		indexesLoadedOnStart = true;
		return warnings;
	}

	public void reloadIndexesAsync(@Nullable IProgress progress,
			@Nullable ReloadIndexesListener listener) {
		reloadIndexesTask = new ReloadIndexesTask(app, progress, listener);
		OsmAndTaskManager.executeTask(reloadIndexesTask, reloadIndexesSingleThreadExecutor);
	}

	public List<String> reloadIndexes(@Nullable IProgress progress,
			@NonNull List<String> warnings) {
		reloadIndexesTask = new ReloadIndexesTask(app, progress, null);
		try {
			warnings.addAll(OsmAndTaskManager.executeTask(reloadIndexesTask, reloadIndexesSingleThreadExecutor).get());
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return warnings;
	}

	public List<String> indexAdditionalMaps(@Nullable IProgress progress) {
		return app.getAppCustomization().onIndexingFiles(progress, indexFileNames);
	}

	public List<String> indexVoiceFiles(@Nullable IProgress progress) {
		File voiceDir = app.getAppPath(VOICE_INDEX_DIR);
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
		File fontDir = app.getAppPath(FONT_INDEX_DIR);
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
		return reloadIndexesTask != null && reloadIndexesTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	private final ExecutorService checkAssetsSingleThreadExecutor = Executors.newSingleThreadExecutor();

	public void checkAssetsAsync(@Nullable IProgress progress, boolean forceUpdate,
			boolean forceCheck, @Nullable CheckAssetsListener listener) {
		CheckAssetsTask task = new CheckAssetsTask(app, progress, forceUpdate, forceCheck, listener);
		OsmAndTaskManager.executeTask(task, checkAssetsSingleThreadExecutor);
	}

	public List<String> checkAssets(@Nullable IProgress progress, boolean forceUpdate,
			boolean forceCheck) {
		List<String> warnings = new ArrayList<>();
		CheckAssetsTask task = new CheckAssetsTask(app, progress, forceUpdate, forceCheck, null);
		try {
			warnings.addAll(OsmAndTaskManager.executeTask(task, checkAssetsSingleThreadExecutor).get());
		} catch (ExecutionException | InterruptedException e) {
			log.error(e);
		}
		return warnings;
	}

	private void renameRoadsFiles(ArrayList<File> files, File roadsPath) {
		Iterator<File> it = files.iterator();
		while (it.hasNext()) {
			File f = it.next();
			if (f.getName().endsWith("-roads" + BINARY_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName().replace("-roads" + BINARY_MAP_INDEX_EXT,
						BINARY_ROAD_MAP_INDEX_EXT)));
			} else if (f.getName().endsWith(BINARY_ROAD_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName()));
			}
		}
	}

	public List<String> indexingMaps(@Nullable IProgress progress) {
		return indexingMaps(progress, Collections.emptyList());
	}

	public List<String> indexingMaps(@Nullable IProgress progress,
			@NonNull List<File> filesToReindex) {
		long val = System.currentTimeMillis();
		ArrayList<File> files = new ArrayList<>();
		File appPath = app.getAppPath(null);
		File roadsPath = app.getAppPath(ROADS_INDEX_DIR);
		roadsPath.mkdirs();

		FileUtils.collectFiles(app.getAppInternalPath(HIDDEN_DIR), BINARY_MAP_INDEX_EXT, files);
		FileUtils.collectFiles(appPath, BINARY_MAP_INDEX_EXT, files);
		renameRoadsFiles(files, roadsPath);
		FileUtils.collectFiles(roadsPath, BINARY_MAP_INDEX_EXT, files);
		if (Version.isPaidVersion(app)) {
			FileUtils.collectFiles(app.getAppPath(WIKI_INDEX_DIR), BINARY_MAP_INDEX_EXT, files);
			FileUtils.collectFiles(app.getAppPath(WIKIVOYAGE_INDEX_DIR), BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, files);
		}
		if (PluginsHelper.isActive(SRTMPlugin.class) || InAppPurchaseUtils.isContourLinesAvailable(app)) {
			FileUtils.collectFiles(app.getAppPath(SRTM_INDEX_DIR), BINARY_MAP_INDEX_EXT, files);
		}
		if (PluginsHelper.isActive(NauticalMapsPlugin.class) || InAppPurchaseUtils.isDepthContoursAvailable(app)) {
			FileUtils.collectFiles(app.getAppPath(NAUTICAL_INDEX_DIR), BINARY_DEPTH_MAP_INDEX_EXT, files);
		}

		changesManager.collectChangesFiles(app.getAppPath(LIVE_INDEX_DIR), BINARY_MAP_INDEX_EXT, files);

		Collections.sort(files, Algorithms.getFileVersionComparator());
		List<String> warnings = new ArrayList<>();
		renderer.clearAllResources();
		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = app.getAppPath(INDEXES_CACHE);
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		File liveDir = app.getAppPath(LIVE_INDEX_DIR);
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
				progress.startTask(app.getString(R.string.indexing_map) + " " + fileName, -1);
			}
			try {
				BinaryMapIndexReader mapReader = null;
				boolean reindex = filesToReindex.contains(f);
				try {
					mapReader = cachedOsmandIndexes.getReader(f, !reindex);
					if (mapReader.getVersion() != BINARY_MAP_VERSION) {
						mapReader = null;
					}
				} catch (Exception e) {
					log.error(String.format("File %s could not be read", fileName), e);
				}
				boolean wikiMap = WikipediaPlugin.containsWikipediaExtension(fileName);
				boolean srtmMap = SrtmDownloadItem.containsSrtmExtension(fileName);
				if (mapReader == null || (!Version.isPaidVersion(app) && wikiMap)) {
					warnings.add(MessageFormat.format(app.getString(R.string.version_index_is_not_supported), fileName)); //$NON-NLS-1$
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
							} catch (Exception e) {
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
					boolean isTravelObf = resource.getFileName().endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT);
					if (mapReader.containsPoiData()) {
						AmenityIndexRepositoryBinary amenityResource = new AmenityIndexRepositoryBinary(f, resource, app);
						amenitySearcher.addAmenityRepository(fileName, amenityResource);
						if (isTravelObf) {
							// reuse until new BinaryMapReaderResourceType.TRAVEL_GPX
							travelRepositories.put(resource.getFileName(), amenityResource);
						}
					}
					fileReaders.put(fileName, resource);
					if (isTravelObf) {
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
							app.getSettings().USE_OSM_LIVE_FOR_ROUTING.get())) {
						resource.setUseForRouting(true);
					}
					if (mapReader.hasTransportData() && (!f.getParentFile().equals(liveDir) ||
							app.getSettings().USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT.get())) {
						resource.setUseForPublicTransport(true);
					}
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e);
				warnings.add(MessageFormat.format(app.getString(R.string.version_index_is_not_supported), fileName));
			} catch (OutOfMemoryError oome) {
				log.error("Exception reading " + f.getAbsolutePath(), oome);
				warnings.add(MessageFormat.format(app.getString(R.string.version_index_is_big_for_memory), fileName));
			}
		}
		Map<PoiCategory, Map<String, PoiType>> toAddPoiTypes = new HashMap<>();
		for (AmenityIndexRepository repo : amenitySearcher.getAmenityRepositories()) {
			Map<String, List<String>> categories = ((AmenityIndexRepositoryBinary) repo).getDeltaPoiCategories();
			if (!categories.isEmpty()) {
				for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
					PoiCategory poiCategory = app.getPoiTypes().getPoiCategoryByName(entry.getKey(), true);
					if (!toAddPoiTypes.containsKey(poiCategory)) {
						toAddPoiTypes.put(poiCategory, new TreeMap<>());
					}
					Map<String, PoiType> poiTypes = toAddPoiTypes.get(poiCategory);
					if (poiTypes != null) {
						for (String s : entry.getValue()) {
							PoiType pt = new PoiType(MapPoiTypes.getDefault(), poiCategory, null, s, null);
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
			Map<String, PoiType> categoryDeltaPoiTypes = next.getValue();
			categoryDeltaPoiTypes.keySet().removeAll(getStandardPoiTypesKeyNames());
			category.addExtraPoiTypes(categoryDeltaPoiTypes);
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

	private Set<String> getStandardPoiTypesKeyNames() {
		if (standardPoiTypesKeyNames == null) {
			MapPoiTypes mapPoiTypes = MapPoiTypes.getDefault();
			Set<String> allPoiTypesKeyNames = new HashSet<>();
			for (PoiCategory poiCategory : mapPoiTypes.getCategories()) {
				for (PoiType poiType : poiCategory.getPoiTypes()) {
					allPoiTypesKeyNames.add(poiType.getKeyName());
				}
			}
			standardPoiTypesKeyNames = allPoiTypesKeyNames;
		}
		return standardPoiTypesKeyNames;
	}

	public List<String> getTravelRepositoryNames() {
		List<String> fileNames = new ArrayList<>(travelRepositories.keySet());
		Collections.sort(fileNames, Algorithms.getStringVersionComparator());
		return fileNames;
	}

	public List<AmenityIndexRepository> getTravelGpxRepositories() {
		AmenitySearcher.Settings settings = getDefaultAmenitySearchSettings();
		return amenitySearcher.getAmenityRepositories(true, settings.fileVisibility());
	}

	public List<AmenityIndexRepository> getWikivoyageRepositories() {
		return new ArrayList<>(travelRepositories.values());
	}

	public boolean hasTravelRepositories() {
		return !travelRepositories.isEmpty();
	}

	public void initMapBoundariesCacheNative() {
		File indCache = app.getAppPath(INDEXES_CACHE);
		if (indCache.exists()) {
			NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
			if (nativeLib != null) {
				nativeLib.initCacheMapFile(indCache.getAbsolutePath());
			}
		}
	}

	////////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<AmenityIndexRepository> getAmenityRepositories() {
		AmenitySearcher.Settings settings = getDefaultAmenitySearchSettings();
		return amenitySearcher.getAmenityRepositories(true, settings.fileVisibility());
	}

	@NonNull
	public List<String> searchPoiSubTypesByPrefix(@NonNull String prefix) {
		Set<String> poiSubTypes = new HashSet<>();
		for (AmenityIndexRepository repository : getAmenityRepositories()) {
			if (repository instanceof AmenityIndexRepositoryBinary binaryRepository) {
				List<PoiSubType> subTypes = binaryRepository.searchPoiSubTypesByPrefix(prefix);
				for (PoiSubType subType : subTypes) {
					poiSubTypes.add(subType.name);
				}
			}
		}
		return new ArrayList<>(poiSubTypes);
	}

	public AmenityIndexRepositoryBinary getAmenityRepositoryByFileName(String filename) {
		return (AmenityIndexRepositoryBinary) amenitySearcher.getAmenityRepository(filename);
	}

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

	private List<BinaryMapIndexReader> getTransportRepositories(double topLat, double leftLon,
			double bottomLat, double rightLon) {
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


	public List<TransportStop> searchTransportSync(double topLat, double leftLon, double bottomLat,
			double rightLon,
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
	public boolean updateRenderedMapNeeded(RotatedTileBox rotatedTileBox,
			DrawSettings drawSettings) {
		return renderer.updateMapIsNeeded(rotatedTileBox, drawSettings);
	}

	public void updateRendererMap(@NonNull RotatedTileBox tileBox) {
		updateRendererMap(tileBox, null, false);
	}

	public void updateRendererMap(@NonNull RotatedTileBox tileBox,
			@Nullable OnMapLoadedListener listener, boolean forceLoadMap) {
		renderer.interruptLoadingMap();
		asyncLoadingThread.requestToLoadMap(new MapLoadRequest(tileBox, listener, forceLoadMap));
	}

	public void interruptRendering() {
		renderer.interruptLoadingMap();
	}

	public MapRenderRepositories getRenderer() {
		return renderer;
	}

	////////////////////////////////////////////// Closing methods ////////////////////////////////////////////////

	public void closeFile(String fileName) {
		amenitySearcher.removeAmenityRepository(fileName);
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
				BinaryMapIndexReader reader = resource.getShallowReader();
				if (reader != null) {
					l.onReaderClosed(reader);
				}
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
		amenitySearcher.clearAmenityRepositories();
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

	public BinaryMapIndexReader[] getQuickSearchFiles(List<String> ignoreExtensions) {
		Collection<BinaryMapReaderResource> fileReaders = getFileReaders();
		List<BinaryMapIndexReader> readers = new ArrayList<>(fileReaders.size());
		for (BinaryMapReaderResource r : fileReaders) {
			boolean allow = true;
			if (!Algorithms.isEmpty(ignoreExtensions)) {
				for (String ext : ignoreExtensions) {
					if (r.getFileName().endsWith(ext)) {
						allow = false;
						break;
					}
				}
			}
			if (allow) {
				BinaryMapIndexReader shallowReader = r.getShallowReader();
				if (shallowReader != null && (shallowReader.containsPoiData() || shallowReader.containsAddressData())) {
					BinaryMapIndexReader reader = r.getReader(BinaryMapReaderResourceType.QUICK_SEARCH);
					if (reader != null) {
						readers.add(reader);
					}
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
		return isMapsPresentInDirectory(null) || isMapsPresentInDirectory(ROADS_INDEX_DIR);
	}

	private boolean isMapsPresentInDirectory(@Nullable String path) {
		File dir = app.getAppPath(path);
		File[] maps = dir.listFiles(pathname -> pathname.getName().endsWith(BINARY_MAP_INDEX_EXT) &&
				!pathname.getName().endsWith("World_basemap_mini.obf"));
		return maps != null && maps.length > 0;
	}

	public Map<String, String> getBackupIndexes(Map<String, String> map) {
		File file = app.getAppPath(BACKUP_INDEX_DIR);
		if (file != null && file.isDirectory()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				DateFormat dateFormat = getDateFormat();
				for (File f : lf) {
					if (f != null && f.getName().endsWith(BINARY_MAP_INDEX_EXT)) {
						map.put(f.getName(), dateFormat.format(f.lastModified()));
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

	protected void resetGeoidAltitudeCorrection() {
		geoidAltitudeCorrection = new GeoidAltitudeCorrection(app.getAppPath(null));
	}

	public OsmandRegions getOsmandRegions() {
		return app.getRegions();
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

	@NonNull
	public AssetsCollection getAssets() throws IOException {
		return assetsCollection == null ? assetsCollection = readBundledAssets() : assetsCollection;
	}

	private static class AssetEntryList {
		List<AssetEntry> assets = new ArrayList<>();
	}

	@NonNull
	private AssetsCollection readBundledAssets() throws IOException {
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

		AssetManager assetManager = app.getAssets();
		InputStream isBundledAssetsXml = assetManager.open("bundled_assets.json");
		AssetEntryList lst = new Gson().fromJson(new InputStreamReader(isBundledAssetsXml), AssetEntryList.class);
		for (AssetEntry ae : lst.assets) {
			if (!Algorithms.isEmpty(ae.version)) {
				try {
					ae.dateVersion = DATE_FORMAT.parse(ae.version);
				} catch (ParseException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		isBundledAssetsXml.close();
		return new AssetsCollection(app, lst.assets);
	}

	public static boolean copyAssets(@NonNull AssetManager manager, @NonNull String name,
			@NonNull File file, @Nullable Long modifiedTime) throws IOException {
		if (file.exists()) {
			Algorithms.removeAllFiles(file);
		}
		file.getParentFile().mkdirs();
		InputStream is = manager.open(name, AssetManager.ACCESS_STREAMING);
		FileOutputStream out = new FileOutputStream(file);
		Algorithms.streamCopy(is, out);
		Algorithms.closeStream(out);
		Algorithms.closeStream(is);

		return modifiedTime != null && file.setLastModified(modifiedTime);
	}

	public AmenitySearcher getAmenitySearcher() {
		return amenitySearcher;
	}

	public AmenitySearcher.Settings getDefaultAmenitySearchSettings() {
		return new AmenitySearcher.Settings(
				() -> app.getSettings().MAP_PREFERRED_LOCALE.get(),
				() -> app.getSettings().MAP_TRANSLITERATE_NAMES.get(),
				(fileName) -> app.getTravelRendererHelper().getFileVisibilityProperty(fileName).get()
		);
	}

	public static String getMapFileName(String regionName) {
		return Algorithms.capitalizeFirstLetterAndLowercase(regionName) + BINARY_MAP_INDEX_EXT;
	}

	public static String getRoadMapFileName(String regionName) {
		return Algorithms.capitalizeFirstLetterAndLowercase(regionName) + BINARY_ROAD_MAP_INDEX_EXT;
	}
}
