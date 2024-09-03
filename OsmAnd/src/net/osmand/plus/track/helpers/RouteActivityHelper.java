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

public class RouteActivityHelper {

	private static final Log LOG = PlatformUtil.getLog(RouteActivityHelper.class);

	private static final String ROUTE_ACTIVITIES_FILE = "activities.json";

	private final List<RouteActivityGroup> cachedGroups = new ArrayList<>();
	private final List<RouteActivity> cachedActivities = new ArrayList<>();
	private OnResultCallback<RouteActivity> activitySelectionListener;
	private RouteActivity selectedActivity;

	public RouteActivityHelper(@NonNull OsmandApplication app) {
		collectRouteActivities(app);
	}

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

	@NonNull
	public List<RouteActivityGroup> getActivityGroups() {
		return cachedGroups;
	}

	@NonNull
	public List<RouteActivity> getActivities() {
		return cachedActivities;
	}

	private void collectRouteActivities(@NonNull OsmandApplication app) {
		try {
			readActivitiesFromJson(app);
		} catch (JSONException e) {
			LOG.error("Failed to read activities from JSON", e);
		}
	}

	private void readActivitiesFromJson(@NonNull OsmandApplication app) throws JSONException {
		String activitiesJsonStr = null;
		try {
			InputStream is = app.getAssets().open(ROUTE_ACTIVITIES_FILE);
			activitiesJsonStr = Algorithms.readFromInputStream(is).toString();
		} catch (IOException e) {
			LOG.error("Failed to read activities source file", e);
		}
		if (Algorithms.isEmpty(activitiesJsonStr)) return;

		JSONObject json = new JSONObject(activitiesJsonStr);
		JSONArray groupsArray = json.getJSONArray("groups");
		for (int i = 0; i < groupsArray.length(); i++) {
			JSONObject groupJson = groupsArray.getJSONObject(i);
			String id = groupJson.getString("id");
			String label = groupJson.getString("label");
			JSONArray activitiesJson = groupJson.getJSONArray("activities");

			List<RouteActivity> activities = new ArrayList<>();
			for (int j = 0; j < activitiesJson.length(); j++) {
				JSONObject activityJson = activitiesJson.getJSONObject(j);
				String activityId = activityJson.getString("id");
				String activityLabel = activityJson.getString("label");
				String iconName = activityJson.getString("icon_name");
				RouteActivity activity = new RouteActivity(activityId, activityLabel, iconName);
				cachedActivities.add(activity);
				activities.add(activity);
			}
			cachedGroups.add(new RouteActivityGroup(id, label, activities));
		}
	}
}
