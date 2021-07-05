package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum SettingsItemType {
	GLOBAL,
	PROFILE,
	PLUGIN,
	DATA,
	FILE,
	RESOURCES,
	GPX,
	QUICK_ACTIONS,
	POI_UI_FILTERS,
	MAP_SOURCES,
	AVOID_ROADS,
	SUGGESTED_DOWNLOADS,
	DOWNLOADS,
	OSM_NOTES,
	OSM_EDITS,
	FAVOURITES,
	ACTIVE_MARKERS,
	HISTORY_MARKERS,
	SEARCH_HISTORY,
	ONLINE_ROUTING_ENGINES,
	ITINERARY_GROUPS;

	@Nullable
	public static SettingsItemType fromName(@NonNull String name) {
		if (name.equals("QUICK_ACTION")) {
			return SettingsItemType.QUICK_ACTIONS;
		}
		try {
			return SettingsItemType.valueOf(name);
		} catch (RuntimeException e) {
			return null;
		}
	}
}