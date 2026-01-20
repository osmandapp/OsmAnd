package net.osmand.plus.views.mapwidgets;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.IComplexWidget;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportSidePanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportVerticalPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesBaseWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class MapWidgetRegistry {

	public static final String COLLAPSED_PREFIX = "+";
	public static final String HIDE_PREFIX = "-";
	public static final String SETTINGS_SEPARATOR = ";";

	public static final String WIDGET_COMPASS = "compass";

	public static final int DISABLED_MODE = 0x1;
	public static final int ENABLED_MODE = 0x2;
	public static final int AVAILABLE_MODE = 0x4;
	public static final int DEFAULT_MODE = 0x8;
	public static final int MATCHING_PANELS_MODE = 0x10;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private Map<WidgetsPanel, Set<MapWidgetInfo>> allWidgets = new HashMap<>();

	private List<WidgetsRegistryListener> listeners = new ArrayList<>();

	public MapWidgetRegistry(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public void updateWidgetsInfo(@NonNull MapActivity activity, @NonNull ApplicationMode appMode, @NonNull DrawSettings drawSettings) {
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
		for (MapWidgetInfo widgetInfo : getAllWidgets()) {
			if (widgetInfo.isEnabledForAppMode(appMode, layoutMode) || widgetInfo instanceof CenterWidgetInfo) {
				widgetInfo.widget.updateInfo(drawSettings);
			}
		}
	}

	public void removeSideWidgetInternal(TextInfoWidget widget) {
		Iterator<MapWidgetInfo> it = getLeftWidgets().iterator();
		while (it.hasNext()) {
			if (it.next().widget == widget) {
				it.remove();
			}
		}
		it = getRightWidgets().iterator();
		while (it.hasNext()) {
			if (it.next().widget == widget) {
				it.remove();
			}
		}
	}

	public void clearWidgets() {
		allWidgets.clear();
		notifyWidgetsCleared();
	}

	public boolean isAnyWidgetOfTypeVisible(@NonNull MapActivity activity, @NonNull WidgetType widgetType) {
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
		List<MapWidgetInfo> widgets = getWidgetInfoForType(widgetType);
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isEnabledForAppMode(appMode, layoutMode)) {
				return true;
			}
		}
		return false;
	}

	public boolean isWidgetVisible(@NonNull MapActivity activity, @NonNull String widgetId) {
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);

		MapWidgetInfo widgetInfo = getWidgetInfoById(widgetId);
		return widgetInfo != null && widgetInfo.isEnabledForAppMode(appMode, layoutMode);
	}

	public void enableDisableWidgetForMode(@NonNull ApplicationMode appMode,
	                                       @NonNull MapWidgetInfo widgetInfo,
	                                       @Nullable Boolean enabled,
										   @Nullable ScreenLayoutMode layoutMode,
	                                       boolean recreateControls) {
		widgetInfo.enableDisableForMode(appMode, enabled, layoutMode);
		notifyWidgetVisibilityChanged(widgetInfo);

		if (widgetInfo.isCustomWidget() && (enabled == null || !enabled)) {
			settings.getCustomWidgetsKeys(layoutMode).removeValue(widgetInfo.key);
		}

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (recreateControls && mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	public void removeWidget(@NonNull MapActivity mapActivity,
	                         @NonNull ApplicationMode appMode,
	                         @NonNull MapWidgetInfo widgetInfo,
	                         @Nullable ScreenLayoutMode layoutMode) {
		List<Set<MapWidgetInfo>> widgets = getPagedWidgetsForPanel(mapActivity, appMode, layoutMode, widgetInfo.getWidgetPanel(),
				AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE);

		Set<MapWidgetInfo> rowSet = widgets.stream()
				.filter(set -> set.contains(widgetInfo))
				.findFirst()
				.orElse(null);

		enableDisableWidgetForMode(appMode, widgetInfo, false, layoutMode, true);

		if (rowSet != null) {
			recreateWidgets(rowSet);
		}
	}

	private void recreateWidgets(@NonNull Set<MapWidgetInfo> mapWidgetInfos) {
		mapWidgetInfos.stream()
				.map(info -> info == null ? null : info.widget)
				.filter(Objects::nonNull)
				.filter(ISupportWidgetResizing.class::isInstance)
				.map(w -> (ISupportWidgetResizing) w)
				.forEach(ISupportWidgetResizing::recreateView);
	}

	public void addWidgetsRegistryListener(@NonNull WidgetsRegistryListener listener) {
		listeners = CollectionUtils.addToList(listeners, listener);
	}

	public void removeWidgetsRegistryListener(@NonNull WidgetsRegistryListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	private void notifyWidgetRegistered(@NonNull MapWidgetInfo widgetInfo) {
		if (!Algorithms.isEmpty(listeners)) {
			for (WidgetsRegistryListener listener : listeners) {
				listener.onWidgetRegistered(widgetInfo);
			}
		}
	}

	private void notifyWidgetsCleared() {
		if (!Algorithms.isEmpty(listeners)) {
			for (WidgetsRegistryListener listener : listeners) {
				listener.onWidgetsCleared();
			}
		}
	}

	private void notifyWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {
		if (!Algorithms.isEmpty(listeners)) {
			for (WidgetsRegistryListener listener : listeners) {
				listener.onWidgetVisibilityChanged(widgetInfo);
			}
		}
	}

	public void reorderWidgets(@Nullable ScreenLayoutMode layoutMode) {
		reorderWidgets(getAllWidgets(), layoutMode);
	}

	private void reorderWidgets(@NonNull List<MapWidgetInfo> widgetInfos, @Nullable ScreenLayoutMode layoutMode) {
		Map<WidgetsPanel, Set<MapWidgetInfo>> newAllWidgets = new HashMap<>();
		for (MapWidgetInfo widget : widgetInfos) {
			WidgetsPanel panel = widget.getUpdatedPanel(layoutMode);
			widget.pageIndex = panel.getWidgetPage(widget.key, settings, layoutMode);
			widget.priority = panel.getWidgetOrder(widget.key, settings, layoutMode);

			Set<MapWidgetInfo> widgetsOfPanel = newAllWidgets.get(panel);
			if (widgetsOfPanel == null) {
				widgetsOfPanel = new TreeSet<>();
				newAllWidgets.put(panel, widgetsOfPanel);
			}
			widgetsOfPanel.add(widget);
		}

		allWidgets = newAllWidgets;
	}

	@NonNull
	public List<MapWidgetInfo> getWidgetInfoForType(@NonNull WidgetType widgetType) {
		List<MapWidgetInfo> widgets = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : getAllWidgets()) {
			if (widgetInfo.getWidgetType() == widgetType) {
				widgets.add(widgetInfo);
			}
		}
		return widgets;
	}

	@Nullable
	public MapWidgetInfo getWidgetInfoById(@NonNull String widgetId) {
		for (MapWidgetInfo widgetInfo : getAllWidgets()) {
			if (widgetId.equals(widgetInfo.key)) {
				return widgetInfo;
			}
		}
		return null;
	}

	@NonNull
	public Set<MapWidgetInfo> getLeftWidgets() {
		return getWidgetsForPanel(WidgetsPanel.LEFT);
	}

	@NonNull
	public Set<MapWidgetInfo> getRightWidgets() {
		return getWidgetsForPanel(WidgetsPanel.RIGHT);
	}

	@NonNull
	public Set<MapWidgetInfo> getSideWidgets() {
		Set<MapWidgetInfo> sideWidgetsInfo = new HashSet<>();
		sideWidgetsInfo.addAll(getWidgetsForPanel(WidgetsPanel.LEFT));
		sideWidgetsInfo.addAll(getWidgetsForPanel(WidgetsPanel.RIGHT));
		return sideWidgetsInfo;
	}

	@NonNull
	public Set<MapWidgetInfo> getVerticalWidgets() {
		Set<MapWidgetInfo> verticalWidgetsInfo = new HashSet<>();
		verticalWidgetsInfo.addAll(getWidgetsForPanel(WidgetsPanel.TOP));
		verticalWidgetsInfo.addAll(getWidgetsForPanel(WidgetsPanel.BOTTOM));
		return verticalWidgetsInfo;
	}

	@NonNull
	public List<MapWidgetInfo> getAllWidgets() {
		List<MapWidgetInfo> widgets = new ArrayList<>();
		for (Set<MapWidgetInfo> panelWidgets : allWidgets.values()) {
			widgets.addAll(panelWidgets);
		}
		return widgets;
	}

	@NonNull
	public List<Set<MapWidgetInfo>> getPagedWidgetsForPanel(@NonNull MapActivity mapActivity,
	                                                        @NonNull ApplicationMode appMode,
	                                                        @Nullable ScreenLayoutMode layoutMode,
	                                                        @NonNull WidgetsPanel panel,
	                                                        int filterModes) {
		Map<Integer, Set<MapWidgetInfo>> widgetsByPages = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : getWidgetsForPanel(mapActivity, appMode, layoutMode, filterModes, Collections.singletonList(panel))) {
			int page = widgetInfo.pageIndex;
			Set<MapWidgetInfo> widgetsOfPage = widgetsByPages.get(page);
			if (widgetsOfPage == null) {
				widgetsOfPage = new TreeSet<>();
				widgetsByPages.put(page, widgetsOfPage);
			}
			widgetsOfPage.add(widgetInfo);
		}
		return new ArrayList<>(widgetsByPages.values());
	}

	@NonNull
	public Set<MapWidgetInfo> getWidgetsForPanel(@NonNull MapActivity mapActivity,
	                                             @NonNull ApplicationMode appMode,
	                                             @Nullable ScreenLayoutMode layoutMode,
	                                             int filterModes,
	                                             @NonNull List<WidgetsPanel> panels) {
		List<Class<?>> includedWidgetTypes = new ArrayList<>();
		boolean sidePanel = false, verticalPanel = false;
		if (panels.contains(WidgetsPanel.LEFT) || panels.contains(WidgetsPanel.RIGHT)) {
			sidePanel = true;
			includedWidgetTypes.add(SideWidgetInfo.class);
			includedWidgetTypes.add(SimpleWidgetInfo.class);
		}
		if (panels.contains(WidgetsPanel.TOP) || panels.contains(WidgetsPanel.BOTTOM)) {
			verticalPanel = true;
			includedWidgetTypes.add(CenterWidgetInfo.class);
			includedWidgetTypes.add(SimpleWidgetInfo.class);
		}
		List<MapWidgetInfo> widgetInfos = new ArrayList<>();
		if (settings.getApplicationMode() == appMode && (layoutMode == null || ScreenLayoutMode.getDefault(mapActivity) == layoutMode)) {
			widgetInfos.addAll(getAllWidgets());
		} else {
			widgetInfos.addAll(WidgetsInitializer.createAllControls(mapActivity, appMode, layoutMode));
		}
		Set<MapWidgetInfo> filteredWidgets = new TreeSet<>();
		for (MapWidgetInfo widget : widgetInfos) {
			boolean panelSupported = false;
			if (sidePanel) {
				panelSupported = widget.widget instanceof ISupportSidePanel;
			} else if (verticalPanel) {
				panelSupported = widget.widget instanceof ISupportVerticalPanel;
			}
			if (panelSupported || includedWidgetTypes.contains(widget.getClass())) {
				boolean disabledMode = (filterModes & DISABLED_MODE) == DISABLED_MODE;
				boolean enabledMode = (filterModes & ENABLED_MODE) == ENABLED_MODE;
				boolean availableMode = (filterModes & AVAILABLE_MODE) == AVAILABLE_MODE;
				boolean defaultMode = (filterModes & DEFAULT_MODE) == DEFAULT_MODE;
				boolean matchingPanelsMode = (filterModes & MATCHING_PANELS_MODE) == MATCHING_PANELS_MODE;

				boolean passDisabled = !disabledMode || !widget.isEnabledForAppMode(appMode, layoutMode);
				boolean passEnabled = !enabledMode || widget.isEnabledForAppMode(appMode, layoutMode);
				boolean passAvailable = !availableMode || WidgetsAvailabilityHelper.isWidgetAvailable(app, widget.key, appMode);
				boolean defaultAvailable = !defaultMode || !widget.isCustomWidget();
				boolean passMatchedPanels = !matchingPanelsMode || panels.contains(widget.getWidgetPanel());
				boolean passTypeAllowed = widget.getWidgetType() == null || widget.getWidgetType().isAllowed();
				boolean passPanelAllowed = widget.getWidgetType() == null || widget.getWidgetType().isPanelsAllowed(panels);

				if (passDisabled && passEnabled && passAvailable && defaultAvailable && passMatchedPanels && passTypeAllowed && passPanelAllowed) {
					filteredWidgets.add(widget);
				}
			}
		}
		return filteredWidgets;
	}

	@NonNull
	public Set<MapWidgetInfo> getWidgetsForPanel(@NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgets = allWidgets.get(panel);
		if (widgets == null) {
			widgets = new TreeSet<>();
			allWidgets.put(panel, widgets);
		}
		return widgets;
	}

	@ColorRes
	public int getStatusBarColor(@NonNull Context context, @NonNull ApplicationMode appMode, boolean nightMode) {
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(context);
		Set<MapWidgetInfo> topWidgetsInfo = getWidgetsForPanel(WidgetsPanel.TOP);
		for (MapWidgetInfo widgetInfo : topWidgetsInfo) {
			MapWidget widget = widgetInfo.widget;
			if (!widget.isViewVisible() || !widgetInfo.isEnabledForAppMode(appMode, layoutMode)) {
				continue;
			}
			if (widget instanceof StreetNameWidget) {
				return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
			} else if (widget instanceof MapMarkersBarWidget) {
				return R.color.status_bar_main_dark;
			} else if (widget instanceof SimpleWidget || widget instanceof CoordinatesBaseWidget || widget instanceof IComplexWidget) {
				return nightMode ? R.color.status_bar_secondary_dark : R.color.status_bar_secondary_light;
			} else {
				return -1;
			}
		}

		return -1;
	}

	public boolean getStatusBarContentNightMode(@NonNull Context context, @NonNull ApplicationMode appMode, boolean nightMode) {
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(context);
		Set<MapWidgetInfo> topWidgetsInfo = getWidgetsForPanel(WidgetsPanel.TOP);
		for (MapWidgetInfo widgetInfo : topWidgetsInfo) {
			MapWidget widget = widgetInfo.widget;
			if (!widget.isViewVisible() || !widgetInfo.isEnabledForAppMode(appMode, layoutMode)) {
				continue;
			}
			if (widget instanceof SimpleWidget || widget instanceof CoordinatesBaseWidget || widget instanceof IComplexWidget) {
				return nightMode;
			} else {
				return true;
			}
		}
		return true;
	}

	public void registerWidget(@NonNull MapWidgetInfo widgetInfo) {
		getWidgetsForPanel(widgetInfo.getWidgetPanel()).add(widgetInfo);
		notifyWidgetRegistered(widgetInfo);
	}

	public void registerAllControls(@NonNull MapActivity mapActivity) {
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
		List<MapWidgetInfo> infos = WidgetsInitializer.createAllControls(mapActivity, appMode, layoutMode);
		reorderWidgets(infos, layoutMode);

		for (MapWidgetInfo widgetInfo : infos) {
			notifyWidgetRegistered(widgetInfo);
		}
	}

	public interface WidgetsRegistryListener {
		void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo);

		void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo);

		void onWidgetsCleared();
	}
}