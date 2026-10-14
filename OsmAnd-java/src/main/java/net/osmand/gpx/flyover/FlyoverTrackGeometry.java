package net.osmand.gpx.flyover;

import net.osmand.data.LatLon;

import java.util.List;

/**
 * Polyline of a recorded track with cumulative distances along it.
 * Drives the flyover camera: positions, headings and projections of places onto the track.
 * Distances use an equirectangular approximation, which is accurate enough for a track of a few hundred km.
 */
public class FlyoverTrackGeometry {

	private static final double METERS_PER_DEGREE = 111_320d;

	private final double[] lats;
	private final double[] lons;
	private final double[] cumulative;
	private final double length;
	private final double minLat;
	private final double maxLat;
	private final double minLon;
	private final double maxLon;

	public FlyoverTrackGeometry(List<LatLon> points) {
		if (points == null || points.isEmpty()) {
			throw new IllegalArgumentException("Track has no points");
		}
		int n = points.size();
		lats = new double[n];
		lons = new double[n];
		cumulative = new double[n];
		double minLat = Double.POSITIVE_INFINITY;
		double maxLat = Double.NEGATIVE_INFINITY;
		double minLon = Double.POSITIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < n; i++) {
			LatLon point = points.get(i);
			lats[i] = point.getLatitude();
			lons[i] = point.getLongitude();
			minLat = Math.min(minLat, lats[i]);
			maxLat = Math.max(maxLat, lats[i]);
			minLon = Math.min(minLon, lons[i]);
			maxLon = Math.max(maxLon, lons[i]);
			if (i > 0) {
				cumulative[i] = cumulative[i - 1] + distance(lats[i - 1], lons[i - 1], lats[i], lons[i]);
			}
		}
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.length = cumulative[n - 1];
	}

	public int getPointCount() {
		return lats.length;
	}

	/** Length of the track in meters. */
	public double getLength() {
		return length;
	}

	public double getMinLat() {
		return minLat;
	}

	public double getMaxLat() {
		return maxLat;
	}

	public double getMinLon() {
		return minLon;
	}

	public double getMaxLon() {
		return maxLon;
	}

	public double getCenterLat() {
		return (minLat + maxLat) / 2;
	}

	public double getCenterLon() {
		return (minLon + maxLon) / 2;
	}

	/** Distance from the start of the track to the point with the given index. */
	public double distanceAtPoint(int index) {
		return cumulative[index];
	}

	/** Point on the track at the given distance from its start, clamped to the track. */
	public LatLon positionAt(double distance) {
		int last = lats.length - 1;
		if (last == 0 || distance <= 0) {
			return new LatLon(lats[0], lons[0]);
		}
		if (distance >= length) {
			return new LatLon(lats[last], lons[last]);
		}
		int i = segmentIndex(distance);
		double segmentLength = cumulative[i + 1] - cumulative[i];
		double t = segmentLength <= 0 ? 0 : (distance - cumulative[i]) / segmentLength;
		return new LatLon(lats[i] + (lats[i + 1] - lats[i]) * t, lons[i] + (lons[i + 1] - lons[i]) * t);
	}

	/**
	 * Direction of travel in degrees (0 = north, 90 = east) at the given distance, measured over
	 * the next {@code lookahead} meters so that GPS jitter does not swing the camera.
	 */
	public double bearingAt(double distance, double lookahead) {
		if (length <= 0) {
			return 0;
		}
		double from;
		double to;
		if (lookahead >= length) {
			from = 0;
			to = length;
		} else {
			from = Math.max(0, Math.min(distance, length - lookahead));
			to = from + lookahead;
		}
		return bearing(positionAt(from), positionAt(to));
	}

	/**
	 * Projects a location onto the track.
	 *
	 * @return {distance along the track in meters, distance from the track in meters}
	 */
	public double[] project(double lat, double lon) {
		int n = lats.length;
		double cosLat = Math.cos(Math.toRadians(lat));
		if (n == 1) {
			return new double[] {0, distance(lat, lon, lats[0], lons[0])};
		}
		double bestAlong = 0;
		double bestDistance = Double.POSITIVE_INFINITY;
		for (int i = 0; i < n - 1; i++) {
			double ax = (lons[i] - lon) * cosLat * METERS_PER_DEGREE;
			double ay = (lats[i] - lat) * METERS_PER_DEGREE;
			double bx = (lons[i + 1] - lon) * cosLat * METERS_PER_DEGREE;
			double by = (lats[i + 1] - lat) * METERS_PER_DEGREE;
			double dx = bx - ax;
			double dy = by - ay;
			double segmentSquared = dx * dx + dy * dy;
			double t = segmentSquared <= 0 ? 0 : -(ax * dx + ay * dy) / segmentSquared;
			t = Math.max(0, Math.min(1, t));
			double cx = ax + dx * t;
			double cy = ay + dy * t;
			double d = Math.sqrt(cx * cx + cy * cy);
			if (d < bestDistance) {
				bestDistance = d;
				bestAlong = cumulative[i] + (cumulative[i + 1] - cumulative[i]) * t;
			}
		}
		return new double[] {bestAlong, bestDistance};
	}

	private int segmentIndex(double distance) {
		int lo = 0;
		int hi = lats.length - 2;
		while (lo < hi) {
			int mid = (lo + hi + 1) >>> 1;
			if (cumulative[mid] <= distance) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return lo;
	}

	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		double cosLat = Math.cos(Math.toRadians((lat1 + lat2) / 2));
		double dx = (lon2 - lon1) * cosLat * METERS_PER_DEGREE;
		double dy = (lat2 - lat1) * METERS_PER_DEGREE;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/** Bearing from {@code a} to {@code b} in degrees, 0 = north, 90 = east, in [0, 360). */
	public static double bearing(LatLon a, LatLon b) {
		double cosLat = Math.cos(Math.toRadians((a.getLatitude() + b.getLatitude()) / 2));
		double dx = (b.getLongitude() - a.getLongitude()) * cosLat;
		double dy = b.getLatitude() - a.getLatitude();
		if (dx == 0 && dy == 0) {
			return 0;
		}
		double degrees = Math.toDegrees(Math.atan2(dx, dy));
		return degrees < 0 ? degrees + 360 : degrees;
	}
}
