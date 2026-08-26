package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.plus.search.history.SearchHistoryHelper.*;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.search.core.ObjectType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class HistorySettingsItem extends CollectionSettingsItem<HistoryEntry> {
	private static final int APPROXIMATE_SEARCH_HISTORY_SIZE_BYTES = 320;

	protected SearchHistoryHelper searchHistoryHelper;

	public HistorySettingsItem(@NonNull OsmandApplication app, @NonNull List<HistoryEntry> items) {
		super(app, null, items);
	}

	public HistorySettingsItem(@NonNull OsmandApplication app, @Nullable HistorySettingsItem baseItem, @NonNull List<HistoryEntry> items) {
		super(app, baseItem, items);
	}

	public HistorySettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		searchHistoryHelper = app.getSearchHistoryHelper();
		existingItems = getHistoryEntries();
	}

	@NonNull
	protected abstract List<HistoryEntry> getHistoryEntries();

	@NonNull
	@Override
	public abstract SettingsItemType getType();

	@NonNull
	@Override
	public abstract String getName();

	@NonNull
	@Override
	public abstract String getPublicName(@NonNull Context ctx);

	@Override
	public long getLocalModifiedTime() {
		return searchHistoryHelper.getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		searchHistoryHelper.setLastModifiedTime(lastModifiedTime);
	}

	@Override
	public void apply() {
		List<HistoryEntry> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			// leave the last accessed history entry between the duplicate and the original
			for (HistoryEntry duplicate : duplicateItems) {
				PointDescription name = duplicate.getName();
				HistoryEntry original = searchHistoryHelper.getEntryByName(name, duplicate.getSource());
				if (original != null && original.getLastAccessTime() < duplicate.getLastAccessTime()) {
					appliedItems.remove(original);
					appliedItems.add(duplicate);
				}
			}
			searchHistoryHelper.addItemsToHistory(appliedItems);
		}
	}

	@Override
	protected void deleteItem(HistoryEntry item) {
		// TODO: delete settings item
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
				double lat = object.optDouble("latitude");
				double lon = object.optDouble("longitude");
				String serializedPointDescription = object.optString("pointDescription");
				long lastAccessed = object.optLong("lastAccessedTime");
				String intervals = object.optString("intervals");
				String intervalValues = object.optString("intervalValues");
				HistorySource source = HistorySource.getHistorySourceByName(object.optString("source"));

				HistoryEntry historyEntry = new HistoryEntry(lat, lon,
						PointDescription.deserializeFromString(serializedPointDescription, new LatLon(lat, lon)), source);
				historyEntry.setLastAccessTime(lastAccessed);
				historyEntry.setFrequency(intervals, intervalValues);
				historyEntry.setObjectType(getObjectType(object.optString("objectType")));
				historyEntry.setCityType(getCityType(object.optString("cityType")));
				historyEntry.setDisplayName(getOptionalString(object, "displayName"));
				historyEntry.setPoiCategoryKey(getOptionalString(object, "poiCategoryKey"));
				historyEntry.setPoiSubtypeKey(getOptionalString(object, "poiSubtypeKey"));
				historyEntry.setTypeName(getOptionalString(object, "typeName"));
				historyEntry.setAddress(getOptionalString(object, "address"));
				historyEntry.setRelatedObjectName(getOptionalString(object, "relatedObjectName"));
				historyEntry.setOpeningHours(getOptionalString(object, "openingHours"));
				historyEntry.setAlternateName(getOptionalString(object, "alternateName"));
				historyEntry.setPhotoUrl(getOptionalString(object, "photoUrl"));
				if (object.has("osmId") && !object.isNull("osmId")) {
					historyEntry.setOsmId(object.optLong("osmId"));
				}
				items.add(historyEntry);
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
				for (HistoryEntry historyEntry : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("latitude", historyEntry.getLat());
					jsonObject.put("longitude", historyEntry.getLon());
					jsonObject.put("pointDescription",
							PointDescription.serializeToString(historyEntry.getName()));
					jsonObject.put("lastAccessedTime", historyEntry.getLastAccessTime());
					jsonObject.put("intervals", historyEntry.getIntervals());
					jsonObject.put("intervalValues", historyEntry.getIntervalsValues());
					jsonObject.put("source", historyEntry.getSource().name());
					if (historyEntry.getObjectType() != null) {
						jsonObject.put("objectType", historyEntry.getObjectType().name());
					}
					if (historyEntry.getCityType() != null) {
						jsonObject.put("cityType", historyEntry.getCityType().name());
					}
					putOptionalString(jsonObject, "displayName", historyEntry.getDisplayName());
					putOptionalString(jsonObject, "poiCategoryKey", historyEntry.getPoiCategoryKey());
					putOptionalString(jsonObject, "poiSubtypeKey", historyEntry.getPoiSubtypeKey());
					putOptionalString(jsonObject, "typeName", historyEntry.getTypeName());
					putOptionalString(jsonObject, "address", historyEntry.getAddress());
					putOptionalString(jsonObject, "relatedObjectName", historyEntry.getRelatedObjectName());
					putOptionalString(jsonObject, "openingHours", historyEntry.getOpeningHours());
					putOptionalString(jsonObject, "alternateName", historyEntry.getAlternateName());
					putOptionalString(jsonObject, "photoUrl", historyEntry.getPhotoUrl());
					if (historyEntry.getOsmId() != null) {
						jsonObject.put("osmId", historyEntry.getOsmId());
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
	private String getOptionalString(@NonNull JSONObject object, @NonNull String key) {
		String value = object.optString(key, null);
		return Algorithms.isEmpty(value) ? null : value;
	}

	private void putOptionalString(@NonNull JSONObject object, @NonNull String key, @Nullable String value)
			throws JSONException {
		if (!Algorithms.isEmpty(value)) {
			object.put(key, value);
		}
	}

	@Nullable
	private ObjectType getObjectType(@Nullable String objectTypeName) {
		if (!Algorithms.isEmpty(objectTypeName)) {
			try {
				return ObjectType.valueOf(objectTypeName);
			} catch (IllegalArgumentException e) {
				SettingsHelper.LOG.warn("Unsupported history object type: " + objectTypeName);
			}
		}
		return null;
	}

	@Nullable
	private CityType getCityType(@Nullable String cityTypeName) {
		if (!Algorithms.isEmpty(cityTypeName)) {
			try {
				return CityType.valueOf(cityTypeName);
			} catch (IllegalArgumentException e) {
				SettingsHelper.LOG.warn("Unsupported history city type: " + cityTypeName);
			}
		}
		return null;
	}

	@Override
	public boolean isDuplicate(@NonNull HistoryEntry historyEntry) {
		PointDescription pointDescription = historyEntry.getName();
		for (HistoryEntry entry : existingItems) {
			if (historyEntry.getSource() == entry.getSource()
					&& Algorithms.objectEquals(pointDescription, entry.getName())) {
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
	public HistoryEntry renameItem(@NonNull HistoryEntry item) {
		return item;
	}

	@Override
	public long getEstimatedItemSize(@NonNull HistoryEntry item) {
		return APPROXIMATE_SEARCH_HISTORY_SIZE_BYTES;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader(false);
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
