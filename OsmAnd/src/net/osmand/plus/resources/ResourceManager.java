package net.osmand.plus.resources;


import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.AndroidUtils;
import net.osmand.GeoidAltitudeCorrection;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.CachedOsmandIndexes;
import net.osmand.data.Amenity;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.BusyIndicator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.Version;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.resources.AsyncLoadingThread.MapLoadRequest;
import net.osmand.plus.resources.AsyncLoadingThread.TileLoadDownloadRequest;
import net.osmand.plus.resources.AsyncLoadingThread.TransportLoadRequest;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 */
public class ResourceManager {

	public static final String VECTOR_MAP = "#vector_map"; //$NON-NLS-1$
	private static final String INDEXES_CACHE = "ind.cache";
	
	
	private static final Log log = PlatformUtil.getLog(ResourceManager.class);
	
	
	protected static ResourceManager manager = null;
	
	// it is not good investigated but no more than 64 (satellite images)
	// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
	// at least 3*9?
	protected int maxImgCacheSize = 28;
	
	protected Map<String, Bitmap> cacheOfImages = new LinkedHashMap<String, Bitmap>();
	protected Map<String, Boolean> imagesOnFS = new LinkedHashMap<String, Boolean>() ;
	
	protected File dirWithTiles ;
	
	private final OsmandApplication context;
	
	private BusyIndicator busyIndicator;
	
	public interface ResourceWatcher {
		
		
		public boolean indexResource(File f);
		
		public List<String> getWatchWorkspaceFolder();
	}
	
	
	// Indexes
	private final Map<String, RegionAddressRepository> addressMap = new ConcurrentHashMap<String, RegionAddressRepository>();
	protected final Map<String, AmenityIndexRepository> amenityRepositories =  new ConcurrentHashMap<String, AmenityIndexRepository>();
	protected final Map<String, String> indexFileNames = new ConcurrentHashMap<String, String>();
	protected final Map<String, String> basemapFileNames = new ConcurrentHashMap<String, String>();
	protected final Map<String, BinaryMapIndexReader> routingMapFiles = new ConcurrentHashMap<String, BinaryMapIndexReader>();
	protected final Map<String, TransportIndexRepository> transportRepositories = new ConcurrentHashMap<String, TransportIndexRepository>();
	
	protected final IncrementalChangesManager changesManager = new IncrementalChangesManager(this);
	
	protected final MapRenderRepositories renderer;

	protected final MapTileDownloader tileDownloader;
	
	public final AsyncLoadingThread asyncLoadingThread = new AsyncLoadingThread(this);
	
	private HandlerThread renderingBufferImageThread;
	
	protected boolean internetIsNotAccessible = false;
	private java.text.DateFormat dateFormat;
	
	public ResourceManager(OsmandApplication context) {
		
		this.context = context;
		this.renderer = new MapRenderRepositories(context);
		asyncLoadingThread.start();
		renderingBufferImageThread = new HandlerThread("RenderingBaseImage");
		renderingBufferImageThread.start();

		tileDownloader = MapTileDownloader.getInstance(Version.getFullVersion(context));
		dateFormat = DateFormat.getDateFormat(context);
		resetStoreDirectory();
		WindowManager mgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
		// at least 3*9?
		float tiles = (dm.widthPixels / 256 + 2) * (dm.heightPixels / 256 + 2) * 3;
		log.info("Tiles to load in memory : " + tiles);
		maxImgCacheSize = (int) (tiles) ;
	}
	
	public MapTileDownloader getMapTileDownloader() {
		return tileDownloader;
	}
	
	public HandlerThread getRenderingBufferImageThread() {
		return renderingBufferImageThread;
	}

	
	public void resetStoreDirectory() {
		dirWithTiles = context.getAppPath(IndexConstants.TILES_INDEX_DIR);
		dirWithTiles.mkdirs();
		context.getAppPath(IndexConstants.GPX_INDEX_DIR).mkdirs();
		// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery app
		try {
			context.getAppPath(".nomedia").createNewFile(); //$NON-NLS-1$
		} catch( Exception e ) {
		}
	}
	
