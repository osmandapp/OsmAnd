package com.osmand;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.logging.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.osmand.data.Amenity;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.MapUtils;
import com.osmand.views.POIMapLayer;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 *
 */
public class ResourceManager {

	private static final String POI_PATH = "osmand/" + IndexConstants.POI_INDEX_DIR; //$NON-NLS-1$
	private static final String ADDRESS_PATH = "osmand/" + IndexConstants.ADDRESS_INDEX_DIR; //$NON-NLS-1$
	private static final String TILES_PATH = "osmand/tiles/"; //$NON-NLS-1$
	
	private static final Log log = LogUtil.getLog(ResourceManager.class);
	
	protected static ResourceManager manager = null;
	
	public static ResourceManager getResourceManager(){
		if(manager == null){
			manager = new ResourceManager();
		}
		return manager;
	}
	
	// it is not good investigated but no more than 64 (satellite images)
	protected final int maxImgCacheSize = 64;
	
	protected Map<String, Bitmap> cacheOfImages = new LinkedHashMap<String, Bitmap>();
	
	protected File dirWithTiles ;
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();

	// Indexes
	private Map<String, RegionAddressRepository> addressMap = new TreeMap<String, RegionAddressRepository>(Collator.getInstance());
	
	protected List<AmenityIndexRepository> amenityRepositories = new ArrayList<AmenityIndexRepository>();
	
	public AsyncLoadingThread asyncLoadingTiles = new AsyncLoadingThread();
	
	
	
	
	public ResourceManager() {
		// TODO start/stop this thread when needed?
		asyncLoadingTiles.start();
		dirWithTiles = new File(Environment.getExternalStorageDirectory(), TILES_PATH);
		if(Environment.getExternalStorageDirectory().canRead()){
			dirWithTiles.mkdirs();
		}
		
	}
	
	////////////////////////////////////////////// Working with tiles ////////////////////////////////////////////////
	
	public Bitmap getTileImageForMapAsync(ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(map, x, y, zoom, loadFromInternetIfNeeded, false, true);
	}
	
	
	public Bitmap getTileImageFromCache(ITileSource map, int x, int y, int zoom){
		return getTileImageForMap(map, x, y, zoom, false, false, false);
	}
	
	public Bitmap getTileImageForMapSync(ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(map, x, y, zoom, loadFromInternetIfNeeded, true, true);
	}
	
	public boolean tileExistOnFileSystem(ITileSource map, int x, int y, int zoom){
		// TODO implement
		return false;
	}
	
	public void clearTileImageForMap(ITileSource map, int x, int y, int zoom){
		getTileImageForMap(map, x, y, zoom, true, false, false, true);
	}
	protected Bitmap getTileImageForMap(ITileSource map, int x, int y, int zoom, 
			boolean loadFromInternetIfNeeded, boolean sync, boolean loadFromFs) {
		return getTileImageForMap(map, x, y, zoom, loadFromInternetIfNeeded, sync, loadFromFs, false);
	}
	
	// introduce cache in order save memory
	protected StringBuilder builder = new StringBuilder(40);
	protected synchronized Bitmap getTileImageForMap(ITileSource map, int x, int y, int zoom, 
			boolean loadFromInternetIfNeeded, boolean sync, boolean loadFromFs, boolean deleteBefore) {
		if (map == null) {
			return null;
		}
		builder.setLength(0);
		builder.append(map.getName()).append('/').append(zoom).	append('/').append(x).
				append('/').append(y).append(map.getTileFormat()).append(".tile"); //$NON-NLS-1$
		String file = builder.toString();
		if(deleteBefore){
			cacheOfImages.remove(file);
			File f = new File(dirWithTiles, file);
			if(f.exists()){
				f.delete();
			}
		}
		
		if (loadFromFs && cacheOfImages.get(file) == null) {
			String url = loadFromInternetIfNeeded ? map.getUrlToLoad(x, y, zoom) : null;
			TileLoadDownloadRequest req = new TileLoadDownloadRequest(dirWithTiles, file, url, new File(dirWithTiles, file), 
					x, y, zoom);
			if(sync){
				return getRequestedImageTile(req);
			} else {
				asyncLoadingTiles.requestToLoadImage(req);
			}
		}
		return cacheOfImages.get(file);
	}
	
	
	
