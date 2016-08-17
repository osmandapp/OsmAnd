package net.osmand.plus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;
import android.os.AsyncTask;

public class CurrentPositionHelper {
	
	private RouteDataObject lastFound;
	private Location lastAskedLocation = null;
	private RoutingContext ctx;
	private RoutingContext defCtx;
	private OsmandApplication app;
	private ApplicationMode am;
	private List<BinaryMapReaderResource> usedReaders = new ArrayList<>();
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(CurrentPositionHelper.class);

	public CurrentPositionHelper(OsmandApplication app) {
		this.app = app;
	}
	
	
	public Location getLastAskedLocation() {
		return lastAskedLocation;
	}
	
	public boolean getRouteSegment(Location loc, ResultMatcher<RouteDataObject> result) {
		return scheduleRouteSegmentFind(loc, false, null, result);
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
		if(last != null && last.distanceTo(loc) < 10) {
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
			}.execute((Void) null);
			res = true;
		}
		return res;
	}
	
	private void initCtx(OsmandApplication app, List<BinaryMapReaderResource> checkReaders) {
		am = app.getSettings().getApplicationMode();
		String p ;
		if (am.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			p = GeneralRouterProfile.BICYCLE.name().toLowerCase();
		} else if (am.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			p = GeneralRouterProfile.PEDESTRIAN.name().toLowerCase();
		} else if (am.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			p = GeneralRouterProfile.CAR.name().toLowerCase();
		} else {
			p = "geocoding";
		}
		
		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[checkReaders.size()];
		if (rs.length > 0) {
			int i = 0;
			for (BinaryMapReaderResource rep : checkReaders) {
				rs[i++] = rep.getReader(BinaryMapReaderResourceType.STREET_LOOKUP);
			}
			RoutingConfiguration cfg = app.getDefaultRoutingConfig().build(p, 10,
					new HashMap<String, String>());
			ctx = new RoutePlannerFrontEnd(false).buildRoutingContext(cfg, null, rs);
			RoutingConfiguration defCfg = app.getDefaultRoutingConfig().build("geocoding", 10,
					new HashMap<String, String>());
			defCtx = new RoutePlannerFrontEnd(false).buildRoutingContext(defCfg, null, rs);
		} else {
			ctx = null;
			defCtx = null;
		}
		usedReaders = checkReaders;
	}

	// single synchronized method
	private synchronized void processGeocoding(Location loc, ResultMatcher<GeocodingResult> geoCoding, boolean storeFound, final ResultMatcher<RouteDataObject> result) throws IOException {
		final List<GeocodingResult> gr = runUpdateInThread(loc.getLatitude(), loc.getLongitude(),
				geoCoding != null);
		if (storeFound) {
			lastAskedLocation = loc;
			lastFound = gr == null || gr.isEmpty() ? null : gr.get(0).point.getRoad();
		} else if(geoCoding != null) {
			justifyResult(gr, geoCoding);
		} else if(result != null) {
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					result.publish(gr == null || gr.isEmpty() ? null : gr.get(0).point.getRoad());
				}
			});
		}
	}
	
	private List<GeocodingResult> runUpdateInThread(double lat, double lon, boolean geocoding) throws IOException {
		List<BinaryMapReaderResource> checkReaders = checkReaders(lat, lon, usedReaders);
		if (ctx == null || am != app.getSettings().getApplicationMode() || checkReaders != usedReaders) {
			initCtx(app, checkReaders);
			if (ctx == null) {
				return null;
			}
		}
		try {
			return new GeocodingUtilities().reverseGeocodingSearch(geocoding ? defCtx : ctx, lat, lon);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private List<BinaryMapReaderResource> checkReaders(double lat, double lon,
			List<BinaryMapReaderResource> ur) {
		List<BinaryMapReaderResource> res = ur;
		for(BinaryMapReaderResource t : ur ) {
			if(t.isClosed()) {
				res = new ArrayList<>();
				break;
			}
		}
		int y31 = MapUtils.get31TileNumberY(lat);
		int x31 = MapUtils.get31TileNumberX(lon);
		for(BinaryMapReaderResource r : app.getResourceManager().getFileReaders()) {
			if(!r.isClosed() && r.getShallowReader().containsRouteData(x31, y31, x31, y31, 15)) {
				if(!res.contains(r)) {
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
		if (res != null) {
			for (GeocodingResult r : res) {
				BinaryMapIndexReader foundRepo = null;
				List<BinaryMapReaderResource> rts  = usedReaders;
				for (BinaryMapReaderResource rt : rts) {
					if(rt.isClosed()) {
						continue;
					}
					BinaryMapIndexReader reader = rt.getReader(BinaryMapReaderResourceType.STREET_LOOKUP);
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
						justified = new GeocodingUtilities().justifyReverseGeocodingSearch(r, foundRepo,
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
		}

		if (result.isCancelled()) {
			app.runInUIThread(new Runnable() {
				public void run() {
					result.publish(null);
				}
			});
			return;
		}
		Collections.sort(complete, GeocodingUtilities.DISTANCE_COMPARATOR);
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

	public static double getOrthogonalDistance(RouteDataObject r, Location loc){
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
