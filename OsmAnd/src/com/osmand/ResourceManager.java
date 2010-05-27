package com.osmand;

import java.io.File;
import java.util.ArrayList;
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
import com.osmand.data.Region;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.MapUtils;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 *
 */
public class ResourceManager {

	private static final String POI_PATH = "osmand/" + IndexConstants.POI_INDEX_DIR;
	private static final String ADDRESS_PATH = "osmand/" + IndexConstants.ADDRESS_INDEX_DIR;
	private static final String TILES_PATH = "osmand/tiles/";
	
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
	private Map<String, Region> addressMap = new TreeMap<String, Region>();
	
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
		return getTileImageForMap(map, x, y, zoom, loadFromInternetIfNeeded, false);
	}
	
	public Bitmap getTileImageForMapSync(ITileSource map, int x, int y, int zoom, boolean loadFromInternetIfNeeded) {
		return getTileImageForMap(map, x, y, zoom, loadFromInternetIfNeeded, true);
	}
	
	protected Bitmap getTileImageForMap(ITileSource map, int x, int y, int zoom, 
			boolean loadFromInternetIfNeeded, boolean sync) {
		if (map == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder(40);
		builder.append(map.getName()).append('/').append(zoom).	append('/').append(x).
				append('/').append(y).append(map.getTileFormat()).append(".tile");
		String file = builder.toString();
		if (cacheOfImages.get(file) == null) {
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
			onLowMemory();
		}
		
		if (!downloader.isFileCurrentlyDownloaded(en) && req.dirWithTiles.canRead()) {
			if (en.exists()) {
				long time = System.currentTimeMillis();
				cacheOfImages.put(req.fileToLoad, BitmapFactory.decodeFile(en.getAbsolutePath()));
				if (log.isDebugEnabled()) {
					log.debug("Loaded file : " + req.fileToLoad + " " + -(time - System.currentTimeMillis()) + " ms");
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
	// POI INDEX //
	public void indexingPoi(final IProgress progress) {
		File file = new File(Environment.getExternalStorageDirectory(), POI_PATH);
		amenityRepositories.clear();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
					AmenityIndexRepository repository = new AmenityIndexRepository();
					progress.startTask("Indexing poi " + f.getName(), -1);
					repository.initialize(progress, f);
					amenityRepositories.add(repository);
				}
			}
		}
	}
	
		
	public void indexingAddresses(final IProgress progress){
		File file = new File(Environment.getExternalStorageDirectory(), ADDRESS_PATH);
		addressMap.clear();
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)) {
					// TODO fill in address index
					
				}
			}
		}
	}
	
	////////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<Amenity> searchAmenities(double latitude, double longitude, int zoom, int limit) {
		double tileNumberX = Math.floor(MapUtils.getTileNumberX(zoom, longitude));
		double tileNumberY = Math.floor(MapUtils.getTileNumberY(zoom, latitude));
		double topLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY);
		double bottomLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY + 1);
		double leftLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX);
		double rightLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX + 1);
		List<Amenity> amenities = new ArrayList<Amenity>();
		for(AmenityIndexRepository index : amenityRepositories){
			if(index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)){
				if(!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, amenities)){
					index.searchAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, limit, amenities);
				}
			}
		}
		
		return amenities;
	}
	
	public void searchAmenitiesAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, List<Amenity> toFill){
		for(AmenityIndexRepository index : amenityRepositories){
			if(index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)){
				if(!index.checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, toFill)){
					// TODO add request to another thread
					index.evaluateCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, toFill);
				}
			}
		}
	}
	
	

	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size());
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size()/2; i ++) {
			Bitmap bmp = cacheOfImages.remove(list.get(i));
			if(bmp != null){
				bmp.recycle();
			}
		}
		// TODO clear amenity indexes & addresses indexes
		System.gc();
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
	
	
	public class AsyncLoadingThread extends Thread {
		Stack<TileLoadDownloadRequest> requests = new Stack<TileLoadDownloadRequest>();
		
		public AsyncLoadingThread(){
			super("Async loading tiles");
		}
		
		@Override
		public void run() {
			while(true){
				try {
					boolean update = false;
					while(!requests.isEmpty()){
						TileLoadDownloadRequest r = requests.pop();
						if(cacheOfImages.get(r.fileToLoad) == null) {
							update |= getRequestedImageTile(r) != null;
						}
					}
					if(update){
						// use downloader callback
						for(IMapDownloaderCallback c : downloader.getDownloaderCallbacks()){
							c.tileDownloaded(null);
						}
					}
					sleep(750);
				} catch (InterruptedException e) {
					log.error(e);
				} catch (RuntimeException e){
					log.error(e);
				}
			}
		}
		
		public void requestToLoadImage(TileLoadDownloadRequest req){
			requests.push(req);
		}
	};
}