	public java.text.DateFormat getDateFormat() {
		return dateFormat;
	}
	
	public OsmandApplication getContext() {
		return context;
	}
	
	////////////////////////////////////////////// Working with tiles ////////////////////////////////////////////////
	
	public Bitmap getTileImageForMapAsync(String file, ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(file, map, x, y, zoom, loadFromInternetIfNeeded, false, true);
	}
	
	
	public synchronized Bitmap getTileImageFromCache(String file){
		return cacheOfImages.get(file);
	}
	
	public synchronized void putTileInTheCache(String file, Bitmap bmp) {
		cacheOfImages.put(file, bmp);
	}
	
	
	public Bitmap getTileImageForMapSync(String file, ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(file, map, x, y, zoom, loadFromInternetIfNeeded, true, true);
	}
	
	public synchronized void tileDownloaded(DownloadRequest request){
		if(request instanceof TileLoadDownloadRequest){
			TileLoadDownloadRequest req = ((TileLoadDownloadRequest) request);
			imagesOnFS.put(req.tileId, Boolean.TRUE);
/*			if(req.fileToSave != null && req.tileSource instanceof SQLiteTileSource){
				try {
					((SQLiteTileSource) req.tileSource).insertImage(req.xTile, req.yTile, req.zoom, req.fileToSave);
				} catch (IOException e) {
					log.warn("File "+req.fileToSave.getName() + " couldn't be read", e);  //$NON-NLS-1$//$NON-NLS-2$
				}
				req.fileToSave.delete();
				String[] l = req.fileToSave.getParentFile().list();
				if(l == null || l.length == 0){
					req.fileToSave.getParentFile().delete();
					l = req.fileToSave.getParentFile().getParentFile().list();
					if(l == null || l.length == 0){
						req.fileToSave.getParentFile().getParentFile().delete();
					}
				}
			}*/
		}
		
	}
	
	public synchronized boolean tileExistOnFileSystem(String file, ITileSource map, int x, int y, int zoom){
		if(!imagesOnFS.containsKey(file)){
			boolean ex = false;
			if(map instanceof SQLiteTileSource){
				if(((SQLiteTileSource) map).isLocked()){
					return false;
				}
				ex = ((SQLiteTileSource) map).exists(x, y, zoom);
			} else {
				if(file == null){
					file = calculateTileId(map, x, y, zoom);
				}
				ex = new File(dirWithTiles, file).exists();
			}
			if (ex) {
				imagesOnFS.put(file, Boolean.TRUE);
			} else {
				imagesOnFS.put(file, null);
			}
		}
		return imagesOnFS.get(file) != null || cacheOfImages.get(file) != null;		
	}
	
	public void clearTileImageForMap(String file, ITileSource map, int x, int y, int zoom){
		getTileImageForMap(file, map, x, y, zoom, true, false, true, true);
	}
	
	/**
	 * @param file - null could be passed if you do not call very often with that param
	 */
	protected Bitmap getTileImageForMap(String file, ITileSource map, int x, int y, int zoom, 
			boolean loadFromInternetIfNeeded, boolean sync, boolean loadFromFs) {
		return getTileImageForMap(file, map, x, y, zoom, loadFromInternetIfNeeded, sync, loadFromFs, false);
	}

	// introduce cache in order save memory
	
	protected StringBuilder builder = new StringBuilder(40);
	protected char[] tileId = new char[120];
	private GeoidAltitudeCorrection geoidAltitudeCorrection;
	private boolean searchAmenitiesInProgress;

