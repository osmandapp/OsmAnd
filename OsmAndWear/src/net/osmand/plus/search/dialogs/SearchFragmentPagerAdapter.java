package net.osmand.plus.search.dialogs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class SearchFragmentPagerAdapter extends FragmentPagerAdapter {

	private static final String[] SEARCH_FRAGMENTS = {
			QuickSearchHistoryListFragment.class.getName(),
			QuickSearchCategoriesListFragment.class.getName(),
			QuickSearchAddressListFragment.class.getName()
	};

	private final Context context;
	private final String[] titles;

	public SearchFragmentPagerAdapter(@NonNull Context context, @NonNull FragmentManager manager) {
		super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		this.context = context;

		int[] titleIds = {
				QuickSearchHistoryListFragment.TITLE,
				QuickSearchCategoriesListFragment.TITLE,
				QuickSearchAddressListFragment.TITLE
		};
		titles = new String[titleIds.length];
		for (int i = 0; i < titleIds.length; i++) {
			titles[i] = context.getString(titleIds[i]);
		}
	}

	@Override
	public int getCount() {
		return SEARCH_FRAGMENTS.length;
	}

	@NonNull
	@Override
	public Fragment getItem(int position) {
		return Fragment.instantiate(context, SEARCH_FRAGMENTS[position]);
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position];
	}
}
