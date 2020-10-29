package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AvoidRoadsSettingsItem extends CollectionSettingsItem<AvoidSpecificRoads.AvoidRoadInfo> {

	private OsmandSettings settings;
	private AvoidSpecificRoads specificRoads;

	public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull List<AvoidSpecificRoads.AvoidRoadInfo> items) {
		super(app, null, items);
	}

	public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @Nullable AvoidRoadsSettingsItem baseItem, @NonNull List<AvoidSpecificRoads.AvoidRoadInfo> items) {
		super(app, baseItem, items);
	}

	AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		settings = app.getSettings();
		specificRoads = app.getAvoidSpecificRoads();
		existingItems = new ArrayList<>(specificRoads.getImpassableRoads().values());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.AVOID_ROADS;
	}

	@NonNull
	@Override
	public String getName() {
		return "avoid_roads";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return "avoid_roads";
	}

	@Override
	public void apply() {
		List<AvoidSpecificRoads.AvoidRoadInfo> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);
			for (AvoidSpecificRoads.AvoidRoadInfo duplicate : duplicateItems) {
				if (shouldReplace) {
					LatLon latLon = new LatLon(duplicate.latitude, duplicate.longitude);
					if (settings.removeImpassableRoad(latLon)) {
						settings.addImpassableRoad(duplicate);
					}
				} else {
					settings.addImpassableRoad(renameItem(duplicate));
				}
			}
			for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : appliedItems) {
				settings.addImpassableRoad(avoidRoad);
			}
			specificRoads.loadImpassableRoads();
			specificRoads.initRouteObjects(true);
		}
	}

	@Override
	public boolean isDuplicate(@NonNull AvoidSpecificRoads.AvoidRoadInfo item) {
		return existingItems.contains(item);
	}

	@Override
	public boolean shouldReadOnCollecting() {
		return true;
	}

	@NonNull
	@Override
	public AvoidSpecificRoads.AvoidRoadInfo renameItem(@NonNull AvoidSpecificRoads.AvoidRoadInfo item) {
		int number = 0;
		while (true) {
			number++;
			AvoidSpecificRoads.AvoidRoadInfo renamedItem = new AvoidSpecificRoads.AvoidRoadInfo();
			renamedItem.name = item.name + "_" + number;
			if (!isDuplicate(renamedItem)) {
				renamedItem.id = item.id;
				renamedItem.latitude = item.latitude;
				renamedItem.longitude = item.longitude;
				renamedItem.appModeKey = item.appModeKey;
				return renamedItem;
			}
		}
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			JSONArray jsonArray = json.getJSONArray("items");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				double latitude = object.optDouble("latitude");
				double longitude = object.optDouble("longitude");
				String name = object.optString("name");
				String appModeKey = object.optString("appModeKey");
				AvoidSpecificRoads.AvoidRoadInfo roadInfo = new AvoidSpecificRoads.AvoidRoadInfo();
				roadInfo.id = 0;
				roadInfo.latitude = latitude;
				roadInfo.longitude = longitude;
				roadInfo.name = name;
				if (ApplicationMode.valueOfStringKey(appModeKey, null) != null) {
					roadInfo.appModeKey = appModeKey;
				} else {
					roadInfo.appModeKey = app.getRoutingHelper().getAppMode().getStringKey();
				}
				items.add(roadInfo);
			}
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		if (!items.isEmpty()) {
			try {
				for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("latitude", avoidRoad.latitude);
					jsonObject.put("longitude", avoidRoad.longitude);
					jsonObject.put("name", avoidRoad.name);
					jsonObject.put("appModeKey", avoidRoad.appModeKey);
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
