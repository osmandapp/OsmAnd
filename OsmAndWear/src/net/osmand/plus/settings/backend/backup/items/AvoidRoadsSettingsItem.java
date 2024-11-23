package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.avoidroads.AvoidRoadsHelper;
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

	private static final int APPROXIMATE_AVOID_ROAD_SIZE_BYTES = 185;

	private OsmandSettings settings;
	private AvoidRoadsHelper avoidRoadsHelper;

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
		avoidRoadsHelper = app.getAvoidSpecificRoads();
		existingItems = new ArrayList<>(avoidRoadsHelper.getImpassableRoads());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.AVOID_ROADS;
	}

	@Override
	public long getLocalModifiedTime() {
		return avoidRoadsHelper.getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		avoidRoadsHelper.setLastModifiedTime(lastModifiedTime);
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
				LatLon latLon = duplicate.getLatLon();
				if (settings.removeImpassableRoad(latLon)) {
					settings.addImpassableRoad(duplicate);
				}
			}
			for (AvoidRoadInfo avoidRoad : appliedItems) {
				settings.addImpassableRoad(avoidRoad);
			}
			avoidRoadsHelper.loadImpassableRoads();
			avoidRoadsHelper.initRouteObjects(true);
		}
	}

	@Override
	protected void deleteItem(AvoidRoadInfo item) {
		avoidRoadsHelper.removeImpassableRoad(item);
	}

	@Override
	public boolean isDuplicate(@NonNull AvoidRoadInfo item) {
		for (AvoidRoadInfo roadInfo : existingItems) {
			if (roadInfo.getId() == item.getId()) {
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
	public long getEstimatedItemSize(@NonNull AvoidRoadInfo item) {
		return APPROXIMATE_AVOID_ROAD_SIZE_BYTES;
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

				String modeKey = ApplicationMode.valueOfStringKey(appModeKey, null) != null
						? appModeKey : app.getRoutingHelper().getAppMode().getStringKey();

				items.add(new AvoidRoadInfo(id, direction, latitude, longitude, name, modeKey));
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
					jsonObject.put("latitude", avoidRoad.getLatitude());
					jsonObject.put("longitude", avoidRoad.getLongitude());
					jsonObject.put("name", avoidRoad.getName(app));
					jsonObject.put("appModeKey", avoidRoad.getAppModeKey());
					jsonObject.put("roadId", avoidRoad.getId());
					if (!Double.isNaN(avoidRoad.getDirection())) {
						jsonObject.put("direction", avoidRoad.getDirection());
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
