package com.osmand;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MapTileDownloader {
	
	private static MapTileDownloader downloader = null;
	private static Log log = LogFactory.getLog(MapTileDownloader.class);
	
	private ThreadPoolExecutor threadPoolExecutor;
	private IMapDownloaderCallback callback;
	private Map<String, DownloadRequest> requestsToLoad;
	
	
	public static MapTileDownloader getInstance(){
		if(downloader == null){
			downloader = new MapTileDownloader(DefaultLauncherConstants.TILE_DOWNLOAD_THREADS);
		}
		return downloader;
	}
	
	/**
	 * Callback for map downloader 
	 */
	public interface IMapDownloaderCallback {

		public void tileDownloaded(String dowloadedUrl, DownloadRequest fileSaved);
	}
	
	/**
	 * Download request could subclassed to create own detailed request 
	 */
	public static class DownloadRequest {
		public final File fileToSave;
		public final int zoom;
		public final int xTile;
		public final int yTile;
		
		public DownloadRequest(File fileToSave, int xTile, int yTile, int zoom) {
			this.fileToSave = fileToSave;
			this.xTile = xTile;
			this.yTile = yTile;
			this.zoom = zoom;
		}
		
		public DownloadRequest(File fileToSave) {
			this.fileToSave = fileToSave;
			xTile = -1;
			yTile = -1;
			zoom = -1;
		}
		
		
		
	}
	
	
	public MapTileDownloader(int numberOfThreads){
		threadPoolExecutor = new ThreadPoolExecutor(1, numberOfThreads, DefaultLauncherConstants.TILE_DOWNLOAD_SECONTS_TO_WORK, 
				TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
		requestsToLoad = Collections.synchronizedMap(new LinkedHashMap<String, DownloadRequest>());
		
	}
	
	public void setDownloaderCallback(IMapDownloaderCallback callback){
		this.callback = callback;
	}
	
	public IMapDownloaderCallback getDownloaderCallback() {
		return callback;
	}
	
	
	public void refuseAllPreviousRequests(){
		requestsToLoad.clear();
	}
	
	public void requestToDownload(String url, DownloadRequest request){
		requestsToLoad.put(url, request);
		threadPoolExecutor.execute(new DownloadMapWorker(url, request));
	}
	
	
	private class DownloadMapWorker implements Runnable, Comparable<DownloadMapWorker> {
		private long time = System.currentTimeMillis();
		private final String downloadUrl;
		private DownloadRequest request;
		
		private DownloadMapWorker(String downloadUrl, DownloadRequest request){
			this.downloadUrl = downloadUrl;
			this.request = request;
		}
		
		@Override
		public void run() {
			synchronized (requestsToLoad) {
				if(!requestsToLoad.containsKey(downloadUrl)){
					return;
				}
				request = requestsToLoad.remove(downloadUrl);
			}
			
			try {
				URL url = new URL(downloadUrl);
//				if(log.isDebugEnabled()){
					log.debug("Downloading tile : " + downloadUrl);
//				}
				URLConnection connection = url.openConnection();
				connection.setRequestProperty("User-Agent", DefaultLauncherConstants.APP_NAME+"/"+DefaultLauncherConstants.APP_VERSION);
				BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
				try {
				if(request != null && request.fileToSave != null){
					request.fileToSave.getParentFile().mkdirs();
					FileOutputStream stream = new FileOutputStream(request.fileToSave);
					try {
						Algoritms.streamCopy(inputStream, stream);
					} finally {
						Algoritms.closeStream(stream);
					}
				}
				} finally {
					Algoritms.closeStream(inputStream);
				}
				if(log.isDebugEnabled()){
					log.debug("Downloading tile : " + downloadUrl + " successfull");
				}
				if(callback != null){
					callback.tileDownloaded(downloadUrl, request);
				}
			} catch (UnknownHostException e) {
				log.error("UnknownHostException, cannot download tile " + downloadUrl, e);
			} catch (IOException e) {
				log.warn("Cannot download tile : " + downloadUrl, e);
			}
		} 
		
		@Override
		public int compareTo(DownloadMapWorker o) {
			return (int) (time - o.time);
		}
		
	}
}
