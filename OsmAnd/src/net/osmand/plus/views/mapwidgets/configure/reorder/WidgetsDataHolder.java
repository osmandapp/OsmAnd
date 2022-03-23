package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.HashMap;
import java.util.List;

public class WidgetsDataHolder {

	private static final String ORDERS_ATTR = "orders_key";
	private static final String SELECTED_PANEL_KEY = "selected_panel_key";

	private WidgetsPanel selectedPanel;
	private HashMap<String, Integer> orders = new HashMap<>();

	public WidgetsPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void setSelectedPanel(@NonNull WidgetsPanel selectedPanel) {
		this.selectedPanel = selectedPanel;
	}

	@NonNull
	public HashMap<String, Integer> getOrders() {
		return orders;
	}

	public int getOrder(@NonNull String key) {
		Integer order = orders.get(key);
		return order != null ? order : -1;
	}

	public void initOrders(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode, boolean orderByDefault) {
		orders.clear();
		if (orderByDefault) {
			List<String> orderIds = selectedPanel.getOriginalOrder();
			MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			for (MapWidgetInfo widgetInfo : widgetRegistry.getAvailableWidgetsForPanel(appMode, selectedPanel)) {
				int order = orderIds.indexOf(widgetInfo.key);
				if (order == -1) {
					order = orderIds.size() + 1;
				}
				orders.put(widgetInfo.key, order);
			}
		} else {
			MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
			for (MapWidgetInfo widgetInfo : widgetRegistry.getAvailableWidgetsForPanel(appMode, selectedPanel)) {
				int order = selectedPanel.getWidgetOrder(appMode, widgetInfo.key, app.getSettings());
				orders.put(widgetInfo.key, order);
			}
		}
	}

	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(ORDERS_ATTR, orders);
		outState.putString(SELECTED_PANEL_KEY, selectedPanel.name());
	}

	public void restoreData(@NonNull Bundle bundle) {
		orders = (HashMap<String, Integer>) bundle.getSerializable(ORDERS_ATTR);
		selectedPanel = WidgetsPanel.valueOf(bundle.getString(SELECTED_PANEL_KEY));
	}
}