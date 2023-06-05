package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;

class SplitSegment {

	TrkSegment segment;
	double startCoeff = 0;
	int startPointInd;
	double endCoeff = 0;
	int endPointInd;
	double metricEnd;
	double secondaryMetricEnd;

	public SplitSegment(TrkSegment segment) {
		startPointInd = 0;
		startCoeff = 0;
		endPointInd = segment.points.size() - 2;
		endCoeff = 1;
		this.segment = segment;
	}

	public SplitSegment(int startInd, int endInd, TrkSegment segment) {
		startPointInd = startInd;
		startCoeff = 0;
		endPointInd = endInd - 2;
		endCoeff = 1;
		this.segment = segment;
	}

	public SplitSegment(TrkSegment segment, int pointInd, double cf) {
		this.segment = segment;
		this.startPointInd = pointInd;
		this.startCoeff = cf;
	}

	public int getNumberOfPoints() {
		return endPointInd - startPointInd + 2;
	}

	public WptPt get(int j) {
		final int ind = j + startPointInd;
		if (j == 0) {
			if (startCoeff == 0) {
				return segment.points.get(ind);
			}
			return approx(segment.points.get(ind), segment.points.get(ind + 1), startCoeff);
		}
		if (j == getNumberOfPoints() - 1) {
			if (endCoeff == 1) {
				return segment.points.get(ind);
			}
			return approx(segment.points.get(ind - 1), segment.points.get(ind), endCoeff);
		}
		return segment.points.get(ind);
	}

	private WptPt approx(WptPt w1, WptPt w2, double cf) {
		long time = value(w1.time, w2.time, 0, cf);
		double speed = value(w1.speed, w2.speed, 0, cf);
		double ele = value(w1.ele, w2.ele, 0, cf);
		double hdop = value(w1.hdop, w2.hdop, 0, cf);
		double lat = value(w1.lat, w2.lat, -360, cf);
		double lon = value(w1.lon, w2.lon, -360, cf);
		return new WptPt(lat, lon, time, ele, speed, hdop);
	}

	private double value(double vl, double vl2, double none, double cf) {
		if (vl == none || Double.isNaN(vl)) {
			return vl2;
		} else if (vl2 == none || Double.isNaN(vl2)) {
			return vl;
		}
		return vl + cf * (vl2 - vl);
	}

	private long value(long vl, long vl2, long none, double cf) {
		if (vl == none) {
			return vl2;
		} else if (vl2 == none) {
			return vl;
		}
		return vl + ((long) (cf * (vl2 - vl)));
	}

	public double setLastPoint(int pointInd, double endCf) {
		endCoeff = endCf;
		endPointInd = pointInd;
		return endCoeff;
	}
}