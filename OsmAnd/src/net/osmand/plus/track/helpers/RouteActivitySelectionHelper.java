package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.shared.gpx.primitives.RouteActivity;

public class RouteActivitySelectionHelper {

	private OnResultCallback<RouteActivity> activitySelectionListener;
	private RouteActivity selectedActivity;

	public void setActivitySelectionListener(@NonNull OnResultCallback<RouteActivity> listener) {
		this.activitySelectionListener = listener;
	}

	public void setSelectedActivity(@Nullable RouteActivity routeActivity) {
		this.selectedActivity = routeActivity;
	}

	public void onSelectRouteActivity(@Nullable RouteActivity activity) {
		setSelectedActivity(activity);
		if (activitySelectionListener != null) {
			activitySelectionListener.onResult(activity);
		}
	}

	@Nullable
	public RouteActivity getSelectedActivity() {
		return selectedActivity;
	}
}
