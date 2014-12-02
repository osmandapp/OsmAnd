package net.osmand.plus.activities;


import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.support.v4.app.FragmentManager;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.dashboard.*;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.sherpafy.SherpafyLoadingFragment;
import net.osmand.plus.sherpafy.TourViewActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 */
public class DashboardActivity extends BaseDownloadActivity {
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		if(getIntent() != null){
			Intent intent = getIntent();
			if(intent.getExtras() != null && intent.getExtras().containsKey(APP_EXIT_KEY)){
				getMyApplication().closeApplication(this);
				return;
			}
		}
		if(Version.isSherpafy(getMyApplication())) {
			final Intent mapIntent = new Intent(this, TourViewActivity.class);
			startActivity(mapIntent);
			finish();
			return;
		}
		setContentView(R.layout.dashboard);
		
		final String textVersion = Version.getFullVersion(getMyApplication());
		getSupportActionBar().setTitle(textVersion);
		ColorDrawable color = new ColorDrawable(getResources().getColor(R.color.actionbar_color));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);
		
		initApp(this, getMyApplication());
		addFragments();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == APP_EXIT_CODE){
			getMyApplication().closeApplication(this);
		}
	}
	
	protected void setupContributionVersion() {
		findViewById(R.id.credentials).setVisibility(View.VISIBLE);
		final TextView textVersionView = (TextView) findViewById(R.id.Copyright);
		final Calendar inst = Calendar.getInstance();
		inst.setTime(new Date());
		final String textVersion = "\u00A9 OsmAnd " + inst.get(Calendar.YEAR);
		textVersionView.setText(textVersion);
		final SharedPreferences prefs = getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
		textVersionView.setOnClickListener(new OnClickListener(){

			int i = 0;
			@Override
			public void onClick(View v) {
				if(i++ > 8 && Version.isDeveloperVersion(getMyApplication())) {
					prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
					enableLink(DashboardActivity.this, textVersion, textVersionView);
				}
			}
		});
		// only one commit should be with contribution version flag
//		 prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
		if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(getMyApplication())) {
			enableLink(this, textVersion, textVersionView);
		}
		final TextView about = (TextView) findViewById(R.id.About);
		final String aboutString = getString(R.string.about_settings);
		SpannableString ss = new SpannableString(aboutString);
		ClickableSpan clickableSpan = new ClickableSpan() {
		    @Override
		    public void onClick(View textView) {
		    	showAboutDialog(DashboardActivity.this, getMyApplication());
		    }
		};
		ss.setSpan(clickableSpan, 0, aboutString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		about.setText(ss);
		about.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	public void addFragments() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction fragmentTransaction = manager.beginTransaction();
		//after rotation list of fragments in fragment transaction is not cleared
		//so we need to check whether some fragments are already existing
		if (manager.findFragmentByTag(DashMapFragment.TAG) == null){
			DashMapFragment mapFragment = new DashMapFragment();
			fragmentTransaction.add(R.id.content, mapFragment, DashMapFragment.TAG);
		}
		if (manager.findFragmentByTag(DashSearchFragment.TAG) == null){
			DashSearchFragment searchFragment = new DashSearchFragment();
			fragmentTransaction.add(R.id.content, searchFragment, DashSearchFragment.TAG);
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
		setupContributionVersion();
	}
	
	public static void showAboutDialog(final Activity activity, final OsmandApplication app) {
		Builder bld = new AlertDialog.Builder(activity);
		bld.setTitle(R.string.about_settings);
        ScrollView sv = new ScrollView(activity);
        TextView tv = new TextView(activity);
        sv.addView(tv);
		String version = Version.getFullVersion(app);
		String vt = activity.getString(R.string.about_version) +"\t";
		int st = vt.length();
		String edition = "";
        SharedPreferences prefs = app.getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
        if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(app)) {
            try {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
                Date date = new Date(new File(appInfo.sourceDir).lastModified());
                edition = activity.getString(R.string.local_index_installed) + " :\t" + DateFormat.getDateFormat(app).format(date);
            } catch (Exception e) {
            }
            SpannableString content = new SpannableString(vt + version +"\n" +
    				edition +"\n\n"+
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
        	tv.setText(vt + version +"\n\n" +
    				activity.getString(R.string.about_content));
        }
        
		tv.setPadding(5, 0, 5, 5);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		bld.setView(sv);
		bld.setPositiveButton(R.string.default_buttons_ok, null);
		bld.show();
		
	}

	
	protected void initApp(final Activity activity, OsmandApplication app) {
		final OsmAndAppCustomization appCustomization = app.getAppCustomization();
		// restore follow route mode
		if(app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()){
			final Intent mapIndent = new Intent(this, appCustomization.getMapActivity());
			startActivityForResult(mapIndent, 0);
			return;
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
					if(TIPS_AND_TRICKS) {
					TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(this);
					Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
					dlg.show();
					} else {
						if(appVersionChanged) {
							final Intent helpIntent = new Intent(activity, HelpActivity.class);
							helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion(getMyApplication()));
							helpIntent.putExtra(HelpActivity.URL, "changes-1.9.html");
							activity.startActivity(helpIntent);
						}
					}
				}
			}
		}

		if(appCustomization.checkExceptionsOnStart()){
			checkPreviousRunsForExceptions(firstTime);
		}
	}
	
	private void applicationInstalledFirstTime() {
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null && !Version.isFreeVersion(getMyApplication());
		} catch (NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}
		
		if(netOsmandWasInstalled){
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
					startActivity(new Intent(DashboardActivity.this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
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
				String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
				Builder builder = new AccessibleAlertBuilder(DashboardActivity.this);
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
						} catch (NameNotFoundException e) {
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
			if(TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == SETTINGS_ID){
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == EXIT_ID){
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
}
