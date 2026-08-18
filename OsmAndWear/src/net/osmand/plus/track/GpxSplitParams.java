package net.osmand.plus.track;

import static net.osmand.shared.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.shared.gpx.GpxDataItem;
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

	public GpxSplitParams(@NonNull OsmandApplication app, @NonNull GpxDataItem item) {
		GpxAppearanceHelper helper = new GpxAppearanceHelper(app);
		splitType = GpxSplitType.getSplitTypeByTypeId(helper.getParameter(item, SPLIT_TYPE));
		splitInterval = helper.getParameter(item, SPLIT_INTERVAL);
		joinSegments = helper.getParameter(item, JOIN_SEGMENTS);
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