	public synchronized String calculateTileId(ITileSource map, int x, int y, int zoom) {
		builder.setLength(0);
		if (map == null) {
			builder.append(IndexConstants.TEMP_SOURCE_TO_LOAD);
		} else {
			builder.append(map.getName());
		}

		if (map instanceof SQLiteTileSource) {
			builder.append('@');
		} else {
			builder.append('/');
		}
		builder.append(zoom).append('/').append(x).append('/').append(y).
				append(map == null ? ".jpg" : map.getTileFormat()).append(".tile"); //$NON-NLS-1$ //$NON-NLS-2$
		return builder.toString();
	}
	

	protected synchronized Bitmap getTileImageForMap(String tileId, ITileSource map, int x, int y, int zoom,
			boolean loadFromInternetIfNeeded, boolean sync, boolean loadFromFs, boolean deleteBefore) {
		if (tileId == null) {
			tileId = calculateTileId(map, x, y, zoom);
			if(tileId == null){
				return null;
			}
		}
		
		if(deleteBefore){
			cacheOfImages.remove(tileId);
			if (map instanceof SQLiteTileSource) {
				((SQLiteTileSource) map).deleteImage(x, y, zoom);
			} else {
				File f = new File(dirWithTiles, tileId);
				if (f.exists()) {
					f.delete();
				}
			}
			imagesOnFS.put(tileId, null);
		}
		
		if (loadFromFs && cacheOfImages.get(tileId) == null && map != null) {
			boolean locked = map instanceof SQLiteTileSource && ((SQLiteTileSource) map).isLocked();
			if(!loadFromInternetIfNeeded && !locked && !tileExistOnFileSystem(tileId, map, x, y, zoom)){
				return null;
			}
			String url = loadFromInternetIfNeeded ? map.getUrlToLoad(x, y, zoom) : null;
			File toSave = null;
			if (url != null) {
				if (map instanceof SQLiteTileSource) {
					toSave = new File(dirWithTiles, calculateTileId(((SQLiteTileSource) map).getBase(), x, y, zoom));
				} else {
					toSave = new File(dirWithTiles, tileId);
				}
			}
			TileLoadDownloadRequest req = new TileLoadDownloadRequest(dirWithTiles, url, toSave, 
					tileId, map, x, y, zoom, map.getReferer());
			if(sync){
				return getRequestedImageTile(req);
			} else {
				asyncLoadingThread.requestToLoadImage(req);
			}
		}
		return cacheOfImages.get(tileId);
	}
	
	
	
