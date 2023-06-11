package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.WptPt;

public abstract class ElevationDiffsCalculator {

	private final int pointsCount;

	private int currIndex;
	private double diffElevationUp = 0;
	private double diffElevationDown = 0;

	public ElevationDiffsCalculator() {
		this.pointsCount = getPointsCount();
	}

	public abstract double getElevation(int index);

	public abstract int getPointsCount();

	public double getDiffElevationUp() {
		return diffElevationUp;
	}

	public double getDiffElevationDown() {
		return diffElevationDown;
	}

	public void calculateElevationDiffs() {
		currIndex = 0;
		double initialElevation = getNextElevation();
		if (Double.isNaN(initialElevation)) {
			return;
		}
		double nextInitialElevation = getNextElevation();
		if (Double.isNaN(nextInitialElevation)) {
			return;
		}
		double prevElevation = nextInitialElevation;
		double eleDiffSumm = nextInitialElevation - initialElevation;
		double elevation = getNextElevation();
		while (!Double.isNaN(elevation)) {
			double eleDiff = elevation - prevElevation;
			if (Math.abs(eleDiffSumm + eleDiff) >= Math.abs(eleDiffSumm)) {
				eleDiffSumm += eleDiff;
			} else {
				takeDiffs(eleDiffSumm);
				eleDiffSumm = eleDiff;
			}
			prevElevation = elevation;
			elevation = getNextElevation();
		}
		takeDiffs(eleDiffSumm);
	}

	private void takeDiffs(double eleDiffSumm) {
		if (eleDiffSumm > 0) {
			diffElevationUp += eleDiffSumm;
		} else {
			diffElevationDown -= eleDiffSumm;
		}
	}

	private double getNextElevation() {
		while (currIndex < pointsCount) {
			double ele = getElevation(currIndex);
			currIndex++;
			if (!Double.isNaN(ele)) {
				return ele;
			}
		}
		return Double.NaN;
	}
}