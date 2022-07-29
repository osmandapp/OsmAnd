package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE;
import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetType.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.ELEVATION_PROFILE;
import static net.osmand.plus.views.mapwidgets.WidgetType.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetType.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.LANES;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetType.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.WidgetType.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetType.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;
import static net.osmand.plus.views.mapwidgets.WidgetType.SMALL_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetType.STREET_NAME;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_INTERMEDIATE;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRUE_BEARING;

import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
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
import java.util.concurrent.CopyOnWriteArrayList;

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

	private final List<WidgetsRegistryListener> listeners = new CopyOnWriteArrayList<>();

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
		listeners.add(listener);
	}

	public void removeWidgetsRegistryListener(@NonNull WidgetsRegistryListener listener) {
		listeners.remove(listener);
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
			widgetInfos.addAll(createAllControls(mapActivity, appMode));
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

	public void registerWidget(@NonNull MapWidgetInfo widgetInfo) {
		getWidgetsForPanel(widgetInfo.widgetPanel).add(widgetInfo);
		notifyWidgetRegistered(widgetInfo);
	}

	public void registerAllControls(@NonNull MapActivity mapActivity) {
		List<MapWidgetInfo> widgetInfos = createAllControls(mapActivity, settings.getApplicationMode());
		reorderWidgets(widgetInfos);

		for (MapWidgetInfo widgetInfo : widgetInfos) {
			notifyWidgetRegistered(widgetInfo);
		}
	}

	@NonNull
	public List<MapWidgetInfo> createAllControls(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode) {
		List<MapWidgetInfo> widgetInfos = new ArrayList<>();
		MapWidgetsFactory widgetsFactory = new MapWidgetsFactory(mapActivity);

		createTopWidgets(widgetsFactory, widgetInfos, appMode);
		createBottomWidgets(widgetsFactory, widgetInfos, appMode);
		createLeftWidgets(widgetsFactory, widgetInfos, appMode);
		createRightWidgets(widgetsFactory, widgetInfos, appMode);

		OsmandPlugin.createMapWidgets(mapActivity, widgetInfos, appMode);
		app.getAidlApi().createWidgetControls(mapActivity, widgetInfos, appMode);
		createCustomWidgets(widgetsFactory, appMode, widgetInfos);

		return widgetInfos;
	}

	private void createTopWidgets(@NonNull MapWidgetsFactory factory, @NonNull List<MapWidgetInfo> infos, @NonNull ApplicationMode appMode) {
		infos.add(createWidgetInfo(factory, COORDINATES, appMode));
		infos.add(createWidgetInfo(factory, STREET_NAME, appMode));
		infos.add(createWidgetInfo(factory, LANES, appMode));
		infos.add(createWidgetInfo(factory, MARKERS_TOP_BAR, appMode));
	}

	private void createBottomWidgets(@NonNull MapWidgetsFactory factory, @NonNull List<MapWidgetInfo> infos, @NonNull ApplicationMode appMode) {
		infos.add(createWidgetInfo(factory, ELEVATION_PROFILE, appMode));
	}

	private void createLeftWidgets(@NonNull MapWidgetsFactory factory, @NonNull List<MapWidgetInfo> infos, @NonNull ApplicationMode appMode) {
		infos.add(createWidgetInfo(factory, NEXT_TURN, appMode));
		infos.add(createWidgetInfo(factory, SMALL_NEXT_TURN, appMode));
		infos.add(createWidgetInfo(factory, SECOND_NEXT_TURN, appMode));
	}

	private void createRightWidgets(@NonNull MapWidgetsFactory factory, @NonNull List<MapWidgetInfo> infos, @NonNull ApplicationMode appMode) {
		infos.add(createWidgetInfo(factory, INTERMEDIATE_DESTINATION, appMode));
		infos.add(createWidgetInfo(factory, DISTANCE_TO_DESTINATION, appMode));
		infos.add(createWidgetInfo(factory, RELATIVE_BEARING, appMode));
		infos.add(createWidgetInfo(factory, MAGNETIC_BEARING, appMode));
		infos.add(createWidgetInfo(factory, TRUE_BEARING, appMode));
		infos.add(createWidgetInfo(factory, CURRENT_SPEED, appMode));
		infos.add(createWidgetInfo(factory, AVERAGE_SPEED, appMode));
		infos.add(createWidgetInfo(factory, MAX_SPEED, appMode));
		infos.add(createWidgetInfo(factory, ALTITUDE, appMode));
		infos.add(createWidgetInfo(factory, GPS_INFO, appMode));
		infos.add(createWidgetInfo(factory, CURRENT_TIME, appMode));
		infos.add(createWidgetInfo(factory, BATTERY, appMode));
		infos.add(createWidgetInfo(factory, RADIUS_RULER, appMode));
		infos.add(createWidgetInfo(factory, TIME_TO_INTERMEDIATE, appMode));
		infos.add(createWidgetInfo(factory, TIME_TO_DESTINATION, appMode));
		infos.add(createWidgetInfo(factory, SIDE_MARKER_1, appMode));
		infos.add(createWidgetInfo(factory, SIDE_MARKER_2, appMode));
	}

	@Nullable
	private MapWidgetInfo createWidgetInfo(@NonNull MapWidgetsFactory widgetsFactory, @NonNull WidgetType widgetType, @NonNull ApplicationMode appMode) {
		MapWidget mapWidget = widgetsFactory.createMapWidget(widgetType);
		if (mapWidget != null) {
			return createWidgetInfo(mapWidget, appMode);
		}
		return null;
	}

	@Nullable
	public MapWidgetInfo createWidgetInfo(@NonNull MapWidget widget) {
		return createWidgetInfo(widget, settings.getApplicationMode());
	}

	@Nullable
	public MapWidgetInfo createWidgetInfo(@NonNull MapWidget widget, @NonNull ApplicationMode appMode) {
		WidgetType widgetType = widget.getWidgetType();
		if (widgetType != null) {
			String widgetId = widgetType.id;
			WidgetsPanel panel = widgetType.getPanel(widgetId, appMode, settings);
			int page = panel.getWidgetPage(appMode, widgetId, settings);
			int order = panel.getWidgetOrder(appMode, widgetId, settings);
			return createWidgetInfo(widgetId, widget, widgetType.dayIconId, widgetType.nightIconId,
					widgetType.titleId, null, page, order, panel);
		}
		return null;
	}

	@NonNull
	public MapWidgetInfo createWidgetInfo(@NonNull String key,
	                                      @NonNull MapWidget widget,
	                                      @DrawableRes int daySettingsIconId,
	                                      @DrawableRes int nightSettingIconId,
	                                      @StringRes int messageId,
	                                      @Nullable String message,
	                                      int page,
	                                      int order,
	                                      @NonNull WidgetsPanel widgetPanel) {
		if (widget instanceof TextInfoWidget) {
			TextInfoWidget textInfoWidget = ((TextInfoWidget) widget);
			return new SideWidgetInfo(key, textInfoWidget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		} else {
			return new CenterWidgetInfo(key, widget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		}
	}

	@NonNull
	public MapWidgetInfo createExternalWidget(@NonNull String widgetId,
	                                          @NonNull MapWidget widget,
	                                          @DrawableRes int settingsIconId,
	                                          @Nullable String message,
	                                          @NonNull WidgetsPanel defaultPanel,
	                                          int order,
	                                          @NonNull ApplicationMode appMode) {
		WidgetsPanel panel = getExternalWidgetPanel(widgetId, defaultPanel, appMode);
		int page = panel.getWidgetPage(appMode, widgetId, settings);
		int savedOrder = panel.getWidgetOrder(appMode, widgetId, settings);
		if (savedOrder != WidgetsPanel.DEFAULT_ORDER) {
			order = savedOrder;
		}
		return createWidgetInfo(widgetId, widget, settingsIconId,
				settingsIconId, MapWidgetInfo.INVALID_ID, message, page, order, panel);
	}

	private void createCustomWidgets(@NonNull MapWidgetsFactory factory, @NonNull ApplicationMode appMode, @NonNull List<MapWidgetInfo> widgetInfos) {
		List<String> widgetKeys = settings.CUSTOM_WIDGETS_KEYS.getStringsListForProfile(appMode);
		if (!Algorithms.isEmpty(widgetKeys)) {
			for (String key : widgetKeys) {
				WidgetType widgetType = WidgetType.getById(key);
				if (widgetType != null) {
					MapWidgetInfo widgetInfo = createCustomWidget(factory, key, widgetType, appMode);
					if (widgetInfo != null) {
						widgetInfos.add(widgetInfo);
					}
				}
			}
		}
	}

	private MapWidgetInfo createCustomWidget(@NonNull MapWidgetsFactory factory, @NonNull String key,
	                                         @NonNull WidgetType widgetType, @NonNull ApplicationMode appMode) {
		MapWidget widget = factory.createMapWidget(key, widgetType);
		if (widget != null) {
			WidgetsPanel panel = widgetType.getPanel(key, appMode, settings);
			return createCustomWidget(key, widget, widgetType, panel, appMode);
		}
		return null;
	}

	@NonNull
	public MapWidgetInfo createCustomWidget(@NonNull String widgetId,
	                                        @NonNull MapWidget widget,
	                                        @NonNull WidgetType widgetType,
	                                        @NonNull WidgetsPanel panel,
	                                        @NonNull ApplicationMode appMode) {
		int page = panel.getWidgetPage(appMode, widgetId, settings);
		int order = panel.getWidgetOrder(appMode, widgetId, settings);

		return createWidgetInfo(widgetId, widget, widgetType.dayIconId, widgetType.nightIconId, widgetType.titleId, null, page, order, panel);
	}

	@NonNull
	private WidgetsPanel getExternalWidgetPanel(@NonNull String widgetId,
	                                            @NonNull WidgetsPanel defaultPanel,
	                                            @NonNull ApplicationMode appMode) {
		boolean storedInLeftPanel = WidgetsPanel.LEFT.getWidgetOrder(appMode, widgetId, settings) != WidgetsPanel.DEFAULT_ORDER;
		boolean storedInRightPanel = WidgetsPanel.RIGHT.getWidgetOrder(appMode, widgetId, settings) != WidgetsPanel.DEFAULT_ORDER;
		if (storedInLeftPanel) {
			return WidgetsPanel.LEFT;
		} else if (storedInRightPanel) {
			return WidgetsPanel.RIGHT;
		}
		return defaultPanel;
	}

	public interface WidgetsRegistryListener {
		void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo);

		void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo);

		void onWidgetsCleared();
	}
}