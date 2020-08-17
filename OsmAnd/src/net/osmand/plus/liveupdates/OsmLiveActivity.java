package net.osmand.plus.liveupdates;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.osmand.AndroidNetworkUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.ChoosePlanDialogListener;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OsmLiveActivity extends AbstractDownloadActivity implements DownloadEvents, ChoosePlanDialogListener {
	private final static Log LOG = PlatformUtil.getLog(OsmLiveActivity.class);
	public final static String SHOW_SETTINGS_ONLY_INTENT_PARAM = "show_settings_only_intent_param";

	private LiveUpdatesFragmentPagerAdapter pagerAdapter;
	private boolean showSettingOnly;
	private GetLastUpdateDateTask getLastUpdateDateTask;
	private static final String URL = "https://osmand.net/api/osmlive_status";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_livie_updates);

		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			showSettingOnly = intent.getExtras().getBoolean(SHOW_SETTINGS_ONLY_INTENT_PARAM, false);
		} else if (savedInstanceState != null) {
			showSettingOnly = savedInstanceState.getBoolean(SHOW_SETTINGS_ONLY_INTENT_PARAM, false);
		}

		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		pagerAdapter = new LiveUpdatesFragmentPagerAdapter(getSupportFragmentManager(), getResources(), showSettingOnly);
		viewPager.setAdapter(pagerAdapter);

		final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		tabLayout.setupWithViewPager(viewPager);
		if (showSettingOnly) {
			tabLayout.setVisibility(View.GONE);
		} else {
			getLastUpdateDateTask = new GetLastUpdateDateTask(this);
			getLastUpdateDateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		boolean nightMode = !app.getSettings().isLightContent();
		int normalTabColor = ContextCompat.getColor(app,
				nightMode ? R.color.searchbar_tab_inactive_dark : R.color.searchbar_tab_inactive_light);
		int selectedTabColor = ContextCompat.getColor(app,
				nightMode ? R.color.text_color_tab_active_dark : R.color.text_color_tab_active_light);
		tabLayout.setTabTextColors(normalTabColor, selectedTabColor);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication()) && showSettingOnly) {
			ChoosePlanDialogFragment.showOsmLiveInstance(getSupportFragmentManager());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		getMyApplication().getDownloadThread().resetUiActivity(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (getLastUpdateDateTask != null) {
			getLastUpdateDateTask.cancel(true);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_SETTINGS_ONLY_INTENT_PARAM, showSettingOnly);
	}

	@Override
	public void onChoosePlanDialogDismiss() {
		//finish();
	}

	public boolean isShowSettingOnly() {
		return showSettingOnly;
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	@Override
	public void newDownloadIndexes() {
	}

	@Override
	public void downloadInProgress() {

	}

	@Override
	public void downloadHasFinished() {
		((LiveUpdatesFragment) pagerAdapter.fragments[0]).notifyLiveUpdatesChanged();
	}

	private static class GetLastUpdateDateTask extends AsyncTask<Void, Void, String> {

		private OsmandApplication app;
		private WeakReference<OsmLiveActivity> activity;

		GetLastUpdateDateTask(OsmLiveActivity activity) {
			this.activity = new WeakReference<>(activity);
			app = activity.getMyApplication();
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				return AndroidNetworkUtils.sendRequest(app, URL, null, "Requesting map updates info...", false, false);
			} catch (Exception e) {
				LOG.error("Error: " + "Requesting map updates info error", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String response) {
			OsmLiveActivity a = activity.get();
			if (response != null && a != null) {
				ActionBar actionBar = a.getSupportActionBar();
				if (actionBar != null) {
					SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
					source.setTimeZone(TimeZone.getTimeZone("UTC"));
					SimpleDateFormat dest = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
					dest.setTimeZone(TimeZone.getDefault());
					try {
						Date parsed = source.parse(response);
						actionBar.setSubtitle(dest.format(parsed));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static class LiveUpdatesFragmentPagerAdapter extends FragmentPagerAdapter {
		private final Fragment[] fragments = new Fragment[] { new LiveUpdatesFragment(), new ReportsFragment() };
		private static final int[] titleIds = new int[] { LiveUpdatesFragment.TITLE, ReportsFragment.TITLE };
		private final String[] titles;
		private final boolean showSettingsOnly;

		LiveUpdatesFragmentPagerAdapter(FragmentManager fm, Resources res, boolean showSettingsOnly) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.showSettingsOnly = showSettingsOnly;
			titles = new String[titleIds.length];
			for (int i = 0; i < titleIds.length; i++) {
				titles[i] = res.getString(titleIds[i]);
			}
		}

		@Override
		public int getCount() {
			return showSettingsOnly ? 1 : fragments.length;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments[position];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}
	}
}
