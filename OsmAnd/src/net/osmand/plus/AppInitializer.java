package net.osmand.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TipsAndTricksActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.render.MapRenderRepositories;

import java.io.File;
import java.util.Random;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class AppInitializer {

	public static final boolean TIPS_AND_TRICKS = false;
	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String TIPS_SHOW = "TIPS_SHOW"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$

	public static final String LATEST_CHANGES_URL = "changes-1.9.html";
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";


	public boolean initApp(final Activity activity, OsmandApplication app) {
		final OsmAndAppCustomization appCustomization = app.getAppCustomization();
		// restore follow route mode
		if (app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()) {
			if(!(activity instanceof MapActivity)) {
				startMapActivity(activity);
			}
			return false;
		}


		boolean firstTime = false;
		SharedPreferences pref = activity.getPreferences(Context.MODE_WORLD_WRITEABLE);
		boolean appVersionChanged = false;
		if (!pref.contains(FIRST_TIME_APP_RUN)) {
			firstTime = true;
			pref.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
		} else if (!Version.getFullVersion(app).equals(pref.getString(VERSION_INSTALLED, ""))) {
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			appVersionChanged = true;
		}
		if (appCustomization.showFirstTimeRunAndTips(firstTime, appVersionChanged)) {
			if (firstTime) {
				applicationInstalledFirstTime(activity);
			} else {
				int i = pref.getInt(TIPS_SHOW, 0);
				if (i < 7) {
					pref.edit().putInt(TIPS_SHOW, ++i).commit();
				}
				if (i == 1 || i == 5 || appVersionChanged) {
					if (TIPS_AND_TRICKS) {
						TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(activity);
						Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
						dlg.show();
					} else {
						if (appVersionChanged) {
							final Intent helpIntent = new Intent(activity, HelpActivity.class);
							helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion((OsmandApplication)activity.getApplication()));
							helpIntent.putExtra(HelpActivity.URL, LATEST_CHANGES_URL);
							activity.startActivity(helpIntent);
						}
					}
				}
			}
		}

		return firstTime;
	}

	public boolean checkPreviousRunsForExceptions(Activity ctx,boolean firstTime) {
		long size = ctx.getPreferences(Context.MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final OsmandApplication app = ((OsmandApplication) ctx.getApplication());
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				return true;
			}
			ctx.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
		} else {
			if (size > 0) {
				ctx.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
		}
		return false;
	}

	public void checkVectorIndexesDownloaded(final Activity ctx) {
		OsmandApplication app = (OsmandApplication)ctx.getApplication();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		SharedPreferences pref = ctx.getPreferences(Context.MODE_WORLD_WRITEABLE);
		boolean check = pref.getBoolean(VECTOR_INDEXES_CHECK, true);
		// do not show each time
		if (check && new Random().nextInt() % 5 == 1) {
			AlertDialog.Builder builder = new AccessibleAlertBuilder(ctx);
			if (maps.isEmpty()) {
				builder.setMessage(R.string.vector_data_missing);
			} else if (!maps.basemapExists()) {
				builder.setMessage(R.string.basemap_missing);
			} else {
				return;
			}
			builder.setPositiveButton(R.string.download_files, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					ctx.startActivity(new Intent(ctx, DownloadActivity.class));
				}

			});
			builder.setNeutralButton(R.string.vector_map_not_needed, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ctx.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putBoolean(VECTOR_INDEXES_CHECK, false).commit();
				}
			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}

	}

	public void startMapActivity(final Activity ctx) {
		final Intent mapIndent = new Intent(ctx, ((OsmandApplication)ctx.getApplication()).getAppCustomization().getMapActivity());
		ctx.startActivityForResult(mapIndent, 0);
	}

	private void applicationInstalledFirstTime(final Activity ctx) {
		final OsmandApplication app = (OsmandApplication)ctx.getApplication();
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = ctx.getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null && !Version.isFreeVersion(app);
		} catch (PackageManager.NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}

		if (netOsmandWasInstalled) {
//			Builder builder = new AccessibleAlertBuilder(this);
//			builder.setMessage(R.string.osmand_net_previously_installed);
//			builder.setPositiveButton(R.string.shared_string_ok, null);
//			builder.show();
		} else {
			AlertDialog.Builder builder = new AccessibleAlertBuilder(ctx);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					ctx.startActivity(new Intent(ctx, app.getAppCustomization().getDownloadIndexActivity()));
				}

			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
	}



}
