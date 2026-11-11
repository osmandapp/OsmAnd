package net.osmand.router.gtfs.guidance;

import java.util.List;

import net.osmand.data.LatLon;

/** Small distance helpers (spherical earth, local flat projection for segments). */
final class GeoMath {

	private static final double R = 6371008.8;

	private GeoMath() {
	}

	static double distance(double lat1, double lon1, double lat2, double lon2) {
		double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
		double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
		return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
	}

	static double distance(double lat, double lon, LatLon p) {
		return distance(lat, lon, p.getLatitude(), p.getLongitude());
	}

	/** Distance from a point to the segment a-b, in metres. */
	static double toSegment(double lat, double lon, LatLon a, LatLon b) {
		double cos = Math.cos(Math.toRadians(lat));
		double ax = (a.getLongitude() - lon) * cos, ay = a.getLatitude() - lat;
		double bx = (b.getLongitude() - lon) * cos, by = b.getLatitude() - lat;
		double dx = bx - ax, dy = by - ay;
		double len2 = dx * dx + dy * dy;
		double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / len2));
		double px = ax + t * dx, py = ay + t * dy;
		return Math.sqrt(px * px + py * py) * Math.PI / 180 * R;
	}

	/** Distance from a point to a polyline (a single point counts as a degenerate line). */
	static double toPath(double lat, double lon, List<LatLon> path) {
		double best = Double.MAX_VALUE;
		if (path.size() == 1) {
			return distance(lat, lon, path.get(0));
		}
		for (int i = 0; i + 1 < path.size(); i++) {
			best = Math.min(best, toSegment(lat, lon, path.get(i), path.get(i + 1)));
		}
		return best;
	}
}
