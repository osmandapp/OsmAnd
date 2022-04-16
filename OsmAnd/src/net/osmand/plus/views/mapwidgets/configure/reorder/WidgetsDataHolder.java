package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.os.Bundle;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import androidx.annotation.NonNull;

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

	public void initOrders(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode, boolean orderByDefault) {
		pages.clear();
		orders.clear();
		if (orderByDefault) {
			List<String> orderIds = selectedPanel.getOriginalOrder();
			MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			for (MapWidgetInfo widgetInfo : widgetRegistry.getAvailableWidgetsForPanel(appMode, selectedPanel)) {
				int order = orderIds.indexOf(widgetInfo.key);
				if (order == -1) {
					order = WidgetsPanel.DEFAULT_ORDER;
				}
				addWidgetToPage(widgetInfo.key, 0);
				orders.put(widgetInfo.key, order);
			}
		} else {
			MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			for (MapWidgetInfo widgetInfo : widgetRegistry.getAvailableWidgetsForPanel(appMode, selectedPanel)) {
				int page = selectedPanel.getWidgetPage(appMode, widgetInfo.key, app.getSettings());
				int order = selectedPanel.getWidgetOrder(appMode, widgetInfo.key, app.getSettings());
				addWidgetToPage(widgetInfo.key, page);
				orders.put(widgetInfo.key, order);
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
		for (List<String> widgetsOfPage : pages.values()) {
			widgetsOfPage.remove(widgetId);
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