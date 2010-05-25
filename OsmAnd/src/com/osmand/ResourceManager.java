package com.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.osmand.data.Amenity;
import com.osmand.data.DataTileManager;
import com.osmand.data.Region;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.ITileSource;
import com.osmand.osm.LatLon;
import com.osmand.osm.io.OsmIndexStorage;
import com.osmand.osm.io.OsmLuceneRepository;

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
	private static final String LUCENE_PATH = "osmand/lucene/";
	
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
	
	protected OsmLuceneRepository amenityIndexSearcher = new OsmLuceneRepository();
	
	
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
	
	private interface IndexVisitor {
		/**
		 * returns if entry was visited succesfully
		 */
		public boolean visitEntry(String entryName, InputStream stream) throws IOException, SAXException;
	}
	
	
	public void indexingFiles(String pathToIndex, String ext, IProgress progress, String objectToIndex, IndexVisitor visitor) {
		File file = new File(Environment.getExternalStorageDirectory(), pathToIndex);
		if (file.exists() && file.canRead()) {
			for (File f : file.listFiles()) {
				InputStream stream = null;
				ZipFile zipFile = null;
				Enumeration<? extends ZipEntry> entries = null;
				try {
					if (f.getName().endsWith(".zip")) {
						zipFile = new ZipFile(f);
						entries = zipFile.entries();
					} else {
						stream = new FileInputStream(f);
					}
				} catch (IOException e) {
					log.error("Can't read file " + f.getAbsolutePath(), e);
					continue;
				}
				String entryName = f.getName();
				do {
					try {
						if (entries != null && entries.hasMoreElements()) {
							ZipEntry entry = entries.nextElement();
							entryName = entry.getName();
							stream = zipFile.getInputStream(entry);
						}

						if (entryName != null && entryName.endsWith(ext)) {
							long start = System.currentTimeMillis();
							if (log.isDebugEnabled()) {
								log.debug("Starting index " + objectToIndex + " " + f.getAbsolutePath());
							}

							if (progress != null) {
								progress.startTask("Indexing " + objectToIndex + " " + f.getName(), stream.available());
							}
							visitor.visitEntry(f.getName(), stream);
							if (log.isDebugEnabled()) {
								log.debug("Finished index " + objectToIndex + " " + f.getAbsolutePath() + " "
										+ (System.currentTimeMillis() - start) + "ms");
							}
						}
					} catch (IOException e) {
						log.error("Can't read file " + f.getAbsolutePath(), e);
					} catch (SAXException e) {
						log.error("Can't read file " + f.getAbsolutePath(), e);
					} finally {
						Algoritms.closeStream(stream);
					}
				} while (zipFile != null && entries.hasMoreElements());
			}
		}
	}
	
	// POI INDEX //
	public void indexingPoi(final IProgress progress) {
		if (poiIndex == null) {
			poiIndex = new DataTileManager<Amenity>();
			indexingFiles(POI_PATH, ".osmand", progress, "POI", new IndexVisitor() {
				@Override
				public boolean visitEntry(String entryName, InputStream stream) throws IOException, SAXException {
					OsmIndexStorage storage = new OsmIndexStorage(new Region());
					storage.parseOSM(stream, progress);
					Region region = ((OsmIndexStorage) storage).getRegion();
					for (Amenity a : region.getAmenityManager().getAllObjects()) {
						LatLon location = a.getLocation();
						poiIndex.registerObject(location.getLatitude(), location.getLongitude(), a);
					}
					return true;
				}

			});
		}
	}
	
	public void indexingLucene(final IProgress progress){
		// read index
		File file = new File(Environment.getExternalStorageDirectory(), LUCENE_PATH);
		if (file.exists() && file.canRead()) {
			amenityIndexSearcher.indexing(progress, file);
		}
	}
	
	public void indexingAddresses(final IProgress progress){
		indexingFiles(ADDRESS_PATH, ".osmand", progress, "address", new IndexVisitor() {
			@Override
			public boolean visitEntry(String entryName, InputStream stream) throws IOException, SAXException {
				String name = entryName.substring(0, entryName.indexOf('.'));
				Region region = new Region();
				region.setName(name);
				addressMap.put(name, region);
				OsmIndexStorage storage = new OsmIndexStorage(region);
				storage.parseOSM(stream, progress);
				return true;
			}
		});
	}
	
	public DataTileManager<Amenity> getPoiIndex() {
		if(poiIndex == null){
			indexingPoi(null);
		}
		return poiIndex;
	}
	
	
	public OsmLuceneRepository getAmenityIndexSearcher(){
		return amenityIndexSearcher;
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
