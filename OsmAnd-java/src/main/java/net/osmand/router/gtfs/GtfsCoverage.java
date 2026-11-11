package net.osmand.router.gtfs;

import net.osmand.data.LatLon;

/**
 * Bounding box of the stops of an index. A trip can only start or end within walking distance of a stop, so
 * a point further than that from the box cannot be served and the caller should use another router.
 */
public final class GtfsCoverage {

	private static final double METERS_PER_DEGREE = 111_320;

	private final double minLat, maxLat, minLon, maxLon;

	public GtfsCoverage(double minLat, double maxLat, double minLon, double maxLon) {
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.minLon = minLon;
		this.maxLon = maxLon;
	}

	/** Null when the index has no stops. */
	public static GtfsCoverage of(GtfsIndex index) {
		double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
		int n = index.getStopCount();
		if (n == 0) {
			return null;
		}
		for (int s = 0; s < n; s++) {
			double lat = index.getStopLat(s), lon = index.getStopLon(s);
			minLat = Math.min(minLat, lat);
			maxLat = Math.max(maxLat, lat);
			minLon = Math.min(minLon, lon);
			maxLon = Math.max(maxLon, lon);
		}
		return new GtfsCoverage(minLat, maxLat, minLon, maxLon);
	}

	/** True when the point is inside the box, widened by the given distance on every side. */
	public boolean contains(LatLon p, double marginMeters) {
		if (p == null) {
			return false;
		}
		double dLat = marginMeters / METERS_PER_DEGREE;
		double refLat = Math.max(Math.abs(minLat), Math.abs(maxLat));
		double dLon = marginMeters / (METERS_PER_DEGREE * Math.max(0.01, Math.cos(Math.toRadians(Math.min(refLat, 89.9)))));
		return p.getLatitude() >= minLat - dLat && p.getLatitude() <= maxLat + dLat
				&& p.getLongitude() >= minLon - dLon && p.getLongitude() <= maxLon + dLon;
	}
}
