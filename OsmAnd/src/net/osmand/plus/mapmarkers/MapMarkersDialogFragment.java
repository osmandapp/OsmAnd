package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.Arrays;
import java.util.List;

public class MapMarkersDialogFragment extends android.support.v4.app.DialogFragment {

	public static final String TAG = "MapMarkersDialogFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		boolean isLightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View mainView = inflater.inflate(R.layout.fragment_map_markers_dialog, container);

		final LockableViewPager viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setSwipeLocked(true);
		viewPager.setAdapter(new MapMarkersViewPagerAdapter(getChildFragmentManager()));

		BottomNavigationView bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.action_active:
						viewPager.setCurrentItem(0);
						return true;
					case R.id.action_history:
						viewPager.setCurrentItem(1);
						return true;
				}
				return false;
			}
		});

		return mainView;
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		try {
			if (mapActivity.isActivityDestroyed()) {
				return false;
			}
			MapMarkersDialogFragment fragment = new MapMarkersDialogFragment();
			fragment.show(mapActivity.getSupportFragmentManager(), TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private class MapMarkersViewPagerAdapter extends FragmentPagerAdapter {

		private final List<Fragment> fragments;

		MapMarkersViewPagerAdapter(FragmentManager fm) {
			super(fm);
			fragments = Arrays.asList(new MapMarkersActiveFragment(), new MapMarkersHistoryFragment());
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
