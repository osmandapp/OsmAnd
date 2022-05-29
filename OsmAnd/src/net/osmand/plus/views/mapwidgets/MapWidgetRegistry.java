package net.osmand.plus.views.mapwidgets;

import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class MapWidgetRegistry {

	public static final String COLLAPSED_PREFIX = "+";
	public static final String HIDE_PREFIX = "-";
	public static final String SETTINGS_SEPARATOR = ";";

	public static final String WIDGET_COMPASS = "compass";

	public static final int DISABLED_MODE = 0x1;
	public static final int ENABLED_MODE = 0x2;
	public static final int AVAILABLE_MODE = 0x4;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private Map<WidgetsPanel, Set<MapWidgetInfo>> allWidgets = new HashMap<>();

	private Set<WidgetsVisibilityListener> visibilityListeners = new HashSet<>();

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
			if (widgetInfo.isEnabledForAppMode(appMode)) {
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
	}

	public <T extends TextInfoWidget> T getSideWidget(Class<T> cl) {
		for (MapWidgetInfo ri : getLeftWidgets()) {
			if (cl.isInstance(ri)) {
				//noinspection unchecked
				return (T) ri.widget;
			}
		}
		for (MapWidgetInfo ri : getRightWidgets()) {
			if (cl.isInstance(ri)) {
				//noinspection unchecked
				return (T) ri.widget;
			}
		}
		return null;
	}

	@NonNull
	public MapWidgetInfo registerWidget(@NonNull String key,
	                                    @NonNull MapWidget widget,
	                                    @Nullable WidgetState widgetState,
	                                    @DrawableRes int daySettingsIconId,
	                                    @DrawableRes int nightSettingIconId,
	                                    @StringRes int messageId,
	                                    @Nullable String message,
	                                    int page,
	                                    int order,
	                                    @NonNull WidgetsPanel widgetPanel) {
		MapWidgetInfo widgetInfo;
		if (widget instanceof TextInfoWidget) {
			TextInfoWidget textInfoWidget = ((TextInfoWidget) widget);
			widgetInfo = new SideWidgetInfo(key, textInfoWidget, widgetState, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		} else {
			widgetInfo = new CenterWidgetInfo(key, widget, widgetState, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		}

		getWidgetsForPanel(widgetPanel).add(widgetInfo);

		return widgetInfo;
	}

	public boolean isWidgetVisible(@NonNull String widgetId) {
		ApplicationMode appMode = settings.getApplicationMode();
		MapWidgetInfo widgetInfo = getWidgetInfoById(widgetId);
		return widgetInfo != null && widgetInfo.isEnabledForAppMode(appMode);
	}

	public void enableDisableWidget(@NonNull MapWidgetInfo widgetInfo, boolean enabled) {
		ApplicationMode appMode = settings.getApplicationMode();
		enableDisableWidgetForMode(appMode, widgetInfo, enabled);
	}

	public void enableDisableWidgetForMode(@NonNull ApplicationMode appMode,
	                                       @NonNull MapWidgetInfo widgetInfo,
	                                       boolean enabled) {
		widgetInfo.enableDisableForMode(appMode, enabled);
		notifyWidgetVisibilityChanged(widgetInfo);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	public void addWidgetsVisibilityListener(@NonNull WidgetsVisibilityListener visibilityListener) {
		Set<WidgetsVisibilityListener> visibilityListeners = new HashSet<>(this.visibilityListeners);
		visibilityListeners.add(visibilityListener);
		this.visibilityListeners = visibilityListeners;
	}

	public void removeWidgetsVisibilityListener(@NonNull WidgetsVisibilityListener visibilityListener) {
		Set<WidgetsVisibilityListener> visibilityListeners = new HashSet<>(this.visibilityListeners);
		visibilityListeners.remove(visibilityListener);
		this.visibilityListeners = visibilityListeners;
	}

	private void notifyWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {
		if (!Algorithms.isEmpty(visibilityListeners)) {
			for (WidgetsVisibilityListener listener : visibilityListeners) {
				listener.onWidgetVisibilityChanged(widgetInfo);
			}
		}
	}

	public void reorderWidgets() {
		Map<WidgetsPanel, Set<MapWidgetInfo>> newAllWidgets = new HashMap<>();
		for (MapWidgetInfo widget : getAllWidgets()) {
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
	public List<Set<MapWidgetInfo>> getPagedWidgetsForPanel(@NonNull ApplicationMode appMode,
	                                                        @NonNull WidgetsPanel panel,
	                                                        int filterModes) {
		Map<Integer, Set<MapWidgetInfo>> widgetsByPages = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : getWidgetsForPanel(appMode, filterModes, panel)) {
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
	public Set<MapWidgetInfo> getWidgetsForPanel(@NonNull ApplicationMode appMode,
	                                             int filterModes,
	                                             @NonNull WidgetsPanel... panels) {
		Set<MapWidgetInfo> filteredWidgets = new TreeSet<>();
		List<WidgetsPanel> panelsList = Arrays.asList(panels);

		for (MapWidgetInfo widget : getAllWidgets()) {

			if (!panelsList.contains(widget.widgetPanel)) {
				continue;
			}

			boolean disabledMode = (filterModes & DISABLED_MODE) == DISABLED_MODE;
			boolean enabledMode = (filterModes & ENABLED_MODE) == ENABLED_MODE;
			boolean availableMode = (filterModes & AVAILABLE_MODE) == AVAILABLE_MODE;

			boolean passDisabled = !disabledMode || !widget.isEnabledForAppMode(appMode);
			boolean passEnabled = !enabledMode || widget.isEnabledForAppMode(appMode);
			boolean passAvailable = !availableMode || appMode.isWidgetAvailable(widget.key);

			if (passDisabled && passEnabled && passAvailable) {
				filteredWidgets.add(widget);
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

			if (widget instanceof CoordinatesWidget) {
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

	public interface WidgetsVisibilityListener {
		void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo);
	}
}