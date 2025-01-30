package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum RouteInfoDisplayMode {
	ARRIVAL_TIME(R.string.side_marker_eta),
	TIME_TO_GO(R.string.map_widget_time),
	DISTANCE(R.string.map_widget_distance);

	@StringRes
	private final int titleId;

	RouteInfoDisplayMode(int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public static RouteInfoDisplayMode[] values(@NonNull RouteInfoDisplayMode primary) {
		RouteInfoDisplayMode[] values = values();
		RouteInfoDisplayMode[] ordered = new RouteInfoDisplayMode[values.length];
		ordered[0] = primary;

		int index = 1;
		for (RouteInfoDisplayMode mode : values) {
			if (mode != primary) {
				ordered[index++] = mode;
			}
		}
		return ordered;
	}
}
