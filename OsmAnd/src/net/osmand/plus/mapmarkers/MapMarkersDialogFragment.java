package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment.OnPointsSavedListener;
import net.osmand.plus.mapmarkers.DirectionIndicationDialogFragment.DirectionIndicationFragmentListener;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkersSortByDef;
import net.osmand.plus.mapmarkers.MapMarkersHelper.OnGroupSyncedListener;
import net.osmand.plus.mapmarkers.OptionsBottomSheetDialogFragment.MarkerOptionsFragmentListener;
import net.osmand.plus.mapmarkers.OrderByBottomSheetDialogFragment.OrderByFragmentListener;
import net.osmand.plus.mapmarkers.SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener;
import net.osmand.plus.track.TrackMenuFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.mapmarkers.OptionsBottomSheetDialogFragment.GROUPS_MARKERS_MENU;
import static net.osmand.plus.mapmarkers.OptionsBottomSheetDialogFragment.HISTORY_MARKERS_MENU;

public class MapMarkersDialogFragment extends DialogFragment implements OnGroupSyncedListener {

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
	private BottomNavigationView bottomNav;
	private ProgressBar progressBar;

	private boolean lightTheme;
	private String groupIdToOpen;

	private int statusBarColor = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().isLightContent();
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = new Dialog(getActivity(), getTheme()) {
			@Override
			public void onBackPressed() {
				if (!dismissOptionsMenuFragment()) {
					super.onBackPressed();
				}
			}
		};
		if (!getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().getAttributes().windowAnimations = R.style.Animations_Alpha;
		}
		return dialog;
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
			((CoordinateInputDialogFragment) coordinateInputDialog).setListener(createOnPointsSavedListener());
		}

		View mainView = inflater.inflate(R.layout.fragment_map_markers_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.map_markers_toolbar);
		Drawable icArrowBack = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(getContext()),
				lightTheme ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark);
		toolbar.setNavigationIcon(icArrowBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.setSwipeLocked(true);
		final MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);

		TextView toolbarTitle = mainView.findViewById(R.id.map_markers_toolbar_title);
		bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		toolbarTitle.setTextColor(ContextCompat.getColor(getContext(), lightTheme ? R.color.active_buttons_and_links_text_light : R.color.text_color_primary_dark));
		bottomNav.setItemIconTintList(ContextCompat.getColorStateList(getContext(), lightTheme ? R.color.bottom_navigation_color_selector_light : R.color.bottom_navigation_color_selector_dark));
		bottomNav.setItemTextColor(ContextCompat.getColorStateList(getContext(), lightTheme ? R.color.bottom_navigation_color_selector_light : R.color.bottom_navigation_color_selector_dark));
		if (groupIdToOpen != null) {
			activeFragment.stopLocationUpdate();
			groupsFragment.startLocationUpdate();
			groupsFragment.setGroupIdToOpen(groupIdToOpen);
			viewPager.setCurrentItem(GROUPS_POSITION, false);
			bottomNav.getMenu().findItem(R.id.action_groups).setChecked(true);
		}
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				int i = menuItem.getItemId();
				if (i == R.id.action_active) {
					setupLocationUpdate(true, false);
					setupActiveFragment(ACTIVE_MARKERS_POSITION);
					return true;
				} else if (i == R.id.action_groups) {
					setupLocationUpdate(false, true);
					setupActiveFragment(GROUPS_POSITION);
					return true;
				} else if (i == R.id.action_history) {
					setupLocationUpdate(false, false);
					setupActiveFragment(HISTORY_MARKERS_POSITION);
					return true;
				} else if (i == R.id.action_more) {
					showOptionsMenuFragment();
					return true;
				}
				return false;
			}
		});
		bottomNav.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
			@Override
			public void onNavigationItemReselected(@NonNull MenuItem menuItem) {
				if (menuItem.getItemId() == R.id.action_more) {
					dismissOptionsMenuFragment();
				}
			}
		});

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMyApplication().getMapMarkersHelper().addSyncListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getMapMarkersHelper().removeSyncListener(this);
	}

	@Override
	public void onSyncStarted() {
		switchProgressbarVisibility(true);
	}

	@Override
	public void onSyncDone() {
		updateAdapters();
		switchProgressbarVisibility(false);
	}

	private void switchProgressbarVisibility(boolean visible) {
		if (progressBar != null) {
			progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	private void setupLocationUpdate(boolean activeFr, boolean groupsFr) {
		if (activeFr) {
			activeFragment.startLocationUpdate();
		} else {
			activeFragment.stopLocationUpdate();
		}
		if (groupsFr) {
			groupsFragment.startLocationUpdate();
		} else {
			groupsFragment.stopLocationUpdate();
		}
	}

	private void setupActiveFragment(int position) {
		dismissOptionsMenuFragment();
		if (viewPager.getCurrentItem() != position) {
			hideSnackbar();
			switch (position) {
				case ACTIVE_MARKERS_POSITION:
					activeFragment.updateAdapter();
					groupsFragment.hideSnackbar();
					historyFragment.hideSnackbar();
					break;
				case GROUPS_POSITION:
					activeFragment.hideSnackbar();
					groupsFragment.updateAdapter();
					historyFragment.hideSnackbar();
					break;
				case HISTORY_MARKERS_POSITION:
					activeFragment.hideSnackbar();
					groupsFragment.hideSnackbar();
					historyFragment.updateAdapter();
					break;
			}
			viewPager.setCurrentItem(position);
		}
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

	private OnPointsSavedListener createOnPointsSavedListener() {
		return new OnPointsSavedListener() {
			@Override
			public void onPointsSaved() {
				updateAdapters();
			}
		};
	}

	public void blurStatusBar() {
		if (Build.VERSION.SDK_INT >= 21) {
			Dialog dialog = getDialog();
			if (dialog != null && dialog.getWindow() != null) {
				statusBarColor = dialog.getWindow().getStatusBarColor();
				dialog.getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(),
						lightTheme ? R.color.status_bar_dim_light : R.color.status_bar_dim_dark));
			}
		}
	}

	public void clearStatusBar() {
		if (Build.VERSION.SDK_INT >= 21 && statusBarColor != -1) {
			Dialog dialog = getDialog();
			if (dialog != null && dialog.getWindow() != null) {
				dialog.getWindow().setStatusBarColor(statusBarColor);
			}
		}
	}

	private void showOptionsMenuFragment() {
		OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
		fragment.setListener(createOptionsFragmentListener());
		Bundle args = new Bundle();
		args.putBoolean(GROUPS_MARKERS_MENU, viewPager.getCurrentItem() == GROUPS_POSITION);
		args.putBoolean(HISTORY_MARKERS_MENU, viewPager.getCurrentItem() == HISTORY_MARKERS_POSITION);
		fragment.setArguments(args);
		getChildFragmentManager().beginTransaction()
				.add(R.id.menu_container, fragment, OptionsBottomSheetDialogFragment.TAG)
				.commitAllowingStateLoss();
	}

	private boolean dismissOptionsMenuFragment() {
		Fragment optionsMenu = getChildFragmentManager().findFragmentByTag(OptionsBottomSheetDialogFragment.TAG);
		if (optionsMenu != null) {
			((DialogFragment) optionsMenu).dismiss();
			return true;
		}
		return false;
	}

	private void restoreSelectedNavItem() {
		if (bottomNav.getSelectedItemId() == R.id.action_more) {
			int id = -1;
			switch (viewPager.getCurrentItem()) {
				case ACTIVE_MARKERS_POSITION:
					id = R.id.action_active;
					break;
				case GROUPS_POSITION:
					id = R.id.action_groups;
					break;
				case HISTORY_MARKERS_POSITION:
					id = R.id.action_history;
					break;
			}
			if (id != -1) {
				bottomNav.getMenu().findItem(id).setChecked(true);
			}
		}
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
					fragment.setListener(createOnPointsSavedListener());
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
						MapMarkersDialogFragment.this.dismiss();
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
					final List<MapMarker> markers = new ArrayList<>(helper.getMapMarkers());
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
					UiUtilities.setupSnackbar(snackbar, !lightTheme);
					snackbar.show();
				}
			}

			@Override
			public void dismiss() {
				restoreSelectedNavItem();
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
				final String gpxPath = mapActivity.getMyApplication().getMapMarkersHelper().getDataHelper().saveMarkersToFile(fileName);
				snackbar = Snackbar.make(viewPager, String.format(getString(R.string.shared_string_file_is_saved), fileName) + ".", Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_show, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								TrackMenuFragment.openTrack(mapActivity, new File(gpxPath), null);
							}
						});
				UiUtilities.setupSnackbar(snackbar, !lightTheme);
				snackbar.show();
			}
		};
	}

	private OrderByFragmentListener createOrderByFragmentListener() {
		return new OrderByFragmentListener() {
			@Override
			public void onMapMarkersOrderByModeChanged(@MapMarkersSortByDef int sortByMode) {
				OsmandApplication app = getMyApplication();
				MapActivity mapActivity = getMapActivity();

				Location location = app.getLocationProvider().getLastKnownLocation();
				boolean useCenter = !(mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation() && location != null);
				LatLon loc = useCenter ? mapActivity.getMapLocation() : new LatLon(location.getLatitude(), location.getLongitude());

				app.getMapMarkersHelper().sortMarkers(sortByMode, loc);
				activeFragment.updateAdapter();
			}
		};
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
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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
