package net.osmand.plus.base;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;

public class FailSafeFunctions {

	private static boolean quitRouteRestoreDialog;
	private static final Log log = PlatformUtil.getLog(FailSafeFunctions.class);

	public static void restoreRoutingMode(MapActivity ma) {
		OsmandApplication app = ma.getMyApplication();
		OsmandSettings settings = app.getSettings();
		Handler uiHandler = new Handler();
		String gpxPath = settings.FOLLOW_THE_GPX_ROUTE.get();
		TargetPointsHelper targetPoints = app.getTargetPointsHelper();
		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (pointToNavigate == null && gpxPath == null) {
			notRestoreRoutingMode(ma, app);
		} else {
			quitRouteRestoreDialog = false;
			Runnable encapsulate = new Runnable() {
				int delay = 7;
				Runnable delayDisplay;

				@Override
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(ma);
					TextView tv = new TextView(ma);
					tv.setText(ma.getString(R.string.continue_follow_previous_route_auto, delay + ""));
					tv.setPadding(7, 5, 7, 5);
					builder.setView(tv);
					builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quitRouteRestoreDialog = true;
							restoreRoutingModeInner();

						}
					});
					builder.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quitRouteRestoreDialog = true;
							notRestoreRoutingMode(ma, app);
						}
					});
					AlertDialog dlg = builder.show();
					dlg.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							quitRouteRestoreDialog = true;
						}
					});
					dlg.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							quitRouteRestoreDialog = true;
						}
					});
					delayDisplay = new Runnable() {
						@Override
						public void run() {
							if (!quitRouteRestoreDialog) {
								delay--;
								tv.setText(ma.getString(R.string.continue_follow_previous_route_auto, delay + ""));
								if (delay <= 0) {
									try {
										if (dlg.isShowing() && !quitRouteRestoreDialog) {
											dlg.dismiss();
										}
										quitRouteRestoreDialog = true;
										restoreRoutingModeInner();
									} catch (Exception e) {
										// swalow view not attached exception
										log.error(e.getMessage() + "", e);
									}
								} else {
									uiHandler.postDelayed(delayDisplay, 1000);
								}
							}
						}
					};
					delayDisplay.run();
				}

				private void restoreRoutingModeInner() {
					@SuppressLint("StaticFieldLeak")
					AsyncTask<String, Void, GPXFile> task = new AsyncTask<String, Void, GPXFile>() {
						@Override
						protected GPXFile doInBackground(String... params) {
							if (gpxPath != null) {
								// Reverse also should be stored ?
								GPXFile f = GPXUtilities.loadGPXFile(new File(gpxPath));
								if (f.error != null) {
									return null;
								}
								return f;
							} else {
								return null;
							}
						}

						@Override
						protected void onPostExecute(GPXFile result) {
							GPXRouteParamsBuilder gpxRoute;
							if (result != null) {
								gpxRoute = new GPXRouteParamsBuilder(result, settings);
								if (settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
									gpxRoute.setCalculateOsmAndRouteParts(true);
								}
								if (settings.GPX_ROUTE_CALC.get()) {
									gpxRoute.setCalculateOsmAndRoute(true);
								}
								int segmentIndex = settings.GPX_SEGMENT_INDEX.get();
								if (segmentIndex != -1) {
									gpxRoute.setSelectedSegment(segmentIndex);
								}
								int routeIndex = settings.GPX_ROUTE_INDEX.get();
								if (routeIndex != -1) {
									gpxRoute.setSelectedRoute(routeIndex);
								}
							} else {
								gpxRoute = null;
							}
							TargetPoint endPoint = pointToNavigate;
							if (endPoint == null) {
								notRestoreRoutingMode(ma, app);
							} else {
								enterRoutingMode(ma, gpxRoute);
							}
						}
					};
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxPath);

				}
			};
			encapsulate.run();
		}
	}
	
	public static void enterRoutingMode(@NonNull MapActivity mapActivity, @Nullable GPXRouteParamsBuilder gpxRoute) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.logRoutingEvent("enterRoutingMode gpxRoute " + gpxRoute);

		mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
		RoutingHelper routingHelper = app.getRoutingHelper();
		app.getSettings().FOLLOW_THE_GPX_ROUTE.set(gpxRoute != null ? gpxRoute.getFile().path : null);

		routingHelper.setGpxParams(gpxRoute);
		if (app.getTargetPointsHelper().getPointToStart() == null) {
			app.getTargetPointsHelper().setStartPoint(null, false, null);
		}
		app.getSettings().FOLLOW_THE_ROUTE.set(true);
		routingHelper.setFollowingMode(true);
		app.getTargetPointsHelper().updateRouteAndRefresh(true);
		app.initVoiceCommandPlayer(mapActivity, routingHelper.getAppMode(), null,
				true, false, false, false);
		if (mapActivity.getDashboard().isVisible()) {
			mapActivity.getDashboard().hideDashboard();
		}
	}

	private static void notRestoreRoutingMode(MapActivity ma, OsmandApplication app) {
		ma.updateApplicationModeSettings();
		app.getRoutingHelper().clearCurrentRoute(null, new ArrayList<>());
		app.getTargetPointsHelper().removeAllWayPoints(false, false);
		ma.refreshMap();
	}

	public static void quitRouteRestoreDialog() {
		quitRouteRestoreDialog = true;
	}
}
