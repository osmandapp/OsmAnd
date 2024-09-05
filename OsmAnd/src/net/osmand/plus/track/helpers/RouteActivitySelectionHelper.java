package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.RouteActivityGroup;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
