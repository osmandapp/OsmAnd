package net.osmand.plus.views.mapwidgets.configure;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

public class CopyWidgetsHelper {

	private final OsmandSettings settings;
	private final MapActivity mapActivity;
	private final ApplicationMode fromAppMode;
	private final ApplicationMode toAppMode;

	private final MapWidgetRegistry widgetRegistry;
	private final MapWidgetsFactory widgetsFactory;

	public static void copyWidgets(@NonNull MapActivity mapActivity,
	                               @NonNull ApplicationMode fromAppMode,
	                               @NonNull ApplicationMode toAppMode,
	                               @NonNull List<WidgetsPanel> targetPanels) {
		CopyWidgetsHelper copyWidgetsHelper = new CopyWidgetsHelper(mapActivity, fromAppMode, toAppMode);
		for (WidgetsPanel panel : targetPanels) {
			copyWidgetsHelper.copyWidgetsForPanel(panel);
		}
	}

	private CopyWidgetsHelper(@NonNull MapActivity mapActivity,
	                          @NonNull ApplicationMode fromAppMode,
	                          @NonNull ApplicationMode toAppMode) {
		OsmandApplication app = mapActivity.getMyApplication();

		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.fromAppMode = fromAppMode;
		this.toAppMode = toAppMode;
		this.widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		this.widgetsFactory = new MapWidgetsFactory(mapActivity);
	}

	private void copyWidgetsForPanel(@NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		List<MapWidgetInfo> defaultWidgetInfos = getDefaultWidgetInfos(panel);
		Set<MapWidgetInfo> widgetInfosToCopy = widgetRegistry
				.getWidgetsForPanel(mapActivity, fromAppMode, ENABLED_MODE | AVAILABLE_MODE, panels);
		List<List<String>> newPagedOrder = new ArrayList<>();
		int previousPage = -1;

		for (MapWidgetInfo widgetInfoToCopy : widgetInfosToCopy) {
			if (!toAppMode.isWidgetAvailable(widgetInfoToCopy.key)) {
				continue;
			}

			WidgetType widgetTypeToCopy = widgetInfoToCopy.widget.getWidgetType();
			boolean duplicateNotPossible = widgetTypeToCopy == null || !panel.isDuplicatesAllowed();
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfoToCopy.key);
			MapWidgetInfo defaultWidgetInfo = getWidgetInfoById(defaultWidgetId, defaultWidgetInfos);

			if (defaultWidgetInfo != null) {
				String widgetIdToAdd;
				boolean disabled = !defaultWidgetInfo.isEnabledForAppMode(toAppMode);
				boolean inAnotherPanel = defaultWidgetInfo.widgetPanel != panel;
				if (duplicateNotPossible || (disabled && !inAnotherPanel)) {
					widgetRegistry.enableDisableWidgetForMode(toAppMode, defaultWidgetInfo, true, false);
					widgetIdToAdd = defaultWidgetInfo.key;
				} else {
					MapWidgetInfo duplicateWidgetInfo = createDuplicateWidgetInfo(widgetTypeToCopy, panel);
					widgetIdToAdd = duplicateWidgetInfo != null ? duplicateWidgetInfo.key : null;
				}

				if (!Algorithms.isEmpty(widgetIdToAdd)) {
					if (previousPage != widgetInfoToCopy.pageIndex || newPagedOrder.size() == 0) {
						previousPage = widgetInfoToCopy.pageIndex;
						newPagedOrder.add(new ArrayList<>());
					}
					newPagedOrder.get(newPagedOrder.size() - 1).add(widgetIdToAdd);
				}
			}
		}

		panel.setWidgetsOrder(toAppMode, newPagedOrder, settings);
	}

	@NonNull
	private List<MapWidgetInfo> getDefaultWidgetInfos(@NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, toAppMode, 0, panel.getMergedPanels());
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetInfo.widgetPanel == panel) {
				Boolean visibility = WidgetType.isOriginalWidget(widgetInfo.key) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(toAppMode, widgetInfo, visibility, false);
			}
		}
		panel.getOrderPreference(settings).resetModeToDefault(toAppMode);
		return new ArrayList<>(widgetInfos);
	}

	@Nullable
	private MapWidgetInfo createDuplicateWidgetInfo(@NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		String duplicateWidgetId = WidgetType.getDuplicateWidgetId(widgetType.id);
		MapWidget duplicateWidget = widgetsFactory.createMapWidget(duplicateWidgetId, widgetType);
		if (duplicateWidget != null) {
			settings.CUSTOM_WIDGETS_KEYS.addModeValue(toAppMode, duplicateWidgetId);
			MapWidgetInfo duplicateWidgetInfo = widgetRegistry.createCustomWidget(duplicateWidgetId,
					duplicateWidget, widgetType, panel, toAppMode);
			widgetRegistry.enableDisableWidgetForMode(toAppMode, duplicateWidgetInfo, true, false);
			return duplicateWidgetInfo;
		}
		return null;
	}

	@Nullable
	private MapWidgetInfo getWidgetInfoById(@NonNull String widgetId, @NonNull List<MapWidgetInfo> widgetInfos) {
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetId.equals(widgetInfo.key)) {
				return widgetInfo;
			}
		}
		return null;
	}
}