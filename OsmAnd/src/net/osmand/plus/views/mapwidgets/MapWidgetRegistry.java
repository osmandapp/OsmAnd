package net.osmand.plus.views.mapwidgets;

import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesBaseWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private Map<WidgetsPanel, Set<MapWidgetInfo>> allWidgets = new HashMap<>();

	private List<WidgetsRegistryListener> listeners = new ArrayList<>();

	public MapWidgetRegistry(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public void populateControlsContainer(@NonNull ViewGroup container,
	                                      @NonNull ApplicationMode mode,
	                                      @NonNull WidgetsPanel widgetPanel) {
		Set<MapWidgetInfo> widgets = getWidgetsForPanel(widgetPanel);

		List<MapWidget> widgetsToShow = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isEnabledForAppMode(mode)) {
				widgetsToShow.add(widgetInfo.widget);
			} else {
				widgetInfo.widget.detachView();
			}
		}

		for (int i = 0; i < widgetsToShow.size(); i++) {
			MapWidget widget = widgetsToShow.get(i);
			List<MapWidget> followingWidgets = i + 1 == widgetsToShow.size()
					? new ArrayList<>()
					: widgetsToShow.subList(i + 1, widgetsToShow.size());
			widget.attachView(container, i, followingWidgets);
		}
	}

	public void updateWidgetsInfo(@NonNull ApplicationMode appMode, @NonNull DrawSettings drawSettings) {
		for (MapWidgetInfo widgetInfo : getAllWidgets()) {
			if (widgetInfo.isEnabledForAppMode(appMode) || widgetInfo instanceof CenterWidgetInfo) {
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

	public boolean isAnyWidgetOfTypeVisible(@NonNull WidgetType widgetType) {
		ApplicationMode appMode = settings.getApplicationMode();
		List<MapWidgetInfo> widgets = getWidgetInfoForType(widgetType);
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isEnabledForAppMode(appMode)) {
				return true;
			}
		}
		return false;
	}

	public boolean isWidgetVisible(@NonNull MapWidgetInfo widgetInfo) {
		return isWidgetVisible(widgetInfo.key);
	}

	public boolean isWidgetVisible(@NonNull String widgetId) {
		ApplicationMode appMode = settings.getApplicationMode();
		MapWidgetInfo widgetInfo = getWidgetInfoById(widgetId);
		return widgetInfo != null && widgetInfo.isEnabledForAppMode(appMode);
	}

	public void enableDisableWidgetForMode(@NonNull ApplicationMode appMode,
	                                       @NonNull MapWidgetInfo widgetInfo,
	                                       @Nullable Boolean enabled,
	                                       boolean recreateControls) {
		widgetInfo.enableDisableForMode(appMode, enabled);
		notifyWidgetVisibilityChanged(widgetInfo);

		if (widgetInfo.isCustomWidget() && (enabled == null || !enabled)) {
			settings.CUSTOM_WIDGETS_KEYS.removeValue(widgetInfo.key);
		}

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (recreateControls && mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	public void addWidgetsRegistryListener(@NonNull WidgetsRegistryListener listener) {
		listeners = Algorithms.addToList(listeners, listener);
	}

	public void removeWidgetsRegistryListener(@NonNull WidgetsRegistryListener listener) {
		listeners = Algorithms.removeFromList(listeners, listener);
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

	public void reorderWidgets() {
		reorderWidgets(getAllWidgets());
	}

	private void reorderWidgets(@NonNull List<MapWidgetInfo> widgetInfos) {
		Map<WidgetsPanel, Set<MapWidgetInfo>> newAllWidgets = new HashMap<>();
		for (MapWidgetInfo widget : widgetInfos) {
			WidgetsPanel panel = widget.getUpdatedPanel();
			widget.pageIndex = panel.getWidgetPage(widget.key, settings);
			widget.priority = panel.getWidgetOrder(widget.key, settings);

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
	                                                        @NonNull WidgetsPanel panel,
	                                                        int filterModes) {
		Map<Integer, Set<MapWidgetInfo>> widgetsByPages = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : getWidgetsForPanel(mapActivity, appMode, filterModes, Collections.singletonList(panel))) {
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
	                                             int filterModes,
	                                             @NonNull List<WidgetsPanel> panels) {
		List<MapWidgetInfo> widgetInfos = new ArrayList<>();
		if (settings.getApplicationMode() == appMode) {
			widgetInfos.addAll(getAllWidgets());
		} else {
			widgetInfos.addAll(WidgetsInitializer.createAllControls(mapActivity, appMode));
		}
		Set<MapWidgetInfo> filteredWidgets = new TreeSet<>();
		for (MapWidgetInfo widget : widgetInfos) {
			if (panels.contains(widget.widgetPanel)) {
				boolean disabledMode = (filterModes & DISABLED_MODE) == DISABLED_MODE;
				boolean enabledMode = (filterModes & ENABLED_MODE) == ENABLED_MODE;
				boolean availableMode = (filterModes & AVAILABLE_MODE) == AVAILABLE_MODE;
				boolean defaultMode = (filterModes & DEFAULT_MODE) == DEFAULT_MODE;

				boolean passDisabled = !disabledMode || !widget.isEnabledForAppMode(appMode);
				boolean passEnabled = !enabledMode || widget.isEnabledForAppMode(appMode);
				boolean passAvailable = !availableMode || WidgetsAvailabilityHelper.isWidgetAvailable(app, widget.key, appMode);
				boolean defaultAvailable = !defaultMode || !widget.isCustomWidget();

				if (passDisabled && passEnabled && passAvailable && defaultAvailable) {
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
	public int getStatusBarColorForTopWidget(boolean night) {
		Set<MapWidgetInfo> topWidgetsInfo = getWidgetsForPanel(WidgetsPanel.TOP);
		for (MapWidgetInfo widgetInfo : topWidgetsInfo) {
			MapWidget widget = widgetInfo.widget;
			if (!widget.isViewVisible()) {
				continue;
			}

			if (widget instanceof CoordinatesBaseWidget) {
				return R.color.status_bar_main_dark;
			} else if (widget instanceof StreetNameWidget) {
				return night ? R.color.status_bar_route_dark : R.color.status_bar_route_light;
			} else if (widget instanceof MapMarkersBarWidget) {
				return R.color.status_bar_color_dark;
			} else {
				return -1;
			}
		}

		return -1;
	}

	public void registerWidget(@NonNull MapWidgetInfo widgetInfo) {
		getWidgetsForPanel(widgetInfo.widgetPanel).add(widgetInfo);
		notifyWidgetRegistered(widgetInfo);
	}

	public void registerAllControls(@NonNull MapActivity mapActivity) {
		ApplicationMode appMode = settings.getApplicationMode();
		List<MapWidgetInfo> infos = WidgetsInitializer.createAllControls(mapActivity, appMode);
		reorderWidgets(infos);

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