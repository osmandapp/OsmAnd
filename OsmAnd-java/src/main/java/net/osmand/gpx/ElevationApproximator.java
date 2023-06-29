package net.osmand.gpx;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class ElevationApproximator {

	private static final double CULLING_DISTANCE = 24.0;
	private static final double SLOPE_THRESHOLD = 20.0;

	private List<ApproxPoint> approxPoints = new ArrayList<>();

	public static class ApproxPoint {
		private final double lat;
		private final double lon;
		private double dist;
		private final double ele;

		public ApproxPoint(double lat, double lon, double dist, double ele) {
			this.lat = lat;
			this.lon = lon;
			this.dist = dist;
			this.ele = ele;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		public double getDist() {
			return dist;
		}

		public double getEle() {
			return ele;
		}
	}

	public ElevationApproximator() {
	}

	public abstract double getPointLatitude(int index);

	public abstract double getPointLongitude(int index);

	public abstract double getPointElevation(int index);

	public abstract int getPointsCount();

	public List<ApproxPoint> getApproxPoints() {
		return approxPoints;
	}

	public boolean approximate() {
		int pointsCount = getPointsCount();
		if (pointsCount < 4) {
			return false;
		}

		boolean[] survivor = new boolean[pointsCount];
		cullRamerDouglasPeucer(survivor, 0, pointsCount - 1);
		survivor[0] = true;
		List<ApproxPoint> survivedPoints = new ArrayList<>();
		ApproxPoint prevPoint = null;
		for (int i = 0; i < pointsCount; i++) {
			if (survivor[i]) {
				double lat = getPointLatitude(i);
				double lon = getPointLongitude(i);
				double distance = prevPoint != null ? MapUtils.getDistance(lat, lon, prevPoint.lat, prevPoint.lon) : 0;
				ApproxPoint pt = new ApproxPoint(lat, lon, distance, getPointElevation(i));
				survivedPoints.add(pt);
				prevPoint = pt;
			}
		}
		if (survivedPoints.size() < 4) {
			return false;
		}

		List<ApproxPoint> approxPoints = new ArrayList<>();
		ApproxPoint prevApproxPt = survivedPoints.get(0);
		for (int i = 1; i < survivedPoints.size() - 1; i++) {
			ApproxPoint pt = survivedPoints.get(i);
			ApproxPoint prevPt = survivedPoints.get(i - 1);
			ApproxPoint nextPt = survivedPoints.get(i + 1);
			double slopeA = (pt.ele - prevPt.ele) * 100 / Math.abs(pt.dist - prevPt.dist);
			double slopeB = (nextPt.ele - pt.ele) * 100 / Math.abs(nextPt.dist - pt.dist);
			if (Math.signum(slopeA) != Math.signum(slopeB)
					&& Math.abs(slopeA) > SLOPE_THRESHOLD && Math.abs(slopeB) > SLOPE_THRESHOLD) {
				continue;
			}
			pt.dist = MapUtils.getDistance(pt.lat, pt.lon, prevApproxPt.lat, prevApproxPt.lon);
			approxPoints.add(pt);
			prevApproxPt = pt;
		}
		this.approxPoints = approxPoints;
		return true;
	}

	private void cullRamerDouglasPeucer(boolean[] survivor, int start, int end) {
		double dmax = Double.NEGATIVE_INFINITY;
		int index = -1;

		double startLat = getPointLatitude(start);
		double startLon = getPointLongitude(start);
		double endLat = getPointLatitude(end);
		double endLon = getPointLongitude(end);

		for (int i = start + 1; i < end; i++) {
			double lat = getPointLatitude(i);
			double lon = getPointLongitude(i);
			double d = MapUtils.getOrthogonalDistance(lat, lon, startLat, startLon, endLat, endLon);
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if (dmax > CULLING_DISTANCE) {
			cullRamerDouglasPeucer(survivor, start, index);
			cullRamerDouglasPeucer(survivor, index, end);
		} else {
			survivor[end] = true;
		}
	}
}
