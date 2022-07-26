package net.osmand.plus.views.mapwidgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.views.mapwidgets.widgets.AltitudeWidget;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.BatteryWidget;
import net.osmand.plus.views.mapwidgets.widgets.BearingWidget;
import net.osmand.plus.views.mapwidgets.widgets.BearingWidget.BearingType;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesWidget;
import net.osmand.plus.views.mapwidgets.widgets.CurrentSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.CurrentTimeWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DistanceToDestinationWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DistanceToIntermediateDestinationWidget;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;
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
import net.osmand.plus.views.mapwidgets.widgets.TimeToNavigationPointWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState;
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
		switch (widgetType) {
			case NEXT_TURN:
				return new NextTurnWidget(mapActivity, false);
			case SMALL_NEXT_TURN:
				return new NextTurnWidget(mapActivity, true);
			case SECOND_NEXT_TURN:
				return new SecondNextTurnWidget(mapActivity);
			case COORDINATES:
				return new CoordinatesWidget(mapActivity);
			case STREET_NAME:
				return new StreetNameWidget(mapActivity);
			case MARKERS_TOP_BAR:
				return new MapMarkersBarWidget(mapActivity);
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
			case ALTITUDE:
				return new AltitudeWidget(mapActivity);
			case GPS_INFO:
				return new GpsInfoWidget(mapActivity);
			case CURRENT_TIME:
				return new CurrentTimeWidget(mapActivity);
			case BATTERY:
				return new BatteryWidget(mapActivity);
			case RADIUS_RULER:
				return new RadiusRulerWidget(mapActivity);
			case ELEVATION_PROFILE:
				return new ElevationProfileWidget(mapActivity);
			default:
				return OsmandPlugin.createMapWidget(mapActivity, widgetType);
		}
	}
}