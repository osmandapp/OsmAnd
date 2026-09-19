package net.osmand.plus.views.layers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static net.osmand.data.PointDescription.POINT_TYPE_LOCATION;
import static net.osmand.plus.base.ContextMenuFragment.MenuState.HEADER_ONLY;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.MapActions;

public class MapActionsHelper {

	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_FAB_PERMISSION = 201;
	private static final int REQUEST_LOCATION_FOR_ADD_DESTINATION_PERMISSION = 202;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapControlsLayer layer;

	@Nullable
	private LatLon requestedLatLon;

	public MapActionsHelper(@NonNull MapControlsLayer layer) {
		this.layer = layer;
		this.app = layer.getApplication();
		this.settings = app.getSettings();
	}

	public void stopNavigation() {
		MapActivity activity = layer.getMapActivity();
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

	public void stopNavigationWithoutConfirm() {
		MapActivity activity = layer.getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().hide();
			activity.getMapActions().stopNavigationWithoutConfirm();
		}
	}

	public void showRouteInfoControlDialog() {
		MapActivity activity = layer.getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().showHideMenu();
		}
	}

	public void showRouteInfoMenu() {
		MapActivity activity = layer.getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().setShowMenu(MapRouteInfoMenu.DEFAULT_MENU_STATE);
		}
	}

	public void doRoute() {
		onNavigationClick();
	}

	public void doNavigate() {
		MapActivity activity = layer.getMapActivity();
		if (activity != null) {
			activity.getMapRouteInfoMenu().hide();
			startNavigation();
		}
	}

	private void onNavigationClick() {
		MapActivity activity = layer.getMapActivity();
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
		MapActivity activity = layer.getMapActivity();
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
		MapActivity activity = layer.getMapActivity();
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
		MapActivity activity = layer.getMapActivity();
		MapActions mapActions = activity != null ? activity.getMapActions() : app.getOsmandMap().getMapActions();
		mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null, true, true, HEADER_ONLY);
	}

	private PointDescription getPointDescriptionForTarget(@NonNull LatLon latLon) {
		MapActivity activity = layer.getMapActivity();
		MapContextMenu menu = activity != null ? activity.getContextMenu() : null;
		return menu != null && menu.isActive() && latLon.equals(menu.getLatLon())
				? menu.getPointDescriptionForTarget() : new PointDescription(POINT_TYPE_LOCATION, null);
	}

	public void addDestination(@NonNull LatLon latLon) {
		addDestination(latLon, null);
	}

	public void addDestination(@NonNull LatLon latLon, @Nullable PointDescription pointDescription) {
		MapActivity activity = layer.getMapActivity();
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
				MapActivity activity = layer.getMapActivity();
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
			MapActivity activity = layer.getMapActivity();
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
		layer.resetTouchEvent();
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
			MapActivity activity = layer.getMapActivity();
			if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
				if (activity != null) {
					activity.getMapRouteInfoMenu().show();
				}
			} else {
				layer.resetTouchEvent();
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

	public void selectAddress(String name, double latitude, double longitude, QuickSearchType searchType) {
		PointType pointType = null;
		switch (searchType) {
			case START_POINT:
				pointType = PointType.START;
				break;
			case DESTINATION:
			case DESTINATION_AND_START:
				pointType = PointType.TARGET;
				break;
			case INTERMEDIATE:
				pointType = PointType.INTERMEDIATE;
				break;
			case HOME_POINT:
				pointType = PointType.HOME;
				break;
			case WORK_POINT:
				pointType = PointType.WORK;
				break;
		}
		MapActivity activity = layer.getMapActivity();
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
