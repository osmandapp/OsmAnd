package net.osmand.plus.activities;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import net.osmand.Location;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.dashboard.*;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.sherpafy.TourViewActivity;
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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.osmand.plus.views.controls.FloatingActionButton;

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

	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	private static final int HELP_ID = 0;
	private static final int SETTINGS_ID = 1;
	private static final int EXIT_ID = 2;
	private OsmAndLocationProvider lp;


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
		lp = getMyApplication().getLocationProvider();

		String textVersion = Version.getFullVersion(getMyApplication());
		if (textVersion.indexOf("#") != -1) {
			textVersion = textVersion.substring(0, textVersion.indexOf("#") + 1);
		}
		getSupportActionBar().setTitle(textVersion);
		ColorDrawable color = new ColorDrawable(getResources().getColor(R.color.actionbar_color));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);

		boolean firstTime = initApp(this, getMyApplication());
		if (getMyApplication().getAppCustomization().checkExceptionsOnStart()) {
			checkPreviousRunsForExceptions(firstTime);
		}
		setupContributionVersion();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			final FloatingActionButton fabButton = new FloatingActionButton.Builder(this)
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

			final ScrollView mainScroll = (ScrollView) findViewById(R.id.main_scroll);
			if (mainScroll == null){
				return;
			}
			mainScroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
				private int previousScroll = 0;

				@Override
				public void onScrollChanged() {
					int scrollY = mainScroll.getScrollY();
					if (previousScroll == scrollY || mainScroll.getChildCount() == 0){
						return;
					}

					if (scrollY > previousScroll && previousScroll >= 0){
						if (!fabButton.isHidden() ){
							fabButton.hideFloatingActionButton();
						}
					} else {
						int layoutHeight = mainScroll.getChildAt(0).getMeasuredHeight();
						int scrollHeight = scrollY + mainScroll.getHeight();
						//scroll can actually be more than entire layout height
						if (fabButton.isHidden() && scrollHeight < layoutHeight){
							fabButton.showFloatingActionButton();
						}
					}
					previousScroll = scrollY;
				}

			});
		}
		getLocationProvider().addCompassListener(this);
		getLocationProvider().registerOrUnregisterCompassListener(true);
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

	protected void setupContributionVersion() {
		findViewById(R.id.credentials).setVisibility(View.VISIBLE);
//Copyright notes and links have been put on the 'About' screen
//		final TextView textVersionView = (TextView) findViewById(R.id.Copyright);
//		final Calendar inst = Calendar.getInstance();
//		inst.setTime(new Date());
//		final String textVersion = "\u00A9 OsmAnd " + inst.get(Calendar.YEAR);
//		textVersionView.setText(textVersion);
		final SharedPreferences prefs = getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
//		textVersionView.setOnClickListener(new OnClickListener(){
//			int i = 0;
//			@Override
//			public void onClick(View v) {
//				if(i++ > 8 && Version.isDeveloperVersion(getMyApplication())) {
//					prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
//					enableLink(DashboardActivity.this, textVersion, textVersionView);
//				}
//			}
//		});
		// only one commit should be with contribution version flag
//		 prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
//		if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(getMyApplication())) {
//			enableLink(this, textVersion, textVersionView);
//		}
		final TextView about = (TextView) findViewById(R.id.About);
		final String aboutString = getString(R.string.about_settings);
		SpannableString ss = new SpannableString(aboutString);
		ClickableSpan clickableSpan = new ClickableSpan() {
			@Override
			public void onClick(View textView) {
				showAboutDialog(MainMenuActivity.this, getMyApplication());
			}
		};
		ss.setSpan(clickableSpan, 0, aboutString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		about.setText(ss);
		about.setMovementMethod(LinkMovementMethod.getInstance());
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
		if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(app)) {
			//Next 7 lines produced bogus Edition dates in many situtations, let us try (see above) to use the BUILD_ID as delivered from builder
			//try {
			//PackageManager pm = activity.getPackageManager();
			//ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
			//Date date = new Date(new File(appInfo.sourceDir).lastModified());
			//edition = activity.getString(R.string.local_index_installed) + " : \t" + DateFormat.getDateFormat(app).format(date);
			//} catch (Exception e) {
			//}
			SpannableString content = new SpannableString(vt + version + "\n" +
					edition + "\n\n" +
					activity.getString(R.string.about_content));
			content.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					final Intent mapIntent = new Intent(activity, ContributionVersionActivity.class);
					activity.startActivityForResult(mapIntent, 0);
				}

			}, st, st + version.length(), 0);
			tv.setText(content);
		} else {
			tv.setText(vt + version + "\n" +
					edition + "\n\n" +
					activity.getString(R.string.about_content));
		}

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
		menu.add(0, HELP_ID, 0, R.string.tips_and_tricks).setIcon(R.drawable.ic_ac_help)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, SETTINGS_ID, 0, R.string.settings).setIcon(R.drawable.ic_ac_settings)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, EXIT_ID, 0, R.string.exit_Button).setIcon(R.drawable.ic_ac_close)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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

			if (f instanceof DashAudioVideoNotesFragment && !f.isDetached()) {
				((DashAudioVideoNotesFragment) f).setupNotes();
			}
		}
	}

	@Override
	public void updateCompassValue(float value) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashFavoritesFragment && !f.isDetached()) {
				((DashFavoritesFragment) f).updateCompassValue(value);
			}
		}
	}

	@Override
	public void updateLocation(Location location) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashFavoritesFragment && !f.isDetached()) {
				((DashFavoritesFragment) f).updateLocation(location);
			}
		}
	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
	}
}
