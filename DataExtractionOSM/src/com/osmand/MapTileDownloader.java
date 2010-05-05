package com.osmand;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

public class MapTileDownloader {
	
	private static MapTileDownloader downloader = null;
	private static Log log = LogUtil.getLog(MapTileDownloader.class);
	
	
	private ThreadPoolExecutor threadPoolExecutor;
	private IMapDownloaderCallback callback;
	
	private Set<File> currentlyDownloaded;
	
	private int currentErrors = 0;
	
	
	
	
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
		threadPoolExecutor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, DefaultLauncherConstants.TILE_DOWNLOAD_SECONTS_TO_WORK, 
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		// 1.6 method but very useful to kill non-running threads
//		threadPoolExecutor.allowCoreThreadTimeOut(true);
		currentlyDownloaded = Collections.synchronizedSet(new HashSet<File>());
		
	}
	
	public void setDownloaderCallback(IMapDownloaderCallback callback){
		this.callback = callback;
	}
	
	public IMapDownloaderCallback getDownloaderCallback() {
		return callback;
	}
	
	public boolean isFileCurrentlyDownloaded(File f){
		return currentlyDownloaded.contains(f);
	}
	
	
	public void refuseAllPreviousRequests(){
		while(!threadPoolExecutor.getQueue().isEmpty()){
			threadPoolExecutor.getQueue().remove();
		}
	}
	
	public void requestToDownload(String url, DownloadRequest request){
		if(DefaultLauncherConstants.TILE_DOWNLOAD_MAX_ERRORS > 0 && 
				currentErrors > DefaultLauncherConstants.TILE_DOWNLOAD_MAX_ERRORS){
			return;
		}
		
		if (!isFileCurrentlyDownloaded(request.fileToSave)) {
			threadPoolExecutor.execute(new DownloadMapWorker(url, request));
		}
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
			try {
				if(log.isDebugEnabled()){
					log.debug("Start downloading tile : " + downloadUrl);
				}
				URL url = new URL(downloadUrl);
				URLConnection connection = url.openConnection();
				connection.setRequestProperty("User-Agent", DefaultLauncherConstants.APP_NAME+"/"+DefaultLauncherConstants.APP_VERSION);
				BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
				try {
					if (request != null && request.fileToSave != null) {
						request.fileToSave.getParentFile().mkdirs();

						FileOutputStream stream = new FileOutputStream(request.fileToSave);
						currentlyDownloaded.add(request.fileToSave);
						try {
							Algoritms.streamCopy(inputStream, stream);
							stream.flush();
						} finally {
							currentlyDownloaded.remove(request.fileToSave);
							Algoritms.closeStream(stream);
						}
					}
				} finally {
					Algoritms.closeStream(inputStream);
				}
				if(log.isDebugEnabled()){
					log.debug("Downloading tile : " + downloadUrl + " successfull " + (System.currentTimeMillis() - time) + " ms");
				}
				if(callback != null){
					callback.tileDownloaded(downloadUrl, request);
				}
			} catch (UnknownHostException e) {
				currentErrors++;
				log.error("UnknownHostException, cannot download tile " + downloadUrl, e);
			} catch (IOException e) {
				currentErrors++;
				log.warn("Cannot download tile : " + downloadUrl, e);
			}
		} 
		
		@Override
		public int compareTo(DownloadMapWorker o) {
			return 0; //(int) (time - o.time);
		}
		
	}
}
