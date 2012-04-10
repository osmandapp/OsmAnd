package net.osmand.data;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Result;
import java.io.InputStream;


import net.osmand.Algoritms;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;


public class MapTileDownloader {
	// Download manager tile settings
	public static int TILE_DOWNLOAD_THREADS = 4;
	public static int TILE_DOWNLOAD_SECONDS_TO_WORK = 25;
	public static final long TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS = 20000;
	public static final int TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT = 25;
	
	
	private static MapTileDownloader downloader = null;
	private static Log log = LogUtil.getLog(MapTileDownloader.class);
	
	public static String USER_AGENT = "Osmand~";
	
	
	private ThreadPoolExecutor threadPoolExecutor;
	private List<IMapDownloaderCallback> callbacks = new ArrayList<IMapDownloaderCallback>();
	
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
		public boolean multiPageXml;
		public boolean error;
		
		public DownloadRequest(String url, File fileToSave, int xTile, int yTile, int zoom, boolean multiPageXml) {
			this.url = url;
			this.fileToSave = fileToSave;
			this.xTile = xTile;
			this.yTile = yTile;
			this.zoom = zoom;
			this.multiPageXml = multiPageXml;
		}
		
		public DownloadRequest(String url, File fileToSave) {
			this.url = url;
			this.fileToSave = fileToSave;
			xTile = -1;
			yTile = -1;
			zoom = -1;
			multiPageXml = false;
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
		//FIXME it could cause NPE in android implementation think about different style
		// That's very strange because exception in impl of queue (possibly wrong impl)
//		threadPoolExecutor.getQueue().clear();
		while(!threadPoolExecutor.getQueue().isEmpty()){
			threadPoolExecutor.getQueue().poll();
		}
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
					log.debug("Start downloading tile : " + request.url); //$NON-NLS-1$
				}
				if( request.multiPageXml ) {
					request.setError(multiPageXmlDownload(request.url, request.fileToSave));
					if( request.error )
						currentErrors++;
					currentlyDownloaded.remove(request.fileToSave);
				} else {
					long time = System.currentTimeMillis();
					try {
						BufferedInputStream inputStream = new BufferedInputStream(simpleDownload(request.url), 8 * 1024);
						FileOutputStream stream = null;
						request.fileToSave.getParentFile().mkdirs();
						try {
							stream = new FileOutputStream(request.fileToSave);
							Algoritms.streamCopy(inputStream, stream);
							stream.flush();
						} finally {
							Algoritms.closeStream(inputStream);
							Algoritms.closeStream(stream);
						}
						if (log.isDebugEnabled()) {
							log.debug("Downloading tile : " + request.url + " successfull " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					} catch (UnknownHostException e) {
						currentErrors++;
						request.setError(true);
						log.error("UnknownHostException, cannot download tile " + request.url + " " + e.getMessage()); //$NON-NLS-1$  //$NON-NLS-2$
					} catch (IOException e) {
						currentErrors++;
						request.setError(true);
						log.warn("Cannot download tile : " + request.url, e); //$NON-NLS-1$
					} finally {
						currentlyDownloaded.remove(request.fileToSave);
					}
				}
				if (!request.error) {
					for (IMapDownloaderCallback c : new ArrayList<IMapDownloaderCallback>(callbacks)) {
						c.tileDownloaded(request);
					}
				}
			}
				
		}
		
		InputStream simpleDownload(String request) throws UnknownHostException, IOException {
			URL url = new URL(request);
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", USER_AGENT); //$NON-NLS-1$
			connection.setConnectTimeout(35000);
			return connection.getInputStream();
		}
		
		boolean multiPageXmlDownload(String url, File fileToSave) {
			// A complicated download request, as the name implies
			// It will fetch several XML pages and reformat them into the single contiguous XML document
			boolean success = false;
			try {
				long time = System.currentTimeMillis();
				DocumentBuilder xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = xmlParser.parse(simpleDownload(url));
				NodeList rootNodeList = doc.getElementsByTagName("folder"); //$NON-NLS-1$
				if( rootNodeList.getLength() == 0 )
					throw new IOException("Failed to parse XML output from the server: there is no root \"folder\" node"); //$NON-NLS-1$
				Node rootNode = rootNodeList.item(0);
				if( rootNode.getAttributes().getNamedItem("found") == null || //$NON-NLS-1$
					rootNode.getAttributes().getNamedItem("count") == null ) //$NON-NLS-1$
					throw new IOException("Failed to parse XML output from the server: there are no \"found\" or \"count\" attributes"); //$NON-NLS-1$
				int total = Integer.parseInt(rootNode.getAttributes().getNamedItem("found").getNodeValue()); //$NON-NLS-1$
				int perPage = Integer.parseInt(rootNode.getAttributes().getNamedItem("count").getNodeValue()); //$NON-NLS-1$
				int pagesToGo = (int)Math.ceil((double)total/(double)perPage);
				rootNodeList = null;
				rootNode = null;

				fileToSave.getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream(fileToSave);
				out.write(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<folder total=\"" + total + "\">").getBytes("UTF-8"));

				StreamResult xmlOut = new StreamResult(out);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				for( int i = 1; i <= pagesToGo; i++ ) {
					if( i > 1 ) {
						if(log.isDebugEnabled()){
							log.debug("Continuing to download tile : " + url + "&page=" + i); //$NON-NLS-1$
						}
						doc = xmlParser.parse(simpleDownload(url + "&page=" + i)); //$NON-NLS-1$
					}
					NodeList nodes = doc.getElementsByTagName("place"); //$NON-NLS-1$
					// NodeList is not a subclass of a Java List class, and is not iterable, that's so lame
					for( int ii = 0; ii < nodes.getLength(); ii++ ) {
						transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
						transformer.transform( new DOMSource(nodes.item(ii)), xmlOut );
					}
				}
				out.write(("</folder>").getBytes("UTF-8"));
				out.close();
				// This command will save the file to disk
				//TransformerFactory.newInstance().newTransformer().transform( new DOMSource(doc), new StreamResult(new FileOutputStream(fileToSave)) );
				if(log.isDebugEnabled()){
					log.debug("Downloading tile : " + url + " successfull " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				success = true;
			} catch (UnknownHostException e) {
				log.error("UnknownHostException, cannot download tile " + request.url + " " + e.getMessage()); //$NON-NLS-1$  //$NON-NLS-2$
				return false;
			} catch (IOException e) {
				log.warn("Cannot download tile : " + url, e); //$NON-NLS-1$
				return false;
			} catch (Exception e) {
				log.warn("Cannot download tile : " + url, e); //$NON-NLS-1$
				return false;
			} finally {
				Runtime.getRuntime().gc(); // All this stuff eats lot of RAM
				if( !success ) {
					fileToSave.delete();
				}
			}
			return true;
		}
		
		@Override
		public int compareTo(DownloadMapWorker o) {
			return 0; //(int) (time - o.time);
		}
		
	}
}
