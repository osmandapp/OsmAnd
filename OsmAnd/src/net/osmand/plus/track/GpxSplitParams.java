package net.osmand.plus.track;

import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_JOIN_SEGMENTS;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SPLIT_INTERVAL;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SPLIT_TYPE;

import androidx.annotation.NonNull;

import net.osmand.plus.track.helpers.GpxData;
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
		GpxData gpxData = dataItem.getGpxData();
		splitType = GpxSplitType.getSplitTypeByTypeId(gpxData.getValue(GPX_COL_SPLIT_TYPE));
		splitInterval = gpxData.getValue(GPX_COL_SPLIT_INTERVAL);
		joinSegments = gpxData.getValue(GPX_COL_JOIN_SEGMENTS);
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
