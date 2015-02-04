package net.osmand.plus.activities;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import net.osmand.Location;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.dashboard.DashDownloadMapsFragment;
import net.osmand.plus.dashboard.DashErrorFragment;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashUpdatesFragment;
import net.osmand.plus.dashboard.NotifyingScrollView;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.sherpafy.TourViewActivity;
import net.osmand.plus.views.controls.FloatingActionButton;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 */
public class MainMenuActivity extends BaseDownloadActivity implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {
	private static final String LATEST_CHANGES_URL = "changes-1.9.html";
	public static final boolean TIPS_AND_TRICKS = false;
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";

	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String TIPS_SHOW = "TIPS_SHOW"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$

	private static final int HELP_ID = 0;
	private static final int SETTINGS_ID = 1;
	private static final int EXIT_ID = 2;

	private static final int START_ALPHA = 60;

	int defaultMargin;

	private Drawable actionBarBackground;
	FloatingActionButton fabButton;

	private NotifyingScrollView.OnScrollChangedListener onScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
		public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
			//making background of actionbar transparent with scroll
			final int imageHeight = findViewById(R.id.map_image).getMeasuredHeight();
			final int headerHeight = imageHeight - getSupportActionBar().getHeight();
			final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
			final int newAlpha = (int) (ratio * 255);
			int margintop = -(int)(ratio * 60);
			Resources r = getResources();
			int px = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP,
					margintop,
					r.getDisplayMetrics());
			int margin = px + defaultMargin;
			if (headerHeight < t - margin){
				//hiding action bar - showing floating button
				//getSupportActionBar().hide();
				if (fabButton != null) {
					fabButton.showFloatingActionButton();
				}
			} else {
				//getSupportActionBar().show();
				if (fabButton != null) {
					fabButton.hideFloatingActionButton();
				}

				//makes other cards to move on top of the map card to make it look like android animations
				View fragments = findViewById(R.id.fragments);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

				params.setMargins(0, margin, 0, 0);
				fragments.setLayoutParams(params);
			}
			if (newAlpha > START_ALPHA) {
				actionBarBackground.setAlpha(newAlpha);
			}

		}
	};

	private Drawable.Callback mDrawableCallback = new Drawable.Callback() {
		@Override
		public void invalidateDrawable(Drawable who) {
			getSupportActionBar().setBackgroundDrawable(who);
		}

		@Override
		public void scheduleDrawable(Drawable who, Runnable what, long when) {
		}

		@Override
		public void unscheduleDrawable(Drawable who, Runnable what) {
		}
	};

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getLocationProvider().pauseAllUpdates();
		getMyApplication().getLocationProvider().removeLocationListener(this);
		getMyApplication().getLocationProvider().removeCompassListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		haveHomeButton = false;
		if (getIntent() != null) {
			Intent intent = getIntent();
			if (intent.getExtras() != null && intent.getExtras().containsKey(APP_EXIT_KEY)) {
				getMyApplication().closeApplication(this);
				return;
			}
		}
		if (Version.isSherpafy(getMyApplication())) {
			final Intent mapIntent = new Intent(this, TourViewActivity.class);
			startActivity(mapIntent);
			finish();
			return;
		}
		setContentView(R.layout.dashboard);

		String textVersion = Version.getFullVersion(getMyApplication());
		if (textVersion.contains("#")) {
			textVersion = textVersion.substring(0, textVersion.indexOf("#") + 1);
		}
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(textVersion);
		actionBar.setIcon(android.R.color.transparent);
		actionBarBackground = new ColorDrawable(Color.argb(180, 0, 0, 0));
		actionBarBackground.setAlpha(START_ALPHA);
		actionBar.setBackgroundDrawable(actionBarBackground);
		((NotifyingScrollView)findViewById(R.id.main_scroll)).setOnScrollChangedListener(onScrollChangedListener);
		//setting up callback for drawable on actionbar for old android
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			actionBarBackground.setCallback(mDrawableCallback);
		}

		boolean firstTime = initApp(this, getMyApplication());
		if (getMyApplication().getAppCustomization().checkExceptionsOnStart()) {
			checkPreviousRunsForExceptions(firstTime);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			fabButton = new FloatingActionButton.Builder(this)
					.withDrawable(getResources().getDrawable(R.drawable.ic_action_map))
					.withButtonColor(Color.parseColor("#ff8f00"))
					.withGravity(Gravity.BOTTOM | Gravity.RIGHT)
					.withMargins(0, 0, 16, 16)
					.create();
			fabButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					startMapActivity();
				}
			});
			fabButton.hideFloatingActionButton();
		}

		getLocationProvider().addCompassListener(this);
		getLocationProvider().registerOrUnregisterCompassListener(true);

		defaultMargin = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				-40,
				getResources().getDisplayMetrics());
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getMyApplication().getFavorites().getFavouritePoints().size() > 0) {
		//	getLocationProvider().addLocationListener(this);
			getLocationProvider().addCompassListener(this);
			getLocationProvider().registerOrUnregisterCompassListener(true);
		//	getLocationProvider().resumeAllUpdates();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == APP_EXIT_CODE) {
			getMyApplication().closeApplication(this);
		}
	}

	private void addErrorFragment() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction fragmentTransaction = manager.beginTransaction();
		if (manager.findFragmentByTag(DashErrorFragment.TAG) == null) {
			DashErrorFragment errorFragment = new DashErrorFragment();
			fragmentTransaction.add(R.id.content, errorFragment, DashErrorFragment.TAG).commit();
		}
	}

	public static void showAboutDialog(final Activity activity, final OsmandApplication app) {
		Builder bld = new AlertDialog.Builder(activity);
		bld.setTitle(R.string.about_settings);
		ScrollView sv = new ScrollView(activity);
		TextView tv = new TextView(activity);
		sv.addView(tv);
		String version = Version.getFullVersion(app);
		String vt = activity.getString(R.string.about_version) + "\t";
		int st = vt.length();
		String edition = "";
		if (!activity.getString(R.string.app_edition).equals("")) {
			edition = activity.getString(R.string.local_index_installed) + " : \t" + activity.getString(R.string.app_edition);
		}
		SharedPreferences prefs = app.getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);

		tv.setText(vt + version + "\n" +
				edition + "\n\n" +
				activity.getString(R.string.about_content));

		tv.setPadding(5, 0, 5, 5);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		bld.setView(sv);
		bld.setPositiveButton(R.string.default_buttons_ok, null);
		bld.show();

	}


	protected boolean initApp(final Activity activity, OsmandApplication app) {
		final OsmAndAppCustomization appCustomization = app.getAppCustomization();
		// restore follow route mode
		if (app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()) {
			startMapActivity();
			return false;
		}
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
			} else {
				int i = pref.getInt(TIPS_SHOW, 0);
				if (i < 7) {
					pref.edit().putInt(TIPS_SHOW, ++i).commit();
				}
				if (i == 1 || i == 5 || appVersionChanged) {
					if (TIPS_AND_TRICKS) {
						TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(this);
						Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
						dlg.show();
					} else {
						if (appVersionChanged) {
							final Intent helpIntent = new Intent(activity, HelpActivity.class);
							helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion(getMyApplication()));
							helpIntent.putExtra(HelpActivity.URL, LATEST_CHANGES_URL);
							activity.startActivity(helpIntent);
						}
					}
				}
			}
		}

		return firstTime;
	}

	private void startMapActivity() {
		final Intent mapIndent = new Intent(this, getMyApplication().getAppCustomization().getMapActivity());
		startActivityForResult(mapIndent, 0);
	}

	private void applicationInstalledFirstTime() {
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null && !Version.isFreeVersion(getMyApplication());
		} catch (NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}

		if (netOsmandWasInstalled) {
//			Builder builder = new AccessibleAlertBuilder(this);
//			builder.setMessage(R.string.osmand_net_previously_installed);
//			builder.setPositiveButton(R.string.default_buttons_ok, null);
//			builder.show();
		} else {
			Builder builder = new AccessibleAlertBuilder(this);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(MainMenuActivity.this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
				}

			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
	}

	public void checkPreviousRunsForExceptions(boolean firstTime) {
		long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final OsmandApplication app = ((OsmandApplication) getApplication());
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				addErrorFragment();
			}
			getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
		} else {
			if (size > 0) {
				getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add(0, HELP_ID, 0, R.string.tips_and_tricks).setIcon(R.drawable.ic_ac_help);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menu.add(0, SETTINGS_ID, 0, R.string.settings).setIcon(R.drawable.ic_ac_settings);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menu.add(0, EXIT_ID, 0, R.string.exit_Button).setIcon(R.drawable.ic_ac_close);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		if (item.getItemId() == HELP_ID) {
			if (TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == SETTINGS_ID) {
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == EXIT_ID) {
			getMyApplication().closeApplication(this);
		}
		return true;
	}

	protected void checkVectorIndexesDownloaded() {
		MapRenderRepositories maps = getMyApplication().getResourceManager().getRenderer();
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		boolean check = pref.getBoolean(VECTOR_INDEXES_CHECK, true);
		// do not show each time 
		if (check && new Random().nextInt() % 5 == 1) {
			Builder builder = new AccessibleAlertBuilder(this);
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
					startActivity(new Intent(MainMenuActivity.this, DownloadActivity.class));
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

//	private static void enableLink(final Activity activity, String textVersion, TextView textVersionView) {
//		SpannableString content = new SpannableString(textVersion);
//		content.setSpan(new ClickableSpan() {
//
//			@Override
//			public void onClick(View widget) {
//				final Intent mapIntent = new Intent(activity, ContributionVersionActivity.class);
//				activity.startActivityForResult(mapIntent, 0);
//			}
//		}, 0, content.length(), 0);
//		textVersionView.setText(content);
//		textVersionView.setMovementMethod(LinkMovementMethod.getInstance());
//	}

	@Override
	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = BaseDownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashUpdatesFragment && !f.isDetached()) {
				((DashUpdatesFragment) f).updateProgress(basicProgressAsyncTask, updateOnlyProgress);
			}
		}
	}

	@Override
	public void updateDownloadList(List<IndexItem> list) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashUpdatesFragment && !f.isDetached()) {
				if (downloadQueue.size() > 0) {
					startDownload(downloadQueue.get(0));
					downloadQueue.remove(0);
				}
				((DashUpdatesFragment) f).updatedDownloadsList(list);

			}
			if (f instanceof DashDownloadMapsFragment && !f.isDetached()) {
				((DashDownloadMapsFragment) f).refreshData();
			}
		}
	}

	@Override
	public void updateCompassValue(float value) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashLocationFragment && !f.isDetached()) {
				((DashLocationFragment) f).updateCompassValue(value);
			}
		}
	}

	@Override
	public void updateLocation(Location location) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashLocationFragment && !f.isDetached()) {
				((DashLocationFragment) f).updateLocation(location);
			}
		}
	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
	}
}
