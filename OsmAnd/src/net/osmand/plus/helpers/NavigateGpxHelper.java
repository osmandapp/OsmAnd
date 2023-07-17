package net.osmand.plus.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.aidlapi.navigation.NavigateGpxParams;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
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
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
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
	private final GPXFile gpxFile;
	private final GpxNavigationParams navigationParams;


	public NavigateGpxHelper(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile,
	                         @NonNull GpxNavigationParams navigationParams) {
		this.mapActivityRef = new WeakReference<>(mapActivity);
		this.app = mapActivity.getMyApplication();
		this.gpxFile = gpxFile;
		this.navigationParams = navigationParams;
	}

	private void step1_saveGpx() {
		updateFileNameIfNeeded(app, gpxFile, DEFAULT_FILE_NAME);
		SaveGpxHelper.saveGpx(new File(gpxFile.path), gpxFile, errorMessage -> {
			if (errorMessage == null) {
				step2_markImportedIfNeeded();
			}
		});
	}

	public void step2_markImportedIfNeeded() {
		if (navigationParams.isImportedByApi()) {
			GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
			GpxDataItem item = gpxDbHelper.getItem(gpxFile.path, gpxItem -> {
				gpxItem.setImportedByApi(true);
				step3_showGpxOnMap();
			});
			if (item != null) {
				item.setImportedByApi(true);
				step3_showGpxOnMap();
			}
		} else {
			step3_showGpxOnMap();
		}
	}

	public void step3_showGpxOnMap() {
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
		step4_approximateGpxIfNeeded();
	}

	public void step4_approximateGpxIfNeeded() {
		if (navigationParams.isSnapToRoad()) {
			GpxApproximationParams approxParams = new GpxApproximationParams();
			approxParams.setAppMode(ApplicationMode.valueOfStringKey(navigationParams.getSnapToRoadMode(), null));
			approxParams.setDistanceThreshold(navigationParams.getSnapToRoadThreshold());
			GpxApproximationHelper.approximateGpxSilently(app, gpxFile, approxParams, approxGpx -> {
				step5_startNavigation(approxGpx);
				return true;
			});
		} else {
			step5_startNavigation(gpxFile);
		}
	}

	public void step5_startNavigation(@NonNull GPXFile gpxFile) {
		MapActivity mapActivity = mapActivityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			OsmandApplication app = mapActivity.getMyApplication();
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

	public static boolean saveAndNavigateGpx(@NonNull MapActivity mapActivity,
	                                         @Nullable String data, @Nullable Uri uri,
	                                         boolean force, boolean requestLocationPermission) {
		GPXFile gpxFile = loadGpxFile(mapActivity, data, uri);
		if (gpxFile != null) {
			saveAndNavigateGpx(mapActivity, gpxFile, new GpxNavigationParams()
					.setCheckLocationPermission(requestLocationPermission)
					.setImportedByApi(true)
					.setForce(force));
			return true;
		}
		return false;
	}

	public static boolean saveAndNavigateGpx(@NonNull MapActivity mapActivity,
	                                         @NonNull NavigateGpxParams params) {
		GPXFile gpxFile = loadGpxFile(mapActivity, params.getData(), params.getUri());
		if (gpxFile != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			String updatedFileName = updateFileNameIfNeeded(app, gpxFile, params.getFileName());
			if (!Algorithms.isEmpty(updatedFileName)) {
				params.setFileName(updatedFileName);
			}

			GpxNavigationParams navigationParams = new GpxNavigationParams()
					.setCheckLocationPermission(params.isNeedLocationPermission())
					.setPassWholeRoute(params.isPassWholeRoute())
					.setForce(params.isForce())
					.setSnapToRoad(params.isSnapToRoad())
					.setSnapToRoadMode(params.getSnapToRoadMode())
					.setSnapToRoadThreshold(params.getSnapToRoadThreshold())
					.setImportedByApi(true);

			saveAndNavigateGpx(mapActivity, gpxFile, navigationParams);
			return true;
		}
		return false;
	}

	public static void saveAndNavigateGpx(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile,
	                                      @NonNull GpxNavigationParams params) {
		new NavigateGpxHelper(mapActivity, gpxFile, params).step1_saveGpx();
	}

	public static void startNavigation(@NonNull MapActivity mapActivity, @NonNull GPXFile gpx,
	                                   boolean checkLocationPermission, boolean passWholeRoute) {
		startNavigation(mapActivity, gpx, null, null, null, null, null, checkLocationPermission, passWholeRoute);
	}

	public static void startNavigation(@NonNull MapActivity mapActivity,
	                                   @Nullable LatLon from, @Nullable PointDescription fromDesc,
	                                   @Nullable LatLon to, @Nullable PointDescription toDesc,
	                                   @NonNull ApplicationMode mode, boolean checkLocationPermission) {
		startNavigation(mapActivity, null, from, fromDesc, to, toDesc, mode, checkLocationPermission, false);
	}

	private static void startNavigation(@NonNull MapActivity mapActivity, @Nullable GPXFile gpx,
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

	private static String updateFileNameIfNeeded(@NonNull OsmandApplication app,
	                                             @NonNull GPXFile gpxFile,
	                                             @Nullable String preferredFileName) {
		if (Algorithms.isEmpty(gpxFile.path)) {
			String fileName = !Algorithms.isEmpty(preferredFileName) ? preferredFileName : DEFAULT_FILE_NAME;
			String fileNameWithExt = fileName + IndexConstants.GPX_FILE_EXT;

			File destDir = app.getAppPath(IndexConstants.GPX_IMPORT_DIR);
			File destFile = app.getAppPath(IndexConstants.GPX_IMPORT_DIR + fileNameWithExt);
			while (destFile.exists()) {
				fileNameWithExt = AndroidUtils.createNewFileName(fileNameWithExt);
				destFile = new File(destDir, fileNameWithExt);
			}
			gpxFile.path = destFile.getAbsolutePath();
			return fileName;
		}
		return null;
	}

	private static GPXFile loadGpxFile(@NonNull Context context, String data, Uri uri) {
		GPXFile gpx = null;
		if (!Algorithms.isEmpty(data)) {
			gpx = GPXUtilities.loadGPXFile(new ByteArrayInputStream(data.getBytes()));
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
					gpx = GPXUtilities.loadGPXFile(new FileInputStream(fileDescriptor));
				}
			}
		}
		return gpx;
	}

}
