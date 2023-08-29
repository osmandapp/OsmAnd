package net.osmand.plus.settings.fragments.configureitems;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.configureitems.RearrangeMenuItemsAdapter.DIVIDER_TYPE;
import static net.osmand.plus.settings.fragments.configureitems.ScreenType.CONFIGURE_MAP;
import static net.osmand.plus.settings.fragments.configureitems.ScreenType.CONTEXT_MENU_ACTIONS;
import static net.osmand.plus.settings.fragments.configureitems.ScreenType.DRAWER;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.configmap.ConfigureMapMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.menuitems.ContextMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.DrawerMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.MainContextMenuItemsSettings;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RearrangeItemsHelper implements CopyAppModePrefsListener {

	public static final int MAIN_BUTTONS_QUANTITY = 4;

	public static final String SCREEN_TYPE_KEY = "screen_type_key";
	private static final String ITEMS_ORDER_KEY = "items_order_key";
	private static final String HIDDEN_ITEMS_KEY = "hidden_items_key";
	private static final String MAIN_ACTIONS_ITEMS_KEY = "main_actions_items_key";
	private static final String IS_CHANGED_KEY = "is_changed_key";

	public final OsmandApplication app;
	public final OsmandSettings settings;

	public final MapActivity mapActivity;
	public final ConfigureMenuItemsFragment fragment;

	private ScreenType screenType;
	private ApplicationMode appMode;
	private List<String> mainActionItems = new ArrayList<>();
	private List<String> hiddenMenuItems = new ArrayList<>();
	private HashMap<String, Integer> itemsOrder = new HashMap<>();
	private ContextMenuAdapter menuAdapter;
	private boolean isChanged;

	public RearrangeItemsHelper(@NonNull ConfigureMenuItemsFragment fragment) {
		this.fragment = fragment;
		this.mapActivity = (MapActivity) fragment.requireActivity();
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
	}

	@NonNull
	public ScreenType getScreenType() {
		return screenType;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public void setMainActionItems(@NonNull List<String> mainActionItems) {
		this.mainActionItems = mainActionItems;
	}

	public void initSavedIds(@NonNull Bundle bundle) {
		appMode = ApplicationMode.valueOfStringKey(bundle.getString(APP_MODE_KEY), settings.getApplicationMode());
		screenType = AndroidUtils.getSerializable(bundle, SCREEN_TYPE_KEY, ScreenType.class);
		initSavedIds(appMode);
	}

	public void initSavedIds(@NonNull ApplicationMode appMode) {
		initSavedIds(appMode, false);
	}

	public void initSavedIds(@NonNull ApplicationMode appMode, boolean useDefaultValue) {
		ContextMenuItemsSettings itemsSettings = getMenuItemsSettings(appMode, useDefaultValue);
		hiddenMenuItems = new ArrayList<>(itemsSettings.getHiddenIds());
		itemsOrder = new HashMap<>();
		List<String> orderIds = itemsSettings.getOrderIds();
		for (int i = 0; i < orderIds.size(); i++) {
			itemsOrder.put(orderIds.get(i), i);
		}
	}

	public void loadBundle(@NonNull Bundle bundle) {
		appMode = ApplicationMode.valueOfStringKey(bundle.getString(APP_MODE_KEY), settings.getApplicationMode());
		screenType = AndroidUtils.getSerializable(bundle, SCREEN_TYPE_KEY, ScreenType.class);
		itemsOrder = (HashMap<String, Integer>) AndroidUtils.getSerializable(bundle, ITEMS_ORDER_KEY, HashMap.class);
		hiddenMenuItems = bundle.getStringArrayList(HIDDEN_ITEMS_KEY);
		isChanged = bundle.getBoolean(IS_CHANGED_KEY);

		if (screenType == CONTEXT_MENU_ACTIONS) {
			mainActionItems = bundle.getStringArrayList(MAIN_ACTIONS_ITEMS_KEY);
		}
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		bundle.putString(APP_MODE_KEY, appMode.getStringKey());
		bundle.putSerializable(SCREEN_TYPE_KEY, screenType);
		bundle.putSerializable(ITEMS_ORDER_KEY, itemsOrder);
		bundle.putStringArrayList(HIDDEN_ITEMS_KEY, new ArrayList<>(hiddenMenuItems));
		bundle.putBoolean(IS_CHANGED_KEY, isChanged);

		if (screenType == CONTEXT_MENU_ACTIONS) {
			bundle.putStringArrayList(MAIN_ACTIONS_ITEMS_KEY, new ArrayList<>(mainActionItems));
		}
	}

	public void initMainActionsIds(@NonNull ApplicationMode appMode) {
		initMainActionsIds(appMode, false);
	}

	public void initMainActionsIds(@NonNull ApplicationMode appMode, boolean useDefaultValue) {
		ContextMenuItemsSettings itemsSettings = getMenuItemsSettings(appMode, useDefaultValue);
		if (itemsSettings instanceof MainContextMenuItemsSettings) {
			mainActionItems = new ArrayList<>(((MainContextMenuItemsSettings) itemsSettings).getMainIds());
			if (mainActionItems.isEmpty()) {
				List<ContextMenuItem> items = ContextMenuUtils.getCustomizableItems(menuAdapter);
				for (int i = 0; i < MAIN_BUTTONS_QUANTITY && i < items.size(); i++) {
					mainActionItems.add(items.get(i).getId());
				}
			}
		}
	}

	@NonNull
	public ContextMenuItemsSettings getMenuItemsSettings(@NonNull ApplicationMode appMode, boolean useDefaultValue) {
		ContextMenuItemsPreference preference = getSettingForScreen();
		if (useDefaultValue) {
			return preference.getProfileDefaultValue(appMode);
		} else {
			return preference.getModeValue(appMode);
		}
	}

	@NonNull
	public ContextMenuItemsPreference getSettingForScreen() {
		return ContextMenuUtils.getSettingForScreen(app, screenType);
	}

	public void loadItemsOrder() {
		createContextMenuAdapter();
		if (itemsOrder.isEmpty()) {
			for (ContextMenuItem item : ContextMenuUtils.getCustomizableItems(menuAdapter)) {
				itemsOrder.put(item.getId(), item.getOrder());
			}
		}
		if (screenType == CONTEXT_MENU_ACTIONS) {
			initMainActionsIds(appMode);
		}
	}

	public void createContextMenuAdapter() {
		switch (screenType) {
			case DRAWER:
				menuAdapter = mapActivity.getMapActions().createMainOptionsMenu();
				break;
			case CONFIGURE_MAP:
				ConfigureMapMenu configureMapMenu = new ConfigureMapMenu(app);
				menuAdapter = configureMapMenu.createListAdapter(mapActivity);
				break;
			case CONTEXT_MENU_ACTIONS:
				MapContextMenu contextMenu = mapActivity.getContextMenu();
				menuAdapter = contextMenu.getActionsContextMenuAdapter(true);
				break;
		}
		PurchasingUtils.removePromoItems(menuAdapter);
	}

	@NonNull
	public List<String> getItemsIdsToSave() {
		List<String> ids = new ArrayList<>();
		if (!itemsOrder.isEmpty()) {
			List<ContextMenuItem> defItems = ContextMenuUtils.getCustomizableItems(menuAdapter);
			sortByCustomOrder(defItems, itemsOrder);
			for (ContextMenuItem item : defItems) {
				ids.add(item.getId());
			}
		}
		return ids;
	}

	@NonNull
	public ContextMenuItemsSettings getPreferenceToSave(@NonNull List<String> ids) {
		if (screenType == DRAWER) {
			return new DrawerMenuItemsSettings(hiddenMenuItems, ids);
		} else if (screenType == CONTEXT_MENU_ACTIONS) {
			return new MainContextMenuItemsSettings(mainActionItems, hiddenMenuItems, ids);
		} else {
			return new ContextMenuItemsSettings(hiddenMenuItems, ids);
		}
	}

	public void toggleItemVisibility(@NonNull ContextMenuItem item) {
		boolean hidden = !item.isHidden();
		item.setHidden(hidden);

		String id = item.getId();
		if (hidden) {
			hiddenMenuItems.add(id);
			if (screenType == CONTEXT_MENU_ACTIONS) {
				mainActionItems.remove(id);
			}
		} else {
			hiddenMenuItems.remove(id);
			if (screenType == CONTEXT_MENU_ACTIONS && mainActionItems.size() < MAIN_BUTTONS_QUANTITY) {
				mainActionItems.add(id);
			}
		}
		isChanged = true;
	}

	public void onItemMoved(@Nullable String id, int position) {
		itemsOrder.put(id, position);
		isChanged = true;
	}

	public void resetToDefault() {
		hiddenMenuItems.clear();
		itemsOrder.clear();
		isChanged = true;
		if (screenType == CONTEXT_MENU_ACTIONS) {
			mainActionItems.clear();
		}
		createContextMenuAdapter();
		initSavedIds(appMode, true);
		initMainActionsIds(appMode, true);
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		isChanged = true;
		initSavedIds(appMode);
		if (screenType == CONTEXT_MENU_ACTIONS) {
			initMainActionsIds(appMode);
		}
	}

	@NonNull
	public List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(screenType);

		List<Object> visible = getItemsForRearrangeAdapter(hiddenMenuItems, itemsOrder, false);
		List<Object> hiddenItems = getItemsForRearrangeAdapter(hiddenMenuItems, itemsOrder, true);
		if (screenType == CONTEXT_MENU_ACTIONS) {
			int buttonMoreIndex = MAIN_BUTTONS_QUANTITY - 1;
			for (int i = 0; i < visible.size(); i++) {
				ContextMenuItem value = (ContextMenuItem) visible.get(i);
				if (value.getId() != null && value.getId().equals(MAP_CONTEXT_MENU_MORE_ID) && i > buttonMoreIndex) {
					Object third = visible.get(buttonMoreIndex);
					visible.set(buttonMoreIndex, visible.get(i));
					visible.set(i, third);
					value.setOrder(buttonMoreIndex);
					((ContextMenuItem) third).setOrder(i);
					break;
				}
			}

			List<Object> main = new ArrayList<>();
			List<Object> additional = new ArrayList<>();
			for (Object adapterItem : visible) {
				if (mainActionItems.contains(((ContextMenuItem) adapterItem).getId())) {
					main.add(adapterItem);
				} else {
					additional.add(adapterItem);
				}
			}
			items.add(new RearrangeHeaderItem(R.string.main_actions, app.getString(R.string.main_actions_descr)));
			items.addAll(main);
			items.add(new RearrangeHeaderItem(R.string.additional_actions,
					app.getString(R.string.additional_actions_descr, app.getString(R.string.shared_string_actions))));
			if (!additional.isEmpty()) {
				items.addAll(additional);
			}
		} else {
			items.addAll(visible);
		}
		if (!hiddenItems.isEmpty()) {
			items.add(new RearrangeHeaderItem(R.string.shared_string_hidden,
					app.getString(screenType == CONFIGURE_MAP ? R.string.reset_items_descr : R.string.hidden_items_descr)));
			items.addAll(hiddenItems);
		}
		items.add(DIVIDER_TYPE);
		items.add(new RearrangeButtonItem(
				R.string.reset_to_default,
				R.drawable.ic_action_reset_to_default_dark,
				view -> fragment.resetToDefault()));
		items.add(new RearrangeButtonItem(
				R.string.copy_from_other_profile,
				R.drawable.ic_action_copy,
				view -> showCopyAppModeDialog()));
		return items;
	}

	@NonNull
	public List<Object> getItemsForRearrangeAdapter(@Nullable List<String> hiddenItemsIds, @NonNull Map<String, Integer> itemsOrderIds, boolean hidden) {
		List<ContextMenuItem> defItems = ContextMenuUtils.getCustomizableItems(menuAdapter);
		if (!itemsOrderIds.isEmpty()) {
			sortByCustomOrder(defItems, itemsOrderIds);
		}
		List<Object> visibleItems = new ArrayList<>();
		List<Object> hiddenItems = new ArrayList<>();
		for (ContextMenuItem item : defItems) {
			String id = item.getId();
			if (hiddenItemsIds != null && hiddenItemsIds.contains(id)) {
				item.setHidden(true);
				hiddenItems.add(item);
			} else {
				item.setHidden(false);
				visibleItems.add(item);
			}
		}
		return hidden ? hiddenItems : visibleItems;
	}

	public void showCopyAppModeDialog() {
		FragmentManager manager = fragment.getFragmentManager();
		if (manager != null) {
			SelectCopyAppModeBottomSheet.showInstance(manager, fragment, appMode);
		}
	}

	public void sortByCustomOrder(@NonNull List<ContextMenuItem> items, @NonNull Map<String, Integer> orderIds) {
		for (ContextMenuItem item : items) {
			Integer order = orderIds.get(item.getId());
			if (order != null) {
				item.setOrder(order);
			}
		}
		Collections.sort(items, (item1, item2) -> {
			int order1 = item1.getOrder();
			int order2 = item2.getOrder();
			return Integer.compare(order1, order2);
		});
	}
}
