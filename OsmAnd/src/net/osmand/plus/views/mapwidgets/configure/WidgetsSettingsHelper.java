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
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.AndroidAutoWidgetsInitializer.AndroidAutoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.appearance.PanelAppearanceSettingsManager;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WidgetsSettingsHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity mapActivity;

	private final MapWidgetRegistry widgetRegistry;
	private final MapWidgetsFactory widgetsFactory;
	private final AndroidAutoWidgetsFactory androidAutoWidgetsFactory;
	private final MapButtonsHelper mapButtonsHelper;
	private final PanelAppearanceSettingsManager appearanceSettingsManager;

	private ApplicationMode appMode;
	private ScreenLayoutMode layoutMode;

	public WidgetsSettingsHelper(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode) {
		this.app = mapActivity.getApp();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.appMode = appMode;
		this.widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		this.widgetsFactory = new MapWidgetsFactory(mapActivity);
		this.androidAutoWidgetsFactory = new AndroidAutoWidgetsFactory(app);
		this.mapButtonsHelper = app.getMapButtonsHelper();
		this.appearanceSettingsManager = app.getPanelAppearanceSettingsManager();
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public void setLayoutMode(@Nullable ScreenLayoutMode layoutMode) {
		this.layoutMode = layoutMode;
	}

	public void resetConfigureScreenSettings() {
		Set<MapWidgetInfo> allWidgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode,
				layoutMode, MATCHING_PANELS_MODE, WidgetsPanel.getMapPanels());
		for (MapWidgetInfo widgetInfo : allWidgetInfos) {
			widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, null, layoutMode, false);
		}

		Set<MapWidgetInfo> allAndroidAutoWidgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode,
				MATCHING_PANELS_MODE, List.of(WidgetsPanel.ANDROID_AUTO));
		for (MapWidgetInfo widgetInfo : allAndroidAutoWidgetInfos) {
			widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, widgetInfo, null);
		}

		settings.getMapInfoControls(layoutMode).resetModeToDefault(appMode);
		settings.getCustomWidgetsKeys(layoutMode).resetModeToDefault(appMode);
		settings.AA_WIDGETS_VISIBILITY.resetModeToDefault(appMode);
		settings.getAndroidAutoCustomWidgetsKeys().resetModeToDefault(appMode);


		for (WidgetsPanel panel : WidgetsPanel.getMapPanels()) {
			panel.getOrderPreference(settings, layoutMode).resetModeToDefault(appMode);
		}
		WidgetsPanel.ANDROID_AUTO.getOrderPreference(settings, null).resetModeToDefault(appMode);

		settings.getPanelsLayoutMode(mapActivity, layoutMode).resetModeToDefault(appMode);
		settings.getTransparentMapThemePreference(layoutMode).resetModeToDefault(appMode);
		for (WidgetsPanel panel : WidgetsPanel.getMapPanels()) {
			appearanceSettingsManager.get(panel).resetToDefault(appMode, layoutMode);
		}
		appearanceSettingsManager.get(WidgetsPanel.ANDROID_AUTO).resetToDefault(appMode, null);

		mapButtonsHelper.getCompassButtonState().getVisibilityPref().resetModeToDefault(appMode);
		settings.SHOW_DISTANCE_RULER.resetModeToDefault(appMode);
		mapButtonsHelper.resetButtonStatesForMode(appMode, mapButtonsHelper.getAllButtonsStates());
		mapButtonsHelper.getDefaultSizePref().resetModeToDefault(appMode);
		mapButtonsHelper.getDefaultOpacityPref().resetModeToDefault(appMode);
		mapButtonsHelper.getDefaultCornerRadiusPref().resetModeToDefault(appMode);
	}

	public void copyConfigureScreenSettings(@NonNull ApplicationMode fromAppMode) {
		for (WidgetsPanel panel : WidgetsPanel.getMapPanels()) {
			copyWidgetsForPanel(fromAppMode, layoutMode, panel);
		}
		if (fromAppMode.isAndroidAutoCompatible()) {
			copyWidgetsForPanel(fromAppMode, null, WidgetsPanel.ANDROID_AUTO);
		}

		copyPrefFromAppMode(settings.getPanelsLayoutMode(mapActivity, layoutMode), fromAppMode);
		copyPrefFromAppMode(settings.getTransparentMapThemePreference(layoutMode), fromAppMode);
		for (WidgetsPanel panel : WidgetsPanel.getMapPanels()) {
			appearanceSettingsManager.get(panel).copyFromProfile(fromAppMode, appMode, layoutMode);
		}
		if (fromAppMode.isAndroidAutoCompatible()) {
			appearanceSettingsManager.get(WidgetsPanel.ANDROID_AUTO).copyFromProfile(fromAppMode, appMode, null);
		}

		copyPrefFromAppMode(mapButtonsHelper.getCompassButtonState().getVisibilityPref(), fromAppMode);
		copyPrefFromAppMode(settings.SHOW_DISTANCE_RULER, fromAppMode);
		copyPrefFromAppMode(settings.POSITION_PLACEMENT_ON_MAP, fromAppMode);
		copyPrefFromAppMode(settings.DISTANCE_BY_TAP_TEXT_SIZE, fromAppMode);
		copyPrefFromAppMode(settings.SHOW_SPEEDOMETER, fromAppMode);
		copyPrefFromAppMode(settings.SPEEDOMETER_SIZE, fromAppMode);
		copyPrefFromAppMode(mapButtonsHelper.getDefaultSizePref(), fromAppMode);
		copyPrefFromAppMode(mapButtonsHelper.getDefaultOpacityPref(), fromAppMode);
		copyPrefFromAppMode(mapButtonsHelper.getDefaultCornerRadiusPref(), fromAppMode);
		mapButtonsHelper.copyButtonStatesFromMode(appMode, fromAppMode, mapButtonsHelper.getAllButtonsStates());
	}

	public void applyWidgetsSize(@NonNull WidgetsPanel panel, @NonNull WidgetSize size) {
		for (MapWidgetInfo widgetInfo : getEnabledWidgetsForPanel(panel)) {
			if (widgetInfo.widget instanceof ISupportWidgetResizing resizableWidget
					&& resizableWidget.allowResize()) {
				resizableWidget.getWidgetSizePref().setModeValue(appMode, size);
			}
		}
	}

	public void applyWidgetsIconVisibility(@NonNull WidgetsPanel panel, boolean showIcon) {
		for (MapWidgetInfo widgetInfo : getEnabledWidgetsForPanel(panel)) {
			if (widgetInfo.widget instanceof SimpleWidget simpleWidget) {
				simpleWidget.shouldShowIconPref().setModeValue(appMode, showIcon);
			}
		}
	}

	@NonNull
	private Set<MapWidgetInfo> getEnabledWidgetsForPanel(@NonNull WidgetsPanel panel) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		if (panel.isAndroidAutoPanel()) {
			return widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode, filter, Collections.singletonList(panel));
		} else {
			return widgetRegistry.getWidgetsForPanel(mapActivity, appMode, layoutMode, filter,
					Collections.singletonList(panel));
		}
	}

	public void copyWidgetsForPanel(@NonNull ApplicationMode fromAppMode,
	                                @Nullable ScreenLayoutMode fromLayoutMode,
	                                @NonNull WidgetsPanel panel) {
		boolean isAndroidAutoPanel = panel.isAndroidAutoPanel();
		boolean isFromAndroidAutoMode = fromAppMode.isAndroidAutoCompatible();
		boolean isToAndroidAutoMode = appMode.isAndroidAutoCompatible();
		boolean copyAndroidAutoWidgets = isAndroidAutoPanel && isFromAndroidAutoMode && isToAndroidAutoMode;
		boolean copyMapWidgets = !isAndroidAutoPanel;
		if (!(copyMapWidgets || copyAndroidAutoWidgets)) {
			return;
		}

		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfosToCopy;
		if (copyAndroidAutoWidgets) {
			widgetInfosToCopy = widgetRegistry.getAndroidAutoWidgetsForPanel(app, fromAppMode, filter, panels);
		} else {
			widgetInfosToCopy = widgetRegistry.getWidgetsForPanel(mapActivity, fromAppMode, fromLayoutMode, filter, panels);
		}

		int previousPage = -1;
		List<List<String>> newPagedOrder = new ArrayList<>();
		List<MapWidgetInfo> defaultWidgetInfos = getDefaultWidgetInfos(panel);
		List<String> widgetsVisibility;
		if (copyAndroidAutoWidgets) {
			widgetsVisibility = MapWidgetInfo.getAndroidAutoWidgetsVisibility(app, appMode);
		} else {
			widgetsVisibility = MapWidgetInfo.getWidgetsVisibility(app, appMode, layoutMode);
		}

		for (MapWidgetInfo widgetInfoToCopy : widgetInfosToCopy) {
			if (!WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfoToCopy.key, appMode)) {
				continue;
			}

			WidgetType widgetTypeToCopy = widgetInfoToCopy.widget.getWidgetType();
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfoToCopy.key);
			MapWidgetInfo defaultWidgetInfo = getWidgetInfoById(defaultWidgetId, defaultWidgetInfos);

			if (defaultWidgetInfo != null) {
				String widgetIdToAdd = null;
				boolean duplicateNotPossible = widgetTypeToCopy == null;
				boolean disabled = !defaultWidgetInfo.isEnabledForAppMode(appMode, widgetsVisibility);
				boolean inAnotherPanel = defaultWidgetInfo.getWidgetPanel() != panel;
				boolean defaultAlreadyUsed = newPagedOrder.stream()
						.anyMatch(page -> page.contains(defaultWidgetInfo.key));

				boolean canReuseDefault = (duplicateNotPossible || (disabled && !inAnotherPanel)) && !defaultAlreadyUsed;

				if (canReuseDefault) {
					if (copyAndroidAutoWidgets) {
						widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, defaultWidgetInfo, true);
					} else {
						widgetRegistry.enableDisableWidgetForMode(appMode, defaultWidgetInfo, true, layoutMode, false);
					}
					widgetIdToAdd = defaultWidgetInfo.key;
				} else if (widgetTypeToCopy != null) {
					MapWidgetInfo duplicateWidgetInfo = createDuplicateWidgetInfo(widgetTypeToCopy, panel);
					widgetIdToAdd = duplicateWidgetInfo != null ? duplicateWidgetInfo.key : null;
				}

				if (!Algorithms.isEmpty(widgetIdToAdd)) {
					String customId = !widgetIdToAdd.equals(defaultWidgetInfo.key) ? widgetIdToAdd : null;
					widgetInfoToCopy.widget.copySettingsFromMode(fromAppMode, appMode, customId);

					if (previousPage != widgetInfoToCopy.pageIndex || newPagedOrder.isEmpty()) {
						previousPage = widgetInfoToCopy.pageIndex;
						newPagedOrder.add(new ArrayList<>());
					}
					newPagedOrder.get(newPagedOrder.size() - 1).add(widgetIdToAdd);
				}
			}
		}
		panel.setWidgetsOrder(appMode, newPagedOrder, settings, layoutMode);
	}

	public List<List<MapWidgetInfo>> getWidgetInfoPagedOrder(@NonNull ApplicationMode fromAppMode, @NonNull ApplicationMode toAppMode, @NonNull WidgetsPanel panel, int filter) {
		int previousPage = -1;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos;
		if (panel.isAndroidAutoPanel()) {
			widgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, fromAppMode, filter, panels);
		} else {
			widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, fromAppMode, layoutMode, filter, panels);
		}
		List<List<MapWidgetInfo>> pagedOrder = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			String widgetId = widgetInfo.key;
			if (!Algorithms.isEmpty(widgetId) && WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetId, appMode)) {
				if (previousPage != widgetInfo.pageIndex || pagedOrder.isEmpty()) {
					previousPage = widgetInfo.pageIndex;
					pagedOrder.add(new ArrayList<>());
				}
				if (WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetId, toAppMode)) {
					pagedOrder.get(pagedOrder.size() - 1).add(widgetInfo);
				}
			}
		}
		return pagedOrder;
	}

	public List<List<String>> getWidgetsPagedOrder(@NonNull ApplicationMode fromAppMode, @NonNull WidgetsPanel panel, int filter) {
		int previousPage = -1;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, fromAppMode, layoutMode, filter, panels);
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
		Set<MapWidgetInfo> widgetInfos;
		boolean isAndroidAutoPanel = panel.isAndroidAutoPanel();
		if (isAndroidAutoPanel) {
			widgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode, 0, Collections.singletonList(panel));
		} else {
			widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, layoutMode, 0, Collections.singletonList(panel));
		}
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetInfo.getWidgetPanel() == panel) {
				Boolean visibility = WidgetType.isOriginalWidget(widgetInfo.key) ? false : null;
				if (isAndroidAutoPanel) {
					widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, widgetInfo, visibility);
				} else {
					widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, visibility, layoutMode, false);
				}
			}
		}
		panel.getOrderPreference(settings, layoutMode).resetModeToDefault(appMode);
		return new ArrayList<>(widgetInfos);
	}

	@Nullable
	private MapWidgetInfo createDuplicateWidgetInfo(@NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		String duplicateWidgetId = WidgetType.getDuplicateWidgetId(widgetType);
		MapWidget duplicateWidget;
		boolean isAndroidAuto = panel.isAndroidAutoPanel();
		if (isAndroidAuto) {
			duplicateWidget = androidAutoWidgetsFactory.createMapWidget(duplicateWidgetId, widgetType, panel);
		} else {
			duplicateWidget = widgetsFactory.createMapWidget(duplicateWidgetId, widgetType, panel);
		}
		if (duplicateWidget != null) {
			WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);
			MapWidgetInfo duplicateWidgetInfo;
			if (isAndroidAuto) {
				duplicateWidgetInfo = creator.askCreateAndroidWidgetInfo(
						duplicateWidgetId, duplicateWidget, widgetType, panel
				);
			} else {
				duplicateWidgetInfo = creator.askCreateWidgetInfo(
						duplicateWidgetId, duplicateWidget, widgetType, panel
				);
			}
			if (duplicateWidgetInfo != null) {
				if (isAndroidAuto) {
					settings.getAndroidAutoCustomWidgetsKeys().addModeValue(appMode, duplicateWidgetId);
					widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, duplicateWidgetInfo, true);
				} else {
					settings.getCustomWidgetsKeys(layoutMode).addModeValue(appMode, duplicateWidgetId);
					widgetRegistry.enableDisableWidgetForMode(appMode, duplicateWidgetInfo, true, layoutMode, false);
				}
				return duplicateWidgetInfo;
			}
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
		if (panel.isAndroidAutoPanel()) {
			doResetWidgetsForAndroidAutoPanel(panel);
		} else {
			doResetWidgetsForPanel(panel);
		}
	}
	private void doResetWidgetsForPanel(@NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, layoutMode, MATCHING_PANELS_MODE, panels);
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (WidgetType.isOriginalWidget(widgetInfo.key) && WidgetsAvailabilityHelper.isWidgetVisibleByDefault(app, widgetInfo.key, appMode)) {
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, true, layoutMode, false);
			} else {
				// Disable "false" (not reset "null"), because visible by default widget should be disabled in non-default panel
				Boolean enabled = isOriginalWidgetOnAnotherPanel(widgetInfo) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, enabled, layoutMode, false);
			}
		}
		panel.getOrderPreference(settings, layoutMode).resetModeToDefault(appMode);
	}

	private void doResetWidgetsForAndroidAutoPanel(@NonNull WidgetsPanel panel) {
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode, MATCHING_PANELS_MODE, panels);
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (WidgetType.isOriginalWidget(widgetInfo.key) && WidgetsAvailabilityHelper.isWidgetVisibleByDefault(app, widgetInfo.key, appMode)) {
				widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, widgetInfo, true);
			} else {
				// Disable "false" (not reset "null"), because visible by default widget should be disabled in non-default panel
				Boolean enabled = isOriginalWidgetOnAnotherPanel(widgetInfo) ? false : null;
				widgetRegistry.enableDisableAndroidAutoWidgetForMode(appMode, widgetInfo, enabled);
			}
		}
		panel.getOrderPreference(settings, null).resetModeToDefault(appMode);
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