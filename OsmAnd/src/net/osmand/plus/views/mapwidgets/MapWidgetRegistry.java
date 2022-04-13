package net.osmand.plus.views.mapwidgets;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.CallbackWithObject;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuHelper.PopUpMenuWidthType;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

	public static final String WIDGET_NEXT_TURN = "next_turn";
	public static final String WIDGET_NEXT_TURN_SMALL = "next_turn_small";
	public static final String WIDGET_NEXT_NEXT_TURN = "next_next_turn";

	public static final String WIDGET_DISTANCE_TO_DESTINATION = "distance";
	public static final String WIDGET_INTERMEDIATE_DISTANCE = "intermediate_distance";
	public static final String WIDGET_TIME = "time";
	public static final String WIDGET_INTERMEDIATE_TIME = "intermediate_time";
	public static final String WIDGET_MAX_SPEED = "max_speed";
	public static final String WIDGET_CURRENT_SPEED = "speed";
	public static final String WIDGET_ALTITUDE = "altitude";
	public static final String WIDGET_GPS_INFO = "gps_info";
	public static final String WIDGET_MARKER_1 = "map_marker_1st";
	public static final String WIDGET_MARKER_2 = "map_marker_2nd";
	public static final String WIDGET_BEARING = "bearing";
	public static final String WIDGET_PLAIN_TIME = "plain_time";
	public static final String WIDGET_BATTERY = "battery";
	public static final String WIDGET_RADIUS_RULER = "ruler";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final Map<WidgetsPanel, Set<MapWidgetInfo>> allWidgets = new HashMap<>();
	private final Map<ApplicationMode, Set<String>> sideWidgetsVisibilityFromSettings = new LinkedHashMap<>();

	private Set<WidgetsVisibilityListener> visibilityListeners = new HashSet<>();

	public MapWidgetRegistry(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		loadVisibleElementsFromSettings();
		StateChangedListener<String> listener = change -> updateVisibleSideWidgets();
		settings.AVAILABLE_APP_MODES.addListener(listener);
		settings.MAP_INFO_CONTROLS.addListener(listener);
	}

	public void populateControlsContainer(@NonNull ViewGroup container,
	                                      @NonNull ApplicationMode mode,
	                                      @NonNull WidgetsPanel widgetPanel) {
		Set<MapWidgetInfo> widgets = getWidgetsForPanel(widgetPanel);

		List<MapWidget> widgetsToShow = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isVisibleForAppMode(mode)) {
				widgetsToShow.add(widgetInfo.widget);
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
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			Set<MapWidgetInfo> panelWidgets = getWidgetsForPanel(panel);
			updatePanelInfo(panelWidgets, appMode, drawSettings);
		}
	}

	private void updatePanelInfo(@NonNull Set<MapWidgetInfo> panelWidgets,
	                             @NonNull ApplicationMode mode,
	                             @NonNull DrawSettings drawSettings) {
		for (MapWidgetInfo widgetInfo : panelWidgets) {
			if (widgetInfo.isVisibleForAppMode(mode)) {
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
	                                    @DrawableRes int settingsIconId,
	                                    @StringRes int messageId,
	                                    @Nullable String message,
	                                    int page,
	                                    int order,
	                                    @NonNull WidgetsPanel widgetPanel) {
		MapWidgetInfo widgetInfo;
		if (widget instanceof TextInfoWidget) {
			widgetInfo = new SideWidgetInfo(key, widget, widgetState, settingsIconId, messageId, message, page,
					order, widgetPanel);
			processVisibleModes(widgetInfo);
			TextInfoWidget textWidget = ((TextInfoWidget) widget);
			if (message != null) {
				textWidget.setContentTitle(message);
			} else if (messageId != MapWidgetInfo.INVALID_ID) {
				textWidget.setContentTitle(messageId);
			} else if (widgetState != null) {
				textWidget.setContentTitle(widgetState.getMenuTitleId());
			}
		} else {
			widgetInfo = new CenterWidgetInfo(key, widget, widgetState, settingsIconId, messageId, message, page,
					order, widgetPanel);
		}

		getWidgetsForPanel(widgetPanel).add(widgetInfo);

		return widgetInfo;
	}

	private void processVisibleModes(@NonNull MapWidgetInfo widgetInfo) {
		String widgetId = widgetInfo.key;
		for (ApplicationMode appMode : ApplicationMode.values(app)) {
			boolean visible = appMode.isWidgetVisibleByDefault(widgetId);
			Set<String> sideWidgetsVisibility = sideWidgetsVisibilityFromSettings.get(appMode);
			if (sideWidgetsVisibility != null) {
				if (isWidgetVisible(widgetId, sideWidgetsVisibility)) {
					visible = true;
				} else if (sideWidgetsVisibility.contains(HIDE_PREFIX + widgetId)) {
					visible = false;
				}
			}
			widgetInfo.showHideForAppMode(appMode, visible);
		}
	}

	@NonNull
	private Set<String> getWidgetsVisibilityForAppMode(@NonNull Set<MapWidgetInfo> widgetsInfo,
	                                                   @NonNull ApplicationMode mode) {
		Set<String> widgetsVisibility = new LinkedHashSet<>();
		for (MapWidgetInfo widgetInfo : widgetsInfo) {
			if (widgetInfo.isVisibleForAppMode(mode)) {
				widgetsVisibility.add(widgetInfo.key);
			} else {
				widgetsVisibility.add(HIDE_PREFIX + widgetInfo.key);
			}
		}
		return widgetsVisibility;
	}

	public boolean isWidgetVisible(@NonNull String widgetId) {
		ApplicationMode appMode = settings.getApplicationMode();
		Set<String> widgetsVisibility = sideWidgetsVisibilityFromSettings.get(appMode);
		return widgetsVisibility != null && isWidgetVisible(widgetId, widgetsVisibility);
	}

	private boolean isWidgetVisible(@NonNull String widgetId, @NonNull Set<String> widgetsVisibility) {
		return widgetsVisibility.contains(widgetId) || widgetsVisibility.contains(COLLAPSED_PREFIX + widgetId);
	}

	public void setVisibility(@NonNull MapWidgetInfo widgetInfo, boolean visible) {
		OsmandPreference<Boolean> visibilityPref = widgetInfo.widget.getWidgetVisibilityPref();
		if (visibilityPref != null) {
			visibilityPref.set(!visibilityPref.get());
			notifyWidgetVisibilityChanged(widgetInfo);
		} else {
			ApplicationMode mode = settings.APPLICATION_MODE.get();
			setVisibility(mode, widgetInfo, visible);
		}

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	public void setVisibility(@NonNull ApplicationMode mode, @NonNull MapWidgetInfo widgetInfo, boolean visible) {
		Set<String> widgetsVisibility = getOrInitWidgetsVisibility(mode);
		widgetsVisibility.remove(widgetInfo.key);
		widgetsVisibility.remove(COLLAPSED_PREFIX + widgetInfo.key);
		widgetsVisibility.remove(HIDE_PREFIX + widgetInfo.key);
		if (visible) {
			widgetsVisibility.add(widgetInfo.key);
		} else {
			widgetsVisibility.add(HIDE_PREFIX + widgetInfo.key);
		}
		widgetInfo.showHideForAppMode(mode, visible);
		saveWidgetsVisibilityToSettings(widgetsVisibility);
		notifyWidgetVisibilityChanged(widgetInfo);
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

	@NonNull
	private Set<String> getOrInitWidgetsVisibility(@NonNull ApplicationMode appMode) {
		Set<String> widgetsVisibility = sideWidgetsVisibilityFromSettings.get(appMode);
		if (widgetsVisibility == null) {
			widgetsVisibility = new LinkedHashSet<>();
			widgetsVisibility.addAll(getWidgetsVisibilityForAppMode(getLeftWidgets(), appMode));
			widgetsVisibility.addAll(getWidgetsVisibilityForAppMode(getRightWidgets(), appMode));
			sideWidgetsVisibilityFromSettings.put(appMode, widgetsVisibility);
		}
		return widgetsVisibility;
	}

	public void updateVisibleSideWidgets() {
		loadVisibleElementsFromSettings();
		for (MapWidgetInfo widgetInfo : getLeftWidgets()) {
			processVisibleModes(widgetInfo);
		}
		for (MapWidgetInfo widgetInfo : getRightWidgets()) {
			processVisibleModes(widgetInfo);
		}
	}

	private void loadVisibleElementsFromSettings() {
		sideWidgetsVisibilityFromSettings.clear();
		for (ApplicationMode appMode : ApplicationMode.values(app)) {
			String widgetsVisibilityString = settings.MAP_INFO_CONTROLS.getModeValue(appMode);
			boolean useDefaultVisibility = Algorithms.isEmpty(widgetsVisibilityString);
			if (useDefaultVisibility) {
				sideWidgetsVisibilityFromSettings.put(appMode, null);
			} else {
				Set<String> widgetsVisibility = new LinkedHashSet<>();
				Collections.addAll(widgetsVisibility, widgetsVisibilityString.split(SETTINGS_SEPARATOR));
				sideWidgetsVisibilityFromSettings.put(appMode, widgetsVisibility);
			}
		}
	}

	private void saveWidgetsVisibilityToSettings(@NonNull Set<String> widgetsVisibility) {
		StringBuilder widgetsVisibilityString = new StringBuilder();
		for (String widgetVisibility : widgetsVisibility) {
			widgetsVisibilityString.append(widgetVisibility).append(SETTINGS_SEPARATOR);
		}
		settings.MAP_INFO_CONTROLS.set(widgetsVisibilityString.toString());
	}

	public void reorderWidgets() {
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			Set<MapWidgetInfo> oldOrder = getWidgetsForPanel(panel);
			Set<MapWidgetInfo> newOrder = new TreeSet<>();
			for (MapWidgetInfo widgetInfo : oldOrder) {
				widgetInfo.pageIndex = panel.getWidgetPage(widgetInfo.key, settings);
				widgetInfo.priority = panel.getWidgetOrder(widgetInfo.key, settings);
				newOrder.add(widgetInfo);
			}
			allWidgets.put(panel, newOrder);
		}
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
	public List<Set<MapWidgetInfo>> getAvailablePagedWidgetsForPanel(@NonNull ApplicationMode appMode,
	                                                                 @NonNull WidgetsPanel panel) {
		Map<Integer, Set<MapWidgetInfo>> widgetsByPages = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : getAvailableWidgetsForPanel(appMode, panel)) {
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
	public Set<MapWidgetInfo> getAvailableWidgetsForPanel(@NonNull ApplicationMode appMode, @NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgets = new TreeSet<>();
		for (MapWidgetInfo widgetInfo : getWidgetsForPanel(panel)) {

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			if (!appCustomization.areWidgetsCustomized() || !appCustomization.isWidgetAvailable(widgetInfo.key, appMode)) {
				widgets.add(widgetInfo);
			}
		}
		return widgets;
	}

	@NonNull
	public Set<MapWidgetInfo> getVisibleWidgets(@NonNull ApplicationMode appMode, @NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> visible = new TreeSet<>();
		for (MapWidgetInfo widget : getWidgetsForPanel(panel)) {
			if (widget.isSelected(appMode)) {
				visible.add(widget);
			}
		}
		return visible;
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

	public void showPopUpMenu(@NonNull View view,
	                          @NonNull final CallbackWithObject<WidgetState> callback,
	                          @Nullable final WidgetState widgetState,
	                          @NonNull ApplicationMode mode,
	                          boolean selected,
	                          boolean nightMode) {
		final int currentModeColor = mode.getProfileColor(nightMode);
		View parentView = view.findViewById(R.id.text_wrapper);
		List<PopUpMenuItem> items = new ArrayList<>();
		UiUtilities uiUtilities = app.getUIUtilities();

		if (widgetState != null) {
			final int[] menuIconIds = widgetState.getMenuIconIds();
			final int[] menuTitleIds = widgetState.getMenuTitleIds();
			final int[] menuItemIds = widgetState.getMenuItemIds();
			if (menuIconIds != null && menuTitleIds != null && menuItemIds != null &&
					menuIconIds.length == menuTitleIds.length && menuIconIds.length == menuItemIds.length) {
				for (int i = 0; i < menuIconIds.length; i++) {
					int iconId = menuIconIds[i];
					int titleId = menuTitleIds[i];
					final int id = menuItemIds[i];
					boolean checkedItem = id == widgetState.getMenuItemId();
					Drawable icon = checkedItem && selected ?
							uiUtilities.getPaintedIcon(iconId, currentModeColor) :
							uiUtilities.getThemedIcon(iconId);
					items.add(new PopUpMenuItem.Builder(app)
							.setTitle(getString(titleId))
							.setIcon(icon)
							.setOnClickListener(v -> {
								widgetState.changeState(id);
								MapInfoLayer layer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
								if (layer != null) {
									layer.recreateControls();
								}
								callback.processResult(widgetState);
							})
							.showCompoundBtn(currentModeColor)
							.setSelected(checkedItem)
							.create());
				}
			}
		}

		new PopUpMenuHelper.Builder(parentView, items, nightMode)
				.setWidthType(PopUpMenuWidthType.STANDARD)
				.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode))
				.show();
	}

	private String getString(@StringRes int resId) {
		return app.getString(resId);
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