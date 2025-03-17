package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum RouteInfoDisplayMode {
	ARRIVAL_TIME(R.drawable.ic_action_clock, R.string.side_marker_eta),
	TIME_TO_GO(R.drawable.ic_action_timer, R.string.map_widget_time),
	DISTANCE(R.drawable.ic_action_distance, R.string.map_widget_distance);

	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;

	RouteInfoDisplayMode(@DrawableRes int iconId, @StringRes int titleId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public static RouteInfoDisplayMode[] values(@NonNull RouteInfoDisplayMode primary) {
		return switch (primary) {
			case ARRIVAL_TIME -> new RouteInfoDisplayMode[]{ARRIVAL_TIME, DISTANCE, TIME_TO_GO};
			case TIME_TO_GO -> new RouteInfoDisplayMode[]{TIME_TO_GO, DISTANCE, ARRIVAL_TIME};
			case DISTANCE -> new RouteInfoDisplayMode[]{DISTANCE, ARRIVAL_TIME, TIME_TO_GO};
			default -> throw new IllegalArgumentException("Unexpected value: " + primary);
		};
	}
}
