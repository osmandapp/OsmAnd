package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.WptPt;

public abstract class ElevationDiffsCalculator {

	public static final double CALCULATED_GPX_WINDOW_LENGTH = 10d;

	private double windowLength;
	private final int startIndex;
	private final int numberOfPoints;

	private double diffElevationUp = 0;
	private double diffElevationDown = 0;

	public ElevationDiffsCalculator(int startIndex, int numberOfPoints) {
		this.startIndex = startIndex;
		this.numberOfPoints = numberOfPoints;
		WptPt lastPoint = getPoint(startIndex + numberOfPoints - 1);
		this.windowLength = lastPoint.time == 0 ? CALCULATED_GPX_WINDOW_LENGTH : Math.max(20d, lastPoint.distance / numberOfPoints * 4);
	}

	public ElevationDiffsCalculator(double windowLength, int startIndex, int numberOfPoints) {
		this(startIndex, numberOfPoints);
		this.windowLength = windowLength;
	}

	public abstract WptPt getPoint(int index);

	public double getDiffElevationUp() {
		return diffElevationUp;
	}

	public double getDiffElevationDown() {
		return diffElevationDown;
	}

	public void calculateElevationDiffs() {
		WptPt initialPoint = getPoint(startIndex);
		double eleSumm = initialPoint.ele;
		double prevEle = initialPoint.ele;
		int pointsCount = Double.isNaN(eleSumm) ? 0 : 1;
		double eleAvg = Double.NaN;
		double nextWindowPos = initialPoint.distance + windowLength;
		int pointIndex = startIndex + 1;
		while (pointIndex < numberOfPoints + startIndex) {
			WptPt point = getPoint(pointIndex);
			if (point.distance > nextWindowPos) {
				eleAvg = calcAvg(eleSumm, pointsCount, eleAvg);
				if (!Double.isNaN(point.ele)) {
					eleSumm = point.ele;
					prevEle = point.ele;
					pointsCount = 1;
				} else if (!Double.isNaN(prevEle)) {
					eleSumm = prevEle;
					pointsCount = 1;
				} else {
					eleSumm = Double.NaN;
					pointsCount = 0;
				}
				while (nextWindowPos < point.distance) {
					nextWindowPos += windowLength;
				}
			} else {
				if (!Double.isNaN(point.ele)) {
					eleSumm += point.ele;
					prevEle = point.ele;
					pointsCount++;
				} else if (!Double.isNaN(prevEle)) {
					eleSumm += prevEle;
					pointsCount++;
				}
			}
			pointIndex++;
		}
		if (pointsCount > 1) {
			calcAvg(eleSumm, pointsCount, eleAvg);
		}
		diffElevationUp = Math.round(diffElevationUp + 0.3f);
	}

	private double calcAvg(double eleSumm, int pointsCount, double eleAvg) {
		if (Double.isNaN(eleSumm) || pointsCount == 0) {
			return Double.NaN;
		}
		double avg = eleSumm / pointsCount;
		if (!Double.isNaN(eleAvg)) {
			double diff = avg - eleAvg;
			if (diff > 0) {
				diffElevationUp += diff;
			} else {
				diffElevationDown -= diff;
			}
		}
		return avg;
	}
}
