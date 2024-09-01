package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.RouteActivityGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteActivityHelper {

	private static final String SOURCE_FILE_NAME = "activities.json";

	private final Metadata metadata;
	private final RouteKey routeKey;
	private final List<RouteActivityGroup> cachedGroups = new ArrayList<>();
	private List<RouteActivity> cachedActivities;
	private OnResultCallback<RouteActivity> activityChangeListener;

	public RouteActivityHelper(@NonNull Metadata metadata, @Nullable RouteKey routeKey) {
		this.metadata = metadata;
		this.routeKey = routeKey;
		readAvailableRouteActivities();
	}

	public void setActivityChangeListener(@NonNull OnResultCallback<RouteActivity> listener) {
		this.activityChangeListener = listener;
	}

	public void setRouteActivity(@Nullable RouteActivity activity) {
		GpxUtilities.INSTANCE.setRouteActivity(metadata, activity, getActivities());
		if (activityChangeListener != null) {
			activityChangeListener.onResult(activity);
		}
	}

	@Nullable
	public RouteActivity getSelectedRouteActivity() {
		RouteActivity activity = GpxUtilities.INSTANCE.getRouteActivity(metadata, getActivities());
		if (activity == null && routeKey != null) {
			activity = GpxUtilities.INSTANCE.getRouteActivity(routeKey.type.getName(), getActivities());
		}
		return activity;
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
