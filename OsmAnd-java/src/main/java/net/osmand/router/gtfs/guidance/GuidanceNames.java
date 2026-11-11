package net.osmand.router.gtfs.guidance;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.GtfsLeg;

/** What the engine needs to know about stops and routes, so it does not depend on the GTFS index. */
public interface GuidanceNames {

	/** Display name of a stop by index, or null if unknown. */
	String stopName(int stopIdx);

	/** Location of a stop, or null if unknown. Without it, location-based ride progress is off for that leg. */
	default LatLon stopLocation(int stopIdx) {
		return null;
	}

	/** Short line name of a ride leg, e.g. "Q". The default is the route id without its feed prefix. */
	default String routeName(GtfsLeg ride) {
		String id = ride.routeId;
		if (id == null) {
			return "";
		}
		int colon = id.indexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}

	/** Direction shown to the rider, or null if unknown. */
	default String headsign(GtfsLeg ride) {
		return null;
	}
}
