package com.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.osmand.data.Amenity;
import com.osmand.data.DataTileManager;
import com.osmand.data.Region;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.map.ITileSource;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.Node;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.osm.io.OsmIndexStorage;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 *
 */
public class ResourceManager {

	private static final String POI_PATH = "osmand/poi/";
	private static final String ADDRESS_PATH = "osmand/address/";
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
	
	private DataTileManager<Amenity> poiIndex = null;
	
	private Map<String, Region> addressMap = new TreeMap<String, Region>();
	
	protected Map<String, Bitmap> cacheOfImages = new LinkedHashMap<String, Bitmap>();
	
	protected File dirWithTiles ;
	
	private MapTileDownloader downloader = MapTileDownloader.getInstance();
	
	public AsyncLoadingThread asyncLoadingTiles = new AsyncLoadingThread();

	

	
	public ResourceManager() {
		// TODO start/stop this thread when needed?
		asyncLoadingTiles.start();
		dirWithTiles = new File(Environment.getExternalStorageDirectory(), TILES_PATH);
		if(Environment.getExternalStorageDirectory().canRead()){
			dirWithTiles.mkdirs();
		}
		
	}
	
	/// Working with tiles ///
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
						downloader.getDownloaderCallback().tileDownloaded(null);
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
	
	
	// POI INDEX //
	public void indexingPoi(IProgress progress){
		if (poiIndex == null) {
			File file = new File(Environment.getExternalStorageDirectory(), POI_PATH);
			poiIndex = new DataTileManager<Amenity>();
			if (file.exists() && file.canRead()) {
				for (File f : file.listFiles()) {
					if (f.getName().endsWith(".bz2") || f.getName().endsWith(".osm") || f.getName().endsWith(".osmand")) {
						long start = System.currentTimeMillis();
						if (log.isDebugEnabled()) {
							log.debug("Starting index POI " + f.getAbsolutePath());
						}
						boolean zipped = f.getName().endsWith(".bz2");
						InputStream stream = null;
						try {
							OsmBaseStorage storage;
							boolean indexStorage = false;
							if(f.getName().contains(".osmand")){
								storage = new OsmIndexStorage(new Region());
								indexStorage = true;
							} else {
								storage = new OsmBaseStorage();
							}
							stream = new FileInputStream(f);
//							stream = new BufferedInputStream(stream);
							InputStream streamForProgress = stream;
							if (zipped) {
								if (stream.read() != 'B' || stream.read() != 'Z') {
									log.error("Can't read poi file " + f.getAbsolutePath()
											+ "The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
									continue;
								} else {
									stream = new CBZip2InputStream(stream);
								}
							}
							if(progress != null){
								progress.startTask("Indexing poi " + f.getName(), stream.available());
							}
							storage.parseOSM(stream, progress, streamForProgress);
							if(indexStorage){
								Region region = ((OsmIndexStorage) storage).getRegion();
								for(Amenity a : region.getAmenityManager().getAllObjects()){
									LatLon location = a.getLocation();
									poiIndex.registerObject(location.getLatitude(), location.getLongitude(), a);
								}
							} else {
								for (Entity e : storage.getRegisteredEntities().values()) {
									if (e instanceof Node && Amenity.isAmenity((Node) e)) {
										poiIndex.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), new Amenity((Node) e));
									}
								}
							}
							if (log.isDebugEnabled()) {
								log.debug("Finishing index POI " + f.getAbsolutePath() + " " +(System.currentTimeMillis() - start)+"ms");
							}
						} catch (IOException e) {
							log.error("Can't read poi file " + f.getAbsolutePath(), e);
						} catch (SAXException e) {
							log.error("Can't read poi file " + f.getAbsolutePath(), e);
						} finally {
							Algoritms.closeStream(stream);
						}
					}
				}
			}
		}
	}
	
	public void indexingAddresses(IProgress progress){
			File file = new File(Environment.getExternalStorageDirectory(), ADDRESS_PATH);
			if (file.exists() && file.canRead()) {
				for (File f : file.listFiles()) {
					if (f.getName().endsWith(".osmand.bz2") || f.getName().endsWith(".osmand")) {
						long start = System.currentTimeMillis();
						if (log.isDebugEnabled()) {
							log.debug("Starting index address " + f.getAbsolutePath());
						}
						boolean zipped = f.getName().endsWith(".bz2");
						InputStream stream = null;
						String name = f.getName().substring(0, f.getName().indexOf('.'));
						Region region = new Region();
						region.setName(name);
						addressMap.put(name, region);
						try {
							OsmIndexStorage storage = new OsmIndexStorage(region);
							stream = new FileInputStream(f);
//							stream = new BufferedInputStream(stream);
							InputStream streamForProgress = stream;
							if (zipped) {
								if (stream.read() != 'B' || stream.read() != 'Z') {
									log.error("Can't read index file " + f.getAbsolutePath()
											+ "The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
									continue;
								} else {
									stream = new CBZip2InputStream(stream);
								}
							}
							if(progress != null){
								progress.startTask("Indexing address " + f.getName(), stream.available());
							}
							storage.parseOSM(stream, progress, streamForProgress);
							if (log.isDebugEnabled()) {
								log.debug("Finishing index address " + f.getAbsolutePath() + " " +(System.currentTimeMillis() - start)+"ms");
							}
						} catch (IOException e) {
							log.error("Can't read index file " + f.getAbsolutePath(), e);
						} catch (SAXException e) {
							log.error("Can't read index file " + f.getAbsolutePath(), e);
						} finally {
							Algoritms.closeStream(stream);
						}
					}
				}
			}
	}
	
	public DataTileManager<Amenity> getPoiIndex() {
		if(poiIndex == null){
			indexingPoi(null);
		}
		return poiIndex;
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
		System.gc();
	}
	
}
