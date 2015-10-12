package net.osmand.plus.resources;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.map.ITileSource;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.BusyIndicator;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Thread to load map objects (POI, transport stops )async
 */
public class AsyncLoadingThread extends Thread {
	
	public static final int LIMIT_TRANSPORT = 200;
	
	private static final Log log = PlatformUtil.getLog(AsyncLoadingThread.class); 
	
	private Handler asyncLoadingTransport;
	
	Stack<Object> requests = new Stack<Object>();
	TransportLoadRequest transportLoadRequest = null;
	
	
	private final ResourceManager resourceManger;

	public AsyncLoadingThread(ResourceManager resourceManger) {
		super("Loader map objects (synchronizer)"); //$NON-NLS-1$
		this.resourceManger = resourceManger;
	}
	
	
	
	private void startTransportLoadingThread() {
		HandlerThread h = new HandlerThread("Loading transport");
		h.start();
		asyncLoadingTransport = new Handler(h.getLooper());
	}

	private int calculateProgressStatus() {
		int progress = 0;
		if (resourceManger.getMapTileDownloader() != null && resourceManger.getMapTileDownloader().isSomethingBeingDownloaded()) {
			progress = BusyIndicator.STATUS_GREEN;
		} else if (resourceManger.getContext().getRoutingHelper().isRouteBeingCalculated()) {
			progress = BusyIndicator.STATUS_ORANGE;
		} else if (resourceManger.isSearchAmenitiesInProgress()) {
			progress = BusyIndicator.STATUS_BLACK;
		} else if (!requests.isEmpty()) {
			progress = BusyIndicator.STATUS_BLACK;
		} else if (transportLoadRequest != null && transportLoadRequest.isRunning()) {
			progress = BusyIndicator.STATUS_BLACK;
		}
		return progress;
	}

	@Override
	public void run() {
		while (true) {
			try {
				boolean tileLoaded = false;
				boolean amenityLoaded = false;
				boolean transportLoaded = false;
				boolean mapLoaded = false;
				
				int progress = calculateProgressStatus();
				synchronized (resourceManger) {
					if (resourceManger.getBusyIndicator() != null) {
						resourceManger.getBusyIndicator().updateStatus(progress);
					}
				}
				while (!requests.isEmpty()) {
					Object req = requests.pop();
					if (req instanceof TileLoadDownloadRequest) {
						TileLoadDownloadRequest r = (TileLoadDownloadRequest) req;
						tileLoaded |= resourceManger.getRequestedImageTile(r) != null;
					} else if (req instanceof TransportLoadRequest) {
						if (!transportLoaded) {
							if (transportLoadRequest == null || asyncLoadingTransport == null) {
								startTransportLoadingThread();
								transportLoadRequest = (TransportLoadRequest) req;
								asyncLoadingTransport.post(transportLoadRequest.prepareToRun());
							} else if (transportLoadRequest.recalculateRequest((TransportLoadRequest) req)) {
								transportLoadRequest = (TransportLoadRequest) req;
								asyncLoadingTransport.post(transportLoadRequest.prepareToRun());
							}
							transportLoaded = true;
						}
					} else if (req instanceof MapLoadRequest) {
						if (!mapLoaded) {
							MapLoadRequest r = (MapLoadRequest) req;
							resourceManger.getRenderer().loadMap(r.tileBox, resourceManger.getMapTileDownloader());
							mapLoaded = !resourceManger.getRenderer().wasInterrupted();
						}
					}
				}
				if (tileLoaded || amenityLoaded || transportLoaded || mapLoaded) {
					// use downloader callback
					resourceManger.getMapTileDownloader().fireLoadCallback(null);
				}
				int newProgress = calculateProgressStatus();
				if (progress != newProgress) {
					synchronized (resourceManger) {
						if (resourceManger.getBusyIndicator() != null) {
							resourceManger.getBusyIndicator().updateStatus(newProgress);
						}
					}
				}
				sleep(750);
			} catch (InterruptedException e) {
				log.error(e, e);
			} catch (RuntimeException e) {
				log.error(e, e);
			}
		}
	}

	public void requestToLoadImage(TileLoadDownloadRequest req) {
		requests.push(req);
	}

	public void requestToLoadMap(MapLoadRequest req) {
		requests.push(req);
	}

	public void requestToLoadTransport(TransportLoadRequest req) {
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

	

	protected class TransportLoadRequest extends MapObjectLoadRequest<TransportStop> {
		private final List<TransportIndexRepository> repos;
		private int zoom;

		public TransportLoadRequest(List<TransportIndexRepository> repos, int zoom) {
			super();
			this.repos = repos;
			this.zoom = zoom;
		}

		public Runnable prepareToRun() {
			final double ntopLatitude = topLatitude + (topLatitude - bottomLatitude) / 2;
			final double nbottomLatitude = bottomLatitude - (topLatitude - bottomLatitude) / 2;
			final double nleftLongitude = leftLongitude - (rightLongitude - leftLongitude) / 2;
			final double nrightLongitude = rightLongitude + (rightLongitude - leftLongitude) / 2;
			setBoundaries(ntopLatitude, nleftLongitude, nbottomLatitude, nrightLongitude);
			return new Runnable() {
				@Override
				public void run() {
					start();
					try {
						for (TransportIndexRepository repository : repos) {
							repository.evaluateCachedTransportStops(ntopLatitude, nleftLongitude, nbottomLatitude, nrightLongitude, zoom,
									LIMIT_TRANSPORT, TransportLoadRequest.this);
						}
					} finally {
						finish();
					}
				}
			};
		}

		public boolean recalculateRequest(TransportLoadRequest req) {
			if (this.zoom != req.zoom) {
				return true;
			}
			return !isContains(req.topLatitude, req.leftLongitude, req.bottomLatitude, req.rightLongitude);
		}

	}

	protected static class MapLoadRequest {
		public final RotatedTileBox tileBox;

		public MapLoadRequest(RotatedTileBox tileBox) {
			super();
			this.tileBox = tileBox;
		}
	}


}