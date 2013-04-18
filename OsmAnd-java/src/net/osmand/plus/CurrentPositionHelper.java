package net.osmand.plus;

import java.io.IOException;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;

public class CurrentPositionHelper {
	
	private RouteDataObject lastFound;
	private Location lastAskedLocation = null;
	private Thread calculatingThread = null;
	private RoutingContext ctx;
	private ClientContext app;
	private ApplicationMode am;

	public CurrentPositionHelper(ClientContext app) {
		this.app = app;
	}

	private void initCtx(ClientContext app) {
		am = app.getSettings().getApplicationMode();
		GeneralRouterProfile p ;
		if (am == ApplicationMode.BICYCLE) {
			p = GeneralRouterProfile.BICYCLE;
		} else if (am == ApplicationMode.PEDESTRIAN) {
			p = GeneralRouterProfile.PEDESTRIAN;
		} else {
			p = GeneralRouterProfile.CAR;
		}
		RoutingConfiguration cfg = RoutingConfiguration.getDefault().build(p.name().toLowerCase(), 10);
		ctx = new RoutingContext(cfg, null, app.getTodoAPI().getRoutingMapFiles());
	}
	
	private RouteDataObject runUpdateInThread(Location loc) {
		RoutePlannerFrontEnd rp = new RoutePlannerFrontEnd(false);
		try {
			if(ctx == null || am != app.getSettings().getApplicationMode()) {
				initCtx(app);
			}
			RouteSegment sg = rp.findRouteSegment(loc.getLatitude(), loc.getLongitude(), ctx);
			if(sg == null) {
				return null;
			}
			return sg.getRoad();
		} catch (IOException e) {
			return null;
		}
	}
	
	
	private void scheduleRouteSegmentFind(final Location loc){
		if(calculatingThread == Thread.currentThread()) {
			lastFound = runUpdateInThread(loc);
		} else if(calculatingThread == null && loc != null) {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						lastFound = runUpdateInThread(loc);
						if (lastAskedLocation != loc) {
							// refresh and run new task if needed
							getLastKnownRouteSegment(lastAskedLocation);
						}
					} finally {
						calculatingThread = null;
					}
				}
			};
			calculatingThread = app.getRoutingHelper().startTaskInRouteThreadIfPossible(run);
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
	
	public RouteDataObject getLastKnownRouteSegment(Location loc) {
		lastAskedLocation = loc;
		RouteDataObject r = lastFound;
		if (loc == null || loc.getAccuracy() > 50) {
			return null;
		}
		if (r == null) {
			scheduleRouteSegmentFind(loc);
			return null;
		}
		double d = getOrthogonalDistance(r, loc);
		if (d > 25) {
			scheduleRouteSegmentFind(loc);
		}
		if (d < 70) {
			return r;
		}
		return null;
	}

}
