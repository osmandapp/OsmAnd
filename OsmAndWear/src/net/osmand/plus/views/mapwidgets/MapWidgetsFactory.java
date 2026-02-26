package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MY_LOCATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MAP_CENTER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.views.mapwidgets.widgets.AltitudeWidget;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.BatteryWidget;
import net.osmand.plus.views.mapwidgets.widgets.BearingWidget;
import net.osmand.plus.views.mapwidgets.widgets.BearingWidget.BearingType;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesMapCenterWidget;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesCurrentLocationWidget;
import net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.CurrentTimeWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DistanceToDestinationWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DistanceToIntermediateDestinationWidget;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;
import net.osmand.plus.views.mapwidgets.widgets.GlideAverageWidget;
import net.osmand.plus.views.mapwidgets.widgets.GlideTargetWidget;
import net.osmand.plus.views.mapwidgets.widgets.GpsInfoWidget;
import net.osmand.plus.views.mapwidgets.widgets.LanesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.MaxSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.NextTurnWidget;
import net.osmand.plus.views.mapwidgets.widgets.RadiusRulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.SecondNextTurnWidget;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget;
import net.osmand.plus.views.mapwidgets.widgets.TimeToNavigationPointWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.GlideTargetWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.SunriseSunsetWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState;

public class MapWidgetsFactory {

	private final OsmandApplication app;
	private final MapActivity mapActivity;

	public MapWidgetsFactory(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
	}

	@Nullable
	public MapWidget createMapWidget(@NonNull WidgetType widgetType) {
		return createMapWidget(null, widgetType, null);
	}

	public MapWidget createMapWidget(@Nullable String customId, @NonNull WidgetType widgetType, @Nullable WidgetsPanel panel) {
		switch (widgetType) {
			case NEXT_TURN:
				return new NextTurnWidget(mapActivity, customId, false, panel);
			case SMALL_NEXT_TURN:
				return new NextTurnWidget(mapActivity, customId, true , panel);
			case SECOND_NEXT_TURN:
				return new SecondNextTurnWidget(mapActivity, customId, panel);
			case COORDINATES_CURRENT_LOCATION:
				return new CoordinatesCurrentLocationWidget(mapActivity);
			case COORDINATES_MAP_CENTER:
				return new CoordinatesMapCenterWidget(mapActivity);
			case STREET_NAME:
				return new StreetNameWidget(mapActivity);
			case MARKERS_TOP_BAR:
				return new MapMarkersBarWidget(mapActivity, customId);
			case LANES:
				return new LanesWidget(mapActivity);
			case DISTANCE_TO_DESTINATION:
				return new DistanceToDestinationWidget(mapActivity, customId, panel);
			case INTERMEDIATE_DESTINATION:
				return new DistanceToIntermediateDestinationWidget(mapActivity, customId, panel);
			case TIME_TO_INTERMEDIATE:
				TimeToNavigationPointWidgetState state = new TimeToNavigationPointWidgetState(app, customId, true);
				return new TimeToNavigationPointWidget(mapActivity, state, customId, panel);
			case TIME_TO_DESTINATION:
				TimeToNavigationPointWidgetState widgetState = new TimeToNavigationPointWidgetState(app, customId, false);
				return new TimeToNavigationPointWidget(mapActivity, widgetState, customId, panel);
			case SIDE_MARKER_1:
				MapMarkerSideWidgetState firstMarkerState = new MapMarkerSideWidgetState(app, customId, true);
				return new MapMarkerSideWidget(mapActivity, firstMarkerState, customId, panel);
			case SIDE_MARKER_2:
				MapMarkerSideWidgetState secondMarkerState = new MapMarkerSideWidgetState(app, customId, false);
				return new MapMarkerSideWidget(mapActivity, secondMarkerState, customId, panel);
			case RELATIVE_BEARING:
				return new BearingWidget(mapActivity, BearingType.RELATIVE_BEARING, customId, panel);
			case MAGNETIC_BEARING:
				return new BearingWidget(mapActivity, BearingType.MAGNETIC_BEARING, customId, panel);
			case TRUE_BEARING:
				return new BearingWidget(mapActivity, BearingType.TRUE_BEARING, customId, panel);
			case CURRENT_SPEED:
				return new CurrentSpeedWidget(mapActivity, customId, panel);
			case AVERAGE_SPEED:
				return new AverageSpeedWidget(mapActivity, customId, panel);
			case MAX_SPEED:
				return new MaxSpeedWidget(mapActivity, customId, panel);
			case ALTITUDE_MY_LOCATION:
				return new AltitudeWidget(mapActivity, ALTITUDE_MY_LOCATION, customId, panel);
			case ALTITUDE_MAP_CENTER:
				return new AltitudeWidget(mapActivity, ALTITUDE_MAP_CENTER, customId, panel);
			case GPS_INFO:
				return new GpsInfoWidget(mapActivity, customId, panel);
			case CURRENT_TIME:
				return new CurrentTimeWidget(mapActivity, customId, panel);
			case BATTERY:
				return new BatteryWidget(mapActivity, customId, panel);
			case RADIUS_RULER:
				return new RadiusRulerWidget(mapActivity, customId, panel);
			case SUNRISE:
				SunriseSunsetWidgetState sunriseState = new SunriseSunsetWidgetState(app, customId, WidgetType.SUNRISE);
				return new SunriseSunsetWidget(mapActivity, sunriseState, customId, panel);
			case SUNSET:
				SunriseSunsetWidgetState sunsetState = new SunriseSunsetWidgetState(app, customId, WidgetType.SUNSET);
				return new SunriseSunsetWidget(mapActivity, sunsetState, customId, panel);
			case SUN_POSITION:
				SunriseSunsetWidgetState sunriseSunsetWidgetState = new SunriseSunsetWidgetState(app, customId, WidgetType.SUN_POSITION);
				return new SunriseSunsetWidget(mapActivity, sunriseSunsetWidgetState, customId, panel);
			case GLIDE_TARGET:
				GlideTargetWidgetState glideWidgetState = new GlideTargetWidgetState(app, customId);
				return new GlideTargetWidget(mapActivity, glideWidgetState, customId, panel);
			case GLIDE_AVERAGE:
				return new GlideAverageWidget(mapActivity, customId, panel);
			case ELEVATION_PROFILE:
				return new ElevationProfileWidget(mapActivity, customId);
			case AIDL_WIDGET:
				return app.getAidlApi().askCreateExternalWidget(mapActivity, customId, panel);
			default:
				return PluginsHelper.createMapWidget(mapActivity, widgetType, customId, panel);
		}
	}
}