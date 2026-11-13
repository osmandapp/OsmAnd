package net.osmand.router.gtfs;

import net.osmand.data.LatLon;
import net.osmand.router.gtfs.guidance.GuidanceNames;

/** Stop names, stop locations, line names and headsigns for guidance, read from the open {@link GtfsIndex}. */
public final class GtfsIndexGuidanceNames implements GuidanceNames {

	private final GtfsIndex index;

	public GtfsIndexGuidanceNames(GtfsIndex index) {
		this.index = index;
	}

	@Override
	public String stopName(int stopIdx) {
		if (stopIdx < 0 || stopIdx >= index.getStopCount()) {
			return null;
		}
		String name = index.getStopName(stopIdx);
		return name == null || name.isEmpty() ? null : name;
	}

	@Override
	public LatLon stopLocation(int stopIdx) {
		if (stopIdx < 0 || stopIdx >= index.getStopCount()) {
			return null;
		}
		return new LatLon(index.getStopLat(stopIdx), index.getStopLon(stopIdx));
	}

	/** Short name, else long name, as the route cards show it. */
	@Override
	public String routeName(GtfsLeg ride) {
		int r = ride.routeIdx;
		if (r >= 0 && r < index.getRouteCount()) {
			String shortName = index.getRouteShortName(r);
			if (shortName != null && !shortName.isEmpty()) {
				return shortName;
			}
			String longName = index.getRouteLongName(r);
			if (longName != null && !longName.isEmpty()) {
				return longName;
			}
		}
		return GuidanceNames.super.routeName(ride);
	}

	@Override
	public String headsign(GtfsLeg ride) {
		int t = ride.tripIdx;
		if (t < 0 || t >= index.getTripCount()) {
			return null;
		}
		String headsign = index.getTripHeadsign(t);
		return headsign == null || headsign.isEmpty() ? null : headsign;
	}
}
