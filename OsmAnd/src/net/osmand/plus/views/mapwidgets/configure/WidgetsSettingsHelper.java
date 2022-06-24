package net.osmand.plus.views.mapwidgets.configure;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

public class WidgetsSettingsHelper {

	private final OsmandSettings settings;
	private final MapActivity mapActivity;
	private final ApplicationMode appMode;

	private final MapWidgetRegistry widgetRegistry;
	private final MapWidgetsFactory widgetsFactory;

	public static void resetConfigureScreenSettings(@NonNull MapActivity mapActivity,
	                                                @NonNull ApplicationMode appMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		Set<MapWidgetInfo> allWidgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, appMode, 0, Arrays.asList(WidgetsPanel.values()));
		for (MapWidgetInfo widgetInfo : allWidgetInfos) {
			widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, null, false);
		}
		settings.MAP_INFO_CONTROLS.resetModeToDefault(appMode);
		settings.CUSTOM_WIDGETS_KEYS.resetModeToDefault(appMode);

		for (WidgetsPanel panel : WidgetsPanel.values()) {
			panel.getOrderPreference(settings).resetModeToDefault(appMode);
		}

		settings.TRANSPARENT_MAP_THEME.resetModeToDefault(appMode);
		settings.SHOW_COMPASS_ALWAYS.resetModeToDefault(appMode);
		settings.SHOW_DISTANCE_RULER.resetModeToDefault(appMode);
		settings.QUICK_ACTION.resetModeToDefault(appMode);
	}

	public static void copyConfigureScreenSettings(@NonNull MapActivity mapActivity,
	                                               @NonNull ApplicationMode fromAppMode,
	                                               @NonNull ApplicationMode toAppMode) {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		WidgetsSettingsHelper widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, toAppMode);
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			widgetsSettingsHelper.copyWidgetsForPanel(fromAppMode, panel);
		}
		widgetsSettingsHelper.copyPrefFromAppMode(settings.TRANSPARENT_MAP_THEME, fromAppMode);
		widgetsSettingsHelper.copyPrefFromAppMode(settings.SHOW_COMPASS_ALWAYS, fromAppMode);
		widgetsSettingsHelper.copyPrefFromAppMode(settings.SHOW_DISTANCE_RULER, fromAppMode);
		widgetsSettingsHelper.copyPrefFromAppMode(settings.QUICK_ACTION, fromAppMode);
	}

	public static void copyWidgets(@NonNull MapActivity mapActivity,
	                               @NonNull ApplicationMode fromAppMode,
	                               @NonNull ApplicationMode toAppMode,
	                               @NonNull List<WidgetsPanel> targetPanels) {
		WidgetsSettingsHelper widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, toAppMode);
		for (WidgetsPanel panel : targetPanels) {
			widgetsSettingsHelper.copyWidgetsForPanel(fromAppMode, panel);
		}
	}

	public static void resetWidgetsForPanel(@NonNull MapActivity mapActivity,
	                                        @NonNull ApplicationMode appMode,
	                                        @NonNull WidgetsPanel panel) {
		WidgetsSettingsHelper widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, appMode);
		widgetsSettingsHelper.resetWidgetsForPanel(panel);
	}

	private WidgetsSettingsHelper(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode) {
		OsmandApplication app = mapActivity.getMyApplication();

		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.appMode = appMode;
		this.widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		this.widgetsFactory = new MapWidgetsFactory(mapActivity);
	}

	private void copyWidgetsForPanel(@NonNull ApplicationMode fromAppMode, @NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		List<MapWidgetInfo> defaultWidgetInfos = getDefaultWidgetInfos(panel);
		Set<MapWidgetInfo> widgetInfosToCopy = widgetRegistry
				.getWidgetsForPanel(mapActivity, fromAppMode, ENABLED_MODE | AVAILABLE_MODE, panels);
		List<List<String>> newPagedOrder = new ArrayList<>();
		int previousPage = -1;

		for (MapWidgetInfo widgetInfoToCopy : widgetInfosToCopy) {
			if (!appMode.isWidgetAvailable(widgetInfoToCopy.key)) {
				continue;
			}

			WidgetType widgetTypeToCopy = widgetInfoToCopy.widget.getWidgetType();
			boolean duplicateNotPossible = widgetTypeToCopy == null || !panel.isDuplicatesAllowed();
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfoToCopy.key);
			MapWidgetInfo defaultWidgetInfo = getWidgetInfoById(defaultWidgetId, defaultWidgetInfos);

			if (defaultWidgetInfo != null) {
				String widgetIdToAdd;
				boolean disabled = !defaultWidgetInfo.isEnabledForAppMode(appMode);
				boolean inAnotherPanel = defaultWidgetInfo.widgetPanel != panel;
				if (duplicateNotPossible || (disabled && !inAnotherPanel)) {
					widgetRegistry.enableDisableWidgetForMode(appMode, defaultWidgetInfo, true, false);
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

		panel.setWidgetsOrder(appMode, newPagedOrder, settings);
	}

	@NonNull
	private List<MapWidgetInfo> getDefaultWidgetInfos(@NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, appMode, 0, panel.getMergedPanels());
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetInfo.widgetPanel == panel) {
				Boolean visibility = WidgetType.isOriginalWidget(widgetInfo.key) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, visibility, false);
			}
		}
		panel.getOrderPreference(settings).resetModeToDefault(appMode);
		return new ArrayList<>(widgetInfos);
	}

	@Nullable
	private MapWidgetInfo createDuplicateWidgetInfo(@NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		String duplicateWidgetId = WidgetType.getDuplicateWidgetId(widgetType.id);
		MapWidget duplicateWidget = widgetsFactory.createMapWidget(duplicateWidgetId, widgetType);
		if (duplicateWidget != null) {
			settings.CUSTOM_WIDGETS_KEYS.addModeValue(appMode, duplicateWidgetId);
			MapWidgetInfo duplicateWidgetInfo = widgetRegistry.createCustomWidget(duplicateWidgetId,
					duplicateWidget, widgetType, panel, appMode);
			widgetRegistry.enableDisableWidgetForMode(appMode, duplicateWidgetInfo, true, false);
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

	private void resetWidgetsForPanel(@NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, appMode, 0, panels);
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			Boolean newEnableState = isOriginalWidgetOnAnotherPanel(widgetInfo)
					? false // Disable (not reset), because visible by default widget should be disabled in non-default panel
					: null;
			widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, newEnableState, false);
		}
		panel.getOrderPreference(settings).resetModeToDefault(appMode);
	}

	private boolean isOriginalWidgetOnAnotherPanel(@NonNull MapWidgetInfo widgetInfo) {
		boolean original = WidgetType.isOriginalWidget(widgetInfo.key);
		WidgetType widgetType = widgetInfo.widget.getWidgetType();
		return original && widgetType != null && widgetType.defaultPanel != widgetInfo.widgetPanel;
	}

	private <T> void copyPrefFromAppMode(@NonNull OsmandPreference<T> pref, @NonNull ApplicationMode fromAppMode) {
		pref.setModeValue(appMode, pref.getModeValue(fromAppMode));
	}
}