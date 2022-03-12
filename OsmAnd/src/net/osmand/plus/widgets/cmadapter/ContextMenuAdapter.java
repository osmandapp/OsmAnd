package net.osmand.plus.widgets.cmadapter;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.widgets.cmadapter.comparator.MenuItemsComparator;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContextMenuAdapter {

	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	private static final int ITEMS_ORDER_STEP = 10;

	private final OsmandApplication app;
	private final List<ContextMenuItem> items = new ArrayList<>();

	private @LayoutRes int defLayoutId = R.layout.list_menu_item_native;
	private boolean profileDependent = false;

	public ContextMenuAdapter(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public int length() {
		return items.size();
	}

	public String[] getItemNames() {
		String[] itemNames = new String[items.size()];
		for (int i = 0; i < items.size(); i++) {
			itemNames[i] = items.get(i).getTitle();
		}
		return itemNames;
	}

	public void addItem(ContextMenuItem item) {
		String id = item.getId();
		if (id != null) {
			item.setHidden(isItemHidden(id));
			item.setOrder(getItemOrder(id, item.getOrder()));
		}
		items.add(item);
		sortItemsByOrder();
	}

	public ContextMenuItem getItem(int position) {
		return items.get(position);
	}

	public List<ContextMenuItem> getItems() {
		return items;
	}

	public void clearAdapter() {
		items.clear();
	}

	public void setProfileDependent(boolean profileDependent) {
		this.profileDependent = profileDependent;
	}

	public void setDefaultLayoutId(int defaultLayoutId) {
		this.defLayoutId = defaultLayoutId;
	}

	private void sortItemsByOrder() {
		Collections.sort(items, new MenuItemsComparator());
	}

	private boolean isItemHidden(@NonNull String id) {
		OsmAndAppCustomization customization = app.getAppCustomization();
		if (!customization.isFeatureEnabled(id)) {
			return true;
		}
		ContextMenuItemsPreference preference = app.getSettings().getContextMenuItemsPreference(id);
		if (preference == null) {
			return false;
		}
		List<String> hiddenIds = preference.get().getHiddenIds();
		if (!Algorithms.isEmpty(hiddenIds)) {
			return hiddenIds.contains(id);
		}
		return false;
	}

	private int getItemOrder(@NonNull String id, int defaultOrder) {
		ContextMenuItemsPreference preference = app.getSettings().getContextMenuItemsPreference(id);
		if (preference != null) {
			List<String> orderIds = preference.get().getOrderIds();
			if (!Algorithms.isEmpty(orderIds)) {
				int index = orderIds.indexOf(id);
				if (index != -1) {
					return index;
				}
			}
		}
		return getDefaultOrder(defaultOrder);
	}

	private int getDefaultOrder(int defaultOrder) {
		if (defaultOrder == 0 && !items.isEmpty()) {
			return items.get(items.size() - 1).getOrder() + ITEMS_ORDER_STEP;
		} else {
			return defaultOrder;
		}
	}

	public ArrayAdapter<ContextMenuItem> createListAdapter(@NonNull Activity activity, boolean lightTheme) {
		removeHiddenItems(activity);
		ContextMenuItem[] _items = items.toArray(new ContextMenuItem[0]);
		return new ContextMenuArrayAdapter(activity, defLayoutId, _items, !lightTheme, profileDependent);
	}

	private void removeHiddenItems(@NonNull Activity activity) {
		OsmandApplication app = ((OsmandApplication) activity.getApplication());
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

	public List<ContextMenuItem> getDefaultItems() {
		String idScheme = getIdScheme();
		List<ContextMenuItem> items = new ArrayList<>();
		for (ContextMenuItem item : this.items) {
			String id = item.getId();
			if (id != null && (id.startsWith(idScheme))) {
				items.add(item);
			}
		}
		return items;
	}

	private String getIdScheme() {
		String idScheme = "";
		for (ContextMenuItem item : items) {
			String id = item.getId();
			if (id != null) {
				ContextMenuItemsPreference pref = app.getSettings().getContextMenuItemsPreference(id);
				if (pref != null) {
					return pref.getIdScheme();
				}
			}
		}
		return idScheme;
	}

	public List<ContextMenuItem> getVisibleItems() {
		List<ContextMenuItem> visible = new ArrayList<>();
		for (ContextMenuItem item : items) {
			if (!item.isHidden()) {
				visible.add(item);
			}
		}
		return visible;
	}

}