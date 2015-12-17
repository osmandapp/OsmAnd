package net.osmand.plus.liveupdates;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import net.osmand.plus.R;
import net.osmand.plus.download.AbstractDownloadActivity;

public class LiveUpdatesActivity extends AbstractDownloadActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_livie_updates);

		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		final LiveUpdatesFragmentPagerAdapter pagerAdapter =
				new LiveUpdatesFragmentPagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(pagerAdapter);

		final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		tabLayout.setupWithViewPager(viewPager);
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

	public static class LiveUpdatesFragmentPagerAdapter extends FragmentPagerAdapter {
		private final Fragment[] fragments = new Fragment[]{new LiveUpdatesFragment()};
		private final String[] titles = new String[]{LiveUpdatesFragment.TITILE};

		public LiveUpdatesFragmentPagerAdapter(FragmentManager fm) {
			super(fm);
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
