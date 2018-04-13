package net.osmand.plus.wikivoyage.explore;

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
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.WikivoyageBaseDialogFragment;
import net.osmand.plus.wikivoyage.search.WikivoyageSearchDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WikivoyageExploreDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageExploreDialogFragment";

	private static final int EXPLORE_POSITION = 0;
	private static final int SAVED_ARTICLES_POSITION = 1;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflate(R.layout.fragment_wikivoyage_explore_dialog, container);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikivoyageOptionsBottomSheetDialogFragment fragment = new WikivoyageOptionsBottomSheetDialogFragment();
				fragment.setUsedOnMap(false);
				fragment.show(getChildFragmentManager(), WikivoyageOptionsBottomSheetDialogFragment.TAG);
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

		final LockableViewPager viewPager = (LockableViewPager) mainView.findViewById(R.id.view_pager);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setSwipeLocked(true);
		viewPager.setAdapter(new ViewPagerAdapter(getChildFragmentManager()));

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

	private static class ViewPagerAdapter extends FragmentPagerAdapter {

		private final List<Fragment> fragments = new ArrayList<>();

		ViewPagerAdapter(FragmentManager fm) {
			super(fm);
			fragments.addAll(Arrays.asList(new ExploreTabFragment(), new SavedArticlesTabFragment()));
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
