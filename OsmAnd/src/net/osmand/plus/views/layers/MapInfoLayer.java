package net.osmand.plus.views.layers;


import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.StateChangedListener;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.RightWidgetsPanel;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.BearingWidget;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;
import net.osmand.plus.views.mapwidgets.widgets.LanesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.NavigationTimeWidget.ArrivalTimeWidget;
import net.osmand.plus.views.mapwidgets.widgets.NavigationTimeWidget.TimeToGoWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.CompassRulerWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.ElevationProfileWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static net.osmand.plus.views.mapwidgets.WidgetParams.ALTITUDE;
import static net.osmand.plus.views.mapwidgets.WidgetParams.ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetParams.COORDINATES;
import static net.osmand.plus.views.mapwidgets.WidgetParams.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetParams.CURRENT_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetParams.ELEVATION_PROFILE;
import static net.osmand.plus.views.mapwidgets.WidgetParams.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_TIME_TO_GO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.LANES;
import static net.osmand.plus.views.mapwidgets.WidgetParams.MAGNETIC_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetParams.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetParams.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.WidgetParams.RELATIVE_BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SIDE_MARKER_2;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SMALL_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.STREET_NAME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TIME_TO_GO;

public class MapInfoLayer extends OsmandMapLayer {

	private final RouteLayer routeLayer;
	private final OsmandSettings settings;
	private OsmandMapTileView view;

