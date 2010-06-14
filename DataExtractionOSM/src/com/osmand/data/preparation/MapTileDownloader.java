package com.osmand.data.preparation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.osmand.Algoritms;
import com.osmand.LogUtil;
import com.osmand.Version;

public class MapTileDownloader {
	// Download manager tile settings
	public static int TILE_DOWNLOAD_THREADS = 4;
	public static int TILE_DOWNLOAD_SECONDS_TO_WORK = 25;
	public static final int TILE_DOWNLOAD_MAX_ERRORS = -1;
	
	private static MapTileDownloader downloader = null;
	private static Log log = LogUtil.getLog(MapTileDownloader.class);
	
	
	private ThreadPoolExecutor threadPoolExecutor;
	private List<IMapDownloaderCallback> callbacks = new ArrayList<IMapDownloaderCallback>();
	
	private Set<File> currentlyDownloaded;
	
	private int currentErrors = 0;
	
	
	
	
	public static MapTileDownloader getInstance(){
		if(downloader == null){
			downloader = new MapTileDownloader(TILE_DOWNLOAD_THREADS);
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
	}
	
	
	public MapTileDownloader(int numberOfThreads){
		threadPoolExecutor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, TILE_DOWNLOAD_SECONDS_TO_WORK, 
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		// 1.6 method but very useful to kill non-running threads
//		threadPoolExecutor.allowCoreThreadTimeOut(true);
		currentlyDownloaded = Collections.synchronizedSet(new HashSet<File>());
		
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
		//FIXME it could cause NPE in android implementation think abou different style
		// That's very strange because exception in impl of queue (possibly wrong impl)
//		threadPoolExecutor.getQueue().clear();
		while(!threadPoolExecutor.getQueue().isEmpty()){
			try {
				threadPoolExecutor.getQueue().take();
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void requestToDownload(DownloadRequest request){
		if(TILE_DOWNLOAD_MAX_ERRORS > 0 && 
				currentErrors > TILE_DOWNLOAD_MAX_ERRORS){
			return;
		}
		if(request.url == null){
			return;
		}
		
		if (!isFileCurrentlyDownloaded(request.fileToSave)) {
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
				if(currentlyDownloaded.contains(request.fileToSave)){
					return;
				}
				
				currentlyDownloaded.add(request.fileToSave);
				if(log.isDebugEnabled()){
					log.debug("Start downloading tile : " + request.url);
				}
				long time = System.currentTimeMillis();
				try {
					request.fileToSave.getParentFile().mkdirs();
					URL url = new URL(request.url);
					URLConnection connection = url.openConnection();
					connection.setRequestProperty("User-Agent", Version.APP_NAME_VERSION);
					BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
					FileOutputStream stream = null;
					try {
						stream = new FileOutputStream(request.fileToSave);
						Algoritms.streamCopy(inputStream, stream);
						stream.flush();
					} finally {
						Algoritms.closeStream(inputStream);
						Algoritms.closeStream(stream);
					}
					if (log.isDebugEnabled()) {
						log.debug("Downloading tile : " + request.url + " successfull " + (System.currentTimeMillis() - time) + " ms");
					}
				} catch (UnknownHostException e) {
					currentErrors++;
					request.setError(true);
					log.error("UnknownHostException, cannot download tile " + request.url + " " + e.getMessage());
				} catch (IOException e) {
					currentErrors++;
					request.setError(true);
					log.warn("Cannot download tile : " + request.url, e);
				} finally {
					currentlyDownloaded.remove(request.fileToSave);
				}
				for(IMapDownloaderCallback c : callbacks){
					c.tileDownloaded(request);
				}
			}
				
		} 
		
		@Override
		public int compareTo(DownloadMapWorker o) {
			return 0; //(int) (time - o.time);
		}
		
	}
}
