package net.osmand.plus.helpers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.shared.gpx.GpxParameter.API_IMPORTED;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.aidlapi.navigation.NavigateGpxParams;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
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
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class NavigateGpxHelper {

	private static final String DEFAULT_FILE_NAME = "route";

	private final OsmandApplication app;
	private final WeakReference<MapActivity> mapActivityRef;
	private final GpxFile gpxFile;
	private final GpxNavigationParams navigationParams;


	public NavigateGpxHelper(@NonNull MapActivity mapActivity, @NonNull GpxFile gpxFile,
	                         @NonNull GpxNavigationParams navigationParams) {
		this.app = mapActivity.getMyApplication();
		this.mapActivityRef = new WeakReference<>(mapActivity);
		this.gpxFile = gpxFile;
		this.navigationParams = navigationParams;
	}

	private void step1_saveGpx() {
		updateFileNameIfNeeded(app, gpxFile, DEFAULT_FILE_NAME);
		SaveGpxHelper.saveGpx(new File(gpxFile.getPath()), gpxFile, errorMessage -> {
			if (errorMessage == null) {
				step2_markImportedIfNeeded();
			}
		});
	}

	public void step2_markImportedIfNeeded() {
		GpxDataItem item = new GpxDataItem(new KFile(gpxFile.getPath()));
		item.readGpxParams(gpxFile);
		item.setParameter(API_IMPORTED, navigationParams.isImportedByApi());
		app.getGpxDbHelper().add(item);
		step3_showGpxOnMap();
	}

	public void step3_showGpxOnMap() {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpx = helper.getSelectedFileByPath(gpxFile.getPath());
		if (selectedGpx != null) {
			selectedGpx.setGpxFile(gpxFile, app);
		} else {
			GpxSelectionParams selectionParams = GpxSelectionParams.getDefaultSelectionParams();
			helper.selectGpxFile(gpxFile, selectionParams);
		}
		step4_approximateGpxIfNeeded();
	}

	public void step4_approximateGpxIfNeeded() {
		if (navigationParams.isSnapToRoad()) {
			GpxApproximationParams approxParams = new GpxApproximationParams();
			approxParams.setAppMode(ApplicationMode.valueOfStringKey(navigationParams.getSnapToRoadMode(), null));
			approxParams.setDistanceThreshold(navigationParams.getSnapToRoadThreshold());
			GpxApproximationHelper.approximateGpxAsync(app, gpxFile, approxParams, approxGpx -> {
				step5_startNavigation(approxGpx);
				return true;
			});
		} else {
			step5_startNavigation(gpxFile);
		}
	}

	public void step5_startNavigation(@NonNull GpxFile gpxFile) {
		MapActivity mapActivity = mapActivityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			RoutingHelper routingHelper = app.getRoutingHelper();

			boolean passWholeRoute = navigationParams.isPassWholeRoute();
			boolean checkLocationPermission = navigationParams.isCheckLocationPermission();
			if (routingHelper.isFollowingMode() && !navigationParams.isForce()) {
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
	}

	public static void saveAndNavigateGpx(@NonNull MapActivity mapActivity, @NonNull GpxFile gpxFile, @NonNull GpxNavigationParams params) {
		new NavigateGpxHelper(mapActivity, gpxFile, params).step1_saveGpx();
	}

	public static boolean saveAndNavigateGpx(@NonNull MapActivity mapActivity, @Nullable String data,
	                                         @Nullable Uri uri, boolean force, boolean requestLocationPermission) {
		GpxFile gpxFile = loadGpxFile(mapActivity, data, uri);
		if (gpxFile != null && gpxFile.getError() == null) {
			GpxNavigationParams params = new GpxNavigationParams()
					.setForce(force)
					.setImportedByApi(true)
					.setCheckLocationPermission(requestLocationPermission);
			saveAndNavigateGpx(mapActivity, gpxFile, params);
			return true;
		}
		return false;
	}

	public static boolean saveAndNavigateGpx(@NonNull MapActivity mapActivity, @NonNull NavigateGpxParams params) {
		GpxFile gpxFile = loadGpxFile(mapActivity, params.getData(), params.getUri());
		if (gpxFile != null && gpxFile.getError() == null) {
			OsmandApplication app = mapActivity.getMyApplication();

			String fileName = params.getFileName();
			String updatedFileName = updateFileNameIfNeeded(app, gpxFile, fileName);
			if (!Algorithms.isEmpty(updatedFileName)) {
				params.setFileName(updatedFileName);
			}

			GpxNavigationParams navigationParams = new GpxNavigationParams()
					.setImportedByApi(true)
					.setForce(params.isForce())
					.setPassWholeRoute(params.isPassWholeRoute())
					.setCheckLocationPermission(params.isNeedLocationPermission())
					.setSnapToRoad(params.isSnapToRoad())
					.setSnapToRoadMode(params.getSnapToRoadMode())
					.setSnapToRoadThreshold(params.getSnapToRoadThreshold());

			saveAndNavigateGpx(mapActivity, gpxFile, navigationParams);
			return true;
		}
		return false;
	}

	public static void startNavigation(@NonNull MapActivity mapActivity, @NonNull GpxFile gpx,
	                                   boolean checkLocationPermission, boolean passWholeRoute) {
		startNavigation(mapActivity, gpx, null, null, null, null, null, checkLocationPermission, passWholeRoute);
	}

	public static void startNavigation(@NonNull MapActivity mapActivity, @NonNull ApplicationMode mode,
	                                   @Nullable LatLon from, @Nullable PointDescription fromDesc,
	                                   @Nullable LatLon to, @Nullable PointDescription toDesc,
	                                   boolean checkLocationPermission) {
		startNavigation(mapActivity, null, from, fromDesc, to, toDesc, mode, checkLocationPermission, false);
	}

	private static void startNavigation(@NonNull MapActivity mapActivity, @Nullable GpxFile gpx,
	                                    @Nullable LatLon from, @Nullable PointDescription fromDesc,
	                                    @Nullable LatLon to, @Nullable PointDescription toDesc,
	                                    @Nullable ApplicationMode mode,
	                                    boolean checkLocationPermission, boolean passWholeRoute) {
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
		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, from, fromDesc,
				true, false, passWholeRoute);
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
			AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
		}
		if (checkLocationPermission) {
			OsmAndLocationProvider.requestFineLocationPermissionIfNeeded(mapActivity);
		}
	}

	private static String updateFileNameIfNeeded(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile, @Nullable String preferredFileName) {
		if (Algorithms.isEmpty(gpxFile.getPath())) {
			String fileName = !Algorithms.isEmpty(preferredFileName) ? preferredFileName : DEFAULT_FILE_NAME;
			String fileNameWithExt = fileName + GPX_FILE_EXT;

			File destDir = app.getAppPath(GPX_IMPORT_DIR);
			File destFile = app.getAppPath(GPX_IMPORT_DIR + fileNameWithExt);
			while (destFile.exists()) {
				fileNameWithExt = AndroidUtils.createNewFileName(fileNameWithExt);
				destFile = new File(destDir, fileNameWithExt);
			}
			gpxFile.setPath(destFile.getAbsolutePath());
			return destFile.getName();
		}
		return null;
	}

	private static GpxFile loadGpxFile(@NonNull Context context, @Nullable String data, @Nullable Uri uri) {
		GpxFile gpx = null;
		if (!Algorithms.isEmpty(data)) {
			gpx = SharedUtil.loadGpxFile(new ByteArrayInputStream(data.getBytes()));
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (uri != null) {
				ParcelFileDescriptor gpxParcelDescriptor = null;
				try {
					ContentResolver contentResolver = context.getContentResolver();
					gpxParcelDescriptor = contentResolver.openFileDescriptor(uri, "r");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				if (gpxParcelDescriptor != null) {
					FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
					gpx = SharedUtil.loadGpxFile(new FileInputStream(fileDescriptor));
				}
			}
		}
		return gpx;
	}
}