	protected Bitmap getRequestedImageTile(TileLoadDownloadRequest req){
		if(req.tileId == null || req.dirWithTiles == null){
			return null;
		}
		Bitmap cacheBmp = cacheOfImages.get(req.tileId);
		if (cacheBmp != null) {
			return cacheBmp;
		}
		if (cacheOfImages.size() > maxImgCacheSize) {
			clearTiles();
		}
		if (req.dirWithTiles.canRead() && !asyncLoadingThread.isFileCurrentlyDownloaded(req.fileToSave)
			&& !asyncLoadingThread.isFilePendingToDownload(req.fileToSave)) {
			long time = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Start loaded file : " + req.tileId + " " + Thread.currentThread().getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			Bitmap bmp = null;
			if (req.tileSource instanceof SQLiteTileSource) {
				try {
					long[] tm = new long[1];
					bmp = ((SQLiteTileSource) req.tileSource).getImage(req.xTile, req.yTile, req.zoom, tm);
					if (tm[0] != 0) {
						int ts = req.tileSource.getExpirationTimeMillis();
						if (ts != -1 && req.url != null && time - tm[0] > ts) {
							asyncLoadingThread.requestToDownload(req);
						}
					}
				} catch (OutOfMemoryError e) {
					log.error("Out of memory error", e); //$NON-NLS-1$
					clearTiles();
				}
			} else {
				File en = new File(req.dirWithTiles, req.tileId);
				if (en.exists()) {
					try {
						bmp = BitmapFactory.decodeFile(en.getAbsolutePath());
						int ts = req.tileSource.getExpirationTimeMillis();
						if(ts != -1 && req.url != null && time - en.lastModified() > ts) {
							asyncLoadingThread.requestToDownload(req);
						}
					} catch (OutOfMemoryError e) {
						log.error("Out of memory error", e); //$NON-NLS-1$
						clearTiles();
					}
				}
			}

			if (bmp != null) {
				cacheOfImages.put(req.tileId, bmp);
				if (log.isDebugEnabled()) {
					log.debug("Loaded file : " + req.tileId + " " + -(time - System.currentTimeMillis()) + " ms " + cacheOfImages.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}

			if (cacheOfImages.get(req.tileId) == null && req.url != null) {
				asyncLoadingThread.requestToDownload(req);
			}

		}
		return cacheOfImages.get(req.tileId);
	}


    ////////////////////////////////////////////// Working with indexes ////////////////////////////////////////////////

	public List<String> reloadIndexesOnStart(AppInitializer progress, List<String> warnings){
		close();
		// check we have some assets to copy to sdcard
		warnings.addAll(checkAssets(progress));
		progress.notifyEvent(InitEvents.ASSETS_COPIED);
		reloadIndexes(progress, warnings);
		progress.notifyEvent(InitEvents.MAPS_INITIALIZED);
		return warnings;
	}

	public List<String> reloadIndexes(IProgress progress, List<String> warnings) {
		geoidAltitudeCorrection = new GeoidAltitudeCorrection(context.getAppPath(null));
		// do it lazy
		// indexingImageTiles(progress);
		warnings.addAll(indexingMaps(progress));
		warnings.addAll(indexVoiceFiles(progress));
		warnings.addAll(OsmandPlugin.onIndexingFiles(progress));
		warnings.addAll(indexAdditionalMaps(progress));
		return warnings;
	}

	public List<String> indexAdditionalMaps(IProgress progress) {
		return context.getAppCustomization().onIndexingFiles(progress, indexFileNames);
	}


	public List<String> indexVoiceFiles(IProgress progress){
		File file = context.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		file.mkdirs();
		List<String> warnings = new ArrayList<String>();
		if (file.exists() && file.canRead()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f.isDirectory()) {
						File conf = new File(f, "_config.p");
						if (!conf.exists()) {
							conf = new File(f, "_ttsconfig.p");
						}
						if (conf.exists()) {
							indexFileNames.put(f.getName(), dateFormat.format(conf.lastModified())); //$NON-NLS-1$
						}
					}
				}
			}
		}
		return warnings;
	}
	
	private List<String> checkAssets(IProgress progress) {
		String fv = Version.getFullVersion(context);
		if (!fv.equalsIgnoreCase(context.getSettings().PREVIOUS_INSTALLED_VERSION.get())) {
			File applicationDataDir = context.getAppPath(null);
			applicationDataDir.mkdirs();
			if (applicationDataDir.canWrite()) {
				try {
					progress.startTask(context.getString(R.string.installing_new_resources), -1);
					AssetManager assetManager = context.getAssets();
					boolean isFirstInstall = context.getSettings().PREVIOUS_INSTALLED_VERSION.get().equals("");
					unpackBundledAssets(assetManager, applicationDataDir, progress, isFirstInstall);
					context.getSettings().PREVIOUS_INSTALLED_VERSION.set(fv);
					copyRegionsBoundaries();
					copyPoiTypes();
					for (String internalStyle : context.getRendererRegistry().getInternalRenderers().keySet()) {
						File fl = context.getRendererRegistry().getFileForInternalStyle(internalStyle);
						if (fl.exists()) {
							context.getRendererRegistry().copyFileForInternalStyle(internalStyle);
						}
					}
				} catch (SQLiteException e) {
					log.error(e.getMessage(), e);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				} catch (XmlPullParserException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return Collections.emptyList();
	}
	
	private void copyRegionsBoundaries() {
		try {
			File file = context.getAppPath("regions.ocbf");
			if (file != null) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	private void copyPoiTypes() {
		try {
			File file = context.getAppPath("poi_types.xml");
			if (file != null) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(MapPoiTypes.class.getResourceAsStream("poi_types.xml"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private final static String ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall = "alwaysCopyOnFirstInstall";
	private final static String ASSET_COPY_MODE__overwriteOnlyIfExists = "overwriteOnlyIfExists";
	private final static String ASSET_COPY_MODE__alwaysOverwriteOrCopy = "alwaysOverwriteOrCopy";
	private final static String ASSET_COPY_MODE__copyOnlyIfDoesNotExist = "copyOnlyIfDoesNotExist";
	private void unpackBundledAssets(AssetManager assetManager, File appDataDir, IProgress progress, boolean isFirstInstall) throws IOException, XmlPullParserException {
		XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser(); 
		InputStream isBundledAssetsXml = assetManager.open("bundled_assets.xml");
		xmlParser.setInput(isBundledAssetsXml, "UTF-8");
		
		int next = 0;
		while ((next = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
			if (next == XmlPullParser.START_TAG && xmlParser.getName().equals("asset")) {
				final String source = xmlParser.getAttributeValue(null, "source");
				final String destination = xmlParser.getAttributeValue(null, "destination");
				final String combinedMode = xmlParser.getAttributeValue(null, "mode");
				
				final String[] modes = combinedMode.split("\\|");
				if(modes.length == 0) {
					log.error("Mode '" + combinedMode + "' is not valid");
					continue;
				}
				String installMode = null;
				String copyMode = null;
				for(String mode : modes) {
					if(ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall.equals(mode))
						installMode = mode;
					else if(ASSET_COPY_MODE__overwriteOnlyIfExists.equals(mode) ||
							ASSET_COPY_MODE__alwaysOverwriteOrCopy.equals(mode) ||
							ASSET_COPY_MODE__copyOnlyIfDoesNotExist.equals(mode))
						copyMode = mode;
					else
						log.error("Mode '" + mode + "' is unknown");
				}
				
				final File destinationFile = new File(appDataDir, destination);
				
				boolean unconditional = false;
				if(installMode != null)
					unconditional = unconditional || (ASSET_INSTALL_MODE__alwaysCopyOnFirstInstall.equals(installMode) && isFirstInstall);
				if(copyMode == null)
					log.error("No copy mode was defined for " + source);
				unconditional = unconditional || ASSET_COPY_MODE__alwaysOverwriteOrCopy.equals(copyMode);
				
				boolean shouldCopy = unconditional;
				shouldCopy = shouldCopy || (ASSET_COPY_MODE__overwriteOnlyIfExists.equals(copyMode) && destinationFile.exists());
				shouldCopy = shouldCopy || (ASSET_COPY_MODE__copyOnlyIfDoesNotExist.equals(copyMode) && !destinationFile.exists());
				
				if(shouldCopy)
					copyAssets(assetManager, source, destinationFile);
			}
		}
		
		isBundledAssetsXml.close();
	}

	public static void copyAssets(AssetManager assetManager, String assetName, File file) throws IOException {
		if(file.exists()){
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
		if(dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if(lf == null || lf.length == 0) {
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
		while(it.hasNext()) {
			File f = it.next();
			if (f.getName().endsWith("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName().replace("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT,
						IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)));
			} else if (f.getName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName()));
			}
		}
	}

	public List<String> indexingMaps(final IProgress progress) {
		long val = System.currentTimeMillis();
		ArrayList<File> files = new ArrayList<File>();
		File appPath = context.getAppPath(null);
		File roadsPath = context.getAppPath(IndexConstants.ROADS_INDEX_DIR);
		roadsPath.mkdirs();
		
		collectFiles(appPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		renameRoadsFiles(files, roadsPath);
		collectFiles(roadsPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		if(!Version.isFreeVersion(context)) {
			collectFiles(context.getAppPath(IndexConstants.WIKI_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
		}
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			collectFiles(context.getAppPath(IndexConstants.SRTM_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
		}
		
		changesManager.collectChangesFiles(context.getAppPath(IndexConstants.LIVE_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);

		Collections.sort(files, Algorithms.getFileVersionComparator());
		List<String> warnings = new ArrayList<String>();
		renderer.clearAllResources();
		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = context.getAppPath(INDEXES_CACHE);
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache, CachedOsmandIndexes.VERSION);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		File liveDir = context.getAppPath(IndexConstants.LIVE_INDEX_DIR);
		for (File f : files) {
			progress.startTask(context.getString(R.string.indexing_map) + " " + f.getName(), -1); //$NON-NLS-1$
			try {
				BinaryMapIndexReader mapReader = null;
				try {
					mapReader = cachedOsmandIndexes.getReader(f);
					if (mapReader.getVersion() != IndexConstants.BINARY_MAP_VERSION) {
						mapReader = null;
					}
					if (mapReader != null) {
						renderer.initializeNewResource(progress, f, mapReader);
					}
				} catch (IOException e) {
					log.error(String.format("File %s could not be read", f.getName()), e);
				}
				if (mapReader == null || (Version.isFreeVersion(context) && 
						(f.getName().contains("_wiki") || f.getName().contains(".wiki"))
						)) {
					warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
				} else {
					if (mapReader.isBasemap()) {
						basemapFileNames.put(f.getName(), f.getName());
					}
					long dateCreated = mapReader.getDateCreated();
					if (dateCreated == 0) {
						dateCreated = f.lastModified();
					}
					if(f.getParentFile().getName().equals(liveDir.getName())) {
						boolean toUse = changesManager.index(f, dateCreated, mapReader);
						if(!toUse) {
							try {
								mapReader.close();
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
							continue;
						}
					} else {
						changesManager.indexMainMap(f, dateCreated);
						indexFileNames.put(f.getName(), dateFormat.format(dateCreated)); //$NON-NLS-1$
					}
					if (!mapReader.getRegionNames().isEmpty()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							RegionAddressRepositoryBinary rarb = new RegionAddressRepositoryBinary(this,
									new BinaryMapIndexReader(raf, mapReader), f.getName());
							addressMap.put(f.getName(), rarb);
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
							warnings.add(MessageFormat.format(
									context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					if (mapReader.hasTransportData()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							transportRepositories.put(f.getName(), new TransportIndexRepositoryBinary(new BinaryMapIndexReader(raf, mapReader)));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					// disable osmc for routing temporarily due to some bugs
					if (mapReader.containsRouteData() && (!f.getParentFile().equals(liveDir) || 
							context.getSettings().USE_OSM_LIVE_FOR_ROUTING.get())) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							routingMapFiles.put(f.getName(), new BinaryMapIndexReader(raf, mapReader));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					if (mapReader.containsPoiData()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							amenityRepositories.put(f.getName(), new AmenityIndexRepositoryBinary(new BinaryMapIndexReader(raf, mapReader)));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
				warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
			} catch (OutOfMemoryError oome) {
				log.error("Exception reading " + f.getAbsolutePath(), oome); //$NON-NLS-1$
				warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_big_for_memory), f.getName()));
			}
		}
		log.debug("All map files initialized " + (System.currentTimeMillis() - val) + " ms");
		if (files.size() > 0 && (!indCache.exists() || indCache.canWrite())) {
			try {
				cachedOsmandIndexes.writeToFile(indCache);
			} catch (Exception e) {
				log.error("Index file could not be written", e);
			}
		}
		return warnings;
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
	public List<Amenity> searchAmenities(SearchPoiTypeFilter filter,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, final ResultMatcher<Amenity> matcher) {
		final List<Amenity> amenities = new ArrayList<Amenity>();
		searchAmenitiesInProgress = true;
		try {
			if (!filter.isEmpty()) {
				for (AmenityIndexRepository index : amenityRepositories.values()) {
					if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
						List<Amenity> r = index.searchAmenities(MapUtils.get31TileNumberY(topLatitude),
								MapUtils.get31TileNumberX(leftLongitude), MapUtils.get31TileNumberY(bottomLatitude),
								MapUtils.get31TileNumberX(rightLongitude), zoom, filter, matcher);
						if(r != null) {
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
	
	public List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, SearchPoiTypeFilter filter,
			ResultMatcher<Amenity> matcher) {
		searchAmenitiesInProgress = true;
		final List<Amenity> amenities = new ArrayList<Amenity>();
		try {
			if (locations != null && locations.size() > 0) {
				List<AmenityIndexRepository> repos = new ArrayList<AmenityIndexRepository>();
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
					for (AmenityIndexRepository index : amenityRepositories.values()) {
						if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
							repos.add(index);
						}
					}
					if (!repos.isEmpty()) {
						for (AmenityIndexRepository r : repos) {
							List<Amenity> res = r.searchAmenitiesOnThePath(locations, radius, filter, matcher);
							if(res != null) {
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
	
	
	public boolean containsAmenityRepositoryToSearch(boolean searchByName){
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if(searchByName){
				if(index instanceof AmenityIndexRepositoryBinary){
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
		List<Amenity> amenities = new ArrayList<Amenity>();
		List<AmenityIndexRepositoryBinary> list = new ArrayList<AmenityIndexRepositoryBinary>();
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if (index instanceof AmenityIndexRepositoryBinary) {
				if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
					if(index.checkContains(lat, lon)){
						list.add(0, (AmenityIndexRepositoryBinary) index);
					} else {
						list.add((AmenityIndexRepositoryBinary) index);
					}
					
				}
			}
		}
//		int left = MapUtils.get31TileNumberX(leftLongitude);
//		int top = MapUtils.get31TileNumberY(topLatitude);
//		int right = MapUtils.get31TileNumberX(rightLongitude);
//		int bottom = MapUtils.get31TileNumberY(bottomLatitude);
		int left = 0;
		int top = 0;
		int right = Integer.MAX_VALUE;
		int bottom = Integer.MAX_VALUE;
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
	
	public Map<PoiCategory, List<String>> searchAmenityCategoriesByName(String searchQuery, double lat, double lon) {
		Map<PoiCategory, List<String>> map = new LinkedHashMap<PoiCategory, List<String>>();
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if (index instanceof AmenityIndexRepositoryBinary) {
				if (index.checkContains(lat, lon)) {
					((AmenityIndexRepositoryBinary) index).searchAmenityCategoriesByName(searchQuery, map);
				}
			}
		}
		return map;
	}
	
	
	////////////////////////////////////////////// Working with address ///////////////////////////////////////////
	
	public RegionAddressRepository getRegionRepository(String name){
		return addressMap.get(name);
	}
	
	public Collection<RegionAddressRepository> getAddressRepositories(){
		return addressMap.values();
	}
	
	
	////////////////////////////////////////////// Working with transport ////////////////////////////////////////////////
	public List<TransportIndexRepository> searchTransportRepositories(double latitude, double longitude) {
		List<TransportIndexRepository> repos = new ArrayList<TransportIndexRepository>();
		for (TransportIndexRepository index : transportRepositories.values()) {
			if (index.checkContains(latitude,longitude)) {
				repos.add(index);
			}
		}
		return repos;
	}
	
	
	public void searchTransportAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, List<TransportStop> toFill){
		List<TransportIndexRepository> repos = new ArrayList<TransportIndexRepository>();
		for (TransportIndexRepository index : transportRepositories.values()) {
			if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
				if (!index.checkCachedObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, toFill, true)) {
					repos.add(index);
				}
			}
		}
		if(!repos.isEmpty()){
			TransportLoadRequest req = asyncLoadingThread.new TransportLoadRequest(repos, zoom);
			req.setBoundaries(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
			asyncLoadingThread.requestToLoadTransport(req);
		}
	}
	
	////////////////////////////////////////////// Working with map ////////////////////////////////////////////////
	public boolean updateRenderedMapNeeded(RotatedTileBox rotatedTileBox, DrawSettings drawSettings) {
		return renderer.updateMapIsNeeded(rotatedTileBox, drawSettings);
	}
	
	public void updateRendererMap(RotatedTileBox rotatedTileBox){
		renderer.interruptLoadingMap();
		asyncLoadingThread.requestToLoadMap(new MapLoadRequest(rotatedTileBox));
	}
	
	public void interruptRendering(){
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
		AmenityIndexRepository rep = amenityRepositories.remove(fileName);
		if(rep != null) {
			rep.close();
		}
		RegionAddressRepository rar = addressMap.remove(fileName);
		if(rar != null) {
			rar.close();
		}
		TransportIndexRepository tir = transportRepositories.remove(fileName);
		if(tir != null) {
			tir.close();
		}
		BinaryMapIndexReader rmp = routingMapFiles.remove(fileName);
		if(rmp != null) {
			try {
				rmp.close();
			} catch (IOException e) {
				log.error(e, e);
			}
		}
		indexFileNames.remove(fileName);
		renderer.closeConnection(fileName);
	}
	
	public void closeAmenities(){
		for(AmenityIndexRepository r : amenityRepositories.values()){
			r.close();
		}
		amenityRepositories.clear();
	}
	
	public void closeAddresses(){
		for(RegionAddressRepository r : addressMap.values()){
			r.close();
		}
		addressMap.clear();
	}
	
	public void closeTransport(){
		for(TransportIndexRepository r : transportRepositories.values()){
			r.close();
		}
		transportRepositories.clear();
	}
	
	public BusyIndicator getBusyIndicator() {
		return busyIndicator;
	}
	
	public synchronized void setBusyIndicator(BusyIndicator busyIndicator) {
		this.busyIndicator = busyIndicator;
	}

	public synchronized void close(){
		imagesOnFS.clear();
		indexFileNames.clear();
		basemapFileNames.clear();
		renderer.clearAllResources();
		closeAmenities();
		closeRouteFiles();
		closeAddresses();
		closeTransport();
	}
	
	
	public BinaryMapIndexReader[] getRoutingMapFiles() {
		return routingMapFiles.values().toArray(new BinaryMapIndexReader[routingMapFiles.size()]);
	}
	
	public void closeRouteFiles() {
		List<String> map = new ArrayList<String>(routingMapFiles.keySet());
		for(String m : map){
			try {
				BinaryMapIndexReader ind = routingMapFiles.remove(m);
				if(ind != null){
					ind.getRaf().close();
				}
			} catch(IOException e){
				log.error("Error closing resource " + m, e);
			}
		}
		
	}

	public Map<String, String> getIndexFileNames() {
		return new LinkedHashMap<String, String>(indexFileNames);
	}
	
	
	public boolean containsBasemap(){
		return !basemapFileNames.isEmpty();
	}
	
	public Map<String, String> getBackupIndexes(Map<String, String> map) {
		File file = context.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		if (file != null && file.isDirectory()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
						map.put(f.getName(), AndroidUtils.formatDate(context, f.lastModified())); //$NON-NLS-1$		
					}
				}
			}
		}
		return map;
	}
	
	public synchronized void reloadTilesFromFS(){
		imagesOnFS.clear();
	}
	
	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		clearTiles();
		for(RegionAddressRepository r : addressMap.values()){
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
		log.info("Cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size() / 2; i++) {
			cacheOfImages.remove(list.get(i));
		}
	}
	
	public IncrementalChangesManager getChangesManager() {
		return changesManager;
	}
}
