package net.osmand.plus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.GeocodingUtilities;
import net.osmand.binary.GeocodingUtilities.GeocodingResult;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;

public class CurrentPositionHelper {
	
	private RouteDataObject lastFound;
	private Location lastAskedLocation = null;
	private RoutingContext ctx;
	private RoutingContext defCtx;
	private OsmandApplication app;
	private ApplicationMode am;

	public CurrentPositionHelper(OsmandApplication app) {
		this.app = app;
	}

	private void initCtx(OsmandApplication app) {
		am = app.getSettings().getApplicationMode();
		GeneralRouterProfile p ;
		if (am.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			p = GeneralRouterProfile.BICYCLE;
		} else if (am.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			p = GeneralRouterProfile.PEDESTRIAN;
		} else if (am.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			p = GeneralRouterProfile.CAR;
		} else {
			p = GeneralRouterProfile.PEDESTRIAN;
		}
		RoutingConfiguration cfg = app.getDefaultRoutingConfig().build(p.name().toLowerCase(), 10, 
				new HashMap<String, String>());
		ctx = new RoutePlannerFrontEnd(false).buildRoutingContext(cfg, null, app.getResourceManager().getRoutingMapFiles());
		RoutingConfiguration defCfg = app.getDefaultRoutingConfig().build(GeneralRouterProfile.CAR.name().toLowerCase(), 10, 
				new HashMap<String, String>());
		defCtx = new RoutePlannerFrontEnd(false).buildRoutingContext(defCfg, null, app.getResourceManager().getRoutingMapFiles());
	}
	
	
	
	
	private boolean scheduleRouteSegmentFind(final Location loc, final boolean storeFound, final ResultMatcher<GeocodingResult> geoCoding, final ResultMatcher<RouteDataObject> result) {
		boolean res = false;
		if (loc != null) {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
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
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			res = app.getRoutingHelper().startTaskInRouteThreadIfPossible(run);
		}
		return res;
	}
	
	protected void justifyResult(List<GeocodingResult> res, final ResultMatcher<GeocodingResult> result) {
		List<GeocodingResult> complete = new ArrayList<>();
		double minBuildingDistance = 0;
		for (GeocodingResult r : res) {
			Collection<RegionAddressRepository> rar = app.getResourceManager().getAddressRepositories();
			RegionAddressRepository foundRepo = null;
			for (RegionAddressRepository repo : rar) {
				BinaryMapIndexReader reader = repo.getFile();
				for (RouteRegion rb : reader.getRoutingIndexes()) {
					if (r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
						foundRepo = repo;
						break;
					}
					if (result.isCancelled()) {
						break;
					}
				}
				if (foundRepo != null || result.isCancelled()) {
					break;
				}
			}
			if (result.isCancelled()) {
				break;
			} else if (foundRepo != null) {
				List<GeocodingResult> justified = foundRepo.justifyReverseGeocodingSearch(r, minBuildingDistance, result);
				if (!justified.isEmpty()) {
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

	private static double getOrthogonalDistance(RouteDataObject r, Location loc){
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
		if(last != null && last.distanceTo(loc) < 20) {
			return r;
		}
		if (r == null) {
			scheduleRouteSegmentFind(loc, true, null, null);
			return null;
		}
		double d = getOrthogonalDistance(r, loc);
		if (d > 25) {
			scheduleRouteSegmentFind(loc, true, null, null);
		}
		if (d < 70) {
			return r;
		}
		return null;
	}

	
	private synchronized List<GeocodingResult> runUpdateInThread(double lat, double lon, boolean geocoding) throws IOException {
		if (ctx == null || am != app.getSettings().getApplicationMode()) {
			initCtx(app);
			if (ctx == null) {
				return null;
			}
		}
		return new GeocodingUtilities().reverseGeocodingSearch(geocoding ? defCtx : ctx, lat, lon);
	}
}
