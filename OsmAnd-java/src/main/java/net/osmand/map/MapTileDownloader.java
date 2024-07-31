package net.osmand.map;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.LIFOBlockingDeque;

import org.apache.commons.logging.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MapTileDownloader {

	private static final Log log = PlatformUtil.getLog(MapTileDownloader.class);

	// Download manager tile settings
	public static int TILE_DOWNLOAD_THREADS = 8;
	public static int TILE_DOWNLOAD_SECONDS_TO_WORK = 25;
	public static final long TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS = 15000;
	public static final int TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT = 50;
	private static final int CONNECT_TIMEOUT = 30000;
	private static final int READ_TIMEOUT = CONNECT_TIMEOUT * 2;

	private static MapTileDownloader downloader = null;

	public static String USER_AGENT = "OsmAnd~";

	private final ThreadPoolExecutor threadPoolExecutor;
	private List<WeakReference<IMapDownloaderCallback>> callbacks = new LinkedList<>();

	private final Map<File, DownloadRequest> pendingToDownload = new ConcurrentHashMap<>();
	private final Map<File, DownloadRequest> currentlyDownloaded = new ConcurrentHashMap<>();

	private int currentErrors = 0;
	private long timeForErrorCounter = 0;
	private boolean noHttps;

	public static MapTileDownloader getInstance(String userAgent) {
		if (downloader == null) {
			downloader = new MapTileDownloader(TILE_DOWNLOAD_THREADS);
			if (userAgent != null) {
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
		 *
		 * @param request
		 */
		void tileDownloaded(DownloadRequest request);
	}

	/**
	 * Download request could subclassed to create own detailed request
	 */
	public static class DownloadRequest {
		public final File fileToSave;
		public final String tileId;
		public final int zoom;
		public final int xTile;
		public final int yTile;
		public String url;
		public String referer = null;
		public String userAgent = null;
		public boolean error;

		public DownloadRequest(String url, File fileToSave, String tileId, int xTile, int yTile, int zoom) {
			this.url = url;
			this.fileToSave = fileToSave;
			this.tileId = tileId;
			this.xTile = xTile;
			this.yTile = yTile;
			this.zoom = zoom;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public void saveTile(InputStream inputStream) throws IOException {
			fileToSave.getParentFile().mkdirs();
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


	public MapTileDownloader(int numberOfThreads) {
		threadPoolExecutor = new ThreadPoolExecutor(numberOfThreads, numberOfThreads,
				TILE_DOWNLOAD_SECONDS_TO_WORK, TimeUnit.SECONDS, new LIFOBlockingDeque<Runnable>());
	}
	
	public void setNoHttps(boolean noHttps) {
		this.noHttps = noHttps;
	}

	public void addDownloaderCallback(IMapDownloaderCallback callback) {
		LinkedList<WeakReference<IMapDownloaderCallback>> ncall = new LinkedList<>(callbacks);
		ncall.add(new WeakReference<>(callback));
		callbacks = ncall;
	}

	public void removeDownloaderCallback(IMapDownloaderCallback callback) {
		LinkedList<WeakReference<IMapDownloaderCallback>> ncall = new LinkedList<>(callbacks);
		Iterator<WeakReference<IMapDownloaderCallback>> it = ncall.iterator();
		while (it.hasNext()) {
			IMapDownloaderCallback c = it.next().get();
			if (c == callback) {
				it.remove();
			}
		}
		callbacks = ncall;
	}

	public void clearCallbacks() {
		callbacks = new LinkedList<>();
	}

	public List<IMapDownloaderCallback> getDownloaderCallbacks() {
		ArrayList<IMapDownloaderCallback> lst = new ArrayList<>();
		for (WeakReference<IMapDownloaderCallback> c : callbacks) {
			IMapDownloaderCallback ct = c.get();
			if (ct != null) {
				lst.add(ct);
			}
		}
		return lst;
	}

	public boolean isFilePendingToDownload(File f) {
		return f != null && pendingToDownload.containsKey(f);
	}

	public boolean isFileCurrentlyDownloaded(File f) {
		return f != null && currentlyDownloaded.containsKey(f);
	}

	public boolean isSomethingBeingDownloaded() {
		return !currentlyDownloaded.isEmpty();
	}

	public int getRemainingWorkers() {
		return (int) (threadPoolExecutor.getTaskCount());
	}

	public void refuseAllPreviousRequests() {
		// That's very strange because exception in impl of queue (possibly wrong impl)
//		threadPoolExecutor.getQueue().clear();
		while (!threadPoolExecutor.getQueue().isEmpty()) {
			threadPoolExecutor.getQueue().poll();
		}
		pendingToDownload.clear();
	}

	public void requestToDownload(DownloadRequest request) {
		long now = System.currentTimeMillis();
		if ((int) (now - timeForErrorCounter) > TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS) {
			timeForErrorCounter = now;
			currentErrors = 0;
		} else if (shouldSkipRequests()) {
			return;
		}
		if (request.url == null) {
			return;
		}
		if (noHttps) {
			request.url = request.url.replace("https://", "http://");
		}
		if (!isFileCurrentlyDownloaded(request.fileToSave)
				&& !isFilePendingToDownload(request.fileToSave)) {
			pendingToDownload.put(request.fileToSave, request);
			threadPoolExecutor.execute(new DownloadMapWorker(request));
		}
	}

	public boolean shouldSkipRequests() {
		return currentErrors > TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT;
	}

	private class DownloadMapWorker implements Runnable, Comparable<DownloadMapWorker> {

		private final DownloadRequest request;

		private DownloadMapWorker(DownloadRequest request) {
			this.request = request;
		}

		@Override
		public void run() {
			if (request != null && request.fileToSave != null && request.url != null) {
				pendingToDownload.remove(request.fileToSave);
				if (currentlyDownloaded.containsKey(request.fileToSave)) {
					return;
				}

				currentlyDownloaded.put(request.fileToSave, request);
				if (log.isDebugEnabled()) {
					log.debug("Start downloading tile : " + request.url); 
				}
				long time = System.currentTimeMillis();
				request.setError(false);
				HttpURLConnection connection = null;
				try {
					connection = NetworkUtils.getHttpURLConnection(request.url);
					connection.setRequestProperty("User-Agent", Algorithms.isEmpty(request.userAgent) ? USER_AGENT : request.userAgent); 
					if (request.referer != null)
						connection.setRequestProperty("Referer", request.referer); 
					connection.setConnectTimeout(CONNECT_TIMEOUT);
					connection.setReadTimeout(READ_TIMEOUT);
					BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
					request.saveTile(inputStream);
					if (log.isDebugEnabled()) {
						log.debug("Downloading tile : " + request.url + " successfull " + (System.currentTimeMillis() - time) + " ms");  //$NON-NLS-2$ //$NON-NLS-3$
					}
				} catch (UnknownHostException e) {
					currentErrors++;
					timeForErrorCounter = System.currentTimeMillis();
					request.setError(true);
					log.error("UnknownHostException, cannot download tile " + request.url + " " + e.getMessage());   //$NON-NLS-2$
				} catch (Exception e) {
					currentErrors++;
					timeForErrorCounter = System.currentTimeMillis();
					request.setError(true);
					log.warn("Cannot download tile : " + request.url, e); 
				} finally {
					currentlyDownloaded.remove(request.fileToSave);
					if (connection != null) {
						connection.disconnect();
					}
				}
				if (!request.error) {
					fireLoadCallback(request);
				}
			}
		}

		@Override
		public int compareTo(DownloadMapWorker o) {
			return 0;
		}
	}

	public void fireLoadCallback(DownloadRequest request) {
		for (WeakReference<IMapDownloaderCallback> callback : callbacks) {
			IMapDownloaderCallback c = callback != null ? callback.get() : null;
			if (c != null) {
				c.tileDownloaded(request);
			}
		}
	}
}
