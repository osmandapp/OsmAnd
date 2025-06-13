package net.osmand.plus.activities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

/**
 * Created by Denis
 * on 26.01.2015.
 */
public class TabActivity extends ActionBarProgressActivity {

	public TabItem getTabIndicator(int resId, Class<?> fragment) {
		return new TabItem(resId, getString(resId), fragment);
	}

	public static class TabItem {
		public final CharSequence mTitle;
		public final Class<?> fragment;
		public final int resId;

		public TabItem(int resId, CharSequence mTitle, Class<?> fragment) {
			this.resId = resId;
			this.mTitle = mTitle;
			this.fragment = fragment;
		}
	}

	protected void setViewPagerAdapter(ViewPager pager, List<TabItem> items) {
		pager.setAdapter(new OsmandFragmentPagerAdapter(getSupportFragmentManager(), items));
	}

	public static class OsmandFragmentPagerAdapter extends FragmentStatePagerAdapter {

		private final List<TabItem> mTabs;

		public OsmandFragmentPagerAdapter(@NonNull FragmentManager manager, @NonNull List<TabItem> items) {
			super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			mTabs = items;
		}

		/**
		 * Return the {@link Fragment} to be displayed at {@code position}.
		 * <p>
		 */
		@NonNull
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

		/**
		 * Return the title of the item at {@code position}. This is important as what this method
		 * returns is what is displayed in the {@link com.example.android.common.view.SlidingTabLayout}.
		 * <p>
		 */
		@Override
		public CharSequence getPageTitle(int position) {
			return mTabs.get(position).mTitle;
		}

		public void addTab(TabItem tabIndicator) {
			mTabs.add(tabIndicator);
			notifyDataSetChanged();
		}
	}
}