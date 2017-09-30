package net.osmand.plus.mapmarkers;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersOrderByMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.mapmarkers.OptionsBottomSheetDialogFragment.MarkerOptionsFragmentListener;
import net.osmand.plus.mapmarkers.OrderByBottomSheetDialogFragment.OrderByFragmentListener;
import net.osmand.plus.mapmarkers.SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener;
import net.osmand.plus.mapmarkers.ShowDirectionBottomSheetDialogFragment.ShowDirectionFragmentListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapMarkersDialogFragment extends android.support.v4.app.DialogFragment {

	public static final String TAG = "MapMarkersDialogFragment";

	private MapMarkersActiveFragment activeFragment;
	private MapMarkersGroupsFragment groupsFragment;
	private MapMarkersHistoryFragment historyFragment;

	private Snackbar snackbar;
	private LockableViewPager viewPager;

	private boolean lightTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
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
		Fragment optionsFragment = fragmentManager.findFragmentByTag(OptionsBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((OptionsBottomSheetDialogFragment) optionsFragment).setListener(createOptionsFragmentListener());
		}
		Fragment showDirectionFragment = fragmentManager.findFragmentByTag(ShowDirectionBottomSheetDialogFragment.TAG);
		if (showDirectionFragment != null) {
			((ShowDirectionBottomSheetDialogFragment) showDirectionFragment).setListener(createShowDirectionFragmentListener());
		}
		final Fragment orderByFragment = fragmentManager.findFragmentByTag(OrderByBottomSheetDialogFragment.TAG);
		if (orderByFragment != null) {
			((OrderByBottomSheetDialogFragment) orderByFragment).setListener(createOrderByFragmentListener());
		}
		Fragment saveAsTrackFragment = fragmentManager.findFragmentByTag(SaveAsTrackBottomSheetDialogFragment.TAG);
		if (saveAsTrackFragment != null) {
			((SaveAsTrackBottomSheetDialogFragment) saveAsTrackFragment).setListener(createSaveAsTrackFragmentListener());
		}

		View mainView = inflater.inflate(R.layout.fragment_map_markers_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.map_markers_toolbar);
		if (!lightTheme) {
			toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.actionbar_dark_color));
		}
		setOrderByMode(getMyApplication().getSettings().MAP_MARKERS_ORDER_BY_MODE.get());

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
				OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
				fragment.setListener(createOptionsFragmentListener());
				fragment.show(getChildFragmentManager(), OptionsBottomSheetDialogFragment.TAG);
			}
		});

		viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setSwipeLocked(true);
		final MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		BottomNavigationView bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		if (!lightTheme) {
			bottomNav.setItemIconTintList(ContextCompat.getColorStateList(getContext(), R.color.bottom_navigation_color_selector_dark));
			bottomNav.setItemTextColor(ContextCompat.getColorStateList(getContext(), R.color.bottom_navigation_color_selector_dark));
		}
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.action_active:
						activeFragment.startLocationUpdate();
						if (viewPager.getCurrentItem() != 0) {
							hideSnackbar();
							activeFragment.updateAdapter();
							historyFragment.hideSnackbar();
							groupsFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(0);
						optionsButton.setVisibility(View.VISIBLE);
						return true;
					case R.id.action_groups:
						activeFragment.stopLocationUpdate();
						if (viewPager.getCurrentItem() != 1) {
							hideSnackbar();
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
							hideSnackbar();
							historyFragment.updateAdapter();
							groupsFragment.hideSnackbar();
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

	private MarkerOptionsFragmentListener createOptionsFragmentListener() {
		return new MarkerOptionsFragmentListener() {

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void sortByOnClick() {
				OrderByBottomSheetDialogFragment fragment = new OrderByBottomSheetDialogFragment();
				fragment.setListener(createOrderByFragmentListener());
				fragment.show(mapActivity.getSupportFragmentManager(), OrderByBottomSheetDialogFragment.TAG);
			}

			@Override
			public void showDirectionOnClick() {
				ShowDirectionBottomSheetDialogFragment fragment = new ShowDirectionBottomSheetDialogFragment();
				fragment.setListener(createShowDirectionFragmentListener());
				fragment.show(mapActivity.getSupportFragmentManager(), ShowDirectionBottomSheetDialogFragment.TAG);
			}

			@Override
			public void coordinateInputOnClick() {
				CoordinateInputDialogFragment.showInstance(mapActivity);
			}

			@Override
			public void buildRouteOnClick() {
				PlanRouteFragment.showInstance(mapActivity.getSupportFragmentManager());
				dismiss();
			}

			@Override
			public void saveAsNewTrackOnClick() {
				SaveAsTrackBottomSheetDialogFragment fragment = new SaveAsTrackBottomSheetDialogFragment();
				fragment.setListener(createSaveAsTrackFragmentListener());
				fragment.show(mapActivity.getSupportFragmentManager(), SaveAsTrackBottomSheetDialogFragment.TAG);
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

	private MarkerSaveAsTrackFragmentListener createSaveAsTrackFragmentListener() {
		return new MarkerSaveAsTrackFragmentListener() {

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void saveGpx(final String fileName) {
				final String gpxPath = mapActivity.getMyApplication().getMapMarkersHelper().generateGpx(fileName);
				snackbar = Snackbar.make(viewPager, fileName + " " + getString(R.string.is_saved) + ".", Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_show, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								Intent intent = new Intent(mapActivity, getMyApplication().getAppCustomization().getTrackActivity());
								intent.putExtra(TrackActivity.TRACK_FILE_NAME, gpxPath);
								intent.putExtra(TrackActivity.OPEN_POINTS_TAB, true);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
							}
						});
				View snackBarView = snackbar.getView();
				TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
				tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
				snackbar.show();
			}
		};
	}

	private OrderByFragmentListener createOrderByFragmentListener() {
		return new OrderByFragmentListener() {
			@Override
			public void onMapMarkersOrderByModeChanged(MapMarkersOrderByMode orderByMode) {
				setOrderByMode(orderByMode);
			}
		};
	}

	private void setOrderByMode(MapMarkersOrderByMode orderByMode) {
		if (orderByMode != MapMarkersOrderByMode.CUSTOM) {
			getMyApplication().getMapMarkersHelper().orderMarkers(orderByMode);
			activeFragment.updateAdapter();
		}
	}

	private void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
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
