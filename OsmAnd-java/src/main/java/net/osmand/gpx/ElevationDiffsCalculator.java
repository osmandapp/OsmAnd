package net.osmand.gpx;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.MapUtils;

public abstract class ElevationDiffsCalculator {

	private static double ELE_THRESHOLD = 7;

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

	private static double getProjectionDist(double x, double y, double fromx, double fromy, double tox, double toy) {
		double mDist = (fromx - tox) * (fromx - tox) + (fromy - toy) * (fromy - toy);
		double projection = MapUtils.scalarMultiplication(fromx, fromy, tox, toy, x, y);
		double prx;
		double pry;
		if (projection < 0) {
			prx = fromx;
			pry = fromy;
		} else if (projection >= mDist) {
			prx = tox;
			pry = toy;
		} else {
			prx = fromx + (tox - fromx) * (projection / mDist);
			pry = fromy + (toy - fromy) * (projection / mDist);
		}
		return Math.sqrt((prx - x) * (prx - x) + (pry - y) * (pry - y));
	}

	private void findMaximumExtremumBetween(int start, int end, boolean[] points) {
		double firstPointDist = getPointDistance(start);
		double firstPointEle = getPointElevation(start);
		double endPointEle = getPointElevation(end);
		double endPointDist = getPointDistance(end);
		int max = start;
		double maxDiff = ELE_THRESHOLD;
		for (int i = start + 1; i < end; i++) {
			double md = getProjectionDist(getPointDistance(i), getPointElevation(i),
					firstPointDist, firstPointEle, endPointDist, endPointEle);
			if (md > maxDiff) {
				max = i;
				maxDiff = md;
			}
		}
		if (max != start) {
			points[max] = true;
			findMaximumExtremumBetween(start, max, points);
			findMaximumExtremumBetween(max, end, points);
		}
	}

	public void calculateElevationDiffs() {
		int pointsCount = getPointsCount();
		if (pointsCount < 2) {
			return;
		}
		boolean[] points = new boolean[pointsCount];
		points[0] = true;
		points[pointsCount - 1] = true;
		findMaximumExtremumBetween(0, pointsCount - 1, points);

		this.extremums = new ArrayList<>();
		for (int i = 0; i < points.length; i++) {
			if (points[i]) {
				extremums.add(new Extremum(getPointDistance(i), getPointElevation(i)));
			}
		}

		for (int i = 1; i < extremums.size(); i++) {
			double prevElevation = extremums.get(i - 1).ele;
			double elevation = extremums.get(i).ele;
			double eleDiffSumm = elevation - prevElevation;
			if (eleDiffSumm > 0) {
				diffElevationUp += eleDiffSumm;
			} else {
				diffElevationDown -= eleDiffSumm;
			}
		}
	}

	public static void main(String[] args) {
		GPXFile gpxFile = GPXUtilities.loadGPXFile(new File("/Users/crimean/Downloads/2011-09-27_Mulhacen.gpx"));
		List<WptPt> points = gpxFile.tracks.get(0).segments.get(0).points;
		calculateDiffs(points);
		int start = 200;
		for (int i = start + 150; i < start + 160; i++) {
			calculateDiffs(points.subList(start, i));
		}
	}

	private static void calculateDiffs(final List<WptPt> points) {
		ElevationApproximator approximator = new ElevationApproximator() {
			@Override
			public double getPointLatitude(int index) {
				return points.get(index).lat;
			}

			@Override
			public double getPointLongitude(int index) {
				return points.get(index).lon;
			}

			@Override
			public double getPointElevation(int index) {
				return points.get(index).ele;
			}

			@Override
			public int getPointsCount() {
				return points.size();
			}
		};
		approximator.approximate();
		final double[] distances = approximator.getDistances();
		final double[] elevations = approximator.getElevations();

		if (distances != null && elevations != null) {
			double diffElevationUp = 0;
			double diffElevationDown = 0;
			ElevationDiffsCalculator elevationDiffsCalc = new ElevationDiffsCalculator() {
				@Override
				public double getPointDistance(int index) {
					return distances[index];
				}

				@Override
				public double getPointElevation(int index) {
					return elevations[index];
				}

				@Override
				public int getPointsCount() {
					return distances.length;
				}
			};
			elevationDiffsCalc.calculateElevationDiffs();
			diffElevationUp += elevationDiffsCalc.getDiffElevationUp();
			diffElevationDown += elevationDiffsCalc.getDiffElevationDown();

			System.out.println("GPX points=" + points.size() + " approx points=" + distances.length
					+ " diffUp=" + diffElevationUp + " diffDown=" + diffElevationDown);
		}
	}
}