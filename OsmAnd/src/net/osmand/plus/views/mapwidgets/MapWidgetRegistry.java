package net.osmand.plus.views.mapwidgets;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
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
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MapWidgetRegistry {

	public static final String COLLAPSED_PREFIX = "+";
	public static final String HIDE_PREFIX = "-";
	public static final String SHOW_PREFIX = "";
	public static final String SETTINGS_SEPARATOR = ";";

	public static final String WIDGET_COMPASS = "compass";

	public static final String WIDGET_NEXT_TURN = "next_turn";
	public static final String WIDGET_NEXT_TURN_SMALL = "next_turn_small";
	public static final String WIDGET_NEXT_NEXT_TURN = "next_next_turn";

	public static final String WIDGET_DISTANCE = "distance";
	public static final String WIDGET_INTERMEDIATE_DISTANCE = "intermediate_distance";
	public static final String WIDGET_TIME = "time";
	public static final String WIDGET_INTERMEDIATE_TIME = "intermediate_time";
	public static final String WIDGET_MAX_SPEED = "max_speed";
	public static final String WIDGET_SPEED = "speed";
	public static final String WIDGET_ALTITUDE = "altitude";
	public static final String WIDGET_GPS_INFO = "gps_info";
	public static final String WIDGET_MARKER_1 = "map_marker_1st";
	public static final String WIDGET_MARKER_2 = "map_marker_2nd";
	public static final String WIDGET_BEARING = "bearing";
	public static final String WIDGET_PLAIN_TIME = "plain_time";
	public static final String WIDGET_BATTERY = "battery";
	public static final String WIDGET_RADIUS_RULER = "ruler";

	public static final String WIDGET_COORDINATES = "coordinates";
	public static final String WIDGET_STREET_NAME = "street_name";
	public static final String WIDGET_MAP_MARKERS = "map_markers_top";
	public static final String WIDGET_LANES = "lanes";

	public static final String WIDGET_ELEVATION_PROFILE = "elevation_profile";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final Map<WidgetsPanel, Set<MapWidgetInfo>> allWidgets = new HashMap<>();
	private final Map<ApplicationMode, Set<String>> visibleSideWidgetsFromSettings = new LinkedHashMap<>();

	private final StateChangedListener<String> listener;

	public MapWidgetRegistry(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		loadVisibleElementsFromSettings();
		listener = change -> updateVisibleSideWidgets();
		settings.AVAILABLE_APP_MODES.addListener(listener);
		settings.MAP_INFO_CONTROLS.addListener(listener);
	}

	public void populateControlsContainer(@NonNull ViewGroup container, @NonNull ApplicationMode mode,
	                                      @NonNull WidgetsPanel widgetPanel, boolean expanded) {
		Set<MapWidgetInfo> widgets = getWidgetsForPanel(widgetPanel);

		List<MapWidget> widgetsToShow = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isVisible(mode)) {
				widgetsToShow.add(widgetInfo.widget);
			}
		}
		if (expanded) {
			for (MapWidgetInfo widgetInfo : widgets) {
				if (widgetInfo.isVisibleCollapsed(mode)) {
					widgetsToShow.add(widgetInfo.widget);
				}
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

	public boolean hasCollapsibles(ApplicationMode mode) {
		for (MapWidgetInfo r : getLeftWidgets()) {
			if (r.isVisibleCollapsed(mode)) {
				return true;
			}
		}
		for (MapWidgetInfo r : getRightWidgets()) {
			if (r.isVisibleCollapsed(mode)) {
				return true;
			}
		}
		return false;
	}

	public void updateWidgetsInfo(@NonNull ApplicationMode appMode, @NonNull DrawSettings drawSettings,
	                              boolean expanded) {
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			Set<MapWidgetInfo> panelWidgets = getWidgetsForPanel(panel);
			updatePanelInfo(panelWidgets, appMode, drawSettings, expanded);
		}
	}

	private void updatePanelInfo(@NonNull Set<MapWidgetInfo> panelWidgets, @NonNull ApplicationMode mode,
	                             @NonNull DrawSettings drawSettings, boolean expanded) {
		for (MapWidgetInfo widgetInfo : panelWidgets) {
			if (widgetInfo.isVisible(mode) || (widgetInfo.isVisibleCollapsed(mode) && expanded)) {
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
	                                    int priorityOrder,
	                                    @NonNull WidgetsPanel widgetPanel) {
		MapWidgetInfo widgetInfo;
		if (widget instanceof TextInfoWidget) {
			widgetInfo = new SideWidgetInfo(key, widget, widgetState, settingsIconId, messageId, message,
					priorityOrder, widgetPanel);
			processVisibleModes(key, widgetInfo);
			TextInfoWidget textWidget = ((TextInfoWidget) widget);
			if (message != null) {
				textWidget.setContentTitle(message);
			} else if (messageId != MapWidgetInfo.INVALID_ID) {
				textWidget.setContentTitle(messageId);
			} else if (widgetState != null) {
				textWidget.setContentTitle(widgetState.getMenuTitleId());
			}
		} else {
			widgetInfo = new CenterWidgetInfo(key, widget, widgetState, settingsIconId, messageId, message,
					priorityOrder, widgetPanel);
		}

		getWidgetsForPanel(widgetPanel).add(widgetInfo);

		return widgetInfo;
	}

	private void processVisibleModes(String key, MapWidgetInfo widgetInfo) {
		for (ApplicationMode ms : ApplicationMode.values(app)) {
			boolean collapse = ms.isWidgetCollapsible(key);
			boolean visible = ms.isWidgetVisible(key);
			Set<String> set = visibleSideWidgetsFromSettings.get(ms);
			if (set != null) {
				if (set.contains(key)) {
					visible = true;
					collapse = false;
				} else if (set.contains(HIDE_PREFIX + key)) {
					visible = false;
					collapse = false;
				} else if (set.contains(COLLAPSED_PREFIX + key)) {
					visible = false;
					collapse = true;
				}
			}
			if (visible) {
				widgetInfo.addVisible(ms);
			} else if (collapse) {
				widgetInfo.addVisibleCollapsible(ms);
			} else {
				widgetInfo.removeVisible(ms);
				widgetInfo.removeVisibleCollapsible(ms);
			}
		}
	}

	private void restoreModes(Set<String> set, Set<MapWidgetInfo> widgetsInfo, ApplicationMode mode) {
		for (MapWidgetInfo widgetInfo : widgetsInfo) {
			if (widgetInfo.isVisible(mode)) {
				set.add(widgetInfo.key);
			} else if (widgetInfo.isVisibleCollapsed(mode)) {
				set.add(COLLAPSED_PREFIX + widgetInfo.key);
			} else {
				set.add(HIDE_PREFIX + widgetInfo.key);
			}
		}
	}

	public boolean isVisible(String key) {
		ApplicationMode mode = settings.APPLICATION_MODE.get();
		Set<String> elements = visibleSideWidgetsFromSettings.get(mode);
		return elements != null && (elements.contains(key) || elements.contains(COLLAPSED_PREFIX + key));
	}

	public void setVisibility(@NonNull MapWidgetInfo widgetInfo, boolean visible, boolean collapsed) {
		ApplicationMode mode = settings.APPLICATION_MODE.get();
		setVisibility(mode, widgetInfo, visible, collapsed);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	public void setVisibility(ApplicationMode mode, MapWidgetInfo m, boolean visible, boolean collapsed) {
		defineDefaultSettingsElement(mode);
		// clear everything
		visibleSideWidgetsFromSettings.get(mode).remove(m.key);
		visibleSideWidgetsFromSettings.get(mode).remove(COLLAPSED_PREFIX + m.key);
		visibleSideWidgetsFromSettings.get(mode).remove(HIDE_PREFIX + m.key);
		m.removeVisible(mode);
		m.removeVisibleCollapsible(mode);
		if (visible && collapsed) {
			// Set "collapsed" state
			m.addVisibleCollapsible(mode);
			visibleSideWidgetsFromSettings.get(mode).add(COLLAPSED_PREFIX + m.key);
		} else if (visible) {
			// Set "visible" state
			m.addVisible(mode);
			visibleSideWidgetsFromSettings.get(mode).add(SHOW_PREFIX + m.key);
		} else {
			// Set "hidden" state
			visibleSideWidgetsFromSettings.get(mode).add(HIDE_PREFIX + m.key);
		}
		saveVisibleElementsToSettings(mode);
	}

	private void defineDefaultSettingsElement(ApplicationMode mode) {
		if (visibleSideWidgetsFromSettings.get(mode) == null) {
			LinkedHashSet<String> set = new LinkedHashSet<>();
			restoreModes(set, getLeftWidgets(), mode);
			restoreModes(set, getRightWidgets(), mode);
			visibleSideWidgetsFromSettings.put(mode, set);
		}
	}

	public void updateVisibleSideWidgets() {
		loadVisibleElementsFromSettings();
		for (MapWidgetInfo ri : getLeftWidgets()) {
			processVisibleModes(ri.key, ri);
		}
		for (MapWidgetInfo ri : getRightWidgets()) {
			processVisibleModes(ri.key, ri);
		}
	}

	private void loadVisibleElementsFromSettings() {
		visibleSideWidgetsFromSettings.clear();
		for (ApplicationMode ms : ApplicationMode.values(app)) {
			String mpf = settings.MAP_INFO_CONTROLS.getModeValue(ms);
			if (mpf.equals(SHOW_PREFIX)) {
				visibleSideWidgetsFromSettings.put(ms, null);
			} else {
				LinkedHashSet<String> set = new LinkedHashSet<>();
				visibleSideWidgetsFromSettings.put(ms, set);
				Collections.addAll(set, mpf.split(SETTINGS_SEPARATOR));
			}
		}
	}

	private void saveVisibleElementsToSettings(ApplicationMode mode) {
		StringBuilder bs = new StringBuilder();
		for (String ks : visibleSideWidgetsFromSettings.get(mode)) {
			bs.append(ks).append(SETTINGS_SEPARATOR);
		}
		settings.MAP_INFO_CONTROLS.set(bs.toString());
	}


	private void resetDefault(ApplicationMode mode, Set<MapWidgetInfo> set) {
		for (MapWidgetInfo ri : set) {
			ri.removeVisibleCollapsible(mode);
			ri.removeVisible(mode);
			if (mode.isWidgetVisible(ri.key)) {
				if (mode.isWidgetCollapsible(ri.key)) {
					ri.addVisibleCollapsible(mode);
				} else {
					ri.addVisible(mode);
				}
			}
		}
	}

	public void resetToDefault() {
		ApplicationMode appMode = settings.getApplicationMode();
		resetDefault(appMode, getLeftWidgets());
		resetDefault(appMode, getRightWidgets());
		resetDefaultAppearance(appMode);
		visibleSideWidgetsFromSettings.put(appMode, null);
		settings.MAP_INFO_CONTROLS.set(SHOW_PREFIX);
	}

	private void resetDefaultAppearance(ApplicationMode mode) {
		settings.TRANSPARENT_MAP_THEME.resetModeToDefault(mode);
		settings.SHOW_STREET_NAME.resetModeToDefault(mode);
		settings.MAP_MARKERS_MODE.resetModeToDefault(mode);
	}

	public void reorderWidgets() {
		for (WidgetsPanel panel : WidgetsPanel.values()) {
			Set<MapWidgetInfo> oldOrder = getWidgetsForPanel(panel);
			Set<MapWidgetInfo> newOrder = new TreeSet<>();
			for (MapWidgetInfo widgetInfo : oldOrder) {
				widgetInfo.priority = panel.getWidgetOrder(widgetInfo.key, settings);
				newOrder.add(widgetInfo);
			}
			allWidgets.put(panel, newOrder);
		}
	}

	public void updateMapMarkersMode(MapActivity mapActivity) {
		for (MapWidgetInfo info : getRightWidgets()) {
			if (WIDGET_MARKER_1.equals(info.key)) {
				setVisibility(info, settings.MAP_MARKERS_MODE.get().isWidgets()
						&& settings.MARKERS_DISTANCE_INDICATION_ENABLED.get(), false);
			} else if (WIDGET_MARKER_2.equals(info.key)) {
				setVisibility(info, settings.MAP_MARKERS_MODE.get().isWidgets()
						&& settings.MARKERS_DISTANCE_INDICATION_ENABLED.get()
						&& settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2, false);
			}
		}
		mapActivity.refreshMap();
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
	                          @Nullable OnClickListener showBtnListener,
	                          @Nullable OnClickListener hideBtnListener,
	                          @Nullable OnClickListener collapseBtnListener,
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
		if (showBtnListener != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_show)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_view))
					.setOnClickListener(showBtnListener)
					.showTopDivider(items.size() > 0)
					.create());
		}
		if (hideBtnListener != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_hide)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_hide))
					.setOnClickListener(hideBtnListener)
					.create());
		}
		if (collapseBtnListener != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_collapse)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_widget_collapse))
					.setOnClickListener(collapseBtnListener)
					.create());
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
			if (widget.getView().getVisibility() != View.VISIBLE) {
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
}