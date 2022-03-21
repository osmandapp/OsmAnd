package net.osmand.plus.widgets.ctxmenu;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.widgets.ctxmenu.comparator.MenuItemsComparator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.ctxmenu.data.ExpandableCategory;
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
	private int defLayoutId = R.layout.list_menu_item_native;
	private boolean useProfileColor;

	public ContextMenuAdapter(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public int length() {
		return items.size();
	}

	public List<String> getNames() {
		return ContextMenuUtils.getNames(items);
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

	public void setUseProfileColor(boolean useProfileColor) {
		this.useProfileColor = useProfileColor;
	}

	public void setDefaultLayoutId(int defLayoutId) {
		this.defLayoutId = defLayoutId;
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

	public ArrayAdapter<ContextMenuItem> createListAdapter(@NonNull Activity activity, boolean nightMode) { // TODO "createListAdapter" -> "toListAdapter"
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		consolidate();

		Integer controlsColor = null;
		if (useProfileColor) {
			ApplicationMode appMode = settings.getApplicationMode();
			controlsColor = appMode.getProfileColor(nightMode);
		}

		ViewBinder viewCreator = new ViewBinder(activity, defLayoutId, controlsColor, nightMode);

		return new ArrayAdapter<ContextMenuItem>(activity, defLayoutId, R.id.title, items) {

			@Override
			public boolean isEnabled(int position) {
				final ContextMenuItem item = getItem(position);
				if (item != null) {
					return !item.isCategory() && item.isClickable() && item.getLayout() != R.layout.drawer_divider;
				}
				return true;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return viewCreator.getView(getItem(position), convertView);
			}
		};
	}

	public ContextMenuAdapter consolidate() {
		removeHidden();
		processCategories();
		processTotalPosition();
		processDividers();
		return this;
	}

	private void removeHidden() {
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

	private void processDividers() {
		for (int i = 0; i < items.size() - 1; i++) {
			// Hide bottom divider for each item before next category
			ContextMenuItem item = items.get(i);
			ContextMenuItem next = items.get(i + 1);
			item.setHideDivider(next.isCategory());
		}
		// Hide bottom divider for last item in list
		items.get(items.size() - 1).setHideDivider(true);
	}

	private void processTotalPosition() {
		for (int i = 0; i < items.size(); i++) {
			items.get(i).setPosition(i);
		}
	}

	private void processCategories() {
		Set<ContextMenuItem> categories = new HashSet<>();
		for (ContextMenuItem item : items) {
		}
	}

	public List<ContextMenuItem> populateCategorizedItems() {
		ExpandableCategory c = null;
		List<ContextMenuItem> list = new ArrayList<>();
		for (ContextMenuItem item : items) {
			if (item instanceof ExpandableCategory) {
				if (c != null) {
					list.add(c);
				}
				c = (ExpandableCategory) item;
			} else if (c != null) {
				c.addItem(item);
			} else {
				list.add(item);
			}
		}
		return list;
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