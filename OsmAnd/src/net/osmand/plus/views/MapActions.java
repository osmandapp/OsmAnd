package net.osmand.plus.views;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static net.osmand.data.PointDescription.POINT_TYPE_LOCATION;
import static net.osmand.plus.base.ContextMenuFragment.MenuState.HEADER_ONLY;
import static net.osmand.plus.settings.enums.TrackApproximationType.AUTOMATIC;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.MarkersPlanRouteContext;
import net.osmand.plus.measurementtool.GpxApproximationParams;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapActions {

	public static final int START_TRACK_POINT_MY_LOCATION_RADIUS_METERS = 50 * 1000;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION = 201;
	private static final int REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION = 202;


	protected final OsmandApplication app;
	protected final OsmandSettings settings;

	@Nullable
	protected MapActivity activity;

	@Nullable
	protected LatLon requestedLatLon;


	public MapActions(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public boolean hasUiContext() {
		return false;
	}

	public void setGPXRouteParams(@Nullable GpxFile gpxFile) {
		app.logRoutingEvent("setGPXRouteParams result " + (gpxFile != null ? gpxFile.getPath() : null));
		if (gpxFile == null) {
			app.getRoutingHelper().setGpxParams(null);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
		} else {
			GPXRouteParamsBuilder builder = new GPXRouteParamsBuilder(gpxFile, settings);
			builder.setCalculateOsmAndRouteParts(settings.GPX_ROUTE_CALC_OSMAND_PARTS.get());
			builder.setCalculateOsmAndRoute(settings.GPX_ROUTE_CALC.get());
			builder.setSelectedSegment(settings.GPX_SEGMENT_INDEX.get());
			builder.setSelectedRoute(settings.GPX_ROUTE_INDEX.get());
			builder.setPassWholeRoute(settings.GPX_PASS_WHOLE_ROUTE.get());

			ApplicationMode appMode = app.getRoutingHelper().getAppMode();
			if (!gpxFile.isAttachedToRoads() && settings.DETAILED_TRACK_GUIDANCE.getModeValue(appMode) == AUTOMATIC) {
				GpxApproximationParams params = new GpxApproximationParams();
				params.setAppMode(appMode);
				params.setDistanceThreshold(settings.GPX_APPROXIMATION_DISTANCE.getModeValue(appMode));
				builder.setApproximationParams(params);
			}

			List<Location> points = builder.getPoints(settings.getContext());
			app.getRoutingHelper().setGpxParams(builder);
			settings.FOLLOW_THE_GPX_ROUTE.set(gpxFile.getPath());
			if (!points.isEmpty()) {
				Location startLoc = points.get(0);
				Location finishLoc = points.get(points.size() - 1);
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

	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu,
	                                           @Nullable Boolean passWholeRoute) {
		enterRoutePlanningModeGivenGpx(gpxFile, null, from, fromName,
				useIntermediatePointsByDefault, showMenu,
				MapRouteInfoMenu.DEFAULT_MENU_STATE, passWholeRoute);
	}

	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu) {
		enterRoutePlanningModeGivenGpx(gpxFile, from, fromName, useIntermediatePointsByDefault, showMenu,
				MapRouteInfoMenu.DEFAULT_MENU_STATE);
	}

	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, int menuState) {
		enterRoutePlanningModeGivenGpx(gpxFile, null, from, fromName, useIntermediatePointsByDefault, showMenu, menuState, null);
	}

	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, ApplicationMode appMode, LatLon from, PointDescription fromName,
	                                           boolean useIntermediatePointsByDefault, boolean showMenu, int menuState,
	                                           @Nullable Boolean passWholeRoute) {
		settings.USE_INTERMEDIATE_POINTS_NAVIGATION.set(useIntermediatePointsByDefault);
		TargetPointsHelper targets = app.getTargetPointsHelper();

		if (gpxFile != null && gpxFile.hasRtePt() && appMode == null) {
			WptPt routePoint = gpxFile.getRoutePoints().get(0);
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
		// override "pass whole route" option only if it's present
		if (currentGPXRoute != null && passWholeRoute != null) {
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
		if (activity != null) {
			activity.getMapRouteInfoMenu().hide();
		}
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


	public void setMapActivity(MapActivity activity) {
		this.activity = activity;
	}

	public MapActivity getMapActivity() {
		return activity;
	}

	public void stopNavigation() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().hide();
			if (app.getRoutingHelper().isFollowingMode()) {
				activity.getMapActions().stopNavigationActionConfirm(null);
			} else {
				activity.getMapActions().stopNavigationWithoutConfirm();
			}
		} else {
			app.getOsmandMap().getMapActions().stopNavigationWithoutConfirm();
		}
	}

	public void showRouteInfoControlDialog() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().showHideMenu();
		}
	}

	public void showRouteInfoMenu() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().setShowMenu(MapRouteInfoMenu.DEFAULT_MENU_STATE);
		}
	}

	public void doRoute() {
		onNavigationClick();
	}

	public void doNavigate() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().hide();
			startNavigation();
		}
	}

	private void onNavigationClick() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			MapRouteInfoMenu mapRouteInfoMenu = activity.getMapRouteInfoMenu();
			mapRouteInfoMenu.cancelSelectionFromMap();
			MapActivity.clearPrevActivityIntent();

			RoutingHelper routingHelper = app.getRoutingHelper();
			if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
				TargetPointsHelper.TargetPoint start = app.getTargetPointsHelper().getPointToStart();
				if (start != null) {
					LatLon latLon = new LatLon(start.getLatitude(), start.getLongitude());
					activity.getMapActions().enterRoutePlanningMode(latLon, start.getOriginalPointDescription());
				} else {
					activity.getMapActions().enterRoutePlanningMode(null, null);
				}
			} else {
				showRouteInfoControlDialog();
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if ((requestCode == REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION
				|| requestCode == REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION
				|| requestCode == REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION)) {
			if (grantResults.length > 0) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					switch (requestCode) {
						case REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION:
							onNavigationClick();
							break;
						case REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION:
							navigateButton();
							break;
						case REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION:
							if (requestedLatLon != null) {
								addDestination(requestedLatLon);
							}
							break;
					}
				} else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
					app.showToastMessage(R.string.ask_for_location_permission);
				}
			}
		}
	}

	public void navigateButton() {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		if (!OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
			ActivityCompat.requestPermissions(activity,
					new String[] {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION},
					REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION);
		} else {
			MapContextMenu menu = activity.getContextMenu();
			LatLon latLon = menu.getLatLon();
			PointDescription pointDescription = menu.getPointDescriptionForTarget();
			menu.hide();

			TargetPointsHelper targets = app.getTargetPointsHelper();
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				DirectionsDialogs.addWaypointDialogAndLaunchMap(activity, latLon.getLatitude(), latLon.getLongitude(), pointDescription);
			} else if (targets.getIntermediatePoints().isEmpty()) {
				startRoutePlanningWithDestination(latLon, pointDescription, targets);
				menu.close();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(R.string.new_directions_point_dialog);
				int[] defaultVls = {0};
				builder.setSingleChoiceItems(new String[] {activity.getString(R.string.clear_intermediate_points),
						activity.getString(R.string.keep_intermediate_points)}, 0, (dialog, which) -> defaultVls[0] = which);
				builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					if (defaultVls[0] == 0) {
						targets.removeAllWayPoints(false, true);
					}
					targets.navigateToPoint(latLon, true, -1, pointDescription);
					activity.getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true, HEADER_ONLY);
					menu.close();
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
			}
		}
	}

	private void startRoutePlanningWithDestination(LatLon latLon, PointDescription pointDescription, TargetPointsHelper targets) {
		MapActivity activity = getMapActivity();
		MapActions mapActions = activity != null ? activity.getMapActions() : app.getOsmandMap().getMapActions();
		boolean hasPointToStart = settings.restorePointToStart();
		targets.navigateToPoint(latLon, true, -1, pointDescription);
		if (!hasPointToStart) {
			mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true, HEADER_ONLY);
		} else {
			TargetPointsHelper.TargetPoint start = targets.getPointToStart();
			if (start != null) {
				mapActions.enterRoutePlanningModeGivenGpx(null, start.point, start.getOriginalPointDescription(), true, true, HEADER_ONLY);
			} else {
				mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true, HEADER_ONLY);
			}
		}
	}

	public void buildRouteByGivenGpx(GpxFile gpxFile) {
		enterRoutePlanningModeGivenGpx(gpxFile, null, null, true, true, HEADER_ONLY);
	}

	private PointDescription getPointDescriptionForTarget(@NonNull LatLon latLon) {
		MapActivity activity = getMapActivity();
		MapContextMenu menu = activity != null ? activity.getContextMenu() : null;
		return menu != null && menu.isActive() && latLon.equals(menu.getLatLon())
				? menu.getPointDescriptionForTarget() : new PointDescription(POINT_TYPE_LOCATION, null);
	}

	public void addDestination(@NonNull LatLon latLon) {
		addDestination(latLon, null);
	}

	public void addDestination(@NonNull LatLon latLon, @Nullable PointDescription pointDescription) {
		MapActivity activity = getMapActivity();
		if (activity != null && !OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
			requestedLatLon = latLon;
			ActivityCompat.requestPermissions(activity, new String[] {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION);
		} else {
			if (pointDescription == null) {
				pointDescription = getPointDescriptionForTarget(latLon);
			}
			if (activity != null) {
				activity.getContextMenu().close();
			}
			TargetPointsHelper targets = app.getTargetPointsHelper();
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				targets.navigateToPoint(latLon, true, targets.getIntermediatePoints().size() + 1, pointDescription);
			} else if (targets.getIntermediatePoints().isEmpty()) {
				startRoutePlanningWithDestination(latLon, pointDescription, targets);
			}
		}
	}

	public void addFirstIntermediate(LatLon latLon) {
		if (latLon != null) {
			RoutingHelper routingHelper = app.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.getContextMenu().close();
				}
				PointDescription pointDescription = getPointDescriptionForTarget(latLon);
				TargetPointsHelper targets = app.getTargetPointsHelper();
				if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
					targets.navigateToPoint(latLon, true, 0, pointDescription);
				} else if (targets.getIntermediatePoints().isEmpty()) {
					startRoutePlanningWithDestination(latLon, pointDescription, targets);
				}
			} else {
				addDestination(latLon);
			}
		}
	}

	public void replaceDestination(@NonNull LatLon latLon) {
		replaceDestination(latLon, null);
	}

	public void replaceDestination(@NonNull LatLon latLon, @Nullable PointDescription pointDescription) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
			if (pointDescription == null) {
				pointDescription = getPointDescriptionForTarget(latLon);
			}
			MapActivity activity = getMapActivity();
			if (activity != null) {
				activity.getContextMenu().close();
			}
			TargetPointsHelper targets = app.getTargetPointsHelper();
			targets.navigateToPoint(latLon, true, -1, pointDescription);
		} else {
			addDestination(latLon, pointDescription);
		}
	}

	public void switchToRouteFollowingLayout() {
		app.getRoutingHelper().setRoutePlanningMode(false);
		app.getMapViewTrackingUtilities().switchRoutePlanningMode();
		app.getOsmandMap().getMapView().refreshMap();
	}

	public void startNavigation() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (settings.getApplicationMode() != routingHelper.getAppMode()) {
			settings.setApplicationMode(routingHelper.getAppMode(), false);
		}
		if (routingHelper.isFollowingMode()) {
			switchToRouteFollowingLayout();
		} else {
			MapActivity activity = getMapActivity();
			if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
				if (activity != null) {
					activity.getMapRouteInfoMenu().show();
				}
			} else {
				app.logEvent("start_navigation");
				app.getMapViewTrackingUtilities().backToLocationImpl(17, true);
				settings.FOLLOW_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setRoutePlanningMode(false);
				app.getMapViewTrackingUtilities().switchRoutePlanningMode();
				routingHelper.notifyIfRouteIsCalculated();
				if (!settings.simulateNavigation) {
					routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
				} else if (routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
					OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
					if (!sim.isRouteAnimating()) {
						sim.startStopRouteAnimation(activity);
					}
				}
				if (activity != null) {
					AndroidUtils.requestNotificationPermissionIfNeeded(activity);
				}
			}
		}
	}

	public void selectAddress(String name, double latitude, double longitude, QuickSearchDialogFragment.QuickSearchType searchType) {
		MapRouteInfoMenu.PointType pointType = null;
		switch (searchType) {
			case START_POINT:
				pointType = MapRouteInfoMenu.PointType.START;
				break;
			case DESTINATION:
			case DESTINATION_AND_START:
				pointType = MapRouteInfoMenu.PointType.TARGET;
				break;
			case INTERMEDIATE:
				pointType = MapRouteInfoMenu.PointType.INTERMEDIATE;
				break;
			case HOME_POINT:
				pointType = MapRouteInfoMenu.PointType.HOME;
				break;
			case WORK_POINT:
				pointType = MapRouteInfoMenu.PointType.WORK;
				break;
		}
		MapActivity activity = getMapActivity();
		if (pointType != null && activity != null) {
			activity.getMapRouteInfoMenu().selectAddress(name, new LatLon(latitude, longitude), pointType);
		}
	}

	@NonNull
	private String getZoomInfo(@NonNull RotatedTileBox tileBox) {
		StringBuilder builder = new StringBuilder();
		builder.append(tileBox.getZoom());

		double zoomFloatPart = tileBox.getZoomFloatPart() + tileBox.getZoomAnimation();
		int formattedZoomFloatPart = (int) Math.abs(Math.round(zoomFloatPart * 100));
		boolean addLeadingZero = formattedZoomFloatPart < 10;
		builder.append(" ")
				.append(zoomFloatPart < 0 ? "-" : "+")
				.append(".")
				.append(addLeadingZero ? "0" : "")
				.append(formattedZoomFloatPart);

		double mapDensity = Math.abs(tileBox.getMapDensity());
		if (mapDensity != 0) {
			int mapDensity10 = (int) (mapDensity * 10);
			builder.append(" ").append(mapDensity10 / 10);
			if (mapDensity10 % 10 != 0) {
				builder.append(".").append(mapDensity10 % 10);
			}
		}

		return builder.toString();
	}
}
