package net.osmand;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import net.osmand.activities.OsmandApplication;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.TransportStop;
import net.osmand.data.index.IndexConstants;
import net.osmand.data.preparation.MapTileDownloader;
import net.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import net.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.ITileSource;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.render.BaseOsmandRender;
import net.osmand.render.MapRenderRepositories;
import net.osmand.render.RendererRegistry;
import net.osmand.views.POIMapLayer;

import org.apache.commons.logging.Log;

import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 *
 */
public class ResourceManager {

	public static final String APP_DIR = "osmand/"; //$NON-NLS-1$
	public static final String POI_PATH = APP_DIR + IndexConstants.POI_INDEX_DIR; 
	public static final String VOICE_PATH = APP_DIR + IndexConstants.VOICE_INDEX_DIR;
	public static final String MAPS_PATH = APP_DIR;
	public static final String ADDRESS_PATH = APP_DIR + IndexConstants.ADDRESS_INDEX_DIR;
	public static final String TRANSPORT_PATH = APP_DIR + IndexConstants.TRANSPORT_INDEX_DIR;
	public static final String TILES_PATH = APP_DIR+"tiles/"; //$NON-NLS-1$
	public static final String TEMP_SOURCE_TO_LOAD = "temp"; //$NON-NLS-1$
	public static final String VECTOR_MAP = "#vector_map"; //$NON-NLS-1$
	
	public static final int LIMIT_TRANSPORT = 200;
	
	private static final Log log = LogUtil.getLog(ResourceManager.class);
	
	protected static ResourceManager manager = null;
	
	// it is not good investigated but no more than 64 (satellite images)
	// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit 
	protected int maxImgCacheSize = 32;
	
	protected Map<String, Bitmap> cacheOfImages = new LinkedHashMap<String, Bitmap>();
	protected Map<String, Boolean> imagesOnFS = new LinkedHashMap<String, Boolean>() ;
	
	protected File dirWithTiles ;
	
	private final OsmandApplication context;
	
	private BusyIndicator busyIndicator;
	
	private final MapTileDownloader downloader = MapTileDownloader.getInstance();
	// Indexes
	private final Map<String, RegionAddressRepository> addressMap = new TreeMap<String, RegionAddressRepository>(Collator.getInstance());
	
	protected final List<AmenityIndexRepository> amenityRepositories =  new ArrayList<AmenityIndexRepository>();
	
	protected final List<TransportIndexRepository> transportRepositories = new ArrayList<TransportIndexRepository>();
	
	protected final Map<String, String> indexFileNames = new LinkedHashMap<String, String>();
	
	protected final MapRenderRepositories renderer;
	
	public final AsyncLoadingThread asyncLoadingTiles = new AsyncLoadingThread();
	
	protected boolean internetIsNotAccessible = false;
	
	
	public ResourceManager(OsmandApplication context) {
		this.context = context;
		this.renderer = new MapRenderRepositories(context);
		asyncLoadingTiles.start();
		dirWithTiles = new File(Environment.getExternalStorageDirectory(), TILES_PATH);
		if(Environment.getExternalStorageDirectory().canRead()){
			dirWithTiles.mkdirs();
		}
		
	}
	
	public OsmandApplication getContext() {
		return context;
	}
	
	////////////////////////////////////////////// Working with tiles ////////////////////////////////////////////////
	
	public void indexingImageTiles(IProgress progress){
		progress.startTask(Messages.getMessage("reading_cached_tiles"), -1); //$NON-NLS-1$
		imagesOnFS.clear();
		for(File c : dirWithTiles.listFiles()){
			indexImageTilesFS("", c); //$NON-NLS-1$
		}
	}
	
	private void indexImageTilesFS(String prefix, File f){
		if(f.isDirectory()){
			for(File c : f.listFiles()){
				indexImageTilesFS(prefix +f.getName() +"/" , c); //$NON-NLS-1$
			}
		} else if(f.getName().endsWith(".tile")){ //$NON-NLS-1$
			imagesOnFS.put(prefix + f.getName(), Boolean.TRUE);
		} else if(f.getName().endsWith(".sqlitedb")){ //$NON-NLS-1$
			// nothing to do here
		}
	}
	
	
	public Bitmap getTileImageForMapAsync(String file, ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(file, map, x, y, zoom, loadFromInternetIfNeeded, false, true);
	}
	
	
	public Bitmap getTileImageFromCache(String file){
		return cacheOfImages.get(file);
	}
	
	
	public Bitmap getTileImageForMapSync(String file, ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(file, map, x, y, zoom, loadFromInternetIfNeeded, true, true);
	}
	
