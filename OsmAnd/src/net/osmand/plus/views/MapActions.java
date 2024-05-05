package net.osmand.plus.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.MarkersPlanRouteContext;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapActions {

	public static final int START_TRACK_POINT_MY_LOCATION_RADIUS_METERS = 50 * 1000;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public MapActions(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public boolean hasUiContext() {
		return false;
	}

	public void setGPXRouteParams(@Nullable GPXFile result) {
		app.logRoutingEvent("setGPXRouteParams result " + (result != null ? result.path : null));
		if (result == null) {
			app.getRoutingHelper().setGpxParams(null);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
		} else {
			GPXRouteParamsBuilder params = new GPXRouteParamsBuilder(result, settings);
			params.setCalculateOsmAndRouteParts(settings.GPX_ROUTE_CALC_OSMAND_PARTS.get());
			params.setCalculateOsmAndRoute(settings.GPX_ROUTE_CALC.get());
			params.setSelectedSegment(settings.GPX_SEGMENT_INDEX.get());
			params.setSelectedRoute(settings.GPX_ROUTE_INDEX.get());
			List<Location> ps = params.getPoints(settings.getContext());
			app.getRoutingHelper().setGpxParams(params);
			settings.FOLLOW_THE_GPX_ROUTE.set(result.path);
			if (!ps.isEmpty()) {
				Location startLoc = ps.get(0);
				Location finishLoc = ps.get(ps.size() - 1);
				Location location = app.getLocationProvider().getLastKnownLocation();
				TargetPointsHelper pointsHelper = app.getTargetPointsHelper();
				pointsHelper.clearAllIntermediatePoints(false);
				if (location == null || MapUtils.getDistance(location, startLoc) <= START_TRACK_POINT_MY_LOCATION_RADIUS_METERS) {
					pointsHelper.clearStartPoint(false);
				} else {
					pointsHelper.setStartPoint(new LatLon(startLoc.getLatitude(), startLoc.getLongitude()), false, null);
				}
				pointsHelper.navigateToPoint(new LatLon(finishLoc.getLatitude(), finishLoc.getLongitude()), false, -1);
			}
		}
	}

	public void enterRoutePlanningMode(LatLon from, PointDescription fromName) {
		enterRoutePlanningModeGivenGpx(null, from, fromName, true, true);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, boolean passWholeRoute) {
		enterRoutePlanningModeGivenGpx(gpxFile, null, from, fromName, useIntermediatePointsByDefault, showMenu,
				MapRouteInfoMenu.DEFAULT_MENU_STATE, passWholeRoute);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu) {
		enterRoutePlanningModeGivenGpx(gpxFile, from, fromName, useIntermediatePointsByDefault, showMenu,
				MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, int menuState) {
		enterRoutePlanningModeGivenGpx(gpxFile, null, from, fromName, useIntermediatePointsByDefault, showMenu, menuState, false);
	}

	public void enterRoutePlanningModeGivenGpx(GPXFile gpxFile, ApplicationMode appMode, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, int menuState, boolean passWholeRoute) {
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(useIntermediatePointsByDefault);
		TargetPointsHelper targets = app.getTargetPointsHelper();

		if (gpxFile != null && gpxFile.hasRtePt() && appMode == null) {
			GPXUtilities.WptPt routePoint = gpxFile.getRoutePoints().get(0);
			ApplicationMode routePointAppMode = ApplicationMode.valueOfStringKey(routePoint.getProfileType(), ApplicationMode.DEFAULT);
			if (routePointAppMode != ApplicationMode.DEFAULT) {
				appMode = routePointAppMode;
			}
		}
		ApplicationMode mode = appMode != null ? appMode : getRouteMode();
		if (app.getSettings().getApplicationMode() != ApplicationMode.DEFAULT) {
			app.getSettings().setApplicationMode(mode, false);
		}
		RoutingHelper routingHelper = app.getRoutingHelper();
		routingHelper.setAppMode(mode);
		initVoiceCommandPlayer(mode, showMenu);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		routingHelper.setFollowingMode(false);
		routingHelper.setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(from, false, fromName);
		// then set gpx
		setGPXRouteParams(gpxFile);
		GPXRouteParamsBuilder currentGPXRoute = routingHelper.getCurrentGPXRoute();
		if (currentGPXRoute != null) {
			currentGPXRoute.setPassWholeRoute(passWholeRoute);
		}
		// then update start and destination point
		targets.updateRouteAndRefresh(true);

		app.getMapViewTrackingUtilities().switchRoutePlanningMode();
		app.getOsmandMap().refreshMap(true);

		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	protected void initVoiceCommandPlayer(@NonNull ApplicationMode mode, boolean showMenu) {
		app.initVoiceCommandPlayer(app, mode, null, true, false, false, showMenu);
	}

	public void recalculateRoute(boolean showDialog) {
		app.logRoutingEvent("recalculateRoute showDialog " + showDialog);
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(true);
		TargetPointsHelper targets = app.getTargetPointsHelper();

		ApplicationMode mode = getRouteMode();
		//app.getSettings().setApplicationMode(mode, false);
		app.getRoutingHelper().setAppMode(mode);
		//Test for #2810: No need to init player here?
		//app.initVoiceCommandPlayer(mapActivity, true, null, false, false);
		// save application mode controls
		settings.FOLLOW_THE_ROUTE.set(false);
		app.getRoutingHelper().setFollowingMode(false);
		app.getRoutingHelper().setRoutePlanningMode(true);
		// reset start point
		targets.setStartPoint(null, false, null);
		// then update start and destination point
		targets.updateRouteAndRefresh(true);

		app.getMapViewTrackingUtilities().switchRoutePlanningMode();
		app.getOsmandMap().refreshMap(true);

		if (targets.hasTooLongDistanceToNavigate()) {
			app.showToastMessage(R.string.route_is_too_long_v2);
		}
	}

	public ApplicationMode getRouteMode() {
		MarkersPlanRouteContext planRouteContext = app.getMapMarkersHelper().getPlanRouteContext();
		if (planRouteContext.isNavigationFromMarkers() && planRouteContext.getSnappedMode() != ApplicationMode.DEFAULT) {
			planRouteContext.setNavigationFromMarkers(false);
			return planRouteContext.getSnappedMode();
		}
		ApplicationMode mode = settings.DEFAULT_APPLICATION_MODE.get();
		ApplicationMode selected = settings.APPLICATION_MODE.get();
		if (selected != ApplicationMode.DEFAULT) {
			mode = selected;
		} else if (mode == ApplicationMode.DEFAULT) {
			for (ApplicationMode appMode : ApplicationMode.values(app)) {
				if (appMode != ApplicationMode.DEFAULT) {
					mode = appMode;
					break;
				}
			}
			if (settings.LAST_ROUTING_APPLICATION_MODE != null &&
					settings.LAST_ROUTING_APPLICATION_MODE != ApplicationMode.DEFAULT) {
				mode = settings.LAST_ROUTING_APPLICATION_MODE;
			}
		}
		return mode;
	}

	public void stopNavigationWithoutConfirm() {
		app.stopNavigation();
		List<ApplicationMode> modes = ApplicationMode.values(app);
		for (ApplicationMode mode : modes) {
			if (settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(mode)) {
				settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false).setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false).setModeValue(mode, false);
			}
		}
	}
}
