package net.osmand.plus.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.settings.MenuItemsAdapter.MenuItemBase;
import net.osmand.plus.OsmandSettings.ListStringPreference;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_MAP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_SCREEN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIRECTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DIVIDER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DOWNLOAD_MAPS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_HELP_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MAP_MARKERS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MEASURE_DISTANCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_MY_PLACES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_OSMAND_LIVE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_PLUGINS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SEARCH_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_TRAVEL_GUIDES_ID;

public class MenuItemsManager {

	private OsmandApplication app;

	public MenuItemsManager(OsmandApplication app) {
		this.app = app;
	}

	List<MenuItemBase> getDrawerItemsDefault() {
		List<MenuItemBase> items = new ArrayList<>();
		items.add(new MenuItemBase(DRAWER_DASHBOARD_ID, R.string.home, R.string.app_name_osmand, R.drawable.map_dashboard, 0, false));
		items.add(new MenuItemBase(DRAWER_MAP_MARKERS_ID, R.string.map_markers, R.string.app_name_osmand, R.drawable.ic_action_flag_dark, 1, false));
		items.add(new MenuItemBase(DRAWER_MY_PLACES_ID, R.string.shared_string_my_places, R.string.app_name_osmand, R.drawable.ic_action_fav_dark, 2, false));
		items.add(new MenuItemBase(DRAWER_SEARCH_ID, R.string.shared_string_search, R.string.app_name_osmand, R.drawable.ic_action_search_dark, 3, false));
		items.add(new MenuItemBase(DRAWER_DIRECTIONS_ID, R.string.get_directions, R.string.app_name_osmand, R.drawable.ic_action_gdirections_dark, 4, false));
		items.add(new MenuItemBase(DRAWER_CONFIGURE_MAP_ID, R.string.configure_map, R.string.app_name_osmand, R.drawable.ic_action_layers, 5, false));
		items.add(new MenuItemBase(DRAWER_DOWNLOAD_MAPS_ID, R.string.shared_string_download_map, R.string.app_name_osmand, R.drawable.ic_type_archive, 6, false));
		items.add(new MenuItemBase(DRAWER_OSMAND_LIVE_ID, R.string.osm_live, R.string.app_name_osmand, R.drawable.ic_action_osm_live, 7, false));
		items.add(new MenuItemBase(DRAWER_TRAVEL_GUIDES_ID, R.string.wikivoyage_travel_guide, R.string.app_name_osmand, R.drawable.ic_action_travel, 8, false));
		items.add(new MenuItemBase(DRAWER_MEASURE_DISTANCE_ID, R.string.measurement_tool, R.string.app_name_osmand, R.drawable.ic_action_ruler, 9, false));
		items.add(new MenuItemBase(DRAWER_DIVIDER_ID, R.string.shared_string_divider, R.string.shared_string_divider, 0, 10, false));
		items.add(new MenuItemBase(DRAWER_CONFIGURE_SCREEN_ID, R.string.layer_map_appearance, R.string.app_name_osmand, R.drawable.ic_configure_screen_dark, 11, false));
		items.add(new MenuItemBase(DRAWER_PLUGINS_ID, R.string.plugins_screen, R.string.app_name_osmand, R.drawable.ic_extension_dark, 12, false));
		items.add(new MenuItemBase(DRAWER_SETTINGS_ID, R.string.shared_string_settings, R.string.app_name_osmand, R.drawable.ic_action_settings, 13, false));
		items.add(new MenuItemBase(DRAWER_HELP_ID, R.string.shared_string_help, R.string.app_name_osmand, R.drawable.ic_action_help, 14, false));
		if (Version.isDeveloperVersion(app)) {
			items.add(new MenuItemBase(DRAWER_BUILDS_ID, R.string.version_settings, R.string.developer_plugin, R.drawable.ic_action_gabout_dark, 15, false));
		}
		return items;
	}


	public List<String> getDrawerIdsDefaultOrder() {
		return getMenuItemsIdsDefaultOrder(getDrawerItemsDefault());
	}

	public LinkedHashMap<String, Integer> getDrawerItemsSavedOrder() {
		return getMenuItemsOrder(app.getSettings().DRAWER_ITEMS_ORDER);
	}

	private List<String> getMenuItemsIdsDefaultOrder(List<MenuItemBase> items) {
		List<String> itemsIds = new ArrayList<>();
		for (MenuItemBase item : items) {
			itemsIds.add(item.getId());
		}
		return itemsIds;
	}

	private LinkedHashMap<String, Integer> getMenuItemsOrder(ListStringPreference preference) {
		List<String> ids = preference.getStringsList();
		if (ids == null) {
			ids = getDrawerIdsDefaultOrder();
		}
		LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
		for (int i = 0; i < ids.size(); i++) {
			result.put(ids.get(i), i);
		}
		return result;
	}
}
