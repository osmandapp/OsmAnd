package net.osmand.plus.activities;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 26.01.2015.
 */
public class TabActivity extends ActionBarProgressActivity {

	protected TabItem getTabIndicator(int resId, Class<?> fragment){
		return new TabItem(getString(resId), Color.DKGRAY, Color.LTGRAY, fragment);
	}

	protected List<TabItem> mTabs = new ArrayList<TabItem>();

	protected static class TabItem {
		private final CharSequence mTitle;
		private final int mIndicatorColor;
		private final int mDividerColor;
		private final Class<?> fragment;

		public TabItem(CharSequence mTitle, int mIndicatorColor, int mDividerColor, Class<?> fragment) {
			this.mTitle = mTitle;
			this.mIndicatorColor = mIndicatorColor;
			this.mDividerColor = mDividerColor;
			this.fragment = fragment;
		}

	}

	protected void setViewPagerAdapter(ViewPager pager){
		pager.setAdapter(new SearchFragmentPagerAdapter(getSupportFragmentManager()));
	}

	protected class SearchFragmentPagerAdapter extends FragmentPagerAdapter {

		SearchFragmentPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		/**
		 * Return the {@link android.support.v4.app.Fragment} to be displayed at {@code position}.
		 * <p>
		 */
		@Override
		public Fragment getItem(int i) {
			try {
				return (Fragment) mTabs.get(i).fragment.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		// BEGIN_INCLUDE (pageradapter_getpagetitle)
		/**
		 * Return the title of the item at {@code position}. This is important as what this method
		 * returns is what is displayed in the {@link com.example.android.common.view.SlidingTabLayout}.
		 * <p>
		 */
		@Override
		public CharSequence getPageTitle(int position) {
			return mTabs.get(position).mTitle;
		}
	}
}
