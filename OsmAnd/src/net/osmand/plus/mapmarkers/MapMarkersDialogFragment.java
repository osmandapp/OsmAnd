package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.mapmarkers.ShowDirectionBottomSheetDialogFragment.ShowDirectionFragmentListener;
import net.osmand.plus.mapmarkers.MarkerOptionsBottomSheetDialogFragment.MarkerOptionsFragmentListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MapMarkersDialogFragment extends android.support.v4.app.DialogFragment {

	public static final String TAG = "MapMarkersDialogFragment";

	private MapMarkersActiveFragment activeFragment;
	private MapMarkersGroupsFragment groupsFragment;
	private MapMarkersHistoryFragment historyFragment;

	private Snackbar snackbar;
	private LockableViewPager viewPager;

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
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof MapMarkersActiveFragment) {
					activeFragment = (MapMarkersActiveFragment) fragment;
				} else if (fragment instanceof MapMarkersGroupsFragment) {
					groupsFragment = (MapMarkersGroupsFragment) fragment;
				} else if (fragment instanceof MapMarkersHistoryFragment) {
					historyFragment = (MapMarkersHistoryFragment) fragment;
				}
			}
		}
		if (activeFragment == null) {
			activeFragment = new MapMarkersActiveFragment();
		}
		if (groupsFragment == null) {
			groupsFragment = new MapMarkersGroupsFragment();
		}
		if (historyFragment == null) {
			historyFragment = new MapMarkersHistoryFragment();
		}

		FragmentManager fragmentManager = getChildFragmentManager();
		Fragment markerOptionsFragment = fragmentManager.findFragmentByTag(MarkerOptionsBottomSheetDialogFragment.TAG);
		if (markerOptionsFragment != null) {
			((MarkerOptionsBottomSheetDialogFragment) markerOptionsFragment).setListener(createMarkerOptionsFragmentListener());
		}
		Fragment showDirectionFragment = fragmentManager.findFragmentByTag(ShowDirectionBottomSheetDialogFragment.TAG);
		if (showDirectionFragment != null) {
			((ShowDirectionBottomSheetDialogFragment) showDirectionFragment).setListener(createShowDirectionFragmentListener());
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

		viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setSwipeLocked(true);
		final MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		BottomNavigationView bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.action_active:
						activeFragment.startLocationUpdate();
						if (viewPager.getCurrentItem() != 0) {
							activeFragment.updateAdapter();
							historyFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(0);
						optionsButton.setVisibility(View.VISIBLE);
						return true;
					case R.id.action_groups:
						activeFragment.stopLocationUpdate();
						if (viewPager.getCurrentItem() != 1) {
							groupsFragment.updateAdapter();
							activeFragment.hideSnackbar();
							historyFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(1);
						optionsButton.setVisibility(View.GONE);
						return true;
					case R.id.action_history:
						activeFragment.stopLocationUpdate();
						if (viewPager.getCurrentItem() != 2) {
							historyFragment.updateAdapter();
							activeFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(2);
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

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void sortByOnClick() {
				Toast.makeText(getContext(), "Sort by", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void showDirectionOnClick() {
				ShowDirectionBottomSheetDialogFragment fragment = new ShowDirectionBottomSheetDialogFragment();
				fragment.setListener(createShowDirectionFragmentListener());
				fragment.show(mapActivity.getSupportFragmentManager(), ShowDirectionBottomSheetDialogFragment.TAG);
			}

			@Override
			public void buildRouteOnClick() {
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAP_MARKERS_SELECTION);
				dismiss();
			}

			@Override
			public void saveAsNewTrackOnClick() {
				mapActivity.getMyApplication().getMapMarkersHelper().generateGpx();
			}

			@Override
			public void moveAllToHistoryOnClick() {
				final MapMarkersHelper helper = mapActivity.getMyApplication().getMapMarkersHelper();
				final List<MapMarkersHelper.MapMarker> markers = new ArrayList<>(helper.getMapMarkers());
				helper.moveAllActiveMarkersToHistory();
				activeFragment.updateAdapter();
				snackbar = Snackbar.make(viewPager, R.string.all_markers_moved_to_history, Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_undo, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								helper.restoreMarkersFromHistory(markers);
								activeFragment.updateAdapter();
							}
						});
				View snackBarView = snackbar.getView();
				TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
				tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
				snackbar.show();
			}
		};
	}

	private ShowDirectionFragmentListener createShowDirectionFragmentListener() {
		return new ShowDirectionFragmentListener() {

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void onMapMarkersModeChanged(boolean showDirectionEnabled) {
				mapActivity.getMapLayers().getMapWidgetRegistry().updateMapMarkersMode(mapActivity);
				activeFragment.setShowDirectionEnabled(showDirectionEnabled);
				activeFragment.updateAdapter();
			}
		};
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
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
			fragments = Arrays.asList(activeFragment, groupsFragment, historyFragment);
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
