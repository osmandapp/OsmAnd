package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AvoidRoadsSettingsItem extends CollectionSettingsItem<AvoidRoadInfo> {

	private OsmandSettings settings;
	private AvoidSpecificRoads specificRoads;

	public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull List<AvoidRoadInfo> items) {
		super(app, null, items);
	}

	public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @Nullable AvoidRoadsSettingsItem baseItem, @NonNull List<AvoidRoadInfo> items) {
		super(app, baseItem, items);
	}

	public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
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

	@Override
	public long getLocalModifiedTime() {
		return specificRoads.getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		specificRoads.setLastModifiedTime(lastModifiedTime);
	}

	@NonNull
	@Override
	public String getName() {
		return "avoid_roads";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.avoid_road);
	}

	@Override
	public void apply() {
		List<AvoidRoadInfo> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);
			for (AvoidRoadInfo duplicate : duplicateItems) {
				LatLon latLon = new LatLon(duplicate.latitude, duplicate.longitude);
				if (settings.removeImpassableRoad(latLon)) {
					settings.addImpassableRoad(duplicate);
				}
			}
			for (AvoidRoadInfo avoidRoad : appliedItems) {
				settings.addImpassableRoad(avoidRoad);
			}
			specificRoads.loadImpassableRoads();
			specificRoads.initRouteObjects(true);
		}
	}

	@Override
	public boolean isDuplicate(@NonNull AvoidRoadInfo item) {
		for (AvoidRoadInfo roadInfo : existingItems) {
			if (roadInfo.id == item.id) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldShowDuplicates() {
		return false;
	}

	@NonNull
	@Override
	public AvoidRoadInfo renameItem(@NonNull AvoidRoadInfo item) {
		return item;
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
				double direction = object.optDouble("direction");
				String name = object.optString("name");
				String appModeKey = object.optString("appModeKey");
				long id = object.optLong("roadId");
				AvoidRoadInfo roadInfo = new AvoidRoadInfo();
				roadInfo.id = id;
				roadInfo.latitude = latitude;
				roadInfo.longitude = longitude;
				roadInfo.direction = direction;
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

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		if (!items.isEmpty()) {
			try {
				for (AvoidRoadInfo avoidRoad : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("latitude", avoidRoad.latitude);
					jsonObject.put("longitude", avoidRoad.longitude);
					jsonObject.put("name", avoidRoad.name);
					jsonObject.put("appModeKey", avoidRoad.appModeKey);
					jsonObject.put("roadId", avoidRoad.id);
					if (!Double.isNaN(avoidRoad.direction)) {
						jsonObject.put("direction", avoidRoad.direction);
					}
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
		return json;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
