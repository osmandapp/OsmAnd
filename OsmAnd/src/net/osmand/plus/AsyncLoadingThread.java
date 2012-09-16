package net.osmand.plus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.data.TransportStop;
import net.osmand.map.ITileSource;

import org.apache.commons.logging.Log;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Thread to load map objects (POI, transport stops )async
 */
public class AsyncLoadingThread extends Thread {
	
	public static final int LIMIT_TRANSPORT = 200;
	
	private static final Log log = LogUtil.getLog(AsyncLoadingThread.class); 
	
	private Handler asyncLoadingPoi; 
	private Handler asyncLoadingTransport;
	
	Stack<Object> requests = new Stack<Object>();
	AmenityLoadRequest poiLoadRequest = null;
	TransportLoadRequest transportLoadRequest = null;
	
	
	private final ResourceManager resourceManger;

	public AsyncLoadingThread(ResourceManager resourceManger) {
		super("Loader map objects (synchronizer)"); //$NON-NLS-1$
		this.resourceManger = resourceManger;
	}
	
	private void startPoiLoadingThread() {
		HandlerThread h = new HandlerThread("Loading poi");
		h.start();
		asyncLoadingPoi = new Handler(h.getLooper());
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
		} else if (!requests.isEmpty()) {
			progress = BusyIndicator.STATUS_BLACK;
		} else if (poiLoadRequest != null && poiLoadRequest.isRunning()) {
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
					} else if (req instanceof AmenityLoadRequest) {
						if (!amenityLoaded) {
							if (poiLoadRequest == null || asyncLoadingPoi == null) {
								startPoiLoadingThread();
								poiLoadRequest = (AmenityLoadRequest) req;
								asyncLoadingPoi.post(poiLoadRequest.prepareToRun());
							} else if (poiLoadRequest.recalculateRequest((AmenityLoadRequest) req)) {
								poiLoadRequest = (AmenityLoadRequest) req;
								asyncLoadingPoi.post(poiLoadRequest.prepareToRun());
							}
							amenityLoaded = true;
						}
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
							resourceManger.getRenderer().loadMap(r.tileBox, resourceManger.getMapTileDownloader().getDownloaderCallbacks());
							mapLoaded = true;
						}
					}
				}
				if (tileLoaded || amenityLoaded || transportLoaded || mapLoaded) {
					// use downloader callback
					for (IMapDownloaderCallback c : resourceManger.getMapTileDownloader().getDownloaderCallbacks()) {
						c.tileDownloaded(null);
					}
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

	public void requestToLoadAmenities(AmenityLoadRequest req) {
		requests.push(req);
	}

	public void requestToLoadMap(MapLoadRequest req) {
		requests.push(req);
	}

	public void requestToLoadTransport(TransportLoadRequest req) {
		requests.push(req);
	}
	
	public boolean isFileCurrentlyDownloaded(File fileToSave) {
		return resourceManger.getMapTileDownloader().isFileCurrentlyDownloaded(fileToSave);
	}

	public void requestToDownload(TileLoadDownloadRequest req) {
		resourceManger.getMapTileDownloader().requestToDownload(req);
	}

	protected static class TileLoadDownloadRequest extends DownloadRequest {

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
			ArrayList<IMapDownloaderCallback> ls = new ArrayList<IMapDownloaderCallback>(resourceManger.getMapTileDownloader().getDownloaderCallbacks());
			for (IMapDownloaderCallback c : ls) {
				c.tileDownloaded(null);
			}
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

	protected class AmenityLoadRequest extends MapObjectLoadRequest<Amenity> {
		private final List<AmenityIndexRepository> res;
		private final PoiFilter filter;
		private final int zoom;
		private String filterByName;

		public AmenityLoadRequest(List<AmenityIndexRepository> repos, int zoom, PoiFilter filter, String nameFilter) {
			super();
			this.res = repos;
			this.zoom = zoom;
			this.filter = filter;
			this.filterByName = nameFilter;
			if(this.filterByName != null) {
				this.filterByName = this.filterByName.toLowerCase().trim();
			}
		}
		
		@Override
		public boolean publish(Amenity object) {
			if(filterByName == null || filterByName.length() == 0) {
				return true;
			} else {
				String lower = OsmAndFormatter.getPoiStringWithoutType(object, resourceManger.getContext().getSettings().usingEnglishNames()).toLowerCase();
				return lower.indexOf(filterByName) != -1;
			}
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
						for (AmenityIndexRepository repository : res) {
							repository.evaluateCachedAmenities(ntopLatitude, nleftLongitude, nbottomLatitude, nrightLongitude, zoom,
									filter, AmenityLoadRequest.this);
						}
					} finally {
						finish();
					}
				}
			};
		}

		private boolean repoHasChange() {
			for (AmenityIndexRepository r : res) {
				if (r.hasChange()) {
					r.clearChange();
					return true;
				}
			}
			return false;
		}
		public boolean recalculateRequest(AmenityLoadRequest req) {
			if (this.zoom != req.zoom || !Algoritms.objectEquals(this.filter, req.filter) || req.repoHasChange()) {
				return true;
			}
			return !isContains(req.topLatitude, req.leftLongitude, req.bottomLatitude, req.rightLongitude);
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