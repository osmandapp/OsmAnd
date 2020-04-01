package net.osmand.plus.settings;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.settings.MenuItemsAdapter.MenuItemBase;
import net.osmand.plus.settings.ConfigureMenuRootFragment.ScreenType;
import net.osmand.plus.OsmandSettings.ListStringPreference;

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
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AVOID_ROAD;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CREATE_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DOWNLOAD_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_EDIT_GPX_WP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MEASURE_DISTANCE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_OSM_CHANGE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_OPEN_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_SEARCH_NEARBY;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_UPDATE_MAP;

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
		items.add(new MenuItemBase(DRAWER_DIVIDER_ID, R.string.shared_string_divider, R.string.divider_descr, 0, 10, false));
		items.add(new MenuItemBase(DRAWER_CONFIGURE_SCREEN_ID, R.string.layer_map_appearance, R.string.app_name_osmand, R.drawable.ic_configure_screen_dark, 11, false));
		items.add(new MenuItemBase(DRAWER_PLUGINS_ID, R.string.plugins_screen, R.string.app_name_osmand, R.drawable.ic_extension_dark, 12, false));
		items.add(new MenuItemBase(DRAWER_SETTINGS_ID + ".new", R.string.shared_string_settings, R.string.app_name_osmand, R.drawable.ic_action_settings, 13, false));
		items.add(new MenuItemBase(DRAWER_HELP_ID, R.string.shared_string_help, R.string.app_name_osmand, R.drawable.ic_action_help, 14, false));
		if (Version.isDeveloperVersion(app)) {
			items.add(new MenuItemBase(DRAWER_BUILDS_ID, R.string.version_settings, R.string.developer_plugin, R.drawable.ic_action_gabout_dark, 15, false));
		}
		return items;
	}

	@NonNull
	public List<String> getHiddenItemsIds(@NonNull ScreenType type) {
		List<String> hiddenItemsIds = null;
		switch (type) {
			case DRAWER:
				hiddenItemsIds = app.getSettings().HIDDEN_DRAWER_ITEMS.getStringsList();
				break;
			case CONFIGURE_MAP:
				hiddenItemsIds = app.getSettings().HIDDEN_CONFIGURE_MAP_ITEMS.getStringsList();
				break;
			case CONTEXT_MENU_ACTIONS:
				hiddenItemsIds = app.getSettings().HIDDEN_CONTEXT_MENU_ACTIONS_ITEMS.getStringsList();
				break;
		}
		return hiddenItemsIds != null ? hiddenItemsIds : new ArrayList<String>();
	}

	public void saveHiddenItemsIds(@NonNull ScreenType type, @Nullable List<String> hiddenItemsIds) {
		switch (type) {
			case DRAWER:
				app.getSettings().HIDDEN_DRAWER_ITEMS.setStringsList(hiddenItemsIds);
				break;
			case CONFIGURE_MAP:
				app.getSettings().HIDDEN_CONFIGURE_MAP_ITEMS.setStringsList(hiddenItemsIds);
				break;
			case CONTEXT_MENU_ACTIONS:
				app.getSettings().HIDDEN_CONTEXT_MENU_ACTIONS_ITEMS.setStringsList(hiddenItemsIds);
				break;
		}
	}

	public void saveItemsIdsOrder(@NonNull ScreenType type, @Nullable List<String> itemsIdsOrder) {
		switch (type) {
			case DRAWER:
				app.getSettings().DRAWER_ITEMS_ORDER.setStringsList(itemsIdsOrder);
				break;
			case CONFIGURE_MAP:
				break;
			case CONTEXT_MENU_ACTIONS:
				break;
		}
	}

	public void reorderMenuItems(@NonNull List<MenuItemBase> defaultItems, @NonNull HashMap<String, Integer> itemsOrder) {
		for (MenuItemBase item : defaultItems) {
			Integer order = itemsOrder.get(item.getId());
			if (order != null) {
				item.setOrder(order);
			}
		}
		Collections.sort(defaultItems, new Comparator<MenuItemBase>() {
			@Override
			public int compare(MenuItemBase item1, MenuItemBase item2) {
				int order1 = item1.getOrder();
				int order2 = item2.getOrder();
				return (order1 < order2) ? -1 : ((order1 == order2) ? 0 : 1);
			}
		});
	}

	public void resetMenuItems(@NonNull ScreenType type) {
		switch (type) {
			case DRAWER:
				app.getSettings().DRAWER_ITEMS_ORDER.setStringsList(null);
				app.getSettings().HIDDEN_DRAWER_ITEMS.setStringsList(null);
				break;
			case CONFIGURE_MAP:
				break;
			case CONTEXT_MENU_ACTIONS:
				break;
		}
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
