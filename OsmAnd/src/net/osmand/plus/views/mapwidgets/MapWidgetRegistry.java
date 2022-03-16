package net.osmand.plus.views.mapwidgets;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import net.osmand.StateChangedListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.cmadapter.ContextMenuAdapter;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuCategory;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapmarkers.DirectionIndicationDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.ElevationProfileWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.plus.widgets.cmadapter.callback.OnRowItemClick;
import net.osmand.plus.widgets.cmadapter.callback.ItemClickListener;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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

	private void setVisibility(ArrayAdapter<ContextMenuItem> adapter,
	                           MapWidgetInfo r,
	                           int position,
	                           boolean visible,
	                           boolean collapsed) {
		setVisibility(r, visible, collapsed);
		MapInfoLayer mil = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mil != null) {
			mil.recreateControls();
		}

		ContextMenuItem item = adapter.getItem(position);
		item.setSelected(visible);
		item.setColor(app, visible ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
		item.setDescription(visible && collapsed ? getString(R.string.shared_string_collapse) : null);
		adapter.notifyDataSetChanged();
	}

	public void setVisibility(MapWidgetInfo m, boolean visible, boolean collapsed) {
		ApplicationMode mode = settings.APPLICATION_MODE.get();
		setVisibility(mode, m, visible, collapsed);
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
		MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
		if (mil != null) {
			mil.recreateControls();
		}
		mapActivity.refreshMap();
	}

	private void addControlId(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm,
	                          @StringRes int stringId, @NonNull OsmandPreference<Boolean> pref) {
		cm.addItem(new ContextMenuItem(null)
				.setTitleId(stringId, mapActivity)
				.setSelected(pref.get())
				.setListener(new AppearanceItemClickListener(pref, mapActivity)));
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
	public Set<MapWidgetInfo> getWidgetsForPanel(@NonNull WidgetsPanel panel) {
		Set<MapWidgetInfo> widgets = allWidgets.get(panel);
		if (widgets == null) {
			widgets = new TreeSet<>();
			allWidgets.put(panel, widgets);
		}
		return widgets;
	}

	public void addControls(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm,
	                        @NonNull ApplicationMode mode) {
		addQuickActionControl(mapActivity, cm);
		// Right panel
		addHeader(mapActivity, cm, R.string.map_widget_right);
		addControls(mapActivity, cm, getRightWidgets(), mode);
		// Left panel
		addHeader(mapActivity, cm, R.string.map_widget_left);
		addControls(mapActivity, cm, getRightWidgets(), mode);
		// Remaining items
		addHeader(mapActivity, cm, R.string.map_widget_appearance_rem);
		addControlsAppearance(mapActivity, cm, mode);
	}

	private void addHeader(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm, int titleId) {
		cm.addItem(new ContextMenuCategory(null)
				.setTitleId(titleId, mapActivity)
				.setLayout(R.layout.list_group_title_with_switch));
	}

	private void addQuickActionControl(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm) {
		cm.addItem(new ContextMenuCategory(null)
				.setTitleId(R.string.map_widget_right, mapActivity)
				.setLayout(R.layout.list_group_empty_title_with_switch));

		boolean selected = app.getQuickActionRegistry().isQuickActionOn();
		cm.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.configure_screen_quick_action, mapActivity)
				.setIcon(R.drawable.ic_quick_action)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						setVisibility(adapter, position, isChecked);
						return false;
					}

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
//						QuickActionListFragment.showInstance(mapActivity, true, true);
						return true;
					}

					private void setVisibility(ArrayAdapter<ContextMenuItem> adapter,
					                           int position,
					                           boolean visible) {

						app.getQuickActionRegistry().setQuickActionFabState(visible);

						MapQuickActionLayer mil = mapActivity.getMapLayers().getMapQuickActionLayer();
						if (mil != null) {
							mil.refreshLayer();
						}
						ContextMenuItem item = adapter.getItem(position);
						item.setSelected(visible);
						item.setColor(app, visible ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();

					}
				}));
	}

	private void addControls(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter contextMenuAdapter,
	                         @NonNull Set<MapWidgetInfo> groupTitle, @NonNull ApplicationMode mode) {
		for (final MapWidgetInfo r : groupTitle) {
			if (!mode.isWidgetAvailable(r.key)) {
				continue;
			}
			boolean selected = r.isVisibleCollapsed(mode) || r.isVisible(mode);
			String desc = mapActivity.getString(R.string.shared_string_collapse);
			ContextMenuItem item = new ContextMenuItem(null)
					.setIcon(r.getSettingsIconId())
					.setSelected(selected)
					.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setSecondaryIcon(r.widget != null ? R.drawable.ic_action_additional_option : ContextMenuItem.INVALID_ID)
					.setDescription(r.isVisibleCollapsed(mode) ? desc : null)
					.setListener(new OnRowItemClick() {
						@Override
						public boolean onRowItemClick(final ArrayAdapter<ContextMenuItem> adapter,
						                              final View view,
						                              final int itemId,
						                              final int pos) {
							if (r.widget == null) {
								setVisibility(adapter, r, pos, !r.isVisible(mode), false);
								return false;
							}
							boolean selected = r.isVisibleCollapsed(mode) || r.isVisible(mode);
							showPopUpMenu(mapActivity, view, adapter, r.getWidgetState(), r.getMessage(), mode,
									getPopupMenuItemListener(adapter, r, pos, true, false),
									getPopupMenuItemListener(adapter, r, pos, false, false),
									getPopupMenuItemListener(adapter, r, pos, true, true),
									selected, pos);

							return false;
						}

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a,
						                                  int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							setVisibility(a, r, pos, isChecked, false);
							return false;
						}

					});
			if (r.getMessage() != null) {
				item.setTitle(r.getMessage());
			} else {
				item.setTitleId(r.getMessageId(), mapActivity);
			}
			contextMenuAdapter.addItem(item);
		}
	}

	private OnClickListener getPopupMenuItemListener(final ArrayAdapter<ContextMenuItem> adapter,
	                                                      final MapWidgetInfo r,
	                                                      final int pos,
	                                                      final boolean visible,
	                                                      final boolean collapsed) {
		return v -> setVisibility(adapter, r, pos, visible, collapsed);
	}

	public void addControlsAppearance(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm, @NonNull ApplicationMode mode) {
		if (mode != ApplicationMode.DEFAULT) {
			addControlId(mapActivity, cm, R.string.map_widget_top_text, settings.SHOW_STREET_NAME);
		}
		cm.addItem(new ContextMenuItem(null).setTitleId(R.string.coordinates_widget, mapActivity)
				.setIcon(R.drawable.ic_action_coordinates_widget)
				.setSelected(settings.SHOW_COORDINATES_WIDGET.get())
				.setListener(new AppearanceItemClickListener(settings.SHOW_COORDINATES_WIDGET, mapActivity))
				.setLayout(R.layout.list_item_icon_and_switch));

		cm.addItem(new ContextMenuItem(null).setTitleId(R.string.map_widget_distance_by_tap, mapActivity)
				.setIcon(R.drawable.ic_action_ruler_line)
				.setSelected(settings.SHOW_DISTANCE_RULER.get())
				.setListener(new AppearanceItemClickListener(settings.SHOW_DISTANCE_RULER, mapActivity))
				.setLayout(R.layout.list_item_icon_and_switch));

		if (InAppPurchaseHelper.isOsmAndProAvailable(app)) {
			addElevationProfileWidget(mapActivity, cm, mode);
		}

		cm.addItem(new ContextMenuItem(null).setTitleId(R.string.map_markers, mapActivity)
				.setDescription(settings.MAP_MARKERS_MODE.get().toHumanString(mapActivity))
				.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					DirectionIndicationDialogFragment fragment = new DirectionIndicationDialogFragment();
					fragment.setListener(showDirectionEnabled -> {
						updateMapMarkersMode(mapActivity);
						cm.getItem(position).setDescription(settings.MAP_MARKERS_MODE.get().toHumanString(mapActivity));
						adapter.notifyDataSetChanged();
					});
					fragment.show(mapActivity.getSupportFragmentManager(), DirectionIndicationDialogFragment.TAG);
					return false;
				}).setLayout(R.layout.list_item_text_button));
		addControlId(mapActivity, cm, R.string.map_widget_transparent, settings.TRANSPARENT_MAP_THEME);
		addControlId(mapActivity, cm, R.string.show_lanes, settings.SHOW_LANES);
	}

	public void addElevationProfileWidget(@NonNull MapActivity mapActivity, @NonNull ContextMenuAdapter cm,
	                                      @NonNull ApplicationMode mode) {
		final OsmandPreference<Boolean> pref = settings.SHOW_ELEVATION_PROFILE_WIDGET;

		final ElevationProfileWidgetState widgetState = new ElevationProfileWidgetState(app);
		ContextMenuItem item = new ContextMenuItem(null)
				.setTitleId(widgetState.getMenuTitleId(), app)
				.setIcon(R.drawable.ic_action_elevation_profile)
				.setSelected(pref.get())
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(new OnRowItemClick() {
					@Override
					public boolean onRowItemClick(final ArrayAdapter<ContextMenuItem> adapter,
					                              final View view,
					                              final int itemId,
					                              final int pos) {
						boolean selected = pref.get();
						showPopUpMenu(mapActivity, view, adapter, widgetState, null, mode,
								getElevationPopupMenuItemListener(mapActivity, adapter, pref, pos, true),
								getElevationPopupMenuItemListener(mapActivity, adapter, pref, pos, false),
								null, selected, pos);
						return false;
					}

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a,
					                                  int itemId, int pos, boolean isChecked,
					                                  int[] viewCoordinates) {
						updateAppearancePref(mapActivity, pref, a, !pref.get());
						return false;
					}
				});
		cm.addItem(item);
	}

	public OnClickListener getElevationPopupMenuItemListener(@NonNull MapActivity mapActivity,
	                                                         @NonNull ArrayAdapter<ContextMenuItem> adapter,
	                                                         @NonNull OsmandPreference<Boolean> pref,
	                                                         int pos, boolean visible) {
		return v -> {
			adapter.getItem(pos).setSelected(visible);
			updateAppearancePref(mapActivity, pref, adapter, visible);
		};
	}

	public void showPopUpMenu(@NonNull MapActivity mapActivity,
	                          @NonNull View view,
	                          @NonNull final ArrayAdapter<ContextMenuItem> adapter,
	                          @Nullable final WidgetState widgetState,
	                          @Nullable final String message,
	                          @NonNull ApplicationMode mode,
	                          @NonNull OnClickListener showBtnListener,
	                          @NonNull OnClickListener hideBtnListener,
	                          @Nullable OnClickListener collapseBtnListener,
	                          boolean selected,
	                          final int pos) {
		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
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
								MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
								if (mil != null) {
									mil.recreateControls();
								}
								ContextMenuItem item = adapter.getItem(pos);
								item.setIcon(widgetState.getSettingsIconId());
								if (message != null) {
									item.setTitle(message);
								} else {
									item.setTitle(getString(widgetState.getMenuTitleId()));
								}
								adapter.notifyDataSetChanged();
							})
							.showCompoundBtn(currentModeColor)
							.setSelected(checkedItem)
							.create());
				}
			}
		}

		// show
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_view))
				.setOnClickListener(showBtnListener)
				.showTopDivider(items.size() > 0)
				.create());

		// hide
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_hide)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_hide))
				.setOnClickListener(hideBtnListener)
				.create());

		// collapse
		if (collapseBtnListener != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_collapse)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_widget_collapse))
					.setOnClickListener(collapseBtnListener)
					.create());
		}

		new PopUpMenuHelper.Builder(parentView, items, nightMode)
				.setWidthType(PopUpMenuWidthType.STANDARD)
				.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode))
				.show();
	}

	public ContextMenuAdapter getViewConfigureMenuAdapter(@NonNull MapActivity mapActivity) {
		ContextMenuAdapter cm = new ContextMenuAdapter(app);
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		cm.setProfileDependent(true);
		cm.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		cm.addItem(new ContextMenuItem(null).setTitleId(R.string.app_modes_choose, mapActivity)
				.setLayout(R.layout.mode_toggles));
//		cm.setChangeAppModeListener(() -> mapActivity.getDashboard().updateListAdapter(getViewConfigureMenuAdapter(mapActivity)));
		ApplicationMode mode = settings.getApplicationMode();
		addControls(mapActivity, cm, mode);
		return cm;
	}

	private String getString(@StringRes int resId) {
		return app.getString(resId);
	}

	private static void updateAppearancePref(@NonNull MapActivity mapActivity, @NonNull OsmandPreference<Boolean> pref,
	                                         @NonNull ArrayAdapter<ContextMenuItem> a, boolean value) {
		pref.set(value);
		mapActivity.updateApplicationModeSettings();
		a.notifyDataSetChanged();
	}

	static class AppearanceItemClickListener implements ItemClickListener {

		private final MapActivity mapActivity;
		private final OsmandPreference<Boolean> pref;

		public AppearanceItemClickListener(OsmandPreference<Boolean> pref, MapActivity mapActivity) {
			this.pref = pref;
			this.mapActivity = mapActivity;
		}

		@Override
		public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos,
										  boolean isChecked, int[] viewCoordinates) {
			updateAppearancePref(mapActivity, pref, a, !pref.get());
			return false;
		}
	}
}