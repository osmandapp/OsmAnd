package net.osmand.plus.utils;

import static net.osmand.plus.views.mapwidgets.MapWidgetInfo.DELIMITER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
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
		createNewWidgets(activity, widgetsIds, panel, appMode, recreateControls, null, null);
	}

	public static void createNewWidgets(@NonNull MapActivity activity, @NonNull List<String> widgetsIds,
	                                    @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode,
	                                    boolean recreateControls, @Nullable String selectedWidget, @Nullable Boolean addToNext) {
		OsmandApplication app = activity.getMyApplication();
		MapWidgetsFactory widgetsFactory = new MapWidgetsFactory(activity);
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		MapWidgetRegistry widgetRegistry = mapLayers.getMapWidgetRegistry();
		for (String widgetId : widgetsIds) {
			MapWidgetInfo widgetInfo = createDuplicateWidget(activity, widgetId, panel, widgetsFactory, appMode);
			if (widgetInfo != null) {
				if (addToNext != null && selectedWidget != null && widgetInfo.widget instanceof ISupportMultiRow) {
					addWidgetToSpecificPlace(activity, widgetInfo, panel, appMode, selectedWidget, addToNext);
				} else {
					addWidgetToEnd(activity, widgetInfo, panel, appMode);
				}
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, true, false);
			}
		}
		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		if (mapInfoLayer != null && recreateControls) {
			mapInfoLayer.recreateControls();
		}
	}

	@Nullable
	public static MapWidgetInfo createDuplicateWidget(@NonNull MapActivity mapActivity, @NonNull String widgetId, @NonNull WidgetsPanel panel,
													  @NonNull MapWidgetsFactory widgetsFactory, @NonNull ApplicationMode selectedAppMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		WidgetType widgetType = WidgetType.getById(widgetId);
		if (widgetType != null) {
			String id = widgetId.contains(DELIMITER) ? widgetId : WidgetType.getDuplicateWidgetId(widgetId);
			MapWidget widget = widgetsFactory.createMapWidget(id, widgetType, panel);
			if (widget != null) {
				app.getSettings().CUSTOM_WIDGETS_KEYS.addValue(id);
				WidgetInfoCreator creator = new WidgetInfoCreator(app, selectedAppMode);
				return creator.askCreateWidgetInfo(id, widget, widgetType, panel);
			}
		}
		return null;
	}

	private static void addWidgetToSpecificPlace(@NonNull MapActivity mapActivity, @NonNull MapWidgetInfo targetWidget,
	                                             @NonNull WidgetsPanel widgetsPanel, @NonNull ApplicationMode selectedAppMode, @NonNull String selectedWidget, boolean addToNext) {
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
			List<String> orders = pagedOrder.computeIfAbsent(page, k -> new ArrayList<>());
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
			List<List<String>> orders = new ArrayList<>(pagedOrder.values());
			int insertPage = 0;
			int insertOrder = 0;
			for (int page = 0; page < orders.size(); page++) {
				List<String> widgetPage = orders.get(page);
				for (int order = 0; order < widgetPage.size(); order++) {
					String widgetId = widgetPage.get(order);
					if (widgetId.equals(selectedWidget)) {
						insertPage = page;
						insertOrder = order;
					}
				}
			}
			List<String> pageToAddWidget = orders.get(insertPage);
			if (addToNext) {
				insertOrder++;
			}
			pageToAddWidget.add(insertOrder, targetWidget.key);

			for (int i = 0; i < pageToAddWidget.size(); i++) {
				String widgetId = pageToAddWidget.get(i);
				MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
				if (widgetInfo != null) {
					widgetInfo.pageIndex = insertPage;
					widgetInfo.priority = i;
				} else if (widgetId.equals(targetWidget.key)) {
					targetWidget.pageIndex = insertPage;
					targetWidget.priority = i;
				}
			}
			orders.add(insertPage, pageToAddWidget);
			widgetRegistry.getWidgetsForPanel(widgetsPanel).add(targetWidget);
			widgetsPanel.setWidgetsOrder(selectedAppMode, orders, settings);
		}
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
			List<String> orders = pagedOrder.computeIfAbsent(page, k -> new ArrayList<>());
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
