package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchHistorySettingsItem extends CollectionSettingsItem<HistoryEntry> {

	private SearchHistoryHelper searchHistoryHelper;

	public SearchHistorySettingsItem(@NonNull OsmandApplication app, @NonNull List<HistoryEntry> items) {
		super(app, null, items);
	}

	public SearchHistorySettingsItem(@NonNull OsmandApplication app, @Nullable SearchHistorySettingsItem baseItem, @NonNull List<HistoryEntry> items) {
		super(app, baseItem, items);
	}

	SearchHistorySettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		searchHistoryHelper = SearchHistoryHelper.getInstance(app);
		existingItems = searchHistoryHelper.getHistoryEntries(false);
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.SEARCH_HISTORY;
	}

	@NonNull
	@Override
	public String getName() {
		return "search_history";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.shared_string_search_history);
	}

	@Override
	public void apply() {
		List<HistoryEntry> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			// leave the last accessed history entry between the duplicate and the original
			for (HistoryEntry duplicate : duplicateItems) {
				PointDescription name = duplicate.getName();
				HistoryEntry original = searchHistoryHelper.getEntryByName(name);
				if (original.getLastAccessTime() < duplicate.getLastAccessTime()) {
					appliedItems.remove(original);
					appliedItems.add(duplicate);
				}
			}
			searchHistoryHelper.addItemsToHistory(appliedItems);
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
				double lat = object.optDouble("latitude");
				double lon = object.optDouble("longitude");
				String serializedPointDescription = object.optString("pointDescription");
				long lastAccessed = object.optLong("lastAccessedTime");
				String intervals = object.optString("intervals");
				String intervalValues = object.optString("intervalValues");

				HistoryEntry historyEntry = new HistoryEntry(lat, lon,
						PointDescription.deserializeFromString(serializedPointDescription, new LatLon(lat, lon)));
				historyEntry.setLastAccessTime(lastAccessed);
				historyEntry.setFrequency(intervals, intervalValues);
				items.add(historyEntry);
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
				for (HistoryEntry historyEntry : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("latitude", historyEntry.getLat());
					jsonObject.put("longitude", historyEntry.getLon());
					jsonObject.put("pointDescription",
							PointDescription.serializeToString(historyEntry.getName()));
					jsonObject.put("lastAccessedTime", historyEntry.getLastAccessTime());
					jsonObject.put("intervals", historyEntry.getIntervals());
					jsonObject.put("intervalValues", historyEntry.getIntervalsValues());
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull HistoryEntry historyEntry) {
		PointDescription pointDescription = historyEntry.getName();
		for (HistoryEntry entry : existingItems) {
			if (Algorithms.objectEquals(pointDescription, entry.getName())) {
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