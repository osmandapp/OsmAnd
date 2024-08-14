package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class WidgetsDataHolder {

	private static final String PAGES_ATTR = "pages_key";
	private static final String SELECTED_PANEL_KEY = "selected_panel_key";

	private WidgetsPanel selectedPanel;
	private TreeMap<Integer, List<String>> pages = new TreeMap<>();

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

	public void setPages(@NonNull TreeMap<Integer, List<String>> newPageOrder) {
		this.pages = newPageOrder;
	}

	public void copyAppModePrefs(@NonNull MapActivity activity, @NonNull ApplicationMode modeTo, @NonNull ApplicationMode modeFrom) {
		pages.clear();

		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		OsmandApplication app = activity.getMyApplication();
		WidgetsSettingsHelper helper = new WidgetsSettingsHelper(activity, modeFrom);
		List<List<String>> widgetsOrder = helper.getWidgetsPagedOrder(modeFrom, selectedPanel, filter);
		for (int page = 0; page < widgetsOrder.size(); page++) {
			List<String> pageOrder = widgetsOrder.get(page);
			for (int order = 0; order < pageOrder.size(); order++) {
				String widgetId = pageOrder.get(order);
				if (WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetId, modeTo)) {
					addWidgetToPage(widgetId, page);
				}
			}
		}
	}

	public void resetToDefault(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		pages.clear();

		List<String> originalOrder = selectedPanel.getOriginalOrder();
		for (int addedWRowIndex = 0, i = 0; i < originalOrder.size(); i++) {
			String widgetId = originalOrder.get(i);
			if (WidgetsAvailabilityHelper.isWidgetVisibleByDefault(app, widgetId, appMode)) {
				if (selectedPanel.isPanelVertical()) {
					addWidgetToPage(widgetId, addedWRowIndex);
					addedWRowIndex++;
				} else {
					addWidgetToPage(widgetId, 0);
				}
			}
		}
	}

	public void addWidgetToPage(@NonNull String widgetId, int page) {
		List<String> widgetsOfPage = pages.get(page);
		if (widgetsOfPage == null) {
			widgetsOfPage = new ArrayList<>();
			pages.put(page, widgetsOfPage);
		}
		if (!widgetsOfPage.contains(widgetId)) {
			widgetsOfPage.add(widgetId);
		}
	}

	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(PAGES_ATTR, pages);
		outState.putString(SELECTED_PANEL_KEY, selectedPanel.name());
	}

	public void restoreData(@NonNull Bundle bundle) {
		pages = (TreeMap<Integer, List<String>>) AndroidUtils.getSerializable(bundle, PAGES_ATTR, TreeMap.class);
		selectedPanel = WidgetsPanel.valueOf(bundle.getString(SELECTED_PANEL_KEY));
	}
}