	private Bitmap getRequestedImageTile(TileLoadDownloadRequest req){
		if(req.fileToLoad == null || req.dirWithTiles == null){
			return null;
		}
		File en = new File(req.dirWithTiles, req.fileToLoad);
		if (cacheOfImages.size() > maxImgCacheSize) {
			clearTiles();
		}
		
		if (!downloader.isFileCurrentlyDownloaded(en) && req.dirWithTiles.canRead()) {
			if (en.exists()) {
				long time = System.currentTimeMillis();
				cacheOfImages.put(req.fileToLoad, BitmapFactory.decodeFile(en.getAbsolutePath()));
				if (log.isDebugEnabled()) {
					log.debug("Loaded file : " + req.fileToLoad + " " + -(time - System.currentTimeMillis()) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			} 
			
			if(cacheOfImages.get(req.fileToLoad) == null && req.url != null){
				// TODO we could check that network is available (context is required)
//				ConnectivityManager mgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//				NetworkInfo info = mgr.getActiveNetworkInfo();
//				if (info != null && info.isConnected()) {
//					downloader.requestToDownload(req);
//				}
				downloader.requestToDownload(req);
			}
		}
		return cacheOfImages.get(req.fileToLoad);
	}
	
    ////////////////////////////////////////////// Working with indexes ////////////////////////////////////////////////

	public List<String> reloadIndexes(IProgress progress){
		close();
		List<String> warnings = new ArrayList<String>();
		warnings.addAll(indexingPoi(progress));
		warnings.addAll(indexingAddresses(progress));
		return warnings;
	}
	
	// POI INDEX //
	public List<String> indexingPoi(final IProgress progress) {
		File file = new File(Environment.getExternalStorageDirectory(), POI_PATH);
		List<String> warnings = new ArrayList<String>();
		closeAmenities();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
					AmenityIndexRepository repository = new AmenityIndexRepository();
					
					progress.startTask(Messages.getMessage("indexing_poi") + f.getName(), -1); //$NON-NLS-1$
					boolean initialized = repository.initialize(progress, f);
					if (initialized) {
						amenityRepositories.add(repository);
					}else {
						warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
					}
				}
			}
		}
		return warnings;
	}
	
		
	public List<String> indexingAddresses(final IProgress progress){
		File file = new File(Environment.getExternalStorageDirectory(), ADDRESS_PATH);
		List<String> warnings = new ArrayList<String>();
		closeAddresses();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)) {
					RegionAddressRepository repository = new RegionAddressRepository();
					progress.startTask(Messages.getMessage("indexing_address") + f.getName(), -1); //$NON-NLS-1$
					boolean initialized = repository.initialize(progress, f);
					if (initialized) {
						addressMap.put(repository.getName(), repository);
					} else {
						warnings.add(MessageFormat.format(Messages.getMessage("version_index_is_not_supported"), f.getName())); //$NON-NLS-1$
					}
				}
			}
		}
		return warnings;
	}
	
	// //////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<AmenityIndexRepository> searchRepositories(double latitude, double longitude) {
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
				if (!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filter.getFilterId(), amenities)) {
					index.searchAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, limit, filter, amenities);
				}
			}
		}

		return amenities;
	}
	
	public void searchAmenitiesAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, PoiFilter filter, List<Amenity> toFill){
		String filterId = filter == null ? null : filter.getFilterId();
		for(AmenityIndexRepository index : amenityRepositories){
			if(index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)){
				if(!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filterId, toFill, true)){
					asyncLoadingTiles.requestToLoadAmenities(
							new AmenityLoadRequest(index, topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, filter));
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

	public synchronized void close(){
		closeAmenities();
		closeAddresses();
	}
	
	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		clearTiles();
		for(AmenityIndexRepository r : amenityRepositories){
			r.clearCache();
		}
		for(RegionAddressRepository r : addressMap.values()){
			r.clearCities();
		}
		
		System.gc();
	}	
	
	
	protected void clearTiles(){
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size()/2; i ++) {
			Bitmap bmp = cacheOfImages.remove(list.get(i));
			if(bmp != null){
				bmp.recycle();
			}
		}
	}
	
	
	
	

	private static class TileLoadDownloadRequest extends DownloadRequest {

		public final String fileToLoad;
		public final File dirWithTiles; 
		
		public TileLoadDownloadRequest(File dirWithTiles, 
				String fileToLoad, String url, File fileToSave, int tileX, int tileY, int zoom) {
			super(url, fileToSave, tileX, tileY, zoom);
			this.dirWithTiles = dirWithTiles;
			this.fileToLoad = fileToLoad;
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
					while(!requests.isEmpty()){
						Object req = requests.pop();
						if (req instanceof TileLoadDownloadRequest) {
							TileLoadDownloadRequest r = (TileLoadDownloadRequest) req;
							if (cacheOfImages.get(r.fileToLoad) == null) {
								update |= getRequestedImageTile(r) != null;
							}
						} else if(req instanceof AmenityLoadRequest){
							if(!amenityLoaded){
								AmenityLoadRequest r = (AmenityLoadRequest) req;
								r.repository.evaluateCachedAmenities(r.topLatitude, r.leftLongitude, 
										r.bottomLatitude, r.rightLongitude, r.zoom, POIMapLayer.LIMIT_POI, r.filter, null);
								amenityLoaded = true;
							}
						}
					}
					if(update || amenityLoaded){
						// use downloader callback
						for(IMapDownloaderCallback c : downloader.getDownloaderCallbacks()){
							c.tileDownloaded(null);
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
	};
}
