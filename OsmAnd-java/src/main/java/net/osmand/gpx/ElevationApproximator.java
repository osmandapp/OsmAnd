package net.osmand.gpx;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class ElevationApproximator {

	private static final double SLOPE_THRESHOLD = 70.0;

	private double[] distances;
	private double[] elevations;

	public ElevationApproximator() {
	}

	public abstract double getPointLatitude(int index);

	public abstract double getPointLongitude(int index);

	public abstract double getPointElevation(int index);

	public abstract int getPointsCount();

	public double[] getDistances() {
		return distances;
	}

	public double[] getElevations() {
		return elevations;
	}

	public boolean approximate() {
		int pointsCount = getPointsCount();
		if (pointsCount < 4) {
			return false;
		}

		boolean[] survived = new boolean[pointsCount];
		int lastSurvived = 0;
		int survidedCount = 0;
		for (int i = 1; i < pointsCount - 1; i++) {
			double prevEle = getPointElevation(lastSurvived);
			double ele = getPointElevation(i);
			double eleNext = getPointElevation(i + 1);
			if ((ele - prevEle) * (eleNext - ele) > 0) {
				survived[i] = true;
				lastSurvived = i;
				survidedCount++;
			}
		}
		if (survidedCount < 2) {
			return false;
		}

		lastSurvived = 0;
		survidedCount = 0;
		for (int i = 1; i < pointsCount - 1; i++) {
			if (!survived[i]) {
				continue;
			}
			double ele = getPointElevation(i);
			double prevEle = getPointElevation(lastSurvived);
			double dist = MapUtils.getDistance(getPointLatitude(i), getPointLongitude(i),
					getPointLatitude(lastSurvived), getPointLongitude(lastSurvived));
			double slope = (ele - prevEle) * 100 / dist;
			if (Math.abs(slope) > SLOPE_THRESHOLD) {
				survived[i] = false;
				continue;
			}
			lastSurvived = i;
			survidedCount++;
		}
		if (survidedCount < 2) {
			return false;
		}
		survived[0] = true;
		survived[pointsCount - 1] = true;
		double[] distances = new double[survidedCount + 2];
		double[] elevations = new double[survidedCount + 2];
		int k = 0;
		lastSurvived = 0;
		for (int i = 0; i < pointsCount; i++) {
			if (!survived[i] ) {
				continue;
			}
			distances[k] = lastSurvived == 0 ? 0 :
					MapUtils.getDistance(getPointLatitude(i), getPointLongitude(i),
					getPointLatitude(lastSurvived), getPointLongitude(lastSurvived));
			elevations[k] = getPointElevation(i);
			k++;
			lastSurvived = i;
		}
		this.distances = distances;
		this.elevations = elevations;
		return true;
	}
}
