package net.osmand.plus.track;

import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.gpx.GpxParameter.SPLIT_TYPE;

import androidx.annotation.NonNull;

import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.util.Algorithms;

public class GpxSplitParams {

	public final GpxSplitType splitType;
	public final double splitInterval;
	public final boolean joinSegments;

	public GpxSplitParams(@NonNull GpxSplitType splitType, double splitInterval, boolean joinSegments) {
		this.splitType = splitType;
		this.splitInterval = splitInterval;
		this.joinSegments = joinSegments;
	}

	public GpxSplitParams(@NonNull GpxDataItem dataItem) {
		splitType = GpxSplitType.getSplitTypeByTypeId(dataItem.getParameter(SPLIT_TYPE));
		splitInterval = dataItem.getParameter(SPLIT_INTERVAL);
		joinSegments = dataItem.getParameter(JOIN_SEGMENTS);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(splitType, splitInterval, joinSegments);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		GpxSplitParams params = (GpxSplitParams) obj;
		return Double.compare(params.splitInterval, splitInterval) == 0 && joinSegments == params.joinSegments && splitType == params.splitType;
	}

	@Override
	public String toString() {
		return "GpxSplitParams{" +
				"splitType=" + splitType +
				", splitInterval=" + splitInterval +
				", joinSegments=" + joinSegments +
				'}';
	}
}
