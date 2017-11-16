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
import android.widget.Toast;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersOrderByMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.OnMapMarkersSavedListener;
import net.osmand.plus.mapmarkers.DirectionIndicationDialogFragment.DirectionIndicationFragmentListener;
import net.osmand.plus.mapmarkers.OptionsBottomSheetDialogFragment.MarkerOptionsFragmentListener;
import net.osmand.plus.mapmarkers.OrderByBottomSheetDialogFragment.OrderByFragmentListener;
import net.osmand.plus.mapmarkers.SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapMarkersDialogFragment extends android.support.v4.app.DialogFragment {

	public static final String TAG = "MapMarkersDialogFragment";

	public static final String OPEN_MAP_MARKERS_GROUPS = "open_map_markers_groups";

	private static final int ACTIVE_MARKERS_POSITION = 0;
	private static final int GROUPS_POSITION = 1;
	private static final int HISTORY_MARKERS_POSITION = 2;

	private MapMarkersActiveFragment activeFragment;
	private MapMarkersGroupsFragment groupsFragment;
	private MapMarkersHistoryFragment historyFragment;

	private Snackbar snackbar;
	private LockableViewPager viewPager;

	private boolean lightTheme;
	private String groupIdToOpen;

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
		Fragment directionIndicationFragment = fragmentManager.findFragmentByTag(DirectionIndicationDialogFragment.TAG);
		if (directionIndicationFragment != null) {
			((DirectionIndicationDialogFragment) directionIndicationFragment).setListener(createShowDirectionFragmentListener());
		}
		final Fragment orderByFragment = fragmentManager.findFragmentByTag(OrderByBottomSheetDialogFragment.TAG);
		if (orderByFragment != null) {
			((OrderByBottomSheetDialogFragment) orderByFragment).setListener(createOrderByFragmentListener());
		}
		Fragment saveAsTrackFragment = fragmentManager.findFragmentByTag(SaveAsTrackBottomSheetDialogFragment.TAG);
		if (saveAsTrackFragment != null) {
			((SaveAsTrackBottomSheetDialogFragment) saveAsTrackFragment).setListener(createSaveAsTrackFragmentListener());
		}
		Fragment coordinateInputDialog = fragmentManager.findFragmentByTag(CoordinateInputDialogFragment.TAG);
		if (coordinateInputDialog != null) {
			((CoordinateInputDialogFragment) coordinateInputDialog).setListener(createOnMapMarkersSavedListener());
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
				fragment.setUsedOnMap(false);
				fragment.setListener(createOptionsFragmentListener());
				Bundle args = new Bundle();
				int pos = viewPager.getCurrentItem();
				args.putBoolean(OptionsBottomSheetDialogFragment.SHOW_SORT_BY_ROW, pos == ACTIVE_MARKERS_POSITION);
				args.putBoolean(OptionsBottomSheetDialogFragment.SHOW_MOVE_ALL_TO_HISTORY_ROW, pos != HISTORY_MARKERS_POSITION);
				fragment.setArguments(args);
				fragment.show(getChildFragmentManager(), OptionsBottomSheetDialogFragment.TAG);
			}
		});

		viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.setSwipeLocked(true);
		final MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		BottomNavigationView bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		if (!lightTheme) {
			bottomNav.setItemIconTintList(ContextCompat.getColorStateList(getContext(), R.color.bottom_navigation_color_selector_dark));
			bottomNav.setItemTextColor(ContextCompat.getColorStateList(getContext(), R.color.bottom_navigation_color_selector_dark));
		}
		if (groupIdToOpen != null) {
			activeFragment.stopLocationUpdate();
			groupsFragment.startLocationUpdate();
			groupsFragment.setGroupIdToOpen(groupIdToOpen);
			viewPager.setCurrentItem(GROUPS_POSITION, false);
		}
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				switch (menuItem.getItemId()) {
					case R.id.action_active:
						activeFragment.startLocationUpdate();
						groupsFragment.stopLocationUpdate();
						if (viewPager.getCurrentItem() != ACTIVE_MARKERS_POSITION) {
							hideSnackbar();
							activeFragment.updateAdapter();
							historyFragment.hideSnackbar();
							groupsFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(ACTIVE_MARKERS_POSITION);
						return true;
					case R.id.action_groups:
						activeFragment.stopLocationUpdate();
						groupsFragment.startLocationUpdate();
						if (viewPager.getCurrentItem() != GROUPS_POSITION) {
							hideSnackbar();
							groupsFragment.updateAdapter();
							activeFragment.hideSnackbar();
							historyFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(GROUPS_POSITION);
						return true;
					case R.id.action_history:
						activeFragment.stopLocationUpdate();
						groupsFragment.stopLocationUpdate();
						if (viewPager.getCurrentItem() != HISTORY_MARKERS_POSITION) {
							hideSnackbar();
							historyFragment.updateAdapter();
							groupsFragment.hideSnackbar();
							activeFragment.hideSnackbar();
						}
						viewPager.setCurrentItem(HISTORY_MARKERS_POSITION);
						return true;
				}
				return false;
			}
		});

		return mainView;
	}

	private void setGroupIdToOpen(String groupIdToOpen) {
		this.groupIdToOpen = groupIdToOpen;
	}

	private void updateAdapters() {
		activeFragment.updateAdapter();
		groupsFragment.updateAdapter();
		historyFragment.updateAdapter();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private OnMapMarkersSavedListener createOnMapMarkersSavedListener() {
		return new OnMapMarkersSavedListener() {
			@Override
			public void onMapMarkersSaved() {
				updateAdapters();
			}
		};
	}

	private MarkerOptionsFragmentListener createOptionsFragmentListener() {
		return new MarkerOptionsFragmentListener() {

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void sortByOnClick() {
				if (mapActivity != null) {
					OrderByBottomSheetDialogFragment fragment = new OrderByBottomSheetDialogFragment();
					fragment.setUsedOnMap(false);
					fragment.setListener(createOrderByFragmentListener());
					fragment.show(getChildFragmentManager(), OrderByBottomSheetDialogFragment.TAG);
				}
			}

			@Override
			public void showDirectionOnClick() {
				if (mapActivity != null) {
					DirectionIndicationDialogFragment fragment = new DirectionIndicationDialogFragment();
					fragment.setListener(createShowDirectionFragmentListener());
					fragment.show(getChildFragmentManager(), DirectionIndicationDialogFragment.TAG);
				}
			}

			@Override
			public void coordinateInputOnClick() {
				if (mapActivity != null) {
					CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
					fragment.setRetainInstance(true);
					fragment.setListener(createOnMapMarkersSavedListener());
					fragment.show(getChildFragmentManager(), CoordinateInputDialogFragment.TAG);
				}
			}

			@Override
			public void buildRouteOnClick() {
				if (mapActivity != null) {
					if (mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers().isEmpty()) {
						Toast.makeText(mapActivity, getString(R.string.plan_route_no_markers_toast), Toast.LENGTH_SHORT).show();
					} else {
						PlanRouteFragment.showInstance(mapActivity);
						dismiss();
					}
				}
			}

			@Override
			public void saveAsNewTrackOnClick() {
				if (mapActivity != null) {
					if (mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers().isEmpty()) {
						Toast.makeText(mapActivity, getString(R.string.plan_route_no_markers_toast), Toast.LENGTH_SHORT).show();
					} else {
						SaveAsTrackBottomSheetDialogFragment fragment = new SaveAsTrackBottomSheetDialogFragment();
						fragment.setListener(createSaveAsTrackFragmentListener());
						fragment.show(getChildFragmentManager(), SaveAsTrackBottomSheetDialogFragment.TAG);
					}
				}
			}

			@Override
			public void moveAllToHistoryOnClick() {
				if (mapActivity != null) {
					final MapMarkersHelper helper = mapActivity.getMyApplication().getMapMarkersHelper();
					final List<MapMarkersHelper.MapMarker> markers = new ArrayList<>(helper.getMapMarkers());
					helper.moveAllActiveMarkersToHistory();
					if (viewPager.getCurrentItem() == ACTIVE_MARKERS_POSITION) {
						activeFragment.updateAdapter();
					} else {
						groupsFragment.updateAdapter();
					}
					snackbar = Snackbar.make(viewPager, R.string.all_markers_moved_to_history, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									helper.restoreMarkersFromHistory(markers);
									if (viewPager.getCurrentItem() == ACTIVE_MARKERS_POSITION) {
										activeFragment.updateAdapter();
									} else {
										groupsFragment.updateAdapter();
									}
								}
							});
					View snackBarView = snackbar.getView();
					TextView tv = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_action);
					tv.setTextColor(ContextCompat.getColor(mapActivity, R.color.color_dialog_buttons_dark));
					snackbar.show();
				}
			}
		};
	}

	private DirectionIndicationFragmentListener createShowDirectionFragmentListener() {
		return new DirectionIndicationFragmentListener() {

			final MapActivity mapActivity = getMapActivity();

			@Override
			public void onMapMarkersModeChanged(boolean showDirectionEnabled) {
				mapActivity.getMapLayers().getMapWidgetRegistry().updateMapMarkersMode(mapActivity);
				activeFragment.setShowDirectionEnabled(showDirectionEnabled);
				updateAdapters();
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
		return showInstance(mapActivity, null);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, String groupIdToOpen) {
		try {
			if (mapActivity.isActivityDestroyed()) {
				return false;
			}
			MapMarkersDialogFragment fragment = new MapMarkersDialogFragment();
			fragment.setGroupIdToOpen(groupIdToOpen);
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
