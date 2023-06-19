package net.osmand.gpx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.util.MapUtils;

public abstract class ElevationDiffsCalculator {

	private final int pointsCount;

//	public static final double DIST_THRESHOLD = 50.0;
//	public static final double ELE_TRESHOLD = 5;

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


	public static double getProjectionDist(double x, double y, double fromx, double fromy, double tox, double toy) {
		double mDist = (fromx - tox) * (fromx - tox) + (fromy - toy) * (fromy - toy);
		double projection = scalarMultiplication(fromx, fromy, tox, toy, x, y);
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
	
	private static double scalarMultiplication(double xA, double yA, double xB, double yB, double xC, double yC) {
		// Scalar multiplication between (AB, AC)
		return (xB - xA) * (xC - xA) + (yB - yA) * (yC - yA);
	}
	
	private static double ELE_THRESHOLD = 7 ;
	
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
		if(max != start) {
			points[max] = true;
			double md = getProjectionDist(getPointDistance(max), getPointElevation(max), 
					firstPointDist, firstPointEle, endPointDist, endPointEle);
			findMaximumExtremumBetween(start, max, points);
			findMaximumExtremumBetween(max, end, points);
		}
	}

	public void calculateElevationDiffs() {
//		double DIST_THRESHOLD = 50.0;
//		double ELE_TRESHOLD = 5;

		if (pointsCount < 2) {
			return;
		}
		boolean[] points = new boolean[pointsCount];
		points[0] = true;
		points[pointsCount - 1] = true;
		findMaximumExtremumBetween(0, pointsCount - 1, points);
//		int nextSearchExtremum = 0; // 0 undefined, 1 top, -1 bottom
//		double firstPointDist = getPointDistance(0);
//		double firstPointEle = getPointElevation(0);
//		List<Extremum> extremums = new ArrayList<>();
//		double prevExtremum = firstPointEle;
//		extremums.add(new Extremum(getPointElevation(0), getPointElevation(0)));
//
//		for (int i = 1; i < pointsCount; i++) {
//			double currDist = getPointDistance(i);
//			double currEle = getPointElevation(i);
//			boolean localMax = false;
//			boolean localMin = false;
//			if (nextSearchExtremum >= 0 && currEle - prevExtremum >= ELE_TRESHOLD) {
//				localMax = true;
//			} else if (nextSearchExtremum <= 0 && prevExtremum - currEle >= ELE_TRESHOLD) {
//				localMin = true;
//			}
//			if (localMax || localMin) {
//				// look ahead and check
//				for (int k = i; k < pointsCount; k++) {
//					if (Math.abs(getPointDistance(k) - currDist) >= DIST_THRESHOLD) {
//						break;
//					} else if (localMin && getPointElevation(k) < currEle) {
//						localMin = false;
//					} else if (localMax && getPointElevation(k) > currEle) {
//						localMax = false;
//					}
//				}
//				if (localMax || localMin) {
//					nextSearchExtremum = localMax ? -1 : 1;
//					prevExtremum = currEle;
//					extremums.add(new Extremum(currDist, currEle));
//				}
//			}
//		}
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
		final double[][] arr = new double[][] {
			{37.0413712,28.2378197,77.5},
			{37.0414185,28.2380685,79.0},
			{37.0414678,28.2382670,81.5},
			{37.0414806,28.2383931,82.3},
			{37.0414678,28.2389295,81.5},
			{37.0415256,28.2393962,85.0},
			{37.0415384,28.2396054,86.0},
			{37.0415256,28.2398763,85.8},
			{37.0415256,28.2402116,85.5},
			{37.0414678,28.2408714,84.8},
			{37.0414828,28.2410967,85.3},
			{37.0415234,28.2413006,86.3},
			{37.0415770,28.2414642,87.0},
			{37.0416540,28.2416171,88.0},
			{37.0417675,28.2418156,89.5},
			{37.0419837,28.2421052,93.0},
			{37.0421143,28.2423118,96.0},
			{37.0421893,28.2425585,97.0},
			{37.0422406,28.2429636,96.8},
			{37.0423177,28.2433257,98.0},
			{37.0424355,28.2435778,102.5},
			{37.0424547,28.2436931,103.0},
			{37.0424398,28.2438996,102.0},
			{37.0424269,28.2442537,100.8},
			{37.0424248,28.2445085,99.0},
			{37.0424098,28.2450530,92.3},
			{37.0424290,28.2451737,90.8},
			{37.0424761,28.2452971,90.3},
			{37.0426324,28.2454929,95.5},
			{37.0427780,28.2457101,103.0},
			{37.0428487,28.2458442,108.3},
			{37.0428701,28.2459140,110.3},
			{37.0428615,28.2459918,110.8},
			{37.0427523,28.2461956,109.3},
			{37.0426881,28.2463914,108.0},
			{37.0425768,28.2466033,106.5},
			{37.0423712,28.2468957,101.5},
			{37.0422813,28.2470700,98.5},
			{37.0422321,28.2473114,97.0},
			{37.0422299,28.2476601,98.5},
			{37.0421786,28.2479686,97.0},
			{37.0420951,28.2482636,95.3},
			{37.0420394,28.2485265,95.5},
			{37.0419131,28.2488269,90.8},
			{37.0418547,28.2491685,90.3}
		};
		final double[] dist = new double[arr.length];
		final double[] ele = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dist[i] = i == 0 ? 0
					: (dist[i - 1] + MapUtils.getDistance(arr[i][0], arr[i][1], arr[i - 1][0], arr[i - 1][1]));
			ele[i] = arr[i][2];
		}
		ElevationDiffsCalculator tst  = new ElevationDiffsCalculator() {
			
			@Override
			public int getPointsCount() {
				return ele.length;
			}
			
			@Override
			public double getPointElevation(int index) {
				return ele[index];
			}
			
			@Override
			public double getPointDistance(int index) {
				return dist[index];
			}
		};
		tst.calculateElevationDiffs();
		for(Extremum e: tst.getExtremums()) {
			System.out.println(String.format("%.2f %.2f", e.dist, e.ele));
		}
	}
}