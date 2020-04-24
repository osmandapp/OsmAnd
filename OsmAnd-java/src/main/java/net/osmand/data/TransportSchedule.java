package net.osmand.data;

import gnu.trove.list.array.TIntArrayList;

public class TransportSchedule {

	
	public TIntArrayList tripIntervals = new TIntArrayList();
	public TIntArrayList avgStopIntervals = new TIntArrayList();
	public TIntArrayList avgWaitIntervals = new TIntArrayList();

	public TransportSchedule() {
	}

	public TransportSchedule(TIntArrayList tripIntervals, TIntArrayList avgStopIntervals, TIntArrayList avgWaitIntervals) {
		this.tripIntervals = tripIntervals;
		this.avgStopIntervals = avgStopIntervals;
		this.avgWaitIntervals = avgWaitIntervals;
	}

	public int[] getTripIntervals() {
		return tripIntervals.toArray();
	}

	public int[] getAvgStopIntervals() {
		return avgStopIntervals.toArray();
	}

	public int[] getAvgWaitIntervals() {
		return avgWaitIntervals.toArray();
	}

	public boolean compareSchedule(TransportSchedule thatObj) {
		if (this == thatObj) {
			return true;
		} else {
			return tripIntervals.equals(thatObj.tripIntervals) &&
					avgStopIntervals.equals(thatObj.avgStopIntervals) &&
					avgWaitIntervals.equals(thatObj.avgWaitIntervals);
		}
	}
}
