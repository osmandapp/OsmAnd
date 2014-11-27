package net.osmand.plus.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.download.*;
import net.osmand.util.MapUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Denis on 05.11.2014.
 */
public class DashboardActivity extends BaseDownloadActivity {



	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$

	public static final boolean TIPS_AND_TRICKS = false;

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
}
