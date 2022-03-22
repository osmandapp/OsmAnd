package net.osmand.plus.views.layers;


import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ALTITUDE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BATTERY;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BEARING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_COORDINATES;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ELEVATION_PROFILE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_GPS_INFO;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_LANES;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAP_MARKERS;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_1;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_2;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN_SMALL;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_PLAIN_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_STREET_NAME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_TIME;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import net.osmand.StateChangedListener;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;
import net.osmand.plus.views.mapwidgets.widgets.LanesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.BearingWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.CompassRulerWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.ElevationProfileWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.ArrayList;
import java.util.List;

public class MapInfoLayer extends OsmandMapLayer {

	private static boolean WIDGETS_EXPANDED = false;

	private final RouteLayer routeLayer;
	private final OsmandSettings settings;
	private OsmandMapTileView view;

	private ViewGroup topWidgetsContainer;
	private ViewGroup rightWidgetsContainer;
	private ViewGroup leftWidgetsContainer;
	private ViewGroup bottomWidgetsContainer;

	private ImageButton expandButton;
	private View mapRulerLayout;
	private AlarmWidget alarmControl;
	private List<RulerWidget> rulerWidgets;
	private MapWidgetRegistry mapInfoControls;

	private DrawSettings drawSettings;
	private int themeId = -1;

	private TopToolbarView topToolbarView;

	private StateChangedListener<ApplicationMode> appModeChangeListener;

	public MapInfoLayer(@NonNull Context context, @NonNull RouteLayer layer) {
		super(context);
		settings = getApplication().getSettings();
		this.routeLayer = layer;
	}

	public MapWidgetRegistry getMapInfoControls() {
		return mapInfoControls;
	}

