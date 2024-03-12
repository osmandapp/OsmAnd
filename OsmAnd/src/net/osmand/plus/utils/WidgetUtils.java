package net.osmand.plus.utils;

import static net.osmand.plus.views.mapwidgets.MapWidgetInfo.DELIMITER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WidgetUtils {

	public static void createNewWidgets(@NonNull MapActivity activity, @NonNull List<String> widgetsIds,
										@NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode,
										boolean recreateControls) {
		OsmandApplication app = activity.getMyApplication();
		MapWidgetsFactory widgetsFactory = new MapWidgetsFactory(activity);
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		MapWidgetRegistry widgetRegistry = mapLayers.getMapWidgetRegistry();
		for (String widgetId : widgetsIds) {
			MapWidgetInfo widgetInfo = createDuplicateWidget(app, widgetId, panel, widgetsFactory, appMode);
			if (widgetInfo != null) {
				addWidgetToEnd(activity, widgetInfo, panel, appMode);
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, true, false);
			}
		}
		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		if (mapInfoLayer != null && recreateControls) {
			mapInfoLayer.recreateControls();
		}
	}

	public static MapWidgetInfo createDuplicateWidget(@NonNull OsmandApplication app, @NonNull String widgetId, @NonNull WidgetsPanel panel,
													  @NonNull MapWidgetsFactory widgetsFactory, @NonNull ApplicationMode selectedAppMode) {
		WidgetType widgetType = WidgetType.getById(widgetId);
		if (widgetType != null) {
			String id = widgetId.contains(DELIMITER) ? widgetId : WidgetType.getDuplicateWidgetId(widgetId);
			MapWidget widget = widgetsFactory.createMapWidget(id, widgetType, panel);
			if (widget != null) {
				app.getSettings().CUSTOM_WIDGETS_KEYS.addValue(id);
				WidgetInfoCreator creator = new WidgetInfoCreator(app, selectedAppMode);
				return creator.createCustomWidgetInfo(id, widget, widgetType, panel);
			}
		}
		return null;
	}

	private static void addWidgetToEnd(@NonNull MapActivity mapActivity, @NonNull MapWidgetInfo targetWidget,
									   @NonNull WidgetsPanel widgetsPanel, @NonNull ApplicationMode selectedAppMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		Map<Integer, List<String>> pagedOrder = new TreeMap<>();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(mapActivity,
				selectedAppMode, ENABLED_MODE | MATCHING_PANELS_MODE, Collections.singletonList(widgetsPanel));

		widgetRegistry.getWidgetsForPanel(targetWidget.getWidgetPanel()).remove(targetWidget);
		targetWidget.setWidgetPanel(widgetsPanel);

		for (MapWidgetInfo widget : enabledWidgets) {
			int page = widget.pageIndex;
			List<String> orders = pagedOrder.get(page);
			if (orders == null) {
				orders = new ArrayList<>();
				pagedOrder.put(page, orders);
			}
			orders.add(widget.key);
		}

		if (Algorithms.isEmpty(pagedOrder)) {
			targetWidget.pageIndex = 0;
			targetWidget.priority = 0;
			widgetRegistry.getWidgetsForPanel(widgetsPanel).add(targetWidget);

			List<List<String>> flatOrder = new ArrayList<>();
			flatOrder.add(Collections.singletonList(targetWidget.key));
			widgetsPanel.setWidgetsOrder(selectedAppMode, flatOrder, settings);
		} else {
			List<Integer> pages = new ArrayList<>(pagedOrder.keySet());
			List<List<String>> orders = new ArrayList<>(pagedOrder.values());
			List<String> lastPageOrder = orders.get(orders.size() - 1);

			if (widgetsPanel.isPanelVertical()) {
				List<String> newPage = new ArrayList<>();
				newPage.add(targetWidget.key);
				orders.add(newPage);
				targetWidget.pageIndex = getNewNextPageIndex(pages) + 1;
				targetWidget.priority = 0;
			} else {
				lastPageOrder.add(targetWidget.key);

				String previousLastWidgetId = lastPageOrder.get(lastPageOrder.size() - 2);
				MapWidgetInfo previousLastVisibleWidgetInfo = widgetRegistry.getWidgetInfoById(previousLastWidgetId);
				int lastPage;
				int lastOrder;
				if (previousLastVisibleWidgetInfo != null) {
					lastPage = previousLastVisibleWidgetInfo.pageIndex;
					lastOrder = previousLastVisibleWidgetInfo.priority + 1;
				} else {
					lastPage = pages.get(pages.size() - 1);
					lastOrder = lastPageOrder.size() - 1;
				}
				targetWidget.pageIndex = lastPage;
				targetWidget.priority = lastOrder;
			}

			widgetRegistry.getWidgetsForPanel(widgetsPanel).add(targetWidget);
			widgetsPanel.setWidgetsOrder(selectedAppMode, orders, settings);
		}
	}

	private static int getNewNextPageIndex(List<Integer> pages) {
		int maxPage = 0;
		for (Integer integer : pages) {
			if (integer > maxPage) {
				maxPage = integer;
			}
		}
		return maxPage;
	}
}
