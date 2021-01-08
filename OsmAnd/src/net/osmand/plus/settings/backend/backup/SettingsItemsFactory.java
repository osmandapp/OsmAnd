package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SettingsItemsFactory {

	private OsmandApplication app;
	private List<SettingsItem> items = new ArrayList<>();

	SettingsItemsFactory(@NonNull OsmandApplication app, String jsonStr) throws IllegalArgumentException, JSONException {
		this.app = app;
		collectItems(new JSONObject(jsonStr));
	}

	private void collectItems(JSONObject json) throws IllegalArgumentException, JSONException {
		JSONArray itemsJson = json.getJSONArray("items");
		int version = json.has("version") ? json.getInt("version") : 1;
		if (version > SettingsHelper.VERSION) {
			throw new IllegalArgumentException("Unsupported osf version: " + version);
		}
		Map<String, List<SettingsItem>> pluginItems = new HashMap<>();
		for (int i = 0; i < itemsJson.length(); i++) {
			JSONObject itemJson = itemsJson.getJSONObject(i);
			SettingsItem item;
			try {
				item = createItem(itemJson);
				items.add(item);
				String pluginId = item.getPluginId();
				if (pluginId != null && item.getType() != SettingsItemType.PLUGIN) {
					List<SettingsItem> items = pluginItems.get(pluginId);
					if (items != null) {
						items.add(item);
					} else {
						items = new ArrayList<>();
						items.add(item);
						pluginItems.put(pluginId, items);
					}
				}
			} catch (IllegalArgumentException e) {
				SettingsHelper.LOG.error("Error creating item from json: " + itemJson, e);
			}
		}
		if (items.size() == 0) {
			throw new IllegalArgumentException("No items");
		}
		for (SettingsItem item : items) {
			if (item instanceof PluginSettingsItem) {
				PluginSettingsItem pluginSettingsItem = ((PluginSettingsItem) item);
				List<SettingsItem> pluginDependentItems = pluginItems.get(pluginSettingsItem.getName());
				if (!Algorithms.isEmpty(pluginDependentItems)) {
					pluginSettingsItem.getPluginDependentItems().addAll(pluginDependentItems);
				}
			}
		}
	}

	@NonNull
	public List<SettingsItem> getItems() {
		return items;
	}

	@Nullable
	public SettingsItem getItemByFileName(@NonNull String fileName) {
		for (SettingsItem item : items) {
			if (Algorithms.stringsEqual(item.getFileName(), fileName)) {
				return item;
			}
		}
		return null;
	}

	@NonNull
	private SettingsItem createItem(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
		SettingsItem item = null;
		SettingsItemType type = SettingsItem.parseItemType(json);
		OsmandSettings settings = app.getSettings();
		switch (type) {
			case GLOBAL:
				item = new GlobalSettingsItem(settings, json);
				break;
			case PROFILE:
				item = new ProfileSettingsItem(app, json);
				break;
			case PLUGIN:
				item = new PluginSettingsItem(app, json);
				break;
			case DATA:
				item = new DataSettingsItem(app, json);
				break;
			case FILE:
				item = new FileSettingsItem(app, json);
				break;
			case RESOURCES:
				item = new ResourcesSettingsItem(app, json);
				break;
			case QUICK_ACTIONS:
				item = new QuickActionsSettingsItem(app, json);
				break;
			case POI_UI_FILTERS:
				item = new PoiUiFiltersSettingsItem(app, json);
				break;
			case MAP_SOURCES:
				item = new MapSourcesSettingsItem(app, json);
				break;
			case AVOID_ROADS:
				item = new AvoidRoadsSettingsItem(app, json);
				break;
			case SUGGESTED_DOWNLOADS:
				item = new SuggestedDownloadsItem(app, json);
				break;
			case DOWNLOADS:
				item = new DownloadsItem(app, json);
				break;
			case OSM_NOTES:
				item = new OsmNotesSettingsItem(app, json);
				break;
			case OSM_EDITS:
				item = new OsmEditsSettingsItem(app, json);
				break;
			case FAVOURITES:
				item = new FavoritesSettingsItem(app, json);
				break;
			case ACTIVE_MARKERS:
				item = new MarkersSettingsItem(app, json);
				break;
			case HISTORY_MARKERS:
				item = new HistoryMarkersSettingsItem(app, json);
				break;
			case SEARCH_HISTORY:
				item = new SearchHistorySettingsItem(app, json);
				break;
			case GPX:
				item = new GpxSettingsItem(app, json);
				break;
			case ONLINE_ROUTING_ENGINES:
				item = new OnlineRoutingSettingsItem(app, json);
				break;
		}
		return item;
	}
}
