package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.RouteActivityGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteActivitySelectionHelper {

	private static final String SOURCE_FILE_NAME = "activities.json";

	private final List<RouteActivityGroup> cachedGroups = new ArrayList<>();
	private List<RouteActivity> cachedActivities;
	private OnResultCallback<RouteActivity> activitySelectionListener;
	private RouteActivity selectedRouteActivity;

	public RouteActivitySelectionHelper() {
		readAvailableRouteActivities();
	}

	public void setActivitySelectionListener(@NonNull OnResultCallback<RouteActivity> listener) {
		this.activitySelectionListener = listener;
	}

	public void setSelectedRouteActivity(@Nullable RouteActivity routeActivity) {
		this.selectedRouteActivity = routeActivity;
	}

	public void onSelectRouteActivity(@Nullable RouteActivity activity) {
		setSelectedRouteActivity(activity);
		if (activitySelectionListener != null) {
			activitySelectionListener.onResult(activity);
		}
	}

	@Nullable
	public RouteActivity getSelectedRouteActivity() {
		return selectedRouteActivity;
	}

	@NonNull
	public List<RouteActivityGroup> getActivityGroups() {
		return cachedGroups;
	}

	@NonNull
	public List<RouteActivity> getActivities() {
		if (cachedActivities == null) {
			cachedActivities = new ArrayList<>();
			for (RouteActivityGroup group : cachedGroups) {
				cachedActivities.addAll(group.getActivities());
			}
		}
		return cachedActivities;
	}

	private void readAvailableRouteActivities() {
		try {
			readFromJson();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private void readFromJson() throws JSONException {
		JSONObject json = new JSONObject(AvailableRouteActivities.SOURCE_FILE_CONTENT);
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
				activities.add(new RouteActivity(activityId, activityLabel, iconName));
			}
			cachedGroups.add(new RouteActivityGroup(id, label, activities));
		}
	}
}
