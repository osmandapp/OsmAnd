package net.osmand.plus.search.history;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.util.Algorithms;

public class HistoryEntry {

	private static final int[] DEF_INTERVALS_MIN = {
			5, 60, 60 * 24, 5 * 60 * 24, 10 * 60 * 24, 30 * 60 * 24
	};

	private final double lat;
	private final double lon;
	private final PointDescription name;
	private final HistorySource source;

	private long lastAccessedTime;
	private int[] intervals = new int[0];
	private double[] intervalValues = new double[0];


	public HistoryEntry(double lat, double lon, @NonNull PointDescription name,
			@NonNull HistorySource source) {
		this.lat = lat;
		this.lon = lon;
		this.name = name;
		this.source = source;
	}

	private double rankFunction(double cf, double timeDiff) {
		if (timeDiff <= 0) {
			return 0;
		}
		return cf / timeDiff;
	}

	public double getRank(long time) {
		double baseTimeDiff = ((time - lastAccessedTime) / 1000) + 1;
		double timeDiff = 0;
		double vl = 1;
		double rnk = rankFunction(vl, baseTimeDiff + timeDiff);
		for (int k = 0; k < intervals.length; k++) {
			double ntimeDiff = intervals[k] * 60 * 1000;
			double nvl = intervalValues[k];
			if (ntimeDiff < timeDiff || nvl <= vl) {
				continue;
			}
			rnk += rankFunction(nvl - vl, baseTimeDiff + (ntimeDiff - timeDiff) / 2 + timeDiff);
			vl = nvl - vl;
			timeDiff = ntimeDiff;
		}
		return rnk;
	}

	@NonNull
	public PointDescription getName() {
		return name;
	}

	public String getSerializedName() {
		return PointDescription.serializeToString(name);
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	@NonNull
	public HistorySource getSource() {
		return source;
	}

	public void markAsAccessed(long time) {
		int[] nintervals = new int[DEF_INTERVALS_MIN.length];
		double[] nintervalValues = new double[DEF_INTERVALS_MIN.length];
		for (int k = 0; k < nintervals.length; k++) {
			nintervals[k] = DEF_INTERVALS_MIN[k];
			nintervalValues[k] = getUsageLastTime(time, 0, 0, nintervals[k]) + 1;
		}
		intervals = nintervals;
		intervalValues = nintervalValues;
		lastAccessedTime = time;
	}

	double getUsageLastTime(long time, int days, int hours, int minutes) {
		long mins = (minutes + (hours + 24 * days) * 60);
		long timeInPast = time - mins * 60 * 1000;
		if (lastAccessedTime <= timeInPast) {
			return 0;
		}
		double res = 0;
		for (int k = 0; k < intervals.length; k++) {
			long intPast = intervals[k] * 60 * 1000;
			if (intPast > 0) {
				double r;
				if (lastAccessedTime - timeInPast >= intPast) {
					r = intervalValues[k];
				} else {
					r = intervalValues[k] * ((double) lastAccessedTime - timeInPast) / ((double) intPast);
				}
				res = Math.max(res, r);
			}
		}
		return res;
	}

	public void setFrequency(String intervalsString, String values) {
		if (Algorithms.isEmpty(intervalsString) || Algorithms.isEmpty(values)) {
			markAsAccessed(lastAccessedTime);
			return;
		}
		String[] ints = intervalsString.split(",");
		String[] vsl = values.split(",");
		intervals = new int[ints.length];
		intervalValues = new double[ints.length];
		try {
			for (int i = 0; i < ints.length && i < vsl.length; i++) {
				intervals[i] = Integer.parseInt(ints[i]);
				intervalValues[i] = Double.parseDouble(vsl[i]);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	public long getLastAccessTime() {
		return lastAccessedTime;
	}

	@NonNull
	public String getIntervalsValues() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < intervalValues.length; i++) {
			if (i > 0) {
				builder.append(",");
			}
			builder.append(intervalValues[i]);
		}
		return builder.toString();
	}

	@NonNull
	public String getIntervals() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < intervals.length; i++) {
			if (i > 0) {
				builder.append(",");
			}
			builder.append(intervals[i]);
		}
		return builder.toString();
	}

	public void setLastAccessTime(long time) {
		lastAccessedTime = time;
	}
}
