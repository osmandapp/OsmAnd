package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.measurementtool.GpxApproximationHelper;
import net.osmand.plus.measurementtool.GpxApproximationParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.track.helpers.save.SaveGpxListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;

public class NavigateGpxHelper {

	public static void saveAndNavigateGpx(MapActivity mapActivity, GPXFile gpxFile,
	                                      GpxNavigationParams params) {
		WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
		saveGpx(mapActivity, gpxFile, errorMessage -> {
			MapActivity activity = activityRef.get();
			if (errorMessage == null && AndroidUtils.isActivityNotDestroyed(activity)) {
				navigateGpx_ShowOnMap(activity, gpxFile, params);
			}
		});
	}

	private static void saveGpx(MapActivity mapActivity, GPXFile gpxFile, SaveGpxListener listener) {
		if (Algorithms.isEmpty(gpxFile.path)) {
			OsmandApplication app = mapActivity.getMyApplication();
			String destFileName = "route" + IndexConstants.GPX_FILE_EXT;
			File destDir = app.getAppPath(IndexConstants.GPX_IMPORT_DIR);
			File destFile = app.getAppPath(IndexConstants.GPX_IMPORT_DIR + destFileName);
			while (destFile.exists()) {
				destFileName = AndroidUtils.createNewFileName(destFileName);
				destFile = new File(destDir, destFileName);
			}
			gpxFile.path = destFile.getAbsolutePath();
		}
		SaveGpxHelper.saveGpx(new File(gpxFile.path), gpxFile, listener);
	}

	public static void navigateGpx_ShowOnMap(@NonNull MapActivity activity, @NonNull GPXFile gpxFile,
	                                         @NonNull GpxNavigationParams navigationParams) {
		OsmandApplication app = activity.getMyApplication();
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpx = helper.getSelectedFileByPath(gpxFile.path);
		if (selectedGpx != null) {
			selectedGpx.setGpxFile(gpxFile, app);
		} else {
			GpxSelectionParams selectionParams = GpxSelectionParams.newInstance()
					.showOnMap().syncGroup().selectedByUser().addToMarkers()
					.addToHistory().saveSelection();
			helper.selectGpxFile(gpxFile, selectionParams);
		}
		navigateGpx_ApproximateIfNeeded(activity, gpxFile, navigationParams);
	}

	public static void navigateGpx_ApproximateIfNeeded(@NonNull MapActivity mapActivity,
	                                                   @NonNull GPXFile gpxFile,
	                                                   @NonNull GpxNavigationParams params) {
		if (params.isSnapToRoad()) {
			OsmandApplication app = mapActivity.getMyApplication();
			GpxApproximationParams approxParams = new GpxApproximationParams();
			approxParams.setAppMode(ApplicationMode.valueOfStringKey(params.getSnapToRoadMode(), null));
			approxParams.setDistanceThreshold(params.getSnapToRoadThreshold());
			WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
			GpxApproximationHelper.approximateGpxSilently(app, gpxFile, approxParams, approxGpx -> {
				MapActivity activity = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					navigateGpx_FinalCheck(activity, approxGpx, params);
				}
				return true;
			});
		} else {
			navigateGpx_FinalCheck(mapActivity, gpxFile, params);
		}
	}

	public static void navigateGpx_FinalCheck(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile,
	                                          @NonNull GpxNavigationParams params) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean force = params.isForce();
		boolean checkLocationPermission = params.isCheckLocationPermission();
		boolean passWholeRoute = params.isPassWholeRoute();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode() && !force) {
			WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
			mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
				MapActivity activity = activityRef.get();
				if (activity != null && !routingHelper.isFollowingMode()) {
					startNavigation(activity, gpxFile, checkLocationPermission, passWholeRoute);
				}
			});
		} else {
			startNavigation(mapActivity, gpxFile, checkLocationPermission, passWholeRoute);
		}
	}

	public static void startNavigation(MapActivity mapActivity, @NonNull GPXFile gpx, boolean checkLocationPermission, boolean passWholeRoute) {
		startNavigation(mapActivity, gpx, null, null, null, null, null, checkLocationPermission, passWholeRoute);
	}

	public static void startNavigation(MapActivity mapActivity,
	                                   @Nullable LatLon from, @Nullable PointDescription fromDesc,
	                                   @Nullable LatLon to, @Nullable PointDescription toDesc,
	                                   @NonNull ApplicationMode mode, boolean checkLocationPermission) {
		startNavigation(mapActivity, null, from, fromDesc, to, toDesc, mode, checkLocationPermission, false);
	}

	private static void startNavigation(MapActivity mapActivity, GPXFile gpx,
	                                    LatLon from, PointDescription fromDesc,
	                                    LatLon to, PointDescription toDesc,
	                                    ApplicationMode mode,
	                                    boolean checkLocationPermission,
	                                    boolean passWholeRoute) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		MapViewTrackingUtilities mapViewTrackingUtilities = mapActivity.getMapViewTrackingUtilities();
		if (gpx == null) {
			settings.setApplicationMode(mode);
			TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
			targets.removeAllWayPoints(false, true);
			targets.navigateToPoint(to, true, -1, toDesc);
		}
		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(
				gpx, from, fromDesc, true, false, passWholeRoute);
		if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
			mapActivity.getMapRouteInfoMenu().show();
		} else {
			if (settings.APPLICATION_MODE.get() != routingHelper.getAppMode()) {
				settings.setApplicationMode(routingHelper.getAppMode(), false);
			}
			mapViewTrackingUtilities.backToLocationImpl();
			settings.FOLLOW_THE_ROUTE.set(true);
			routingHelper.setFollowingMode(true);
			routingHelper.setRoutePlanningMode(false);
			mapViewTrackingUtilities.switchRoutePlanningMode();
			routingHelper.notifyIfRouteIsCalculated();
			routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
		}
		if (checkLocationPermission) {
			OsmAndLocationProvider.requestFineLocationPermissionIfNeeded(mapActivity);
		}
	}

}