	public void tileDownloaded(DownloadRequest request){
		if(request instanceof TileLoadDownloadRequest){
			TileLoadDownloadRequest req = ((TileLoadDownloadRequest) request);
			imagesOnFS.put(req.tileId, Boolean.TRUE);
			if(req.fileToSave != null && req.tileSource instanceof SQLiteTileSource){
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
			}
		}
		
	}
	
	public synchronized boolean tileExistOnFileSystem(String file, ITileSource map, int x, int y, int zoom){
		if(!imagesOnFS.containsKey(file)){
			boolean ex = false;
			if(map instanceof SQLiteTileSource){
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
		return imagesOnFS.get(file) != null;		
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
	private int insertString(char[] ar, int offset, String s) {
		for (int j = 0; j < s.length(); j++) {
			ar[offset++] = s.charAt(j);
		}
		return offset;
	}
	protected StringBuilder builder = new StringBuilder(40);
	protected char[] tileId = new char[120];
	public synchronized String calculateTileId(ITileSource map, int x, int y, int zoom){
		if(false){
			// performance improve ?
			int ind = 0;
			String mapName = map == null ? TEMP_SOURCE_TO_LOAD : map.getName();
			ind = insertString(tileId, ind, mapName);
			if (map instanceof SQLiteTileSource) {
				tileId[ind++] = '@';
			} else {
				tileId[ind++] = '/';
			}
			ind = insertString(tileId, ind, Integer.toString(zoom));
			tileId[ind++] = '/';
			ind = insertString(tileId, ind, Integer.toString(x));
			tileId[ind++] = '/';
			ind = insertString(tileId, ind, Integer.toString(y));
			ind = insertString(tileId, ind, map == null ? ".jpg" : map.getTileFormat()); //$NON-NLS-1$
			ind = insertString(tileId, ind, ".tile"); //$NON-NLS-1$
			return new String(tileId, 0, ind);
		} else {

			builder.setLength(0);
			if (map == null) {
				builder.append(TEMP_SOURCE_TO_LOAD);
			} else {
				builder.append(map.getName());
			}

			if (map instanceof SQLiteTileSource) {
				builder.append('@');
			} else {
				builder.append('/');
			}
			builder.append(zoom).append('/').append(x).append('/').append(y)
					.append(map == null ? ".jpg" : map.getTileFormat()).append(".tile"); //$NON-NLS-1$ //$NON-NLS-2$
			return builder.toString();
		}
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
			if(!loadFromInternetIfNeeded && !tileExistOnFileSystem(tileId, map, x, y, zoom)){
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
					tileId, map, x, y, zoom);
			if(sync){
				return getRequestedImageTile(req);
			} else {
				asyncLoadingTiles.requestToLoadImage(req);
			}
		}
		return cacheOfImages.get(tileId);
	}
	
	
	
	private Bitmap getRequestedImageTile(TileLoadDownloadRequest req){
		if(req.tileId == null || req.dirWithTiles == null){
			return null;
		}
		if (cacheOfImages.size() > maxImgCacheSize) {
			clearTiles();
		}
		if (req.dirWithTiles.canRead() && !downloader.isFileCurrentlyDownloaded(req.fileToSave)) {
			long time = System.currentTimeMillis();
			Bitmap bmp = null;
			if (req.tileSource instanceof SQLiteTileSource) {
				bmp = ((SQLiteTileSource) req.tileSource).getImage(req.xTile, req.yTile, req.zoom);
			} else {
				File en = new File(req.dirWithTiles, req.tileId);
				if (en.exists()) {
					try {
						bmp = BitmapFactory.decodeFile(en.getAbsolutePath());
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
				downloader.requestToDownload(req);
			}

		}
		return cacheOfImages.get(req.tileId);
	}
	
    ////////////////////////////////////////////// Working with indexes ////////////////////////////////////////////////

	public List<String> reloadIndexes(IProgress progress){
		close();
		initRenderers(progress);
		// do it lazy
		// indexingImageTiles(progress);
		List<String> warnings = new ArrayList<String>();
		warnings.addAll(indexingPoi(progress));
		warnings.addAll(indexingAddresses(progress));
		warnings.addAll(indexingTransport(progress));
		warnings.addAll(indexingMaps(progress));
		return warnings;
	}
	
	private void initRenderers(IProgress progress) {
		File file = new File(Environment.getExternalStorageDirectory(), APP_DIR + IndexConstants.RENDERERS_DIR);
		if(Environment.getExternalStorageDirectory().canRead()){
			file.mkdirs();
		}
		Map<String, File> externalRenderers = new LinkedHashMap<String, File>(); 
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.RENDERER_INDEX_EXT)) {
					String name = f.getName().substring(0, f.getName().length() - IndexConstants.RENDERER_INDEX_EXT.length());
					externalRenderers.put(name, f);
				}
			}
		}
		RendererRegistry.getRegistry().setExternalRenderers(externalRenderers);
		String r = OsmandSettings.getVectorRenderer(OsmandSettings.getPrefs(context));
		if(r != null){
			BaseOsmandRender obj = RendererRegistry.getRegistry().getRenderer(r);
			if(obj != null){
				RendererRegistry.getRegistry().setCurrentSelectedRender(obj);
			}
		}
	}

