package net.osmand.plus.wikivoyage.explore;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PicassoUtils;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.WikivoyageBaseDialogFragment;
import net.osmand.plus.wikivoyage.search.WikivoyageSearchDialogFragment;

import java.util.Arrays;
import java.util.List;

public class WikivoyageExploreDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageExploreDialogFragment";

	private static final int EXPLORE_POSITION = 0;
	private static final int SAVED_ARTICLES_POSITION = 1;

	private ExploreTabFragment exploreTabFragment;
	private SavedArticlesTabFragment savedArticlesTabFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context context = getContext();
		if (context != null) {
			PicassoUtils.setupPicasso(context);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentManager childFm = getChildFragmentManager();
		List<Fragment> fragments = childFm.getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof ExploreTabFragment) {
					exploreTabFragment = (ExploreTabFragment) fragment;
				} else if (fragment instanceof SavedArticlesTabFragment) {
					savedArticlesTabFragment = (SavedArticlesTabFragment) fragment;
				}
			}
		}
		if (exploreTabFragment == null) {
			exploreTabFragment = new ExploreTabFragment();
		}
		if (savedArticlesTabFragment == null) {
			savedArticlesTabFragment = new SavedArticlesTabFragment();
		}

		final View mainView = inflate(R.layout.fragment_wikivoyage_explore_dialog, container);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				if (fm == null) {
					return;
				}
				WikivoyageOptionsBottomSheetDialogFragment fragment = new WikivoyageOptionsBottomSheetDialogFragment();
				fragment.setUsedOnMap(false);
				fragment.setTargetFragment(WikivoyageExploreDialogFragment.this,
						WikivoyageOptionsBottomSheetDialogFragment.REQUEST_CODE);
				fragment.show(fm, WikivoyageOptionsBottomSheetDialogFragment.TAG);
			}
		});

		int searchColorId = nightMode ? R.color.icon_color : R.color.ctx_menu_title_color_dark;
		((TextView) mainView.findViewById(R.id.search_hint)).setTextColor(getResolvedColor(searchColorId));
		((ImageView) mainView.findViewById(R.id.search_icon))
				.setImageDrawable(getIcon(R.drawable.ic_action_search_dark, searchColorId));

		mainView.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikivoyageSearchDialogFragment.showInstance(getFragmentManager());
			}
		});

		ViewPagerAdapter adapter = new ViewPagerAdapter(childFm);
		final LockableViewPager viewPager = (LockableViewPager) mainView.findViewById(R.id.view_pager);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setSwipeLocked(true);
		viewPager.setAdapter(adapter);

		final ColorStateList navColorStateList = createBottomNavColorStateList();
		final BottomNavigationView bottomNav = (BottomNavigationView) mainView.findViewById(R.id.bottom_navigation);
		bottomNav.setItemIconTintList(navColorStateList);
		bottomNav.setItemTextColor(navColorStateList);
		bottomNav.setOnNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				int position = -1;
				switch (item.getItemId()) {
					case R.id.action_explore:
						position = EXPLORE_POSITION;
						break;
					case R.id.action_saved_articles:
						position = SAVED_ARTICLES_POSITION;
						break;
				}
				if (position != -1 && position != viewPager.getCurrentItem()) {
					viewPager.setCurrentItem(position);
					return true;
				}
				return false;
			}
		});

		return mainView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == WikivoyageOptionsBottomSheetDialogFragment.REQUEST_CODE) {
			if (resultCode == WikivoyageOptionsBottomSheetDialogFragment.DOWNLOAD_IMAGES_CHANGED
					|| resultCode == WikivoyageOptionsBottomSheetDialogFragment.CACHE_CLEARED) {
				if (savedArticlesTabFragment != null) {
					savedArticlesTabFragment.updateAdapter();
				}
			}
		}
	}

	private ColorStateList createBottomNavColorStateList() {
		return AndroidUtils.createCheckedColorStateList(getContext(), nightMode,
				R.color.icon_color, R.color.wikivoyage_active_light,
				R.color.icon_color, R.color.wikivoyage_active_dark);
	}

	public static boolean showInstance(FragmentManager fm) {
		try {
			WikivoyageExploreDialogFragment fragment = new WikivoyageExploreDialogFragment();
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {

		private final List<BaseOsmAndFragment> fragments = Arrays.asList(exploreTabFragment, savedArticlesTabFragment);

		ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}
	}
}
