package net.osmand.plus.helpers;

import static net.osmand.plus.settings.enums.TrackApproximationType.AUTOMATIC;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.measurementtool.GpxApproximationHelper;
import net.osmand.plus.measurementtool.GpxApproximationParams;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;

public class RestoreNavigationHelper {

	private static final Log log = PlatformUtil.getLog(RestoreNavigationHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final TargetPointsHelper targetPointsHelper;

	private final MapActivity mapActivity;
	private boolean quitRouteRestoreDialog;

	public RestoreNavigationHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.routingHelper = app.getRoutingHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
	}

	public void checkRestoreRoutingMode() {
		// This situation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if (settings.FOLLOW_THE_ROUTE.get() && !routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
			restoreRoutingMode();
		}
	}

	public void restoreRoutingMode() {
		String gpxPath = settings.FOLLOW_THE_GPX_ROUTE.get();
		TargetPoint pointToNavigate = targetPointsHelper.getPointToNavigate();
		if (pointToNavigate == null && gpxPath == null) {
			notRestoreRoutingMode();
		} else {
			quitRouteRestoreDialog = false;
			if (settings.SHOW_RESTART_NAVIGATION_DIALOG.get()) {
				showRestoreNavigationDialog(pointToNavigate, gpxPath);
			} else {
				restoreRoutingModeInner(pointToNavigate, gpxPath);
			}
		}
	}

	private void showRestoreNavigationDialog(@Nullable TargetPoint pointToNavigate, @Nullable String gpxPath) {
		Handler uiHandler = new Handler();
		Runnable encapsulate = new Runnable() {
			int delay = 7;
			Runnable delayDisplay;

			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
				TextView textView = new TextView(mapActivity);
				textView.setText(mapActivity.getString(R.string.continue_follow_previous_route_auto, String.valueOf(delay)));
				textView.setPadding(7, 5, 7, 5);
				builder.setView(textView);
				builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
					quitRouteRestoreDialog = true;
					restoreRoutingModeInner(pointToNavigate, gpxPath);
				});
				builder.setNegativeButton(R.string.shared_string_no, (dialog, which) -> {
					quitRouteRestoreDialog = true;
					notRestoreRoutingMode();
				});
				AlertDialog dialog = builder.show();
				dialog.setOnDismissListener(d -> quitRouteRestoreDialog = true);
				dialog.setOnCancelListener(d -> quitRouteRestoreDialog = true);

				delayDisplay = () -> {
					if (!quitRouteRestoreDialog) {
						delay--;
						textView.setText(app.getString(R.string.continue_follow_previous_route_auto, String.valueOf(delay)));
						if (delay <= 0) {
							try {
								if (dialog.isShowing() && !quitRouteRestoreDialog) {
									dialog.dismiss();
								}
								quitRouteRestoreDialog = true;
								restoreRoutingModeInner(pointToNavigate, gpxPath);
							} catch (Exception e) {
								// swalow view not attached exception
								log.error(e.getMessage(), e);
							}
						} else {
							uiHandler.postDelayed(delayDisplay, 1000);
						}
					}
				};
				delayDisplay.run();
			}
		};
		encapsulate.run();
	}

	@SuppressLint("StaticFieldLeak")
	private void restoreRoutingModeInner(@Nullable TargetPoint pointToNavigate, @Nullable String gpxPath) {
		AsyncTask<String, Void, GPXFile> task = new AsyncTask<>() {
			@Override
			protected GPXFile doInBackground(String... params) {
				if (gpxPath != null) {
					// Reverse also should be stored ?
					GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(gpxPath));
					return gpxFile.error == null ? gpxFile : null;
				}
				return null;
			}

			@Override
			protected void onPostExecute(@Nullable GPXFile gpxFile) {
				if (pointToNavigate == null) {
					notRestoreRoutingMode();
				} else {
					ApplicationMode appMode = routingHelper.getAppMode();
					if (gpxFile != null && !gpxFile.isAttachedToRoads()
							&& settings.DETAILED_TRACK_GUIDANCE.getModeValue(appMode) == AUTOMATIC) {
						GpxApproximationParams params = new GpxApproximationParams();
						params.setAppMode(appMode);
						params.setDistanceThreshold(settings.GPX_APPROXIMATION_DISTANCE.getModeValue(appMode));
						GpxApproximationHelper.approximateGpxAsync(app, gpxFile, params, approxGpx -> {
							enterRoutingMode(createGpxRouteParams(approxGpx));
							return true;
						});
					} else {
						enterRoutingMode(createGpxRouteParams(gpxFile));
					}
				}
			}

			@Nullable
			private GPXRouteParamsBuilder createGpxRouteParams(@Nullable GPXFile gpxFile) {
				GPXRouteParamsBuilder builder = null;
				if (gpxFile != null) {
					builder = new GPXRouteParamsBuilder(gpxFile, settings);
					if (settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
						builder.setCalculateOsmAndRouteParts(true);
					}
					if (settings.GPX_ROUTE_CALC.get()) {
						builder.setCalculateOsmAndRoute(true);
					}
					int segmentIndex = settings.GPX_SEGMENT_INDEX.get();
					if (segmentIndex != -1) {
						builder.setSelectedSegment(segmentIndex);
					}
					int routeIndex = settings.GPX_ROUTE_INDEX.get();
					if (routeIndex != -1) {
						builder.setSelectedRoute(routeIndex);
					}
				}
				return builder;
			}
		};
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxPath);
	}

	public void enterRoutingMode(@Nullable GPXRouteParamsBuilder gpxRoute) {
		app.logRoutingEvent("enterRoutingMode gpxRoute " + gpxRoute);

		mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
		settings.FOLLOW_THE_GPX_ROUTE.set(gpxRoute != null ? gpxRoute.getFile().path : null);

		routingHelper.setGpxParams(gpxRoute);
		if (targetPointsHelper.getPointToStart() == null) {
			targetPointsHelper.setStartPoint(null, false, null);
		}
		settings.FOLLOW_THE_ROUTE.set(true);
		routingHelper.setFollowingMode(true);
		targetPointsHelper.updateRouteAndRefresh(true);
		app.initVoiceCommandPlayer(mapActivity, routingHelper.getAppMode(), null,
				true, false, false, false);
		if (mapActivity.getDashboard().isVisible()) {
			mapActivity.getDashboard().hideDashboard();
		}
		AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
	}

	private void notRestoreRoutingMode() {
		mapActivity.updateApplicationModeSettings();
		routingHelper.clearCurrentRoute(null, new ArrayList<>());
		targetPointsHelper.removeAllWayPoints(false, false);
		mapActivity.refreshMap();
	}

	public void quitRouteRestoreDialog() {
		quitRouteRestoreDialog = true;
	}
}
