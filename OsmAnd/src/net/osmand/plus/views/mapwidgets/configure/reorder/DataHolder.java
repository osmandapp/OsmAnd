package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.HashMap;

public class DataHolder {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";
	private static final String ORDERS_ATTR = "orders_key";
	private static final String ORDER_MODIFIED_ATTR = "order_modified_key";

	private HashMap<String, Integer> orders = new HashMap<>();
	private boolean orderModified = false;
	private WidgetsPanel selectedPanel;

	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
		outState.putSerializable(ORDERS_ATTR, orders);
		outState.putBoolean(ORDER_MODIFIED_ATTR, orderModified);
	}

	public void restoreData(@NonNull Bundle bundle) {
		String groupName = bundle.getString(SELECTED_GROUP_ATTR);
		selectedPanel = WidgetsPanel.valueOf(groupName);
		orders = (HashMap<String, Integer>) bundle.getSerializable(ORDERS_ATTR);
		orderModified = bundle.getBoolean(ORDER_MODIFIED_ATTR);
	}

	public WidgetsPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void setSelectedPanel(WidgetsPanel selectedPanel) {
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

	public boolean isOrderModified() {
		return orderModified;
	}

	public void orderModified() {
		this.orderModified = true;
	}

	/**
	 * Clear and fill in the order of widgets.
	 * Called each time when need to initialize the order data for some profile.
	 * @param appMode application profile from which to get order.
	 * @param orderByDefault indicates whether you want to get the default order of widgets for this profile.
	 */
	public void initOrders(@NonNull ApplicationMode appMode,
	                       boolean orderByDefault) {
		orders.clear();
		if (orderByDefault) {
			orderModified();
		}
//		List<WidgetItem> widgets = WidgetsRegister.getSortedWidgets(appMode, selectedPanel, orderByDefault);
//		for (WidgetItem widget : widgets) {
//			orders.put(widget.title, widget.priority);
//		}
	}

}
