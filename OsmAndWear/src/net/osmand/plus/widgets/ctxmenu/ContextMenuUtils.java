package net.osmand.plus.widgets.ctxmenu;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.APP_PROFILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_RENDERING_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OPEN_STREET_MAP_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTES_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_CATEGORY_ID;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.settings.fragments.configureitems.ScreenType;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextMenuUtils {

	@NonNull
	public static List<String> getNames(@NonNull List<ContextMenuItem> items) {
		List<String> itemNames = new ArrayList<>();
		for (ContextMenuItem item : items) {
			itemNames.add(item.getTitle());
		}
		return itemNames;
	}

	@Nullable
	public static String getCategoryDescription(@NonNull List<ContextMenuItem> items) {
		List<String> itemNames = new ArrayList<>();
		for (ContextMenuItem item : items) {
			String title = item.getTitle();
			if (title != null) {
				itemNames.add(title);
			}
		}
		if (itemNames.isEmpty()) {
			return null;
		}
		return TextUtils.join(", ", itemNames);
	}

	public static void removeHiddenItems(@NonNull ContextMenuAdapter adapter) {
		OsmandApplication app = adapter.getApplication();
		List<ContextMenuItem> items = adapter.getItems();
		OsmAndAppCustomization custom = app.getAppCustomization();
		Set<ContextMenuItem> hidden = new HashSet<>();
		for (ContextMenuItem item : items) {
			String id = item.getId();
			boolean hiddenInCustomization = !TextUtils.isEmpty(id) && !custom.isFeatureEnabled(id);
			if (item.isHidden() || hiddenInCustomization) {
				hidden.add(item);
			}
		}
		items.removeAll(hidden);
	}

	public static void hideExtraDividers(@NonNull ContextMenuAdapter adapter) {
		List<ContextMenuItem> items = adapter.getItems();
		if (Algorithms.isEmpty(items)) {
			return;
		}
		int itemsSize = items.size();
		for (int i = 0; i < itemsSize - 1; i++) {
			ContextMenuItem item = items.get(i);
			if (!item.shouldHideDivider()) {
				// Hide divider before next category
				ContextMenuItem next = items.get(i + 1);
				item.setHideDivider(next.isCategory());
			}
		}
		// Hide divider for last item
		items.get(itemsSize - 1).setHideDivider(true);
	}

	@NonNull
	public static Map<ContextMenuItem, List<ContextMenuItem>> collectItemsByCategories(@NonNull List<ContextMenuItem> items) {
		ContextMenuItem c = null;
		Map<ContextMenuItem, List<ContextMenuItem>> result = new LinkedHashMap<>();
		for (ContextMenuItem item : items) {
			if (item.isCategory()) {
				c = item;
				result.put(c, new ArrayList<>());
			} else if (c != null) {
				List<ContextMenuItem> list = result.get(c);
				if (list != null) {
					list.add(item);
				}
			} else {
				result.put(item, null);
			}
		}
		return result;
	}

	@NonNull
	public static List<ContextMenuItem> getCustomizableItems(@NonNull ContextMenuAdapter adapter) {
		List<ContextMenuItem> result = new ArrayList<>();
		for (ContextMenuItem item : getDefaultItems(adapter)) {
			if (!APP_PROFILES_ID.equals(item.getId())
					&& !DRAWER_CONFIGURE_PROFILE_ID.equals(item.getId())
					&& !DRAWER_SWITCH_PROFILE_ID.equals(item.getId())) {
				result.add(item);
			}
		}
		return result;
	}

	@NonNull
	private static List<ContextMenuItem> getDefaultItems(@NonNull ContextMenuAdapter adapter) {
		String idScheme = getIdScheme(adapter);
		List<ContextMenuItem> result = new ArrayList<>();
		for (ContextMenuItem item : adapter.getItems()) {
			String id = item.getId();
			if (id != null && (id.startsWith(idScheme))) {
				result.add(item);
			}
		}
		return result;
	}

	@NonNull
	private static String getIdScheme(@NonNull ContextMenuAdapter adapter) {
		String idScheme = "";
		for (ContextMenuItem item : adapter.getItems()) {
			String id = item.getId();
			if (id != null) {
				OsmandApplication app = adapter.getApplication();
				OsmandSettings settings = app.getSettings();
				ContextMenuItemsPreference pref = settings.getContextMenuItemsPreference(id);
				if (pref != null) {
					return pref.getIdScheme();
				}
			}
		}
		return idScheme;
	}

	@NonNull
	public static ContextMenuItemsPreference getSettingForScreen(@NonNull OsmandApplication app, @NonNull ScreenType screenType) throws IllegalArgumentException {
		OsmandSettings settings = app.getSettings();
		switch (screenType) {
			case DRAWER:
				return settings.DRAWER_ITEMS;
			case CONFIGURE_MAP:
				return settings.CONFIGURE_MAP_ITEMS;
			case CONTEXT_MENU_ACTIONS:
				return settings.CONTEXT_MENU_ACTIONS_ITEMS;
			default:
				throw new IllegalArgumentException("Unsupported screen type");
		}
	}

	public static boolean isCategoryItem(@Nullable String id) {
		return CollectionUtils.equalsToAny(id, SHOW_CATEGORY_ID, TERRAIN_CATEGORY_ID,
				OPEN_STREET_MAP_CATEGORY_ID, ROUTES_CATEGORY_ID, MAP_RENDERING_CATEGORY_ID);
	}
}