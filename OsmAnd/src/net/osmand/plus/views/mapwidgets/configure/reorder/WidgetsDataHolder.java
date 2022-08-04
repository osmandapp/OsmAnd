package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class WidgetsDataHolder {

	private static final String PAGES_ATTR = "pages_key";
	private static final String ORDERS_ATTR = "orders_key";
	private static final String SELECTED_PANEL_KEY = "selected_panel_key";

	private WidgetsPanel selectedPanel;
	private TreeMap<Integer, List<String>> pages = new TreeMap<>();
	private HashMap<String, Integer> orders = new HashMap<>();

	public WidgetsPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void setSelectedPanel(@NonNull WidgetsPanel selectedPanel) {
		this.selectedPanel = selectedPanel;
	}

	@NonNull
	public TreeMap<Integer, List<String>> getPages() {
		return pages;
	}

	@NonNull
	public HashMap<String, Integer> getOrders() {
		return orders;
	}

	public void initOrders(@NonNull MapActivity mapActivity, @NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		pages.clear();
		orders.clear();

		int filter = AVAILABLE_MODE | ENABLED_MODE;
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Set<MapWidgetInfo> widgets = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, filter, Collections.singletonList(selectedPanel));
		for (MapWidgetInfo widgetInfo : widgets) {
			int page = selectedPanel.getWidgetPage(appMode, widgetInfo.key, app.getSettings());
			int order = selectedPanel.getWidgetOrder(appMode, widgetInfo.key, app.getSettings());
			addWidgetToPage(widgetInfo.key, page);
			orders.put(widgetInfo.key, order);
		}
	}

	public void copyAppModePrefs(@NonNull OsmandApplication app, @NonNull ApplicationMode modeTo, @NonNull ApplicationMode modeFrom) {
		pages.clear();
		orders.clear();

		OsmandSettings settings = app.getSettings();
		List<List<String>> widgetsOrder = selectedPanel.getWidgetsOrder(modeFrom, settings);
		for (int page = 0; page < widgetsOrder.size(); page++) {
			List<String> pageOrder = widgetsOrder.get(page);
			for (int order = 0; order < pageOrder.size(); order++) {
				String widgetId = pageOrder.get(order);
				if (WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetId, modeTo)) {
					addWidgetToPage(widgetId, page);
					orders.put(widgetId, order);
				}
			}
		}
	}

	public void resetToDefault(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		pages.clear();
		orders.clear();

		List<String> originalOrder = selectedPanel.getOriginalOrder();
		for (int i = 0; i < originalOrder.size(); i++) {
			String widgetId = originalOrder.get(i);
			if (WidgetsAvailabilityHelper.isWidgetVisibleByDefault(app, widgetId, appMode)) {
				addWidgetToPage(widgetId, 0);
				orders.put(widgetId, i);
			}
		}
	}

	public int getWidgetPage(@NonNull String widgetId) {
		for (Entry<Integer, List<String>> entry : pages.entrySet()) {
			List<String> widgetsOfPage = entry.getValue();
			if (widgetsOfPage.contains(widgetId)) {
				return entry.getKey();
			}
		}
		return -1;
	}

	public void addWidgetToPage(@NonNull String widgetId, int page) {
		if (!selectedPanel.isDuplicatesAllowed()) {
			for (List<String> widgetsOfPage : pages.values()) {
				widgetsOfPage.remove(widgetId);
			}
		}

		List<String> widgetsOfPage = pages.get(page);
		if (widgetsOfPage == null) {
			widgetsOfPage = new ArrayList<>();
			pages.put(page, widgetsOfPage);
		}
		widgetsOfPage.add(widgetId);
	}

	public void addEmptyPage(int page) {
		pages.put(page, new ArrayList<>());
	}

	public void deletePage(int page) {
		TreeMap<Integer, List<String>> newPages = new TreeMap<>();
		for (Entry<Integer, List<String>> pageEntry : pages.entrySet()) {
			if (pageEntry.getKey() < page) {
				newPages.put(pageEntry.getKey(), pageEntry.getValue());
			} else if (pageEntry.getKey() > page) {
				newPages.put(pageEntry.getKey() - 1, pageEntry.getValue());
			}
		}

		pages = newPages;
	}

	public void deleteWidget(@NonNull String widgetId) {
		for (List<String> widgetsOfPage : pages.values()) {
			widgetsOfPage.remove(widgetId);
		}
		orders.remove(widgetId);
	}

	public void shiftPageOrdersToRight(int page) {
		for (Entry<String, Integer> entry : orders.entrySet()) {
			String widgetId = entry.getKey();
			int widgetPage = getWidgetPage(widgetId);
			int widgetOrder = entry.getValue();

			if (widgetPage == page) {
				orders.put(widgetId, widgetOrder + 1);
			}
		}
	}

	public int getMaxOrderOfPage(int page) {
		int maxOrder = -1;
		for (Entry<String, Integer> entry : orders.entrySet()) {
			String widgetId = entry.getKey();
			int widgetPage = getWidgetPage(widgetId);
			int widgetOrder = entry.getValue();

			if (widgetPage == page && widgetOrder > maxOrder) {
				maxOrder = widgetOrder;
			}
		}

		return maxOrder;
	}

	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(PAGES_ATTR, pages);
		outState.putSerializable(ORDERS_ATTR, orders);
		outState.putString(SELECTED_PANEL_KEY, selectedPanel.name());
	}

	public void restoreData(@NonNull Bundle bundle) {
		pages = (TreeMap<Integer, List<String>>) bundle.getSerializable(PAGES_ATTR);
		orders = (HashMap<String, Integer>) bundle.getSerializable(ORDERS_ATTR);
		selectedPanel = WidgetsPanel.valueOf(bundle.getString(SELECTED_PANEL_KEY));
	}
}