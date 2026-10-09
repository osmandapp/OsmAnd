package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.enums.HistorySource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class NavigationHistorySettingsItem extends HistorySettingsItem {


	public NavigationHistorySettingsItem(@NonNull OsmandApplication app, @NonNull List<SearchHistoryHelper.HistoryEntry> items) {
		super(app, items);
	}

	public NavigationHistorySettingsItem(@NonNull OsmandApplication app, @Nullable NavigationHistorySettingsItem baseItem, @NonNull List<SearchHistoryHelper.HistoryEntry> items) {
		super(app, baseItem, items);
	}

	public NavigationHistorySettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@NonNull
	@Override
	protected List<SearchHistoryHelper.HistoryEntry> getHistoryEntries() {
		return searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false);
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.NAVIGATION_HISTORY;
	}

	@NonNull
	@Override
	public String getName() {
		return "navigation_history";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.navigation_history);
	}
}
