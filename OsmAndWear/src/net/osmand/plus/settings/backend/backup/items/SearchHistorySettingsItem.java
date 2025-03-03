package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
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

public class SearchHistorySettingsItem extends HistorySettingsItem {

	public SearchHistorySettingsItem(@NonNull OsmandApplication app, @NonNull List<HistoryEntry> items) {
		super(app, items);
	}

	public SearchHistorySettingsItem(@NonNull OsmandApplication app, @Nullable SearchHistorySettingsItem baseItem, @NonNull List<HistoryEntry> items) {
		super(app, baseItem, items);
	}

	public SearchHistorySettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@NonNull
	@Override
	protected List<HistoryEntry> getHistoryEntries() {
		return searchHistoryHelper.getHistoryEntries(HistorySource.SEARCH, false);
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
}