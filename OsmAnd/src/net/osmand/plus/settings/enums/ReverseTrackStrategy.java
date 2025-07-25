package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ReverseTrackStrategy {

	USE_ORIGINAL_GPX(R.string.reverse_mode_original_track, R.string.reverse_mode_original_track_desc),
	RECALCULATE_ALL_ROUTE_POINTS(R.string.reverse_mode_calculate_route, R.string.reverse_mode_calculate_route_desc),
	RECALCULATE_FROM_CLOSEST_ROUTE_POINT(); // faster but less exact

	private final int titleId;
	private final int summaryId;

	ReverseTrackStrategy() {
		this(-1, -1);
	}

	ReverseTrackStrategy(@StringRes int titleId, @StringRes int summaryId) {
		this.titleId = titleId;
		this.summaryId = summaryId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getSummaryId() {
		return summaryId;
	}
}
