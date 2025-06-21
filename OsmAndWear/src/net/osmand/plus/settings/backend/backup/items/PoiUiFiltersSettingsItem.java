package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;

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

	private static final int APPROXIMATE_POI_UI_FILTER_SIZE_BYTES = 500;

	public PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @NonNull List<PoiUIFilter> items) {
		super(app, null, items);
	}

	public PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @Nullable PoiUiFiltersSettingsItem baseItem, @NonNull List<PoiUIFilter> items) {
		super(app, baseItem, items);
	}

	public PoiUiFiltersSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
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
	public long getLocalModifiedTime() {
		return app.getPoiFilters().getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		app.getPoiFilters().setLastModifiedTime(lastModifiedTime);
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
	protected void deleteItem(PoiUIFilter item) {
		// TODO: delete settings item
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

	@Override
	public long getEstimatedItemSize(@NonNull PoiUIFilter item) {
		return APPROXIMATE_POI_UI_FILTER_SIZE_BYTES;
	}

	@NonNull
	@Override
	public String getName() {
		return "poi_ui_filters";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.poi_dialog_poi_type);
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
					PoiCategory a = poiTypes.getPoiCategoryByName(mapItem.getKey());
					acceptedTypesDone.put(a, mapItem.getValue());
				}
				PoiUIFilter filter = new PoiUIFilter(name, filterId, acceptedTypesDone, app);
				if (object.has("filterByName")) {
					String filterByName = object.getString("filterByName");
					filter.setFilterByName(filterByName);
				}
				items.add(filter);
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
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
		}.getType();
		if (!items.isEmpty()) {
			try {
				for (PoiUIFilter filter : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", filter.getName());
					jsonObject.put("filterId", filter.getFilterId());
					jsonObject.put("filterByName", filter.getFilterByName());

					Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = filter.getAcceptedTypes();
					for (PoiCategory category : acceptedTypes.keySet()) {
						LinkedHashSet<String> poiTypes = acceptedTypes.get(category);
						if (poiTypes == null) {
							poiTypes = new LinkedHashSet<>();
							List<PoiType> types = category.getPoiTypes();
							for (PoiType poiType : types) {
								poiTypes.add(poiType.getKeyName());
							}
							acceptedTypes.put(category, poiTypes);
						}
					}

					jsonObject.put("acceptedTypes", gson.toJson(acceptedTypes, type));
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
