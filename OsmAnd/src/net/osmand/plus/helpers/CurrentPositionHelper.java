package net.osmand.plus.helpers;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.RoadsLookup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.MapUtils;

// the road under the current position for the widgets; the roads themselves are found by
// GeocodingLookupService
public class CurrentPositionHelper {

	private RouteDataObject lastFound;
	private Location lastAskedLocation;
	private final OsmandApplication app;

	public CurrentPositionHelper(OsmandApplication app) {
		this.app = app;
	}

	public boolean getRouteSegment(Location loc,
								   @Nullable ApplicationMode appMode,
								   boolean cancelPreviousSearch,
								   ResultMatcher<RouteDataObject> result) {
		GeocodingLookupService service = app.getGeocodingLookupService();
		if (loc == null || service == null) {
			return false;
		}
		service.findRoads(loc, RoadsLookup.ROAD, cancelPreviousSearch, appMode, roads -> {
			RouteDataObject road = roads == null || roads.isEmpty() ? null : roads.get(0).point.getRoad();
			app.runInUIThread(() -> result.publish(road));
		});
		return true;
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
			findCurrentRoad(loc);
			return null;
		}
		double d = getOrthogonalDistance(r, loc);
		if (d > 15) {
			findCurrentRoad(loc);
		}
		if (d < 70) {
			return r;
		}
		return null;
	}

	private void findCurrentRoad(Location loc) {
		GeocodingLookupService service = app.getGeocodingLookupService();
		if (service == null) {
			return;
		}
		service.findRoads(loc, RoadsLookup.CURRENT_POSITION, true, null, roads -> {
			if (roads != null) {
				lastAskedLocation = loc;
				lastFound = roads.isEmpty() ? null : roads.get(0).point.getRoad();
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
