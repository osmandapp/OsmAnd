package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;

import java.util.List;

abstract class SplitMetric {

	public abstract double metric(WptPt p1, WptPt p2);


	static class DistanceSplitMetric extends SplitMetric {

		private final float[] calculations = new float[1];

		@Override
		public double metric(WptPt p1, WptPt p2) {
			net.osmand.Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, calculations);
			return calculations[0];
		}
	}

	static class TimeSplitMetric extends SplitMetric {

		@Override
		public double metric(WptPt p1, WptPt p2) {
			if (p1.time != 0 && p2.time != 0) {
				return (int) Math.abs((p2.time - p1.time) / 1000l);
			}
			return 0;
		}
	}

	static void splitSegment(SplitMetric metric, SplitMetric secondaryMetric, double metricLimit, List<SplitSegment> splitSegments, TrkSegment segment, boolean joinSegments) {
		double currentMetricEnd = metricLimit;
		double secondaryMetricEnd = 0;
		SplitSegment sp = new SplitSegment(segment, 0, 0);
		double total = 0;
		WptPt prev = null;
		for (int k = 0; k < segment.points.size(); k++) {
			WptPt point = segment.points.get(k);
			if (k > 0) {
				double currentSegment = 0;
				if (!(segment.generalSegment && !joinSegments && point.firstPoint)) {
					currentSegment = metric.metric(prev, point);
					secondaryMetricEnd += secondaryMetric.metric(prev, point);
				}
				while (total + currentSegment > currentMetricEnd) {
					double p = currentMetricEnd - total;
					double cf = (p / currentSegment);
					sp.setLastPoint(k - 1, cf);
					sp.metricEnd = currentMetricEnd;
					sp.secondaryMetricEnd = secondaryMetricEnd;
					splitSegments.add(sp);

					sp = new SplitSegment(segment, k - 1, cf);
					currentMetricEnd += metricLimit;
				}
				total += currentSegment;
			}
			prev = point;
		}
		if (segment.points.size() > 0 && !(sp.endPointInd == segment.points.size() - 1 && sp.startCoeff == 1)) {
			sp.metricEnd = total;
			sp.secondaryMetricEnd = secondaryMetricEnd;
			sp.setLastPoint(segment.points.size() - 2, 1);
			splitSegments.add(sp);
		}
	}
}