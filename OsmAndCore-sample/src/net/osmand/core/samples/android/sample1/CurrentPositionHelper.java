package net.osmand.core.samples.android.sample1;

import android.os.AsyncTask;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.CachedOsmandIndexes;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrentPositionHelper {

	private RouteDataObject lastFound;
	private Location lastAskedLocation = null;
	private RoutingContext defCtx;
	private SampleApplication app;
	private List<BinaryMapIndexReader> readers = new ArrayList<>();
	private List<BinaryMapIndexReader> usedReaders = new ArrayList<>();
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(CurrentPositionHelper.class);
	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	public CurrentPositionHelper(SampleApplication app) {
		this.app = app;
		setRepositories();
	}


	public void setRepositories() {
		ArrayList<File> files = new ArrayList<File>();
		File appPath = app.getAppPath(null);
		SampleUtils.collectFiles(appPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);

		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = app.getAppPath("ind_core.cache");
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache, CachedOsmandIndexes.VERSION);
			} catch (Exception e) {
			}
		}
		readers.clear();
		for (File f : files) {
			try {
				BinaryMapIndexReader reader = cachedOsmandIndexes.getReader(f);
				readers.add(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (files.size() > 0 && (!indCache.exists() || indCache.canWrite())) {
			try {
				cachedOsmandIndexes.writeToFile(indCache);
			} catch (Exception e) {
				log.error("Index file could not be written", e);
			}
		}
	}

	public Location getLastAskedLocation() {
		return lastAskedLocation;
	}

	public boolean getGeocodingResult(Location loc, ResultMatcher<GeocodingResult> result) {
		return scheduleRouteSegmentFind(loc, false, result, null);
	}

	public RouteDataObject getLastKnownRouteSegment(Location loc) {
		Location last = lastAskedLocation;
		RouteDataObject r = lastFound;
		if (loc == null || loc.getAccuracy() > 50) {
			return null;
		}
		if (last != null && last.distanceTo(loc) < 10) {
			return r;
		}
		if (r == null) {
			scheduleRouteSegmentFind(loc, true, null, null);
			return null;
		}
		double d = getOrthogonalDistance(r, loc);
		if (d > 15) {
			scheduleRouteSegmentFind(loc, true, null, null);
		}
		if (d < 70) {
			return r;
		}
		return null;
	}

	///////////////////////// PRIVATE IMPLEMENTATION //////////////////////////
	private boolean scheduleRouteSegmentFind(final Location loc, final boolean storeFound, final ResultMatcher<GeocodingResult> geoCoding, final ResultMatcher<RouteDataObject> result) {
		boolean res = false;
		if (loc != null) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					try {
						processGeocoding(loc, geoCoding, storeFound, result);
					} catch (Exception e) {
						log.error("Error processing geocoding", e);
						e.printStackTrace();
					}
					return null;
				}
			}.executeOnExecutor(singleThreadExecutor, (Void) null);
			res = true;
		}
		return res;
	}

	private void initCtx(SampleApplication app, List<BinaryMapIndexReader> checkReaders) {
		BinaryMapIndexReader[] rs = checkReaders.toArray(new BinaryMapIndexReader[checkReaders.size()]);
		if (rs.length > 0) {
			RoutingConfiguration defCfg = RoutingConfiguration.getDefault().build("geocoding", 10,
					new HashMap<String, String>());
			defCtx = new RoutePlannerFrontEnd().buildRoutingContext(defCfg, null, rs);
		} else {
			defCtx = null;
		}
		usedReaders = checkReaders;
	}

	// single synchronized method
	private synchronized void processGeocoding(Location loc, ResultMatcher<GeocodingResult> geoCoding, boolean storeFound, final ResultMatcher<RouteDataObject> result) throws IOException {
		final List<GeocodingResult> gr = runUpdateInThread(loc.getLatitude(), loc.getLongitude());
		if (storeFound) {
			lastAskedLocation = loc;
			lastFound = gr == null || gr.isEmpty() ? null : gr.get(0).point.getRoad();
		} else if (geoCoding != null) {
			justifyResult(gr, geoCoding);
		} else if (result != null) {
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					result.publish(gr == null || gr.isEmpty() ? null : gr.get(0).point.getRoad());
				}
			});
		}
	}

	private List<GeocodingResult> runUpdateInThread(double lat, double lon) throws IOException {
		List<BinaryMapIndexReader> checkReaders = checkReaders(lat, lon);
		if (defCtx == null || checkReaders != usedReaders) {
			initCtx(app, checkReaders);
			if (defCtx == null) {
				return null;
			}
		}
		try {
			return new GeocodingUtilities().reverseGeocodingSearch(defCtx, lat, lon, false);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<BinaryMapIndexReader> checkReaders(double lat, double lon) {
		List<BinaryMapIndexReader> res = new ArrayList<>();
		int y31 = MapUtils.get31TileNumberY(lat);
		int x31 = MapUtils.get31TileNumberX(lon);
		for (BinaryMapIndexReader r : readers) {
			if (r.containsRouteData(x31, y31, x31, y31, 15)) {
				if (!res.contains(r)) {
					res = new ArrayList<>(res);
					res.add(r);
				}
			}
		}
		return res;
	}

	private void justifyResult(List<GeocodingResult> res, final ResultMatcher<GeocodingResult> result) {
		List<GeocodingResult> complete = new ArrayList<>();
		double minBuildingDistance = 0;
		GeocodingUtilities gu = new GeocodingUtilities();
		if (res != null) {
			for (GeocodingResult r : res) {
				BinaryMapIndexReader foundRepo = null;
				List<BinaryMapIndexReader> rts = usedReaders;
				for (BinaryMapIndexReader reader : rts) {
					for (RouteRegion rb : reader.getRoutingIndexes()) {
						if (r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
							foundRepo = reader;
							break;
						}
					}
				}
				if (result.isCancelled()) {
					break;
				} else if (foundRepo != null) {
					List<GeocodingResult> justified = null;
					try {
						justified = gu.justifyReverseGeocodingSearch(r, foundRepo,
								minBuildingDistance, result);
					} catch (IOException e) {
						log.error("Exception happened during reverse geocoding", e);
						e.printStackTrace();
					}
					if (justified != null && !justified.isEmpty()) {
						double md = justified.get(0).getDistance();
						if (minBuildingDistance == 0) {
							minBuildingDistance = md;
						} else {
							minBuildingDistance = Math.min(md, minBuildingDistance);
						}
						complete.addAll(justified);
					}
				} else {
					complete.add(r);
				}
			}
			gu.filterDuplicateRegionResults(complete);
		}

		if (result.isCancelled()) {
			app.runInUIThread(new Runnable() {
				public void run() {
					result.publish(null);
				}
			});
			return;
		}
		// Collections.sort(complete, GeocodingUtilities.DISTANCE_COMPARATOR);
//		for(GeocodingResult rt : complete) {
//			System.out.println(rt.toString());
//		}
		final GeocodingResult rts = complete.size() > 0 ? complete.get(0) : new GeocodingResult();
		app.runInUIThread(new Runnable() {
			public void run() {
				result.publish(rts);
			}
		});
	}

	public static double getOrthogonalDistance(RouteDataObject r, Location loc) {
		double d = 1000;
		if (r.getPointsLength() > 0) {
			double pLt = MapUtils.get31LatitudeY(r.getPoint31YTile(0));
			double pLn = MapUtils.get31LongitudeX(r.getPoint31XTile(0));
			for (int i = 1; i < r.getPointsLength(); i++) {
				double lt = MapUtils.get31LatitudeY(r.getPoint31YTile(i));
				double ln = MapUtils.get31LongitudeX(r.getPoint31XTile(i));
				double od = MapUtils.getOrthogonalDistance(loc.getLatitude(), loc.getLongitude(), pLt, pLn, lt, ln);
				if (od < d) {
					d = od;
				}
				pLt = lt;
				pLn = ln;
			}
		}
		return d;
	}


}
