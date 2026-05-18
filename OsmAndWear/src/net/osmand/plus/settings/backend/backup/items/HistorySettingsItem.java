package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.plus.helpers.SearchHistoryHelper.*;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.enums.HistorySource;
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
		searchHistoryHelper = getInstance(app);
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

	@Override
	public long getEstimatedItemSize(@NonNull HistoryEntry item) {
		return APPROXIMATE_SEARCH_HISTORY_SIZE_BYTES;
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
