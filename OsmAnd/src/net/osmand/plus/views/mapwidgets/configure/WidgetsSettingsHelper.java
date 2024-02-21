package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WidgetsSettingsHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	private final MapWidgetRegistry widgetRegistry;
	private final MapWidgetsFactory widgetsFactory;
	private final MapButtonsHelper mapButtonsHelper;

	private ApplicationMode appMode;

	public WidgetsSettingsHelper(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode) {
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.appMode = appMode;
		this.widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		this.widgetsFactory = new MapWidgetsFactory(mapActivity);
		this.mapButtonsHelper = app.getMapButtonsHelper();
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public void resetConfigureScreenSettings() {
		Set<MapWidgetInfo> allWidgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, MATCHING_PANELS_MODE, Arrays.asList(WidgetsPanel.values()));
		for (MapWidgetInfo widgetInfo : allWidgetInfos) {
			widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, null, false);
		}
		settings.MAP_INFO_CONTROLS.resetModeToDefault(appMode);
		settings.CUSTOM_WIDGETS_KEYS.resetModeToDefault(appMode);

		for (WidgetsPanel panel : WidgetsPanel.values()) {
			panel.getOrderPreference(settings).resetModeToDefault(appMode);
		}

		settings.TRANSPARENT_MAP_THEME.resetModeToDefault(appMode);
		mapButtonsHelper.getCompassButtonState().getVisibilityPref().resetModeToDefault(appMode);
		settings.SHOW_DISTANCE_RULER.resetModeToDefault(appMode);
		mapButtonsHelper.resetQuickActionsForMode(appMode);
	}

	public void copyConfigureScreenSettings(@NonNull ApplicationMode fromAppMode) {
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			copyWidgetsForPanel(fromAppMode, panel);
		}
		copyPrefFromAppMode(settings.TRANSPARENT_MAP_THEME, fromAppMode);
		copyPrefFromAppMode(mapButtonsHelper.getCompassButtonState().getVisibilityPref(), fromAppMode);
		copyPrefFromAppMode(settings.SHOW_DISTANCE_RULER, fromAppMode);
		mapButtonsHelper.copyQuickActionsFromMode(settings.getApplicationMode(), fromAppMode);
	}

	public void copyWidgetsForPanel(@NonNull ApplicationMode fromAppMode, @NonNull WidgetsPanel panel) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfosToCopy = widgetRegistry.getWidgetsForPanel(mapActivity, fromAppMode, filter, panels);

		int previousPage = -1;
		List<List<String>> newPagedOrder = new ArrayList<>();
		List<MapWidgetInfo> defaultWidgetInfos = getDefaultWidgetInfos(panel);

		for (MapWidgetInfo widgetInfoToCopy : widgetInfosToCopy) {
			if (!WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfoToCopy.key, appMode)) {
				continue;
			}

			WidgetType widgetTypeToCopy = widgetInfoToCopy.widget.getWidgetType();
			boolean duplicateNotPossible = widgetTypeToCopy == null;
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfoToCopy.key);
			MapWidgetInfo defaultWidgetInfo = getWidgetInfoById(defaultWidgetId, defaultWidgetInfos);

			if (defaultWidgetInfo != null) {
				String widgetIdToAdd;
				boolean disabled = !defaultWidgetInfo.isEnabledForAppMode(appMode);
				boolean inAnotherPanel = defaultWidgetInfo.getWidgetPanel() != panel;
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

	public List<List<String>> getWidgetsPagedOrder(@NonNull ApplicationMode fromAppMode, @NonNull WidgetsPanel panel, int filter) {
		int previousPage = -1;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, fromAppMode, filter, panels);
		List<List<String>> pagedOrder = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			String widgetId = widgetInfo.key;
			if (!Algorithms.isEmpty(widgetId) && WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetId, appMode)) {
				if (previousPage != widgetInfo.pageIndex || pagedOrder.size() == 0) {
					previousPage = widgetInfo.pageIndex;
					pagedOrder.add(new ArrayList<>());
				}
				pagedOrder.get(pagedOrder.size() - 1).add(widgetId);
			}
		}
		return pagedOrder;
	}

	@NonNull
	private List<MapWidgetInfo> getDefaultWidgetInfos(@NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, 0, Collections.singletonList(panel));
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetInfo.getWidgetPanel() == panel) {
				Boolean visibility = WidgetType.isOriginalWidget(widgetInfo.key) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, visibility, false);
			}
		}
		panel.getOrderPreference(settings).resetModeToDefault(appMode);
		return new ArrayList<>(widgetInfos);
	}

	@Nullable
	private MapWidgetInfo createDuplicateWidgetInfo(@NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		String duplicateWidgetId = WidgetType.getDuplicateWidgetId(widgetType);
		MapWidget duplicateWidget = widgetsFactory.createMapWidget(duplicateWidgetId, widgetType, panel);
		if (duplicateWidget != null) {
			WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
			settings.CUSTOM_WIDGETS_KEYS.addModeValue(appMode, duplicateWidgetId);
			MapWidgetInfo duplicateWidgetInfo = creator.createCustomWidgetInfo(
					duplicateWidgetId, duplicateWidget, widgetType, panel);
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

	public void resetWidgetsForPanel(@NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, MATCHING_PANELS_MODE, panels);
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfo.key);
			if (WidgetsAvailabilityHelper.isWidgetVisibleByDefault(app, defaultWidgetId, appMode)) {
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, true, false);
			} else {
				// Disable "false" (not reset "null"), because visible by default widget should be disabled in non-default panel
				Boolean enabled = isOriginalWidgetOnAnotherPanel(widgetInfo) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, enabled, false);
			}
		}
		panel.getOrderPreference(settings).resetModeToDefault(appMode);
	}

	private boolean isOriginalWidgetOnAnotherPanel(@NonNull MapWidgetInfo widgetInfo) {
		boolean original = WidgetType.isOriginalWidget(widgetInfo.key);
		WidgetType widgetType = widgetInfo.widget.getWidgetType();
		return original && widgetType != null && widgetType.defaultPanel != widgetInfo.getWidgetPanel();
	}

	private <T> void copyPrefFromAppMode(@NonNull OsmandPreference<T> pref, @NonNull ApplicationMode fromAppMode) {
		pref.setModeValue(appMode, pref.getModeValue(fromAppMode));
	}
}