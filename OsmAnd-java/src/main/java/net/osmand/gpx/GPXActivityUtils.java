package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.osm.OsmRouteType;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;

public class GPXActivityUtils {

	public static OsmRouteType fetchActivityType(Metadata metadata, RouteKey routeKey) {
		OsmRouteType activityType = metadata != null ? OsmRouteType.getByTag(metadata.getActivity()) : null;
		if (activityType == null && routeKey != null) {
			activityType = routeKey.type;
		}
		return activityType;
	}

}