	private ViewGroup topWidgetsContainer;
	private RightWidgetsPanel rightWidgetsPanel;
	private ViewGroup leftWidgetsContainer;
	private ViewGroup bottomWidgetsContainer;

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
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			bottomWidgetsContainer = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
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
			rightWidgetsPanel = null;
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
		return appMode -> getApplication().runInUIThread(() -> {
			if (mapInfoControls != null) {
				mapInfoControls.reorderWidgets();
				MapInfoLayer.this.recreateControls();
			}
		});
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
		registerWidget(ELEVATION_PROFILE, widget, widgetState, WidgetsPanel.BOTTOM);
	}

	private void registerTopWidgets(@NonNull MapActivity mapActivity) {
		WidgetsPanel topPanel = WidgetsPanel.TOP;

		MapWidget coordinatedWidget = new CoordinatesWidget(mapActivity);
		registerWidget(COORDINATES, coordinatedWidget, topPanel);

		MapWidget mapMarkersWidget = mapActivity.getMapLayers().getMapMarkersLayer()
				.getMarkersWidgetsHelper().getMapMarkersBarWidget();
		registerWidget(MARKERS_TOP_BAR, mapMarkersWidget, topPanel);

		MapWidget streetNameWidget = new StreetNameWidget(mapActivity);
		registerWidget(STREET_NAME, streetNameWidget, topPanel);

		MapWidget lanesWidget = new LanesWidget(mapActivity);
		registerWidget(LANES, lanesWidget, topPanel);
	}

	private void registerLeftWidgets(@NonNull MapActivity mapActivity,
	                                 @NonNull RouteInfoWidgetsFactory routeWidgetsFactory) {
		WidgetsPanel leftPanel = WidgetsPanel.LEFT;

		MapWidget bigInfoControl = routeWidgetsFactory.createNextInfoControl(mapActivity, false);
		registerWidget(NEXT_TURN, bigInfoControl, leftPanel);

		MapWidget smallInfoControl = routeWidgetsFactory.createNextInfoControl(mapActivity, true);
		registerWidget(SMALL_NEXT_TURN, smallInfoControl, leftPanel);

		MapWidget nextNextInfoControl = routeWidgetsFactory.createNextNextInfoControl(mapActivity, true);
		registerWidget(SECOND_NEXT_TURN, nextNextInfoControl, leftPanel);
	}

	private void registerRightWidgets(@NonNull MapActivity mapActivity,
	                                  @NonNull RouteInfoWidgetsFactory routeWidgetsFactory,
	                                  @NonNull MapInfoWidgetsFactory mapWidgetsFactory) {
		OsmandApplication app = mapActivity.getMyApplication();
		MarkersWidgetsHelper markersWidgetsHelper = mapActivity.getMapLayers().getMapMarkersLayer().getMarkersWidgetsHelper();
		WidgetsPanel rightPanel = WidgetsPanel.RIGHT;

		MapWidget intermediateDist = routeWidgetsFactory.createIntermediateDistanceControl(mapActivity);
		registerWidget(INTERMEDIATE_DESTINATION, intermediateDist, rightPanel);

		MapWidget intermediateArrivalTime = new ArrivalTimeWidget(mapActivity, true);
		registerWidget(INTERMEDIATE_ARRIVAL_TIME, intermediateArrivalTime, rightPanel);

		MapWidget intermediateTimeToGo = new TimeToGoWidget(mapActivity, true);
		registerWidget(INTERMEDIATE_TIME_TO_GO, intermediateTimeToGo, rightPanel);

		MapWidget distanceToDestination = routeWidgetsFactory.createDistanceControl(mapActivity);
		registerWidget(DISTANCE_TO_DESTINATION, distanceToDestination, rightPanel);

		MapWidget arrivalTime = new ArrivalTimeWidget(mapActivity, false);
		registerWidget(ARRIVAL_TIME, arrivalTime, rightPanel);

		MapWidget timeToGo = new TimeToGoWidget(mapActivity, false);
		registerWidget(TIME_TO_GO, timeToGo, rightPanel);

		MapWidget marker = markersWidgetsHelper.getMapMarkerSideWidget(true);
		registerWidget(SIDE_MARKER_1, marker, rightPanel);

		MapWidget relativeBearing = new BearingWidget(mapActivity, true);
		registerWidget(RELATIVE_BEARING, relativeBearing, rightPanel);

		MapWidget magneticBearing = new BearingWidget(mapActivity, false);
		registerWidget(MAGNETIC_BEARING, magneticBearing, rightPanel);

		MapWidget marker2nd = markersWidgetsHelper.getMapMarkerSideWidget(false);
		registerWidget(SIDE_MARKER_2, marker2nd, rightPanel);

		MapWidget currentSpeed = routeWidgetsFactory.createSpeedControl(mapActivity);
		registerWidget(CURRENT_SPEED, currentSpeed, rightPanel);

		MapWidget maxSpeed = routeWidgetsFactory.createMaxSpeedControl(mapActivity);
		registerWidget(MAX_SPEED, maxSpeed, rightPanel);

		MapWidget altitude = mapWidgetsFactory.createAltitudeControl(mapActivity);
		registerWidget(ALTITUDE, altitude, rightPanel);

		MapWidget gpsInfo = mapWidgetsFactory.createGPSInfoControl(mapActivity);
		registerWidget(GPS_INFO, gpsInfo, rightPanel);

		MapWidget plainTime = routeWidgetsFactory.createPlainTimeControl(mapActivity);
		registerWidget(CURRENT_TIME, plainTime, rightPanel);

		MapWidget battery = routeWidgetsFactory.createBatteryControl(mapActivity);
		registerWidget(BATTERY, battery, rightPanel);

		MapWidget radiusRuler = mapWidgetsFactory.createRadiusRulerControl(mapActivity);
		registerWidget(RADIUS_RULER, radiusRuler, new CompassRulerWidgetState(app), rightPanel);
	}

	@Nullable
	public MapWidgetInfo registerWidget(@NonNull WidgetParams widgetParams,
	                                    @NonNull MapWidget widget,
	                                    @NonNull WidgetsPanel widgetPanel) {
		return registerWidget(widgetParams, widget, null, widgetPanel);
	}

	@Nullable
	public MapWidgetInfo registerWidget(@NonNull WidgetParams widgetParams,
	                                    @NonNull MapWidget widget,
	                                    @Nullable WidgetState widgetState,
	                                    @NonNull WidgetsPanel widgetPanel) {
		if (mapInfoControls != null) {
			int page = widgetPanel.getWidgetPage(widgetParams.id, settings);
			int order = widgetPanel.getWidgetOrder(widgetParams.id, settings);
			MapWidgetInfo widgetInfo = mapInfoControls.registerWidget(widgetParams.id, widget, widgetState,
					widgetParams.dayIconId, widgetParams.nightIconId, widgetParams.titleId,
					null, null, page, order, widgetPanel);
			widget.updateColors(calculateTextState());
			return widgetInfo;
		} else {
			return null;
		}
	}

	public void registerExternalWidget(@NonNull String key,
	                                   @NonNull MapWidget widget,
	                                   @DrawableRes int settingsIconId,
	                                   @Nullable String message,
	                                   @NonNull String externalProviderPackage,
	                                   @NonNull WidgetsPanel widgetPanel,
	                                   int order) {
		if (mapInfoControls != null) {
			int page = widgetPanel.getWidgetPage(key, settings);
			int savedOrder = widgetPanel.getWidgetOrder(key, settings);
			if (savedOrder != WidgetsPanel.DEFAULT_ORDER) {
				order = savedOrder;
			}
			mapInfoControls.registerWidget(key, widget, null, settingsIconId,
					settingsIconId, MapWidgetInfo.INVALID_ID, message, externalProviderPackage,
					page, order, widgetPanel);
			widget.updateColors(calculateTextState());
		}
	}

	public void recreateControls() {
		ApplicationMode appMode = settings.getApplicationMode();
		recreateWidgetsPanel(topWidgetsContainer, WidgetsPanel.TOP, appMode);
		recreateWidgetsPanel(leftWidgetsContainer, WidgetsPanel.LEFT, appMode);
		recreateWidgetsPanel(bottomWidgetsContainer, WidgetsPanel.BOTTOM, appMode);
		rightWidgetsPanel.update();
	}

	private void recreateWidgetsPanel(@Nullable ViewGroup container, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		if (container != null) {
			container.removeAllViews();
			if (mapInfoControls != null) {
				mapInfoControls.populateControlsContainer(container, appMode, panel);
			}
			container.requestLayout();
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
		public int boxFree;
		public int textShadowRadius;
		public int rightDividerColorId;
		public int rightBorderColorId;
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
			rightWidgetsPanel.updateColors(ts);

			topWidgetsContainer.invalidate();
			leftWidgetsContainer.invalidate();
			bottomWidgetsContainer.invalidate();

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
			ts.boxFree = R.drawable.btn_round_transparent;
			ts.rightDividerColorId = R.color.widget_divider_transparent;
			ts.rightBorderColorId = R.color.widget_panel_border_transparent;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.rightRes = R.drawable.btn_left_round_night;
			ts.leftRes = R.drawable.btn_right_round_night;
			ts.boxFree = R.drawable.btn_round_night;
			ts.rightDividerColorId = R.color.divider_color_dark;
			ts.rightBorderColorId = R.color.icon_color_secondary_dark;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.rightRes = R.drawable.btn_left_round;
			ts.leftRes = R.drawable.btn_right_round;
			ts.boxFree = R.drawable.btn_round;
			ts.rightDividerColorId = R.color.divider_color_light;
			ts.rightBorderColorId = R.color.stroked_buttons_and_links_outline_light;
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
				mapInfoControls.updateWidgetsInfo(appMode, drawSettings);
			}
			rightWidgetsPanel.update();
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