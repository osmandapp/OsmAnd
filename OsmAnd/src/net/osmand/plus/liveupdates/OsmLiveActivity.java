package net.osmand.plus.liveupdates;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import org.apache.commons.logging.Log;

public class OsmLiveActivity extends AbstractDownloadActivity {
	private final static Log LOG = PlatformUtil.getLog(OsmLiveActivity.class);
	public final static String SHOW_SETTINGS_ONLY_INTENT_PARAM = "show_settings_only_intent_param";

	private LiveUpdatesFragmentPagerAdapter pagerAdapter;
	private boolean showSettingOnly;

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
			ChoosePlanFragment.showInstance(this, OsmAndFeature.HOURLY_MAP_UPDATES);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_SETTINGS_ONLY_INTENT_PARAM, showSettingOnly);
	}

	public boolean isShowSettingOnly() {
		return showSettingOnly;
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	public static class LiveUpdatesFragmentPagerAdapter extends FragmentPagerAdapter {
		private final Fragment[] fragments = new Fragment[] {new ReportsFragment()};
		private static final int[] titleIds = new int[] {ReportsFragment.TITLE};
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
