package net.osmand.plus.liveupdates;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import net.osmand.AndroidNetworkUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.inapp.InAppHelper;

import org.apache.commons.logging.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OsmLiveActivity extends AbstractDownloadActivity implements DownloadIndexesThread.DownloadEvents {
	private final static Log LOG = PlatformUtil.getLog(OsmLiveActivity.class);
	public final static String OPEN_SUBSCRIPTION_INTENT_PARAM = "open_subscription_intent_param";
	private LiveUpdatesFragmentPagerAdapter pagerAdapter;
	private InAppHelper inAppHelper;
	private boolean openSubscription;
	private GetLastUpdateDateTask getLastUpdateDateTask;
	private static final String URL = "https://osmand.net/api/osmlive_status";

	public InAppHelper getInAppHelper() {
		return inAppHelper;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_livie_updates);

		if (Version.isGooglePlayEnabled(getMyApplication())) {
			inAppHelper = new InAppHelper(getMyApplication(), false);
		}
		if (Version.isDeveloperVersion(getMyApplication())) {
			inAppHelper = null;
		}

		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			openSubscription = intent.getExtras().getBoolean(OPEN_SUBSCRIPTION_INTENT_PARAM, false);
		}

		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		pagerAdapter = new LiveUpdatesFragmentPagerAdapter(getSupportFragmentManager(), getResources());
		viewPager.setAdapter(pagerAdapter);

		final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		tabLayout.setupWithViewPager(viewPager);

		getLastUpdateDateTask = new GetLastUpdateDateTask();
		getLastUpdateDateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Pass on the activity result to the helper for handling
		if (inAppHelper == null || !inAppHelper.onActivityResultHandled(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
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
		if (inAppHelper != null) {
			inAppHelper.stop();
		}
		if (getLastUpdateDateTask != null) {
			getLastUpdateDateTask.cancel(true);
		}
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

	public boolean shouldOpenSubscription() {
		return openSubscription;
	}

	private class GetLastUpdateDateTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			try {
				return AndroidNetworkUtils.sendRequest(getMyApplication(), URL, null, "Requesting map updates info...", false, false);
			} catch (Exception e) {
				LOG.error("Error: " + "Requesting map updates info error", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String response) {
			if (response != null) {
				ActionBar actionBar = getSupportActionBar();
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

		public LiveUpdatesFragmentPagerAdapter(FragmentManager fm, Resources res) {
			super(fm);
			titles = new String[titleIds.length];
			for (int i = 0; i < titleIds.length; i++) {
				titles[i] = res.getString(titleIds[i]);
			}
		}

		@Override
		public int getCount() {
			return fragments.length;
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
