package net.osmand.plus.settings.backend.menuitems;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DrawerMenuItemsSettings extends ContextMenuItemsSettings {

	private static final List<String> originalOrderIds = new ArrayList<>();

	static {
		originalOrderIds.add(DRAWER_DASHBOARD_ID);
		originalOrderIds.add(DRAWER_MAP_MARKERS_ID);
		originalOrderIds.add(DRAWER_MY_PLACES_ID);
		originalOrderIds.add(DRAWER_FAVORITES_ID);
		originalOrderIds.add(DRAWER_TRACKS_ID);
		originalOrderIds.add(DRAWER_AV_NOTES_ID);
		originalOrderIds.add(DRAWER_OSM_EDITS_ID);
		originalOrderIds.add(DRAWER_BACKUP_RESTORE_ID);
		originalOrderIds.add(DRAWER_SEARCH_ID);
		originalOrderIds.add(DRAWER_TRIP_RECORDING_ID);
		originalOrderIds.add(DRAWER_DIRECTIONS_ID);
		originalOrderIds.add(DRAWER_CONFIGURE_MAP_ID);
		originalOrderIds.add(DRAWER_DOWNLOAD_MAPS_ID);
		originalOrderIds.add(DRAWER_LIVE_UPDATES_ID);
		originalOrderIds.add(DRAWER_TRAVEL_GUIDES_ID);
		originalOrderIds.add(DRAWER_MEASURE_DISTANCE_ID);
		originalOrderIds.add(DRAWER_WEATHER_FORECAST_ID);
		originalOrderIds.add(DRAWER_ANT_PLUS_ID);
		originalOrderIds.add(DRAWER_VEHICLE_METRICS_ID);
		originalOrderIds.add(DRAWER_DIVIDER_ID);
		originalOrderIds.add(DRAWER_CONFIGURE_SCREEN_ID);
		originalOrderIds.add(DRAWER_PLUGINS_ID);
		originalOrderIds.add(DRAWER_SETTINGS_ID);
		originalOrderIds.add(DRAWER_HELP_ID);
		originalOrderIds.add(DRAWER_BUILDS_ID);
		originalOrderIds.add(DRAWER_OSMAND_VERSION_ID);
	}

	public DrawerMenuItemsSettings() {
	}

	public DrawerMenuItemsSettings(@NonNull List<String> hiddenIds, @NonNull List<String> orderIds) {
		super(hiddenIds, orderIds);
	}

	@NonNull
	@Override
	public List<String> getHiddenIds() {
		updateMissingDrawerItems();
		return super.getHiddenIds();
	}

	@NonNull
	@Override
	public List<String> getOrderIds() {
		updateMissingDrawerItems();
		return super.getOrderIds();
	}

	@NonNull
	@Override
	public DrawerMenuItemsSettings newInstance() {
		return new DrawerMenuItemsSettings();
	}

	private void updateMissingDrawerItems() {
		List<String> hiddenByDefault = getDrawerHiddenItemsByDefault();
		for (int i = 0; i < originalOrderIds.size(); i++) {
			String currentItemId = originalOrderIds.get(i);

			boolean showBeHidden = hiddenByDefault.contains(currentItemId);
			if (isItemUnavailable(currentItemId) && showBeHidden) {
				hiddenIds.add(currentItemId);
			}

			boolean unordered = !orderIds.contains(currentItemId);
			if (unordered) {
				insertItemByOriginalOrder(currentItemId);
			}
		}
	}

	private boolean isItemUnavailable(String item) {
		return !hiddenIds.contains(item) && !orderIds.contains(item);
	}

	private void insertItemByOriginalOrder(String idToInsert) {
		int itemOriginalOrder = originalOrderIds.indexOf(idToInsert);

		for (int i = itemOriginalOrder; i > 0; i--) {
			String prevOriginalItemId = originalOrderIds.get(i - 1);

			if (orderIds.contains(prevOriginalItemId)) {
				int prevItemOrder = orderIds.indexOf(prevOriginalItemId);
				orderIds.add(prevItemOrder + 1, idToInsert);
				return;
			}
		}
		orderIds.add(0, idToInsert);
	}

	@NonNull
	public static DrawerMenuItemsSettings getDrawerDefaultInstance() {
		return new DrawerMenuItemsSettings(getDrawerHiddenItemsByDefault(), new ArrayList<>());
	}

	@NonNull
	private static List<String> getDrawerHiddenItemsByDefault() {
		List<String> hiddenByDefault = new ArrayList<>();
		hiddenByDefault.add(DRAWER_DASHBOARD_ID);
		hiddenByDefault.add(DRAWER_FAVORITES_ID);
		hiddenByDefault.add(DRAWER_TRACKS_ID);
		hiddenByDefault.add(DRAWER_AV_NOTES_ID);
		hiddenByDefault.add(DRAWER_OSM_EDITS_ID);
		hiddenByDefault.add(DRAWER_BACKUP_RESTORE_ID);
		hiddenByDefault.add(DRAWER_LIVE_UPDATES_ID);
		hiddenByDefault.add(DRAWER_OSMAND_VERSION_ID);
		hiddenByDefault.add(DRAWER_ANT_PLUS_ID);
		hiddenByDefault.add(DRAWER_VEHICLE_METRICS_ID);
		return hiddenByDefault;
	}
}