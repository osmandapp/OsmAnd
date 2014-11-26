package net.osmand.plus.dashboard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
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
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.*;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.download.*;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Created by Denis on 05.11.2014.
 */
public class DashboardActivity extends BaseDownloadActivity {

	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";

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

		DashSearchFragment searchFragment = new DashSearchFragment();
		fragmentTransaction.add(R.id.content, searchFragment);
		DashMapFragment mapFragment = new DashMapFragment();
		fragmentTransaction.add(R.id.content, mapFragment);
		DashFavoritesFragment favoritesFragment = new DashFavoritesFragment();
		fragmentTransaction.add(R.id.content, favoritesFragment);
		DashUpdatesFragment updatesFragment = new DashUpdatesFragment();
		fragmentTransaction.add(R.id.content, updatesFragment);
		DashPluginsFragment pluginsFragment = new DashPluginsFragment();
		fragmentTransaction.add(R.id.content, pluginsFragment).commit();

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
	public void updateProgress(boolean updateOnlyProgress) {
		//needed when rotation is performed and progress can be null
		if (progressBar == null){
			return;
		}
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = BaseDownloadActivity.downloadListIndexThread.getCurrentRunningTask();

		if(updateOnlyProgress){
			if(!basicProgressAsyncTask.isIndeterminate()){
				progressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		} else {
			boolean visible = basicProgressAsyncTask != null && basicProgressAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
			if (!visible) {
				return;
			}

			boolean intermediate = basicProgressAsyncTask.isIndeterminate();
			progressBar.setVisibility(intermediate ? View.GONE : View.VISIBLE);
			if (!intermediate) {
				progressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}
