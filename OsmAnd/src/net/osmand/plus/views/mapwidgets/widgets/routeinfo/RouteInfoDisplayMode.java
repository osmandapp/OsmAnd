package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum RouteInfoDisplayMode {
	ARRIVAL_TIME(R.string.side_marker_eta),
	TIME_TO_GO(R.string.map_widget_time),
	DISTANCE(R.string.map_widget_route_information_distance_mode);

	@StringRes
	private final int titleId;

	RouteInfoDisplayMode(int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}
}
