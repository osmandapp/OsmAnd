package net.osmand.map;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;


public class MapTileDownloader {
	// Download manager tile settings
	public static int TILE_DOWNLOAD_THREADS = 4;
	public static int TILE_DOWNLOAD_SECONDS_TO_WORK = 25;
	public static final long TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS = 15000;
	public static final int TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT = 50;
	private static final int CONNECTION_TIMEOUT = 30000;
	
	
	private static MapTileDownloader downloader = null;
	private static Log log = PlatformUtil.getLog(MapTileDownloader.class);
	
	public static String USER_AGENT = "OsmAnd~";
	
	
	private ThreadPoolExecutor threadPoolExecutor;
	private List<IMapDownloaderCallback> callbacks = new ArrayList<IMapDownloaderCallback>();
	
	private Set<File> pendingToDownload;
	private Set<File> currentlyDownloaded;
	
	private int currentErrors = 0;
	private long timeForErrorCounter = 0;
	
	
	public static MapTileDownloader getInstance(String userAgent){
		if(downloader == null){
			downloader = new MapTileDownloader(TILE_DOWNLOAD_THREADS);
			if(userAgent != null) {
				MapTileDownloader.USER_AGENT = userAgent;
			}
		}
		return downloader;
	}
	
	/**
	 * Callback for map downloader 
	 */
	public interface IMapDownloaderCallback {

		/**
		 * Sometimes null cold be passed as request
		 * That means that there were a lot of requests but
		 * once method is called 
		 * (in order to not create a collection of request & reduce calling times) 
		 * @param fileSaved
		 */
		public void tileDownloaded(DownloadRequest request);
	}
	
	/**
	 * Download request could subclassed to create own detailed request 
	 */
	public static class DownloadRequest {
		public final File fileToSave;
		public final int zoom;
		public final int xTile;
		public final int yTile;
		public final String url;
		public boolean error;
		
		public DownloadRequest(String url, File fileToSave, int xTile, int yTile, int zoom) {
			this.url = url;
			this.fileToSave = fileToSave;
			this.xTile = xTile;
			this.yTile = yTile;
			this.zoom = zoom;
		}
		
		public DownloadRequest(String url, File fileToSave) {
			this.url = url;
			this.fileToSave = fileToSave;
			xTile = -1;
			yTile = -1;
			zoom = -1;
		}
		
		public void setError(boolean error){
			this.error = error;
		}
		
		public void saveTile(InputStream inputStream) throws IOException {
			OutputStream stream = null;
			try {
				stream = new FileOutputStream(fileToSave);
				Algorithms.streamCopy(inputStream, stream);
				stream.flush();
			} finally {
				Algorithms.closeStream(inputStream);
				Algorithms.closeStream(stream);
			}
		}
	}
	
	
	public MapTileDownloader(int numberOfThreads){
		
		threadPoolExecutor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, TILE_DOWNLOAD_SECONDS_TO_WORK, 
				TimeUnit.SECONDS, createQueue());
		// 1.6 method but very useful to kill non-running threads
//		threadPoolExecutor.allowCoreThreadTimeOut(true);
		pendingToDownload = Collections.synchronizedSet(new HashSet<File>());
		currentlyDownloaded = Collections.synchronizedSet(new HashSet<File>());
		
	}

	protected BlockingQueue<Runnable> createQueue() {
		boolean loaded = false;
		try {
			Class<?> cl = Class.forName("java.util.concurrent.LinkedBlockingDeque");
			loaded = cl != null;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if(!loaded) {
			// for Android 2.2
			return new LinkedBlockingQueue<Runnable>();
		}
		return createDeque();
	}

	protected static BlockingQueue<Runnable> createDeque() {
		return new net.osmand.util.LIFOBlockingDeque<Runnable>();
	}
	
	public void addDownloaderCallback(IMapDownloaderCallback callback){
		callbacks.add(callback);
	}
	
	public void removeDownloaderCallback(IMapDownloaderCallback callback){
		callbacks.remove(callback);
	}
	
	public List<IMapDownloaderCallback> getDownloaderCallbacks() {
		return callbacks;
	}
	
	public boolean isFilePendingToDownload(File f){
		return pendingToDownload.contains(f);
	}
	
	public boolean isFileCurrentlyDownloaded(File f){
		return currentlyDownloaded.contains(f);
	}
	
	public boolean isSomethingBeingDownloaded(){
		return !currentlyDownloaded.isEmpty();
	}
	
	public int getRemainingWorkers(){
		return (int) (threadPoolExecutor.getTaskCount());
	}
	
	public void refuseAllPreviousRequests(){
		// That's very strange because exception in impl of queue (possibly wrong impl)
//		threadPoolExecutor.getQueue().clear();
		while(!threadPoolExecutor.getQueue().isEmpty()){
			threadPoolExecutor.getQueue().poll();
		}
		pendingToDownload.clear();
	}
	
	public void requestToDownload(DownloadRequest request){
		long now = System.currentTimeMillis();
		if((int)(now - timeForErrorCounter) > TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS ) {
			timeForErrorCounter = now;
			currentErrors = 0;
		} else if(currentErrors > TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT){
			return;
		}
		if(request.url == null){
			return;
		}
		
		if (!isFileCurrentlyDownloaded(request.fileToSave)
				&& !isFilePendingToDownload(request.fileToSave)) {
			pendingToDownload.add(request.fileToSave);
			threadPoolExecutor.execute(new DownloadMapWorker(request));
		}
	}
	
	
	private class DownloadMapWorker implements Runnable, Comparable<DownloadMapWorker> {
		
		private DownloadRequest request;
		
		private DownloadMapWorker(DownloadRequest request){
			this.request = request;
		}
		
		@Override
		public void run() {
			if (request != null && request.fileToSave != null && request.url != null) {
				pendingToDownload.remove(request.fileToSave);
				if(currentlyDownloaded.contains(request.fileToSave)){
					return;
				}
				
				currentlyDownloaded.add(request.fileToSave);
				if(log.isDebugEnabled()){
					log.debug("Start downloading tile : " + request.url); //$NON-NLS-1$
				}
				long time = System.currentTimeMillis();
				request.setError(false);
				try {
					request.fileToSave.getParentFile().mkdirs();
					URL url = new URL(request.url);
					URLConnection connection = url.openConnection();
					connection.setRequestProperty("User-Agent", USER_AGENT); //$NON-NLS-1$
					connection.setConnectTimeout(CONNECTION_TIMEOUT);
					connection.setReadTimeout(CONNECTION_TIMEOUT);
					BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
					request.saveTile(inputStream);
					if (log.isDebugEnabled()) {
						log.debug("Downloading tile : " + request.url + " successfull " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				} catch (UnknownHostException e) {
					currentErrors++;
					timeForErrorCounter = System.currentTimeMillis();
					request.setError(true);
					log.error("UnknownHostException, cannot download tile " + request.url + " " + e.getMessage()); //$NON-NLS-1$  //$NON-NLS-2$
				} catch (IOException e) {
					currentErrors++;
					timeForErrorCounter = System.currentTimeMillis();
					request.setError(true);
					log.warn("Cannot download tile : " + request.url, e); //$NON-NLS-1$
				} finally {
					currentlyDownloaded.remove(request.fileToSave);
				}
				if (!request.error) {
					for (IMapDownloaderCallback c : new ArrayList<IMapDownloaderCallback>(callbacks)) {
						c.tileDownloaded(request);
					}
				}
			}
				
		} 
		
		@Override
		public int compareTo(DownloadMapWorker o) {
			return 0; //(int) (time - o.time);
		}
		
	}
}
