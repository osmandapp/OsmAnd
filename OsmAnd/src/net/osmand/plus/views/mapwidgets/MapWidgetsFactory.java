package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MY_LOCATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.ALTITUDE_MAP_CENTER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
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
		return createMapWidget(null, widgetType);
	}

	public MapWidget createMapWidget(@Nullable String customId, @NonNull WidgetType widgetType) {
		if (isWidgetCreationAllowed(widgetType)) {
			return createMapWidgetImpl(customId, widgetType);
		}
		return null;
	}

	private MapWidget createMapWidgetImpl(@Nullable String customId, @NonNull WidgetType widgetType) {
		switch (widgetType) {
			case NEXT_TURN:
				return new NextTurnWidget(mapActivity, false);
			case SMALL_NEXT_TURN:
				return new NextTurnWidget(mapActivity, true);
			case SECOND_NEXT_TURN:
				return new SecondNextTurnWidget(mapActivity);
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
				return new DistanceToDestinationWidget(mapActivity);
			case INTERMEDIATE_DESTINATION:
				return new DistanceToIntermediateDestinationWidget(mapActivity);
			case TIME_TO_INTERMEDIATE:
				TimeToNavigationPointWidgetState state = new TimeToNavigationPointWidgetState(app, customId, true);
				return new TimeToNavigationPointWidget(mapActivity, state);
			case TIME_TO_DESTINATION:
				TimeToNavigationPointWidgetState widgetState = new TimeToNavigationPointWidgetState(app, customId, false);
				return new TimeToNavigationPointWidget(mapActivity, widgetState);
			case SIDE_MARKER_1:
				MapMarkerSideWidgetState firstMarkerState = new MapMarkerSideWidgetState(app, customId, true);
				return new MapMarkerSideWidget(mapActivity, firstMarkerState);
			case SIDE_MARKER_2:
				MapMarkerSideWidgetState secondMarkerState = new MapMarkerSideWidgetState(app, customId, false);
				return new MapMarkerSideWidget(mapActivity, secondMarkerState);
			case RELATIVE_BEARING:
				return new BearingWidget(mapActivity, BearingType.RELATIVE_BEARING);
			case MAGNETIC_BEARING:
				return new BearingWidget(mapActivity, BearingType.MAGNETIC_BEARING);
			case TRUE_BEARING:
				return new BearingWidget(mapActivity, BearingType.TRUE_BEARING);
			case CURRENT_SPEED:
				return new CurrentSpeedWidget(mapActivity);
			case AVERAGE_SPEED:
				return new AverageSpeedWidget(mapActivity, customId);
			case MAX_SPEED:
				return new MaxSpeedWidget(mapActivity);
			case ALTITUDE_MY_LOCATION:
				return new AltitudeWidget(mapActivity, ALTITUDE_MY_LOCATION);
			case ALTITUDE_MAP_CENTER:
				return new AltitudeWidget(mapActivity, ALTITUDE_MAP_CENTER);
			case GPS_INFO:
				return new GpsInfoWidget(mapActivity);
			case CURRENT_TIME:
				return new CurrentTimeWidget(mapActivity);
			case BATTERY:
				return new BatteryWidget(mapActivity);
			case RADIUS_RULER:
				return new RadiusRulerWidget(mapActivity);
			case SUNRISE:
				SunriseSunsetWidgetState sunriseState = new SunriseSunsetWidgetState(app, customId, true);
				return new SunriseSunsetWidget(mapActivity, sunriseState);
			case SUNSET:
				SunriseSunsetWidgetState sunsetState = new SunriseSunsetWidgetState(app, customId, false);
				return new SunriseSunsetWidget(mapActivity, sunsetState);
			case GLIDE_TARGET:
				GlideTargetWidgetState glideWidgetState = new GlideTargetWidgetState(app, customId);
				return new GlideTargetWidget(mapActivity, glideWidgetState);
			case GLIDE_AVERAGE:
				return new GlideAverageWidget(mapActivity, customId);
			case ELEVATION_PROFILE:
				return new ElevationProfileWidget(mapActivity, customId);
			default:
				return PluginsHelper.createMapWidget(mapActivity, widgetType, customId);
		}
	}

	private boolean isWidgetCreationAllowed(@NonNull WidgetType widgetType) {
		if (widgetType == ALTITUDE_MAP_CENTER) {
			OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			return plugin != null && plugin.is3DMapsEnabled();
		}
		return true;
	}
}