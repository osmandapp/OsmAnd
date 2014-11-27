package net.osmand.plus.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.*;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.download.*;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.util.MapUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Denis on 05.11.2014.
 */
public class DashboardActivity extends BaseDownloadActivity {

	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String TIPS_SHOW = "TIPS_SHOW"; //$NON-NLS-1$

	public static final boolean TIPS_AND_TRICKS = false;

	private ProgressDialog startProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dashboard);

		final String textVersion = getString(R.string.app_name_ver);
		getSupportActionBar().setTitle(textVersion);
		final int abTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
		final SharedPreferences prefs = getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
		findViewById(abTitleId).setOnClickListener(new View.OnClickListener() {
			int i=0;
			@Override
			public void onClick(View view) {
				if(i++ > 8) {
					prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
					enableLink(DashboardActivity.this, textVersion, (TextView)view);
				}
			}
		});
		ColorDrawable color = new ColorDrawable(Color.parseColor("#ff8f00"));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);

		addFragments();

		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		final Activity activity = this;
		OsmandApplication app = getMyApplication();
		// restore follow route mode
		if(app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()){
			final Intent mapIndent = new Intent(this, appCustomization.getMapActivity());
			startActivityForResult(mapIndent, 0);
			return;
		}
		startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
		boolean dialogShown = false;
		boolean firstTime = false;
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
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
				applicationInstalledFirstTime();
				dialogShown = true;
			} else {
				int i = pref.getInt(TIPS_SHOW, 0);
				if (i < 7) {
					pref.edit().putInt(TIPS_SHOW, ++i).commit();
				}
				if (i == 1 || i == 5 || appVersionChanged) {
					if(TIPS_AND_TRICKS) {
						TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(this);
						Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
						dlg.show();
						dialogShown = true;
					} else {
						if(appVersionChanged) {
							final Intent helpIntent = new Intent(activity, HelpActivity.class);
							helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion(getMyApplication()));
							helpIntent.putExtra(HelpActivity.URL, "changes-1.9.html");
							activity.startActivity(helpIntent);
							dialogShown = true;
						}
					}
				}
			}
		}
		if(!dialogShown && appCustomization.checkBasemapDownloadedOnStart()) {
			if (startProgressDialog.isShowing()) {
				startProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						checkVectorIndexesDownloaded();
					}
				});
			} else {
				checkVectorIndexesDownloaded();
			}
		}
		if(appCustomization.checkExceptionsOnStart() && !dialogShown){
			checkPreviousRunsForExceptions(firstTime);
		}

	}

	private void addFragments() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction fragmentTransaction = manager.beginTransaction();
		//after rotation list of fragments in fragment transaction is not cleared
		//so we need to check whether some fragments are already existing
		if (manager.findFragmentByTag(DashSearchFragment.TAG) == null){
			DashSearchFragment searchFragment = new DashSearchFragment();
			fragmentTransaction.add(R.id.content, searchFragment, DashSearchFragment.TAG);
		}
		if (manager.findFragmentByTag(DashMapFragment.TAG) == null){
			DashMapFragment mapFragment = new DashMapFragment();
			fragmentTransaction.add(R.id.content, mapFragment, DashMapFragment.TAG);
		}

		if (manager.findFragmentByTag(DashFavoritesFragment.TAG) == null){
			DashFavoritesFragment favoritesFragment = new DashFavoritesFragment();
			fragmentTransaction.add(R.id.content, favoritesFragment, DashFavoritesFragment.TAG);
		}

		if (manager.findFragmentByTag(DashUpdatesFragment.TAG) == null){
			DashUpdatesFragment updatesFragment = new DashUpdatesFragment();
			fragmentTransaction.add(R.id.content, updatesFragment, DashUpdatesFragment.TAG);
		}

		if (manager.findFragmentByTag(DashPluginsFragment.TAG) == null){
			DashPluginsFragment pluginsFragment = new DashPluginsFragment();
			fragmentTransaction.add(R.id.content, pluginsFragment, DashPluginsFragment.TAG).commit();
		}
	}

	private static void enableLink(final Activity activity, String textVersion, TextView textVersionView) {
		SpannableString content = new SpannableString(textVersion);
		content.setSpan(new ClickableSpan() {

			@Override
			public void onClick(View widget) {
				final Intent mapIntent = new Intent(activity, ContributionVersionActivity.class);
				activity.startActivityForResult(mapIntent, 0);
			}
		}, 0, content.length(), 0);
		textVersionView.setText(content);
		textVersionView.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.close).setIcon(R.drawable.ic_ac_help)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 1, 0, R.string.settings).setIcon(R.drawable.ic_ac_settings)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 2, 0, R.string.exit_Button).setIcon(R.drawable.ic_ac_close)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		if (item.getItemId() == 0) {
			if(TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == 1){
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == 2){
			Intent newIntent = new Intent(this, appCustomization.getMainMenuActivity());
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(MainMenuActivity.APP_EXIT_KEY, MainMenuActivity.APP_EXIT_CODE);
			startActivity(newIntent);
		}
		return true;
	}

	@Override
	public void updateDownloadList(List<IndexItem> list){
		for(WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if(f instanceof DashUpdatesFragment) {
				if(!f.isDetached()) {
					((DashUpdatesFragment) f).updatedDownloadsList(list);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		downloadListIndexThread.resetUiActivity(DashboardActivity.class);
	}

	@Override
	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = BaseDownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		for(WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if(f instanceof DashUpdatesFragment) {
				if(!f.isDetached()) {
					((DashUpdatesFragment) f).updateProgress(basicProgressAsyncTask, updateOnlyProgress);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void checkPreviousRunsForExceptions(boolean firstTime) {
		long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final OsmandApplication app = ((OsmandApplication) getApplication());
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
				AlertDialog.Builder builder = new AccessibleAlertBuilder(DashboardActivity.this);
				builder.setMessage(msg).setNeutralButton(getString(R.string.close), null);
				builder.setPositiveButton(R.string.send_report, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "osmand.app+crash@gmail.com" }); //$NON-NLS-1$
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
						intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
						intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug"); //$NON-NLS-1$
						StringBuilder text = new StringBuilder();
						text.append("\nDevice : ").append(Build.DEVICE); //$NON-NLS-1$
						text.append("\nBrand : ").append(Build.BRAND); //$NON-NLS-1$
						text.append("\nModel : ").append(Build.MODEL); //$NON-NLS-1$
						text.append("\nProduct : ").append(Build.PRODUCT); //$NON-NLS-1$
						text.append("\nBuild : ").append(Build.DISPLAY); //$NON-NLS-1$
						text.append("\nVersion : ").append(Build.VERSION.RELEASE); //$NON-NLS-1$
						text.append("\nApp Version : ").append(Version.getAppName(app)); //$NON-NLS-1$
						try {
							PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
							if (info != null) {
								text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
							}
						} catch (PackageManager.NameNotFoundException e) {
						}
						intent.putExtra(Intent.EXTRA_TEXT, text.toString());
						startActivity(Intent.createChooser(intent, getString(R.string.send_report)));
					}

				});
				builder.show();
			}
			getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
		} else {
			if (size > 0) {
				getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
		}
	}

	private void applicationInstalledFirstTime() {
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null && !Version.isFreeVersion(getMyApplication());
		} catch (PackageManager.NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}

		if(netOsmandWasInstalled){
//			Builder builder = new AccessibleAlertBuilder(this);
//			builder.setMessage(R.string.osmand_net_previously_installed);
//			builder.setPositiveButton(R.string.default_buttons_ok, null);
//			builder.show();
		} else {
			AlertDialog.Builder builder = new AccessibleAlertBuilder(this);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(DashboardActivity.this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
				}

			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
	}

	protected void checkVectorIndexesDownloaded() {
		MapRenderRepositories maps = getMyApplication().getResourceManager().getRenderer();
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		boolean check = pref.getBoolean(VECTOR_INDEXES_CHECK, true);
		// do not show each time
		if (check && new Random().nextInt() % 5 == 1) {
			AlertDialog.Builder builder = new AccessibleAlertBuilder(this);
			if(maps.isEmpty()){
				builder.setMessage(R.string.vector_data_missing);
			} else if(!maps.basemapExists()){
				builder.setMessage(R.string.basemap_missing);
			} else {
				return;
			}
			builder.setPositiveButton(R.string.download_files, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(DashboardActivity.this, DownloadActivity.class));
				}

			});
			builder.setNeutralButton(R.string.vector_map_not_needed, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getPreferences(MODE_WORLD_WRITEABLE).edit().putBoolean(VECTOR_INDEXES_CHECK, false).commit();
				}
			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH
				&& event.getRepeatCount() == 0) {
			final Intent search = new Intent(DashboardActivity.this, SearchActivity.class);
			search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(search);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