	public List<String> indexingMaps(final IProgress progress) {
		File file = new File(Environment.getExternalStorageDirectory(), MAPS_PATH);
		if(Environment.getExternalStorageDirectory().canRead()){
			file.mkdirs();
		}
		List<String> warnings = new ArrayList<String>();
		renderer.clearAllResources();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					progress.startTask(Messages.getMessage("indexing_map") + f.getName(), -1); //$NON-NLS-1$
					try {
						BinaryMapIndexReader index = renderer.initializeNewResource(progress, f);
						if (index == null) {
							warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
						} else {
							indexFileNames.put(f.getName(), MessageFormat.format("{0,date,dd.MM.yyyy}", new Date(f.lastModified()))); //$NON-NLS-1$
							for(String rName : index.getRegionNames()) {
								// skip duplicate names (don't make collision between getName() and name in the map)
								RegionAddressRepositoryBinary rarb = new RegionAddressRepositoryBinary(index, rName);
								addressMap.put(rName, rarb);
							}
							if (index.hasTransportData()) {
								try {
									RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
									transportRepositories.add(new TransportIndexRepositoryBinary(new BinaryMapIndexReader(raf)));
								} catch (IOException e) {
									log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
									warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
								}
							}
							if(index.containsMapData()){
								// that's not fully acceptable
								// TODO
//								try {
//									RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
//									amenityRepositories.add(new AmenityIndexRepositoryBinary(new BinaryMapIndexReader(raf)));
//								} catch (IOException e) {
//									log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
//									warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
//								}
							}
						}
					} catch (SQLiteException e) {
						log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
						warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
					} catch (OutOfMemoryError oome) {
						log.error("Exception reading " + f.getAbsolutePath(), oome); //$NON-NLS-1$
						warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_big_for_memory"), f.getName()));
					}
				} else if(f.getName().endsWith(".map.odb")){ //$NON-NLS-1$
					warnings.add(MessageFormat.format(Messages.getMessage("old_map_index_is_not_supported"), f.getName())); //$NON-NLS-1$
				}
			}
		}
		return warnings;
	}
	
	// POI INDEX //
	public List<String> indexingPoi(final IProgress progress) {
		File file = new File(Environment.getExternalStorageDirectory(), POI_PATH);
		if(Environment.getExternalStorageDirectory().canRead()){
			file.mkdirs();
		}
		List<String> warnings = new ArrayList<String>();
		closeAmenities();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				indexingPoi(progress, warnings, f);
			}
		}
		return warnings;
	}
	
	public void indexingPoi(final IProgress progress, List<String> warnings, File f) {
		if (f.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
			AmenityIndexRepositoryOdb repository = new AmenityIndexRepositoryOdb();
			
			progress.startTask(Messages.getMessage("indexing_poi") + f.getName(), -1); //$NON-NLS-1$
			try {
				boolean initialized = repository.initialize(progress, f);
				if (initialized) {
					amenityRepositories.add(repository);
					indexFileNames.put(f.getName(), MessageFormat.format("{0,date,dd.MM.yyyy}", new Date(f.lastModified()))); //$NON-NLS-1$
				} else {
					warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
				warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
			}
		}
	}
	
	public void updateIndexLastDateModified(File f){
		if(f != null && f.exists()){
			indexFileNames.put(f.getName(), MessageFormat.format("{0,date,dd.MM.yyyy}", new Date(f.lastModified()))); //$NON-NLS-1$
		}
	}
	
		
	public List<String> indexingAddresses(final IProgress progress){
		File file = new File(Environment.getExternalStorageDirectory(), ADDRESS_PATH);
		List<String> warnings = new ArrayList<String>();
		closeAddresses();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				indexingAddress(progress, warnings, f);
			}
		}
		return warnings;
	}

	public void indexingAddress(final IProgress progress, List<String> warnings, File f) {
		if (f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)) {
			RegionAddressRepositoryOdb repository = new RegionAddressRepositoryOdb();
			progress.startTask(Messages.getMessage("indexing_address") + f.getName(), -1); //$NON-NLS-1$
			try {
				boolean initialized = repository.initialize(progress, f);
				if (initialized) {
					addressMap.put(repository.getName(), repository);
					indexFileNames.put(f.getName(), MessageFormat.format("{0,date,dd.MM.yyyy}", new Date(f.lastModified()))); //$NON-NLS-1$
				} else {
					warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
				warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
			}
		}
	}
	
	
	public List<String> indexingTransport(final IProgress progress){
		File file = new File(Environment.getExternalStorageDirectory(), TRANSPORT_PATH);
		List<String> warnings = new ArrayList<String>();
		closeTransport();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				indexingTransport(progress, warnings, f);
			}
		}
		return warnings;
	}

	public void indexingTransport(final IProgress progress, List<String> warnings, File f) {
		if (f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT)) {
			TransportIndexRepositoryOdb repository = new TransportIndexRepositoryOdb();
			progress.startTask(Messages.getMessage("indexing_transport") + f.getName(), -1); //$NON-NLS-1$
			try {
				boolean initialized = repository.initialize(progress, f);
				if (initialized) {
					transportRepositories.add(repository);
					indexFileNames.put(f.getName(), MessageFormat.format("{0,date,dd.MM.yyyy}", new Date(f.lastModified()))); //$NON-NLS-1$
				} else {
					warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
				}
			} catch (SQLiteException e) {
				log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
				warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
			}
		}
	}
	
	////////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<AmenityIndexRepository> searchAmenityRepositories(double latitude, double longitude) {
		List<AmenityIndexRepository> repos = new ArrayList<AmenityIndexRepository>();
		for (AmenityIndexRepository index : amenityRepositories) {
			if (index.checkContains(latitude,longitude)) {
				repos.add(index);
			}
		}
		return repos;
	}
	
	public List<Amenity> searchAmenities(PoiFilter filter, double latitude, double longitude, int zoom, int limit) {
		double tileNumberX = MapUtils.getTileNumberX(zoom, longitude);
		double tileNumberY = MapUtils.getTileNumberY(zoom, latitude);
		double topLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY - 0.5);
		double bottomLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY + 0.5);
		double leftLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX - 0.5);
		double rightLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX + 0.5);
		List<Amenity> amenities = new ArrayList<Amenity>();
		for (AmenityIndexRepository index : amenityRepositories) {
			if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
				if (!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filter.getFilterId(), 
						amenities, false)) {
					index.searchAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, limit, filter, amenities);
				}
			}
		}

		return amenities;
	}
	
	public void searchAmenitiesAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, PoiFilter filter, List<Amenity> toFill){
		if(filter instanceof NameFinderPoiFilter){
			List<Amenity> amenities = ((NameFinderPoiFilter) filter).getSearchedAmenities();
			for(Amenity a : amenities){
				LatLon l = a.getLocation();
				if(l != null && l.getLatitude() <= topLatitude && l.getLatitude() >= bottomLatitude && l.getLongitude() >= leftLongitude && l.getLongitude() <= rightLongitude){
					toFill.add(a);
				}
			}
			
		} else {
			String filterId = filter == null ? null : filter.getFilterId();
			for (AmenityIndexRepository index : amenityRepositories) {
				if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
					if (!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filterId, toFill,
							true)) {
						asyncLoadingTiles.requestToLoadAmenities(new AmenityLoadRequest(index, topLatitude, leftLongitude, bottomLatitude,
								rightLongitude, zoom, filter));
					}
				}
			}
		}
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
		for (TransportIndexRepository index : transportRepositories) {
			if (index.checkContains(latitude,longitude)) {
				repos.add(index);
			}
		}
		return repos;
	}
	
	
	public void searchTransportAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, List<TransportStop> toFill){
		for(TransportIndexRepository index : transportRepositories){
			if(index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)){
				if(!index.checkCachedObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, toFill, true)){
					asyncLoadingTiles.requestToLoadTransport(
							new TransportLoadRequest(index, topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom));
				}
			}
		}
	}
	
	////////////////////////////////////////////// Working with map ////////////////////////////////////////////////
	public boolean updateRenderedMapNeeded(RotatedTileBox rotatedTileBox){
		return renderer.updateMapIsNeeded(rotatedTileBox);
	}
	
	public void updateRendererMap(RotatedTileBox rotatedTileBox){
		renderer.interruptLoadingMap();
		asyncLoadingTiles.requestToLoadMap(
				new MapLoadRequest(new RotatedTileBox(rotatedTileBox)));
	}
	
	public void interruptRendering(){
		renderer.interruptLoadingMap();
	}
	
	public MapRenderRepositories getRenderer() {
		return renderer;
	}
	
	////////////////////////////////////////////// Closing methods ////////////////////////////////////////////////
	
	public void closeAmenities(){
		for(AmenityIndexRepository r : amenityRepositories){
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
		for(TransportIndexRepository r : transportRepositories){
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
		renderer.clearAllResources();
		closeAmenities();
		closeAddresses();
		closeTransport();
	}
	
	public Map<String, String> getIndexFileNames() {
		return indexFileNames;
	}
	
	public synchronized void reloadTilesFromFS(){
		imagesOnFS.clear();
	}
	
	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		clearTiles();
		for(AmenityIndexRepository r : amenityRepositories){
			r.clearCache();
		}
		for(RegionAddressRepository r : addressMap.values()){
			r.clearCache();
		}
		renderer.clearCache();
		
		System.gc();
	}	
	
	
	public synchronized void updateMapSource(boolean useVectorMap, ITileSource source){
		log.info("Clear cache with new source " + cacheOfImages.size()); //$NON-NLS-1$
		cacheOfImages.clear();
		renderer.clearCache();
		if(source == null || source.getBitDensity() == 0){
			maxImgCacheSize = 32;
		} else {
			maxImgCacheSize = Math.max(384 / source.getBitDensity() , 32);
		}
		
	}
	
	
	protected synchronized void clearTiles(){
		log.info("Cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size() /2; i ++) {
			cacheOfImages.remove(list.get(i));
		}
	}
	

	private static class TileLoadDownloadRequest extends DownloadRequest {

		public final String tileId;
		public final File dirWithTiles; 
		public final ITileSource tileSource;
		
		public TileLoadDownloadRequest(File dirWithTiles, String url, File fileToSave, 
				String tileId, ITileSource source, int tileX, int tileY, int zoom) {
			super(url, fileToSave, tileX, tileY, zoom);
			this.dirWithTiles = dirWithTiles;
			tileSource = source;
			this.tileId = tileId;
		}
	}
	
	private static class AmenityLoadRequest {
		public final AmenityIndexRepository repository;
		public final double topLatitude;
		public final double bottomLatitude;
		public final double leftLongitude;
		public final double rightLongitude;
		public final PoiFilter filter;
		public final int zoom;
		
		public AmenityLoadRequest(AmenityIndexRepository repository, double topLatitude, double leftLongitude, 
				double bottomLatitude, double rightLongitude, int zoom, PoiFilter filter) {
			super();
			this.bottomLatitude = bottomLatitude;
			this.leftLongitude = leftLongitude;
			this.repository = repository;
			this.rightLongitude = rightLongitude;
			this.topLatitude = topLatitude;
			this.zoom = zoom;
			this.filter = filter;
		}
	}
	
	
	
	private static class TransportLoadRequest {
		public final TransportIndexRepository repository;
		public final double topLatitude;
		public final double bottomLatitude;
		public final double leftLongitude;
		public final double rightLongitude;
		public final int zoom;
		
		public TransportLoadRequest(TransportIndexRepository repository, double topLatitude, double leftLongitude, 
				double bottomLatitude, double rightLongitude, int zoom) {
			super();
			this.bottomLatitude = bottomLatitude;
			this.leftLongitude = leftLongitude;
			this.repository = repository;
			this.rightLongitude = rightLongitude;
			this.topLatitude = topLatitude;
			this.zoom = zoom;
		}
	}
	
	private static class MapLoadRequest {
		public final RotatedTileBox tileBox;
		
		public MapLoadRequest(RotatedTileBox tileBox) {
			super();
			this.tileBox = tileBox;
		}
	}
	
	public class AsyncLoadingThread extends Thread {
		Stack<Object> requests = new Stack<Object>();
		
		public AsyncLoadingThread(){
			super("Loader map objects (tiles, poi)"); //$NON-NLS-1$
		}
		
		@Override
		public void run() {
			while(true){
				try {
					boolean update = false;
					boolean amenityLoaded = false;
					boolean transportLoaded = false;
					boolean mapLoaded = false;
					int progress = 0;
					if(downloader.isSomethingBeingDownloaded()){
						progress = BusyIndicator.STATUS_GREEN;
					}
					synchronized(ResourceManager.this){
						if(busyIndicator != null){
							if(context.getRoutingHelper().isRouteBeingCalculated()){
								progress = BusyIndicator.STATUS_BLUE;
							} else if(!requests.isEmpty()){
								progress = BusyIndicator.STATUS_BLACK;;
							}
							busyIndicator.updateStatus(progress);
						}
					}
					while(!requests.isEmpty()){
						Object req = requests.pop();
						if (req instanceof TileLoadDownloadRequest) {
							TileLoadDownloadRequest r = (TileLoadDownloadRequest) req;
							if (cacheOfImages.get(r.tileId) == null) {
								update |= getRequestedImageTile(r) != null;
							}
						} else if(req instanceof AmenityLoadRequest){
							if(!amenityLoaded){
								AmenityLoadRequest r = (AmenityLoadRequest) req;
								r.repository.evaluateCachedAmenities(r.topLatitude, r.leftLongitude, 
										r.bottomLatitude, r.rightLongitude, r.zoom, POIMapLayer.LIMIT_POI, r.filter, null);
								amenityLoaded = true;
							}
						} else if(req instanceof TransportLoadRequest){
							if(!transportLoaded){
								TransportLoadRequest r = (TransportLoadRequest) req;
								r.repository.evaluateCachedTransportStops(r.topLatitude, r.leftLongitude, 
										r.bottomLatitude, r.rightLongitude, r.zoom, LIMIT_TRANSPORT, null);
								transportLoaded = true;
							}
						} else if(req instanceof MapLoadRequest){
							if(!mapLoaded){
								MapLoadRequest r = (MapLoadRequest) req;
								renderer.loadMap(r.tileBox, downloader.getDownloaderCallbacks());
								mapLoaded = true;
							}
						}
					}
					if(update || amenityLoaded || transportLoaded || mapLoaded){
						// use downloader callback
						for(IMapDownloaderCallback c : downloader.getDownloaderCallbacks()){
							c.tileDownloaded(null);
						}
					}
					boolean routeBeingCalculated = context.getRoutingHelper().isRouteBeingCalculated();
					if (progress != 0 || routeBeingCalculated || downloader.isSomethingBeingDownloaded()) {
						synchronized (ResourceManager.this) {
							if (busyIndicator != null) {
								if(routeBeingCalculated){
									progress = BusyIndicator.STATUS_BLUE;
								} else if(downloader.isSomethingBeingDownloaded()){
									progress = BusyIndicator.STATUS_GREEN;
								} else {
									progress = 0;
								}
								busyIndicator.updateStatus(progress);
							}
						}
					}
					sleep(750);
				} catch (InterruptedException e) {
					log.error(e, e);
				} catch (RuntimeException e){
					log.error(e, e);
				}
			}
		}
		
		public void requestToLoadImage(TileLoadDownloadRequest req){
			requests.push(req);
		}
		public void requestToLoadAmenities(AmenityLoadRequest req){
			requests.push(req);
		}
		
		public void requestToLoadMap(MapLoadRequest req){
			requests.push(req);
		}
		
		public void requestToLoadTransport(TransportLoadRequest req){
			requests.push(req);
		}
	};
}
