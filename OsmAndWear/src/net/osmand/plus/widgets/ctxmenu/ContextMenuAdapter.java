package net.osmand.plus.widgets.ctxmenu;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.widgets.ctxmenu.comparator.MenuItemsComparator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContextMenuAdapter {

	private static final int ITEMS_ORDER_STEP = 10;

	private final List<ContextMenuItem> items = new ArrayList<>();
	private final OsmandApplication app;

	public ContextMenuAdapter(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public int length() {
		return items.size();
	}

	public void addItem(ContextMenuItem item) {
		String id = item.getId();
		if (id != null) {
			item.setHidden(isItemHidden(id));
			item.setOrder(getItemOrder(id, item.getOrder()));
		}
		items.add(item);
		Collections.sort(items, new MenuItemsComparator());
	}

	public ContextMenuItem getItem(int position) {
		return items.get(position);
	}

	public List<ContextMenuItem> getItems() {
		return items;
	}

	@Nullable
	public ContextMenuItem getItemById(@NonNull String id) {
		for (ContextMenuItem item : items) {
			if (Algorithms.objectEquals(item.getId(), id)) {
				return item;
			}
		}
		return null;
	}

	public void clear() {
		items.clear();
	}

	public OsmandApplication getApplication() {
		return app;
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

	public List<ContextMenuItem> getVisibleItems() {
		List<ContextMenuItem> visible = new ArrayList<>();
		for (ContextMenuItem item : items) {
			if (!item.isHidden()) {
				visible.add(item);
			}
		}
		return visible;
	}

	public ContextMenuListAdapter toListAdapter(@NonNull Activity activity,
	                                            @NonNull ViewCreator viewCreator) {
		ContextMenuUtils.removeHiddenItems(this);
		ContextMenuUtils.hideExtraDividers(this);
		return new ContextMenuListAdapter(activity, viewCreator, items);
	}

}