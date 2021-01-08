package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PoiUiFiltersSettingsItem extends CollectionSettingsItem<PoiUIFilter> {

	public PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @NonNull List<PoiUIFilter> items) {
		super(app, null, items);
	}

	public PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @Nullable PoiUiFiltersSettingsItem baseItem, @NonNull List<PoiUIFilter> items) {
		super(app, baseItem, items);
	}

	PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		existingItems = app.getPoiFilters().getUserDefinedPoiFilters(false);
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.POI_UI_FILTERS;
	}

	@Override
	public void apply() {
		List<PoiUIFilter> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (PoiUIFilter duplicate : duplicateItems) {
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
			for (PoiUIFilter filter : appliedItems) {
				app.getPoiFilters().createPoiFilter(filter, false);
			}
			app.getSearchUICore().refreshCustomPoiFilters();
		}
	}

	@Override
	public boolean isDuplicate(@NonNull PoiUIFilter item) {
		String savedName = item.getName();
		for (PoiUIFilter filter : existingItems) {
			if (filter.getName().equals(savedName)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public PoiUIFilter renameItem(@NonNull PoiUIFilter item) {
		int number = 0;
		while (true) {
			number++;
			PoiUIFilter renamedItem = new PoiUIFilter(item,
					item.getName() + "_" + number,
					item.getFilterId() + "_" + number);
			if (!isDuplicate(renamedItem)) {
				return renamedItem;
			}
		}
	}

	@NonNull
	@Override
	public String getName() {
		return "poi_ui_filters";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return "poi_ui_filters";
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			JSONArray jsonArray = json.getJSONArray("items");
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, LinkedHashSet<String>>>() {
			}.getType();
			MapPoiTypes poiTypes = app.getPoiTypes();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String name = object.getString("name");
				String filterId = object.getString("filterId");
				String acceptedTypesString = object.getString("acceptedTypes");
				HashMap<String, LinkedHashSet<String>> acceptedTypes = gson.fromJson(acceptedTypesString, type);
				Map<PoiCategory, LinkedHashSet<String>> acceptedTypesDone = new HashMap<>();
				for (Map.Entry<String, LinkedHashSet<String>> mapItem : acceptedTypes.entrySet()) {
					final PoiCategory a = poiTypes.getPoiCategoryByName(mapItem.getKey());
					acceptedTypesDone.put(a, mapItem.getValue());
				}
				PoiUIFilter filter = new PoiUIFilter(name, filterId, acceptedTypesDone, app);
				items.add(filter);
			}
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
		}.getType();
		if (!items.isEmpty()) {
			try {
				for (PoiUIFilter filter : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", filter.getName());
					jsonObject.put("filterId", filter.getFilterId());
					jsonObject.put("acceptedTypes", gson.toJson(filter.getAcceptedTypes(), type));
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
