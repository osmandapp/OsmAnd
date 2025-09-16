package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

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

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.OrderByBottomSheetDialogFragment.OrderByFragmentListener;
import net.osmand.plus.mapmarkers.SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener;
import net.osmand.plus.mapmarkers.SyncGroupTask.OnGroupSyncedListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MapMarkersDialogFragment extends BaseFullScreenDialogFragment implements OnGroupSyncedListener {

	public static final String TAG = MapMarkersDialogFragment.class.getSimpleName();

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

	private String groupIdToOpen;

	private int statusBarColor = -1;

	@NonNull
	@Override
	public Dialog createDialog(Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getTheme()) {
			@Override
			public void onBackPressed() {
				if (!dismissOptionsMenuFragment()) {
					super.onBackPressed();
				}
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		for (Fragment fragment : fragments) {
			if (fragment instanceof MapMarkersActiveFragment) {
				activeFragment = (MapMarkersActiveFragment) fragment;
			} else if (fragment instanceof MapMarkersGroupsFragment) {
				groupsFragment = (MapMarkersGroupsFragment) fragment;
			} else if (fragment instanceof MapMarkersHistoryFragment) {
				historyFragment = (MapMarkersHistoryFragment) fragment;
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
		Fragment orderByFragment = fragmentManager.findFragmentByTag(OrderByBottomSheetDialogFragment.TAG);
		if (orderByFragment != null) {
			((OrderByBottomSheetDialogFragment) orderByFragment).setListener(createOrderByFragmentListener());
		}
		Fragment saveAsTrackFragment = fragmentManager.findFragmentByTag(SaveAsTrackBottomSheetDialogFragment.TAG);
		if (saveAsTrackFragment != null) {
			((SaveAsTrackBottomSheetDialogFragment) saveAsTrackFragment).setListener(createSaveAsTrackFragmentListener());
		}
		Fragment coordinateInputDialog = fragmentManager.findFragmentByTag(CoordinateInputDialogFragment.TAG);
		if (coordinateInputDialog != null) {
			((CoordinateInputDialogFragment) coordinateInputDialog).setListener(this::updateAdapters);
		}

		View mainView = inflate(R.layout.fragment_map_markers_dialog, container, false);

		Toolbar toolbar = mainView.findViewById(R.id.map_markers_toolbar);
		int icArrowBackId = AndroidUtils.getNavigationIconResId(app);
		int icColor = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Drawable icArrowBack = getIcon(icArrowBackId, icColor);
		toolbar.setNavigationIcon(icArrowBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(view -> dismiss());

		viewPager = mainView.findViewById(R.id.map_markers_view_pager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.setSwipeLocked(true);
		MapMarkersViewPagerAdapter adapter = new MapMarkersViewPagerAdapter(getChildFragmentManager());
		viewPager.setAdapter(adapter);

		progressBar = mainView.findViewById(R.id.progress_bar);

		TextView toolbarTitle = mainView.findViewById(R.id.map_markers_toolbar_title);
		bottomNav = mainView.findViewById(R.id.map_markers_bottom_navigation);
		toolbarTitle.setTextColor(getColor(!nightMode ? R.color.active_buttons_and_links_text_light : R.color.text_color_primary_dark));
		bottomNav.setItemIconTintList(getColorStateList(!nightMode ? R.color.bottom_navigation_color_selector_light : R.color.bottom_navigation_color_selector_dark));
		bottomNav.setItemTextColor(getColorStateList(!nightMode ? R.color.bottom_navigation_color_selector_light : R.color.bottom_navigation_color_selector_dark));
		if (groupIdToOpen != null) {
			activeFragment.stopLocationUpdate();
			groupsFragment.startLocationUpdate();
			groupsFragment.setGroupIdToOpen(groupIdToOpen);
			viewPager.setCurrentItem(GROUPS_POSITION, false);
			bottomNav.getMenu().findItem(R.id.action_groups).setChecked(true);
		}
		bottomNav.setOnItemSelectedListener(menuItem -> {
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
		});
		bottomNav.setOnItemReselectedListener(menuItem -> {
			if (menuItem.getItemId() == R.id.action_more) {
				dismissOptionsMenuFragment();
			}
		});

		return mainView;
	}

		@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.map_markers_bottom_navigation);
		return ids;
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getMapMarkersHelper().addSyncListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getMapMarkersHelper().removeSyncListener(this);
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

	public void setupBlurStatusBar() {
		Dialog dialog = getDialog();
		Window window = dialog != null ? dialog.getWindow() : null;
		if (window != null) {
			int colorId = nightMode ? R.color.status_bar_dim_dark : R.color.status_bar_dim_light;
			statusBarColor = AndroidUiHelper.setStatusBarColor(window, getColor(colorId));
		}
	}

	public void restoreStatusBarColor() {
		Dialog dialog = getDialog();
		Window window = dialog != null ? dialog.getWindow() : null;
		if (statusBarColor != -1 && window != null) {
			AndroidUiHelper.setStatusBarColor(window, statusBarColor);
		}
	}

	private void showOptionsMenuFragment() {
		boolean group = viewPager.getCurrentItem() == GROUPS_POSITION;
		boolean history = viewPager.getCurrentItem() == HISTORY_MARKERS_POSITION;
		OptionsBottomSheetDialogFragment.showInstance(
				getChildFragmentManager(), group, history, createOptionsFragmentListener());
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
			int id = switch (viewPager.getCurrentItem()) {
				case ACTIVE_MARKERS_POSITION -> R.id.action_active;
				case GROUPS_POSITION -> R.id.action_groups;
				case HISTORY_MARKERS_POSITION -> R.id.action_history;
				default -> -1;
			};
			if (id != -1) {
				bottomNav.getMenu().findItem(id).setChecked(true);
			}
		}
	}

	@Nullable
	private ColorStateList getColorStateList(int colorId) {
		return ContextCompat.getColorStateList(getApp(), colorId);
	}

	@NonNull
	private MarkerOptionsFragmentListener createOptionsFragmentListener() {
		return new MarkerOptionsFragmentListener() {

			@Override
			public void sortByOnClick() {
				if (getActivity() != null) {
					OrderByBottomSheetDialogFragment.showInstance(
							getChildFragmentManager(), createOrderByFragmentListener());
				}
			}

			@Override
			public void showDirectionOnClick() {
				if (getActivity() != null) {
					DirectionIndicationDialogFragment.showInstance(getChildFragmentManager());
				}
			}

			@Override
			public void coordinateInputOnClick() {
				if (getActivity() != null) {
					CoordinateInputDialogFragment.showInstance(
							getChildFragmentManager(), MapMarkersDialogFragment.this::updateAdapters);
				}
			}

			@Override
			public void buildRouteOnClick() {
				final MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					if (app.getMapMarkersHelper().getMapMarkers().isEmpty()) {
						app.showShortToastMessage(R.string.plan_route_no_markers_toast);
					} else {
						PlanRouteFragment.showInstance(mapActivity);
						MapMarkersDialogFragment.this.dismiss();
					}
				}
			}

			@Override
			public void saveAsNewTrackOnClick() {
				if (getActivity() != null) {
					if (app.getMapMarkersHelper().getMapMarkers().isEmpty()) {
						app.showShortToastMessage(R.string.plan_route_no_markers_toast);
					} else {
						SaveAsTrackBottomSheetDialogFragment.showInstance(
								getChildFragmentManager(), createSaveAsTrackFragmentListener());
					}
				}
			}

			@Override
			public void moveAllToHistoryOnClick() {
				if (getActivity() != null) {
					MapMarkersHelper helper = app.getMapMarkersHelper();
					List<MapMarker> markers = new ArrayList<>(helper.getMapMarkers());
					helper.moveAllActiveMarkersToHistory();
					if (viewPager.getCurrentItem() == ACTIVE_MARKERS_POSITION) {
						activeFragment.updateAdapter();
					} else {
						groupsFragment.updateAdapter();
					}
					snackbar = Snackbar.make(viewPager, R.string.all_markers_moved_to_history, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, view -> {
								helper.restoreMarkersFromHistory(markers);
								if (viewPager.getCurrentItem() == ACTIVE_MARKERS_POSITION) {
									activeFragment.updateAdapter();
								} else {
									groupsFragment.updateAdapter();
								}
							});
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			}

			@Override
			public void dismiss() {
				restoreSelectedNavItem();
			}
		};
	}

	@NonNull
	private MarkerSaveAsTrackFragmentListener createSaveAsTrackFragmentListener() {
		return fileName -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				String gpxPath = app.getMapMarkersHelper().getDataHelper().saveMarkersToFile(fileName);
				snackbar = Snackbar.make(viewPager, String.format(getString(R.string.shared_string_file_is_saved), fileName) + ".", Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_show, view -> TrackMenuFragment.openTrack(mapActivity, new File(gpxPath), null));
				UiUtilities.setupSnackbar(snackbar, nightMode);
				snackbar.show();
			}
		};
	}

	@NonNull
	private OrderByFragmentListener createOrderByFragmentListener() {
		return sortByMode -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
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

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		return showInstance(mapActivity, null);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, String groupIdToOpen) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			MapMarkersDialogFragment fragment = new MapMarkersDialogFragment();
			fragment.setGroupIdToOpen(groupIdToOpen);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}

	private class MapMarkersViewPagerAdapter extends FragmentPagerAdapter {

		private final List<Fragment> fragments;

		MapMarkersViewPagerAdapter(FragmentManager fm) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			fragments = Arrays.asList(activeFragment, groupsFragment, historyFragment);
		}

		@NonNull
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