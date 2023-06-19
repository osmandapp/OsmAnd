package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.WptPt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ElevationDiffsCalculator {

	private final int pointsCount;

	private double diffElevationUp = 0;
	private double diffElevationDown = 0;
	private List<Extremum> extremums = new ArrayList<>();

	public static class Extremum {
		private final double dist;
		private final double ele;

		public Extremum(double dist, double ele) {
			this.dist = dist;
			this.ele = ele;
		}

		public double getDist() {
			return dist;
		}

		public double getEle() {
			return ele;
		}
	}

	public ElevationDiffsCalculator() {
		this.pointsCount = getPointsCount();
	}

	public abstract double getPointDistance(int index);

	public abstract double getPointElevation(int index);

	public abstract int getPointsCount();

	public double getDiffElevationUp() {
		return diffElevationUp;
	}

	public double getDiffElevationDown() {
		return diffElevationDown;
	}

	public List<Extremum> getExtremums() {
		return Collections.unmodifiableList(extremums);
	}

	public void calculateElevationDiffs() {
		double DIST_THRESHOLD = 50.0;
		double ELE_TRESHOLD = 2.0;

		if (pointsCount < 2) {
			return;
		}
		double firstPointDist = getPointDistance(0);
		double firstPointEle = getPointElevation(0);
		double lastPointDist = getPointDistance(pointsCount - 1);
		double lastPointEle = getPointElevation(pointsCount - 1);
		double totalDistance = Math.abs(lastPointDist - firstPointDist);
		List<Extremum> extremums = new ArrayList<>();
		extremums.add(new Extremum(firstPointDist, firstPointEle));
		for (int i = 1; i < pointsCount; i++) {
			double currDist = getPointDistance(i);
			double currEle = getPointElevation(i);
			int k = i;
			boolean top = currEle > getPointElevation(k - 1);
			while (k > 0 && Math.abs(currDist - getPointDistance(--k)) < DIST_THRESHOLD) {
				double diff = currEle - getPointElevation(k);
				if (top && diff < 0 || !top && diff > 0) {
					// not extremum
					k = -1;
					break;
				}
			}
			if (i == k + 1) {
				double diff = currEle - getPointElevation(k);
				if (top && diff < 0 || !top && diff > 0) {
					// not extremum
					k = -1;
				}
			}
			// if not extremum - skip it
			if (k == -1 || (/*currDist - getPointDistance(k) < DIST_THRESHOLD
					&& */Math.abs(currEle - getPointElevation(k)) < ELE_TRESHOLD)) {
				continue;
			}
			k = i;
			while (k < pointsCount - 1 && Math.abs(getPointDistance(++k) - currDist) < DIST_THRESHOLD) {
				double diff = currEle - getPointElevation(k);
				if (top && diff < 0 || !top && diff > 0) {
					// not extremum
					k = -1;
					break;
				}
			}
			if (i == k - 1) {
				double diff = currEle - getPointElevation(k);
				if (top && diff < 0 || !top && diff > 0) {
					// not extremum
					k = -1;
				}
			}
			// if extremum - save it
			if (k != -1 && (/*getPointDistance(k) - currDist > DIST_THRESHOLD
					|| */Math.abs(currEle - getPointElevation(k)) > ELE_TRESHOLD)) {
				extremums.add(new Extremum(currDist, currEle));
			}
		}
		extremums.add(new Extremum(lastPointDist, lastPointEle));
		this.extremums = extremums;

		double eleDiffSumm = 0;
		for (int i = 1; i < extremums.size(); i++) {
			double prevElevation = extremums.get(i - 1).ele;
			double elevation = extremums.get(i).ele;
			double eleDiff = elevation - prevElevation;
			if (eleDiffSumm == 0 || eleDiffSumm > 0 && eleDiff > 0) {
				eleDiffSumm += eleDiff;
			} else {
				takeDiffs(eleDiffSumm);
				eleDiffSumm = eleDiff;
			}
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
}