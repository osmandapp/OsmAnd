package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MarkerOptionsBottomSheetDialogFragment.MarkerOptionsFragmentListener;

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
		FragmentManager fragmentManager = getChildFragmentManager();
		Fragment markerOptionsFragment = fragmentManager.findFragmentByTag(MarkerOptionsBottomSheetDialogFragment.TAG);
		if (markerOptionsFragment != null) {
			((MarkerOptionsBottomSheetDialogFragment) markerOptionsFragment).setListener(createMarkerOptionsFragmentListener());
		}

		View mainView = inflater.inflate(R.layout.fragment_map_markers_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.map_markers_toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		final View optionsButton = mainView.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MarkerOptionsBottomSheetDialogFragment fragment = new MarkerOptionsBottomSheetDialogFragment();
				fragment.setListener(createMarkerOptionsFragmentListener());
				fragment.show(getChildFragmentManager(), MarkerOptionsBottomSheetDialogFragment.TAG);
			}
		});

		final LockableViewPager viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setSwipeLocked(true);
		final MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		BottomNavigationView bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.action_active:
						((MapMarkersActiveFragment) adapter.getItem(0)).startLocationUpdate();
						if (viewPager.getCurrentItem() != 0) {
							((MapMarkersActiveFragment) adapter.getItem(0)).updateAdapter();
						}
						viewPager.setCurrentItem(0);
						optionsButton.setVisibility(View.VISIBLE);
						return true;
					case R.id.action_history:
						((MapMarkersActiveFragment) adapter.getItem(0)).stopLocationUpdate();
						if (viewPager.getCurrentItem() != 1) {
							((MapMarkersHistoryFragment) adapter.getItem(1)).updateAdapter();
						}
						viewPager.setCurrentItem(1);
						optionsButton.setVisibility(View.GONE);
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

	private MarkerOptionsFragmentListener createMarkerOptionsFragmentListener() {
		return new MarkerOptionsFragmentListener() {
			@Override
			public void sortByOnClick() {
				Toast.makeText(getContext(), "Sort by", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void showDirectionOnClick() {
				Toast.makeText(getContext(), "Show direction", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void buildRouteOnClick() {
				Toast.makeText(getContext(), "Build route", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void saveAsNewTrackOnClick() {
				Toast.makeText(getContext(), "Save as new track", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void moveAllToHistoryOnClick() {
				Toast.makeText(getContext(), "Move all to history", Toast.LENGTH_SHORT).show();
			}
		};
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
