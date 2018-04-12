package net.osmand.plus.resources;


import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * Thread to load map objects (POI, transport stops )async
 */
public class AsyncLoadingThread extends Thread {
	
	public static final int LIMIT_TRANSPORT = 200;
	
	private static final Log log = PlatformUtil.getLog(AsyncLoadingThread.class); 
	
	Stack<Object> requests = new Stack<Object>();
	private final ResourceManager resourceManger;

	public AsyncLoadingThread(ResourceManager resourceManger) {
		super("Loader map objects (synchronizer)"); //$NON-NLS-1$
		this.resourceManger = resourceManger;
	}

	@Override
	public void run() {
		while (true) {
			try {
				boolean tileLoaded = false;
				boolean mapLoaded = false;
				while (!requests.isEmpty()) {
					Object req = requests.pop();
					if (req instanceof TileLoadDownloadRequest) {
						TileLoadDownloadRequest r = (TileLoadDownloadRequest) req;
						tileLoaded |= resourceManger.hasRequestedTile(r);
					} else if (req instanceof MapLoadRequest) {
						if (!mapLoaded) {
							MapLoadRequest r = (MapLoadRequest) req;
							resourceManger.getRenderer().loadMap(r.tileBox, resourceManger.getMapTileDownloader());
							mapLoaded = !resourceManger.getRenderer().wasInterrupted();
							if (r.mapLoadedListener != null) {
								r.mapLoadedListener.onMapLoaded(!mapLoaded);
							}
						}
					}
				}
				if (tileLoaded  || mapLoaded) {
					// use downloader callback
					resourceManger.getMapTileDownloader().fireLoadCallback(null);
				}
				sleep(750);
			} catch (InterruptedException e) {
				log.error(e, e);
			} catch (RuntimeException e) {
				log.error(e, e);
			}
		}
	}

	public void requestToLoadTile(TileLoadDownloadRequest req) {
		requests.push(req);
	}

	public void requestToLoadMap(MapLoadRequest req) {
		requests.push(req);
	}

	
	public boolean isFilePendingToDownload(File fileToSave) {
		return resourceManger.getMapTileDownloader().isFilePendingToDownload(fileToSave);
	}
	
	public boolean isFileCurrentlyDownloaded(File fileToSave) {
		return resourceManger.getMapTileDownloader().isFileCurrentlyDownloaded(fileToSave);
	}

	public void requestToDownload(TileLoadDownloadRequest req) {
		resourceManger.getMapTileDownloader().requestToDownload(req);
	}

	public static class TileLoadDownloadRequest extends DownloadRequest {

		public final String tileId;
		public final File dirWithTiles;
		public final ITileSource tileSource;

		public TileLoadDownloadRequest(File dirWithTiles, String url, File fileToSave, String tileId, ITileSource source, int tileX,
				int tileY, int zoom) {
			super(url, fileToSave, tileX, tileY, zoom);
			this.dirWithTiles = dirWithTiles;
			this.tileSource = source;
			this.tileId = tileId;
		}
		
		public TileLoadDownloadRequest(File dirWithTiles, String url, File fileToSave, String tileId, ITileSource source, int tileX,
				int tileY, int zoom, String referer) {
			super(url, fileToSave, tileX, tileY, zoom);
			this.dirWithTiles = dirWithTiles;
			this.tileSource = source;
			this.tileId = tileId;
			this.referer = referer;
		}
		
		public void saveTile(InputStream inputStream) throws IOException {
			if(tileSource instanceof SQLiteTileSource){
				ByteArrayOutputStream stream = null;
				try {
					stream = new ByteArrayOutputStream(inputStream.available());
					Algorithms.streamCopy(inputStream, stream);
					stream.flush();

					try {
						((SQLiteTileSource) tileSource).insertImage(xTile, yTile, zoom, stream.toByteArray());
					} catch (IOException e) {
						log.warn("Tile x="+xTile +" y="+ yTile+" z="+ zoom+" couldn't be read", e);  //$NON-NLS-1$//$NON-NLS-2$
					}
				} finally {
					Algorithms.closeStream(inputStream);
					Algorithms.closeStream(stream);
				}				
			}
			else {
				super.saveTile(inputStream);
			}
		}

	}

	protected class MapObjectLoadRequest<T> implements ResultMatcher<T> {
		protected double topLatitude;
		protected double bottomLatitude;
		protected double leftLongitude;
		protected double rightLongitude;
		protected boolean cancelled = false;
		protected volatile boolean running = false;

		public boolean isContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
			boolean inside = this.topLatitude >= topLatitude && this.leftLongitude <= leftLongitude
					&& this.rightLongitude >= rightLongitude && this.bottomLatitude <= bottomLatitude;
			return inside;
		}

		public void setBoundaries(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
			this.topLatitude = topLatitude;
			this.bottomLatitude = bottomLatitude;
			this.leftLongitude = leftLongitude;
			this.rightLongitude = rightLongitude;
		}
		
		public boolean isRunning() {
			return running && !cancelled;
		}
		
		public void start() {
			running = true;
		}
		
		public void finish() {
			running = false;
			// use downloader callback
			resourceManger.getMapTileDownloader().fireLoadCallback(null);
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean publish(T object) {
			return true;
		}

	}

	public interface OnMapLoadedListener {
		void onMapLoaded(boolean interrupted);
	}

	protected static class MapLoadRequest {
		public final RotatedTileBox tileBox;
		public final OnMapLoadedListener mapLoadedListener;

		public MapLoadRequest(RotatedTileBox tileBox, OnMapLoadedListener mapLoadedListener) {
			super();
			this.tileBox = tileBox;
			this.mapLoadedListener = mapLoadedListener;
		}
	}


}