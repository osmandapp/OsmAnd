package net.osmand.plus.base;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.TextView;

public class FailSafeFuntions {
	private static boolean quitRouteRestoreDialog = false;
	private static Log log = PlatformUtil.getLog(FailSafeFuntions.class);
	
	public static void restoreRoutingMode(final MapActivity ma) {
		final OsmandApplication app = ma.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final Handler uiHandler = new Handler();
		final String gpxPath = settings.FOLLOW_THE_GPX_ROUTE.get();
		final TargetPointsHelper targetPoints = app.getTargetPointsHelper();
		final LatLon pointToNavigate = targetPoints.getPointToNavigate();
		if (pointToNavigate == null && gpxPath == null) {
			notRestoreRoutingMode(ma, app);
		} else {
			quitRouteRestoreDialog = false;
			Runnable encapsulate = new Runnable() {
				int delay = 7;
				Runnable delayDisplay = null;

				@Override
				public void run() {
					Builder builder = new AccessibleAlertBuilder(ma);
					final TextView tv = new TextView(ma);
					tv.setText(ma.getString(R.string.continue_follow_previous_route_auto, delay + ""));
					tv.setPadding(7, 5, 7, 5);
					builder.setView(tv);
					builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quitRouteRestoreDialog = true;
							restoreRoutingModeInner();

						}
					});
					builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quitRouteRestoreDialog = true;
							notRestoreRoutingMode(ma, app);
						}
					});
					final AlertDialog dlg = builder.show();
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
							if(!quitRouteRestoreDialog) {
								delay --;
								tv.setText(ma.getString(R.string.continue_follow_previous_route_auto, delay + ""));
								if(delay <= 0) {
									try {
										if (dlg.isShowing() && !quitRouteRestoreDialog) {
											dlg.dismiss();
										}
										quitRouteRestoreDialog = true;
										restoreRoutingModeInner();
									} catch(Exception e) {
										// swalow view not attached exception
										log.error(e.getMessage()+"", e);
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
					AsyncTask<String, Void, GPXFile> task = new AsyncTask<String, Void, GPXFile>() {
						@Override
						protected GPXFile doInBackground(String... params) {
							if (gpxPath != null) {
								// Reverse also should be stored ?
								GPXFile f = GPXUtilities.loadGPXFile(app, new File(gpxPath), false);
								if (f.warning != null) {
									return null;
								}
								return f;
							} else {
								return null;
							}
						}

						@Override
						protected void onPostExecute(GPXFile result) {
							final GPXRouteParams gpxRoute = result == null ? null : new GPXRouteParams(result, false,
									settings.SPEAK_GPX_WPT.get(), settings);
							LatLon endPoint = pointToNavigate != null ? pointToNavigate : gpxRoute.getLastPoint();
							net.osmand.Location startPoint = gpxRoute == null ? null : gpxRoute.getStartPointForRoute();
							if (endPoint == null) {
								notRestoreRoutingMode(ma, app);
							} else {
								ma.followRoute(settings.getApplicationMode(), endPoint, targetPoints.getIntermediatePoints(), startPoint, gpxRoute);
							}
						}
					};
					task.execute(gpxPath);

				}
			};
			encapsulate.run();
		}

	}
	
	private static void notRestoreRoutingMode(MapActivity ma, OsmandApplication app){
		ma.updateApplicationModeSettings();
		app.getRoutingHelper().clearCurrentRoute(null, new ArrayList<LatLon>());
		ma.refreshMap();
	}

	public static void quitRouteRestoreDialog() {
		quitRouteRestoreDialog = true;
	}
}