	@Override
	public void initLayer(@NonNull final OsmandMapTileView view) {
		this.view = view;
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			mapInfoControls = mapActivity.getMapLayers().getMapWidgetRegistry();
			topWidgetsContainer = mapActivity.findViewById(R.id.top_widgets_panel);
			leftWidgetsContainer = mapActivity.findViewById(R.id.map_left_widgets_panel);
			rightWidgetsContainer = mapActivity.findViewById(R.id.map_right_widgets_panel);
			bottomWidgetsContainer = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			expandButton = mapActivity.findViewById(R.id.map_collapse_button);
			mapRulerLayout = mapActivity.findViewById(R.id.map_ruler_layout);

			appModeChangeListener = createAppModeChangeListener();
			settings.APPLICATION_MODE.addListener(appModeChangeListener);

			registerAllControls(mapActivity);
			recreateControls();
		} else {
			settings.APPLICATION_MODE.removeListener(appModeChangeListener);
			appModeChangeListener = null;

			if (mapInfoControls != null) {
				mapInfoControls.clearWidgets();
			}
			mapInfoControls = null;

			topWidgetsContainer = null;
			leftWidgetsContainer = null;
			rightWidgetsContainer = null;
			expandButton = null;
			mapRulerLayout = null;

			alarmControl = null;
			rulerWidgets = null;

			drawSettings = null;
			themeId = -1;

			topToolbarView = null;
		}
	}

	@NonNull
	private StateChangedListener<ApplicationMode> createAppModeChangeListener() {
		return appMode -> {
			if (mapInfoControls != null) {
				mapInfoControls.reorderWidgets();
				recreateControls();
			}
		};
	}

	@Nullable
	public <T extends TextInfoWidget> T getSideWidget(Class<T> cl) {
		return mapInfoControls != null ? mapInfoControls.getSideWidget(cl) : null;
	}

	public void removeSideWidget(TextInfoWidget widget) {
		if (mapInfoControls != null) {
			mapInfoControls.removeSideWidgetInternal(widget);
		}
	}

	public void addTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.addController(controller);
		}
	}

	public void removeTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.removeController(controller);
		}
	}

	public boolean hasTopToolbar() {
		return topToolbarView != null && topToolbarView.getTopController() != null;
	}

	public TopToolbarController getTopToolbarController() {
		return topToolbarView == null ? null : topToolbarView.getTopController();
	}

	@Nullable
	public TopToolbarController getTopToolbarController(TopToolbarControllerType type) {
		return topToolbarView == null ? null : topToolbarView.getController(type);
	}

	public boolean isTopToolbarViewVisible() {
		return topToolbarView != null && topToolbarView.isTopToolbarViewVisible();
	}

	private void registerAllControls(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		rulerWidgets = new ArrayList<>();
		RouteInfoWidgetsFactory routeWidgetsFactory = new RouteInfoWidgetsFactory(app);
		MapInfoWidgetsFactory mapWidgetsFactory = new MapInfoWidgetsFactory(app);

		topToolbarView = new TopToolbarView(mapActivity);
		updateTopToolbar(false);

		alarmControl = RouteInfoWidgetsFactory.createAlarmInfoControl(app, mapActivity);
		alarmControl.setVisibility(false);

		setupRulerWidget(mapRulerLayout);

		registerTopWidgets(mapActivity);
		registerBottomWidgets(mapActivity);
		registerLeftWidgets(mapActivity, routeWidgetsFactory);
		registerRightWidgets(mapActivity, routeWidgetsFactory, mapWidgetsFactory);

		app.getAidlApi().registerWidgetControls(mapActivity);
	}

	private void registerBottomWidgets(@NonNull MapActivity mapActivity) {
		ElevationProfileWidget widget = new ElevationProfileWidget(mapActivity);
		ElevationProfileWidgetState widgetState = new ElevationProfileWidgetState(getApplication());
		registerWidget(WIDGET_ELEVATION_PROFILE, widget, widgetState, WidgetsPanel.BOTTOM);
	}

	private void registerTopWidgets(@NonNull MapActivity mapActivity) {
		WidgetsPanel topPanel = WidgetsPanel.TOP;

		MapWidget coordinatedWidget = new CoordinatesWidget(mapActivity);
		registerWidget(WIDGET_COORDINATES, coordinatedWidget, R.drawable.ic_action_coordinates_widget,
				R.string.coordinates_widget, topPanel);

		MapWidget mapMarkersWidget = mapActivity.getMapLayers().getMapMarkersLayer()
				.getMarkersWidgetsHelper().getMapMarkersBarWidget();
		registerWidget(WIDGET_MAP_MARKERS, mapMarkersWidget, R.drawable.ic_action_marker_dark,
				R.string.map_markers_bar, topPanel);

		MapWidget streetNameWidget = new StreetNameWidget(mapActivity);
		registerWidget(WIDGET_STREET_NAME, streetNameWidget, R.drawable.widget_coordinates_latitude_day,
				R.string.street_name, topPanel);

		MapWidget lanesWidget = new LanesWidget(mapActivity);
		registerWidget(WIDGET_LANES, lanesWidget, R.drawable.ic_action_lanes, R.string.show_lanes, topPanel);
	}

	private void registerLeftWidgets(@NonNull MapActivity mapActivity,
	                                 @NonNull RouteInfoWidgetsFactory routeWidgetsFactory) {
		int settingsIconId = R.drawable.ic_action_next_turn;
		WidgetsPanel leftPanel = WidgetsPanel.LEFT;

		MapWidget bigInfoControl = routeWidgetsFactory.createNextInfoControl(mapActivity, false);
		registerWidget(WIDGET_NEXT_TURN, bigInfoControl, settingsIconId, R.string.map_widget_next_turn, leftPanel);

		MapWidget smallInfoControl = routeWidgetsFactory.createNextInfoControl(mapActivity, true);
		registerWidget(WIDGET_NEXT_TURN_SMALL, smallInfoControl, settingsIconId, R.string.map_widget_next_turn_small, leftPanel);

		MapWidget nextNextInfoControl = routeWidgetsFactory.createNextNextInfoControl(mapActivity, true);
		registerWidget(WIDGET_NEXT_NEXT_TURN, nextNextInfoControl, settingsIconId, R.string.map_widget_next_next_turn, leftPanel);
	}

	private void registerRightWidgets(@NonNull MapActivity mapActivity,
	                                  @NonNull RouteInfoWidgetsFactory routeWidgetsFactory,
	                                  @NonNull MapInfoWidgetsFactory mapWidgetsFactory) {
		OsmandApplication app = mapActivity.getMyApplication();
		MarkersWidgetsHelper markersWidgetsHelper = mapActivity.getMapLayers().getMapMarkersLayer().getMarkersWidgetsHelper();
		WidgetsPanel rightPanel = WidgetsPanel.RIGHT;

		MapWidget intermediateDist = routeWidgetsFactory.createIntermediateDistanceControl(mapActivity);
		registerWidget(WIDGET_INTERMEDIATE_DISTANCE, intermediateDist, R.drawable.ic_action_intermediate, R.string.map_widget_intermediate_distance, rightPanel);

		TextInfoWidget intermediateTime = routeWidgetsFactory.createTimeControl(mapActivity, true);
		registerWidget(WIDGET_INTERMEDIATE_TIME, intermediateTime, new TimeWidgetState(app, true), rightPanel);

		MapWidget dist = routeWidgetsFactory.createDistanceControl(mapActivity);
		registerWidget(WIDGET_DISTANCE, dist, R.drawable.ic_action_target, R.string.map_widget_distance, rightPanel);

		MapWidget time = routeWidgetsFactory.createTimeControl(mapActivity, false);
		registerWidget(WIDGET_TIME, time, new TimeWidgetState(app, false), rightPanel);

		MapWidget marker = markersWidgetsHelper.getMapMarkerSideWidget(true);
		registerWidget(WIDGET_MARKER_1, marker, R.drawable.ic_action_flag, R.string.map_marker_1st, rightPanel);

		MapWidget bearing = routeWidgetsFactory.createBearingControl(mapActivity);
		registerWidget(WIDGET_BEARING, bearing, new BearingWidgetState(app), rightPanel);

		MapWidget marker2nd = markersWidgetsHelper.getMapMarkerSideWidget(false);
		registerWidget(WIDGET_MARKER_2, marker2nd, R.drawable.ic_action_flag, R.string.map_marker_2nd, rightPanel);

		MapWidget speed = routeWidgetsFactory.createSpeedControl(mapActivity);
		registerWidget(WIDGET_SPEED, speed, R.drawable.ic_action_speed, R.string.map_widget_speed, rightPanel);

		MapWidget maxSpeed = routeWidgetsFactory.createMaxSpeedControl(mapActivity);
		registerWidget(WIDGET_MAX_SPEED, maxSpeed, R.drawable.ic_action_speed_limit, R.string.map_widget_max_speed, rightPanel);

		MapWidget alt = mapWidgetsFactory.createAltitudeControl(mapActivity);
		registerWidget(WIDGET_ALTITUDE, alt, R.drawable.ic_action_altitude, R.string.map_widget_altitude, rightPanel);

		MapWidget gpsInfo = mapWidgetsFactory.createGPSInfoControl(mapActivity);
		registerWidget(WIDGET_GPS_INFO, gpsInfo, R.drawable.ic_action_gps_info, R.string.map_widget_gps_info, rightPanel);

		MapWidget plainTime = routeWidgetsFactory.createPlainTimeControl(mapActivity);
		registerWidget(WIDGET_PLAIN_TIME, plainTime, R.drawable.ic_action_time, R.string.map_widget_plain_time, rightPanel);

		MapWidget battery = routeWidgetsFactory.createBatteryControl(mapActivity);
		registerWidget(WIDGET_BATTERY, battery, R.drawable.ic_action_battery, R.string.map_widget_battery, rightPanel);

		MapWidget radiusRuler = mapWidgetsFactory.createRadiusRulerControl(mapActivity);
		registerWidget(WIDGET_RADIUS_RULER, radiusRuler, new CompassRulerWidgetState(app), rightPanel);
	}

	@Nullable
	public MapWidgetInfo registerWidget(@NonNull String key, @NonNull MapWidget widget,
	                                    @DrawableRes int settingsIconId, @StringRes int messageId,
	                                    @NonNull WidgetsPanel widgetPanel) {
		if (mapInfoControls != null) {
			int order = widgetPanel.getWidgetOrder(key, settings);
			MapWidgetInfo widgetInfo = mapInfoControls.registerWidget(key, widget, null,
					settingsIconId, messageId, null, order, widgetPanel);
			widget.updateColors(calculateTextState());
			return widgetInfo;
		} else {
			return null;
		}
	}

	@Nullable
	public MapWidgetInfo registerWidget(@NonNull String key, @NonNull MapWidget widget,
	                                    @DrawableRes int settingsIconId, @Nullable String message,
	                                    @NonNull WidgetsPanel widgetPanel, int order) {
		if (mapInfoControls != null) {
			MapWidgetInfo reg = mapInfoControls.registerWidget(key, widget, null, settingsIconId,
					MapWidgetInfo.INVALID_ID, message, order, widgetPanel);
			widget.updateColors(calculateTextState());
			return reg;
		} else {
			return null;
		}
	}

	public void registerWidget(@NonNull String key, @NonNull MapWidget widget,
	                           @NonNull WidgetState widgetState, @NonNull WidgetsPanel widgetPanel) {
		if (mapInfoControls != null) {
			int order = widgetPanel.getWidgetOrder(key, settings);
			mapInfoControls.registerWidget(key, widget, widgetState, MapWidgetInfo.INVALID_ID,
					MapWidgetInfo.INVALID_ID, null, order, widgetPanel);
			widget.updateColors(calculateTextState());
		}
	}

	public void recreateControls() {
		ApplicationMode appMode = settings.getApplicationMode();
		recreateWidgetsPanel(topWidgetsContainer, WidgetsPanel.TOP, appMode);
		recreateWidgetsPanel(leftWidgetsContainer, WidgetsPanel.LEFT, appMode);
		recreateWidgetsPanel(rightWidgetsContainer, WidgetsPanel.RIGHT, appMode);
		recreateWidgetsPanel(bottomWidgetsContainer, WidgetsPanel.BOTTOM, appMode);
		setupExpandButton();
	}

	private void recreateWidgetsPanel(@Nullable ViewGroup container, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		if (container != null) {
			container.removeAllViews();
			if (mapInfoControls != null) {
				mapInfoControls.populateControlsContainer(container, appMode, panel, WIDGETS_EXPANDED);
			}
			container.requestLayout();
		}
	}

	private void setupExpandButton() {
		if (expandButton != null) {
			AndroidUiHelper.updateVisibility(expandButton, mapInfoControls.hasCollapsibles(settings.getApplicationMode()));
			UiUtilities uiUtilities = getApplication().getUIUtilities();
			int iconId = WIDGETS_EXPANDED ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
			int colorId = ColorUtilities.getMapButtonIconColorId(false);
			Drawable expandIcon = uiUtilities.getIcon(iconId, colorId);
			setMapButtonIcon(expandButton, expandIcon);
			int contentDescrId = WIDGETS_EXPANDED ? R.string.shared_string_collapse : R.string.access_widget_expand;
			expandButton.setContentDescription(getString(contentDescrId));
			expandButton.setOnClickListener(v -> {
				WIDGETS_EXPANDED = !WIDGETS_EXPANDED;
				recreateControls();
			});
		}
	}

	public RulerWidget setupRulerWidget(View mapRulerView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RulerWidget rulerWidget = RouteInfoWidgetsFactory.createRulerControl(mapActivity, mapRulerView);
			rulerWidget.setVisibility(false);

			TextState ts = calculateTextState();
			boolean nightMode = drawSettings != null && drawSettings.isNightMode();
			rulerWidget.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, (int) (2 * view.getDensity()));

			rulerWidgets.add(rulerWidget);

			return rulerWidget;
		} else {
			return null;
		}
	}

	public void removeRulerWidgets(List<RulerWidget> rulers) {
		List<RulerWidget> widgetList = new ArrayList<>(rulerWidgets);
		widgetList.removeAll(rulers);
		rulerWidgets = widgetList;
	}

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		routeLayer.setTrackChartPoints(trackChartPoints);
	}

	public static class TextState {
		public boolean textBold;
		public boolean night;
		public int textColor;
		public int textShadowColor;
		public int boxTop;
		public int rightRes;
		public int leftRes;
		public int expand;
		public int boxFree;
		public int textShadowRadius;
	}

	public void updateColorShadowsOfText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			TextState ts = calculateTextState();
			if (mapInfoControls != null) {
				for (MapWidgetInfo widgetInfo : mapInfoControls.getAllWidgets()) {
					widgetInfo.widget.updateColors(ts);
				}
			}
			updateTopToolbar(nightMode);

			expandButton.setBackgroundResource(ts.expand);
			int padding = expandButton.getPaddingLeft();
			expandButton.setPadding(padding, padding, padding, padding);
			topWidgetsContainer.invalidate();
			rightWidgetsContainer.invalidate();
			leftWidgetsContainer.invalidate();

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, (int) (2 * view.getDensity()));
			}
		}
	}

	private void updateTopToolbar(boolean nightMode) {
		topToolbarView.updateColors(nightMode);
	}

	@NonNull
	private TextState calculateTextState() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.night = nightMode;
		ts.textColor = nightMode ? ContextCompat.getColor(view.getContext(), R.color.widgettext_night) :
				ContextCompat.getColor(view.getContext(), R.color.widgettext_day);
		// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent
		ts.textShadowColor = nightMode ? ContextCompat.getColor(view.getContext(), R.color.widgettext_shadow_night) :
				ContextCompat.getColor(view.getContext(), R.color.widgettext_shadow_day);
		if (!transparent && !nightMode) {
			ts.textShadowRadius = 0;
		} else {
			ts.textShadowRadius = (int) (4 * view.getDensity());
		}
		if (transparent) {
			ts.boxTop = R.drawable.btn_flat_transparent;
			ts.rightRes = R.drawable.btn_left_round_transparent;
			ts.leftRes = R.drawable.btn_right_round_transparent;
			ts.expand = R.drawable.btn_inset_circle_transparent;
			ts.boxFree = R.drawable.btn_round_transparent;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.rightRes = R.drawable.btn_left_round_night;
			ts.leftRes = R.drawable.btn_right_round_night;
			ts.expand = R.drawable.btn_inset_circle_night;
			ts.boxFree = R.drawable.btn_round_night;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.rightRes = R.drawable.btn_left_round;
			ts.leftRes = R.drawable.btn_right_round;
			ts.expand = R.drawable.btn_inset_circle;
			ts.boxFree = R.drawable.btn_round;
		}
		return ts;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		if (getMapActivity() != null) {
			updateColorShadowsOfText();
			if (mapInfoControls != null) {
				ApplicationMode appMode = settings.getApplicationMode();
				mapInfoControls.updateWidgetsInfo(appMode, drawSettings, WIDGETS_EXPANDED);
			}
			topToolbarView.updateInfo();
			alarmControl.updateInfo(drawSettings, false);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateInfo(tileBox, drawSettings);
			}
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
}