package net.osmand.plus;

import java.io.IOException;
import java.util.HashMap;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;

public class CurrentPositionHelper {
	
	private RouteDataObject lastFound;
	private Location lastAskedLocation = null;
	private RoutingContext ctx;
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
	}
	
	
	
	
	private void scheduleRouteSegmentFind(final Location loc, final ResultMatcher<RouteDataObject> result, final boolean storeFound) {
		if (loc != null) {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						RouteDataObject res = runUpdateInThread(loc.getLatitude(), loc.getLongitude(), result);
						if (storeFound) {
							lastAskedLocation = loc;
							lastFound = res;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			app.getRoutingHelper().startTaskInRouteThreadIfPossible(run);
		}
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
	
	public void getRouteSegment(Location loc, ResultMatcher<RouteDataObject> result) {
		scheduleRouteSegmentFind(loc, result, false);
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
			scheduleRouteSegmentFind(loc, null, true);
			return null;
		}
		double d = getOrthogonalDistance(r, loc);
		if (d > 25) {
			scheduleRouteSegmentFind(loc, null, true);
		}
		if (d < 70) {
			return r;
		}
		return null;
	}

	
	private synchronized RouteDataObject runUpdateInThread(double lat, double lon, final ResultMatcher<RouteDataObject> resultMatcher) throws IOException {
		RoutePlannerFrontEnd rp = new RoutePlannerFrontEnd(false);
		if (ctx == null || am != app.getSettings().getApplicationMode()) {
			initCtx(app);
			if (ctx == null) {
				return null;
			}
		}
		final RouteSegmentPoint sg = rp.findRouteSegment(lat, lon, true, ctx);
		final RouteDataObject res;
		if(sg == null) {
			res = null;
		} else {
			RouteSegmentPoint ff = rp.findRouteSegment(lat, lon, false, ctx);
			if(ff == null || ff.dist + 70 * 70 < sg.dist) {
				res = null; 
			} else {
				res = sg.getRoad();
			}
		}
		if(resultMatcher != null) {
			app.runInUIThread(new Runnable() {
				public void run() {
					resultMatcher.publish(res);
				}
			});
		}
		return res;

	}
}
