package net.osmand.plus;

import java.io.IOException;
import java.util.HashMap;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.util.MapUtils;

public class SearchOnTheRouteHelper {

	private OsmandApplication app;
	private PoiFilter filter;
	private Thread calculatingThread;

	public SearchOnTheRouteHelper(OsmandApplication app) {
		this.app = app;
	}
	
	public void searchOnTheRoute(RouteCalculationResult route) {
		scheduleRouteSegmentFind(route);
	}
	
	
	private void scheduleRouteSegmentFind(final RouteCalculationResult route){
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
	


}
