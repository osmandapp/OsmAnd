package net.osmand.plus.helpers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab.ADDRESS;
import static net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab.CATEGORIES;
import static net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab.HISTORY;
import static net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType.REGULAR;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.BackStackEntry;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;

import net.osmand.PlatformUtil;
import net.osmand.SecondSplashScreenFragment;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.XMasDialogFragment;
import net.osmand.plus.dialogs.selectlocation.SelectLocationFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.mapmarkers.PlanRouteFragment;
import net.osmand.plus.measurementtool.GpxApproximationFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.SnapTrackWarningFragment;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.plugins.rastermaps.DownloadTilesFragment;
import net.osmand.plus.plugins.weather.dialogs.WeatherForecastFragment;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ConfigureProfileFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class MapFragmentsHelper implements OnPreferenceStartFragmentCallback {

	private static final Log LOG = PlatformUtil.getLog(MapFragmentsHelper.class);

	public static final String CLOSE_ALL_FRAGMENTS = "close_all_fragments";

	private final MapActivity activity;

	public MapFragmentsHelper(@NonNull MapActivity activity) {
		this.activity = activity;
	}

	@NonNull
	public FragmentManager getSupportFragmentManager() {
		return activity.getSupportFragmentManager();
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
	}

	@Nullable
	public BaseFullScreenFragment getVisibleBaseFullScreenFragment(int... ids) {
		for (int id : ids) {
			Fragment fragment = getSupportFragmentManager().findFragmentById(id);
			if (fragment != null && !fragment.isRemoving() && fragment instanceof BaseFullScreenFragment
					&& ((BaseFullScreenFragment) fragment).getStatusBarColorId() != -1) {
				return (BaseFullScreenFragment) fragment;
			}
		}
		return null;
	}

	@Nullable
	public BaseSettingsFragment getVisibleBaseSettingsFragment(int... ids) {
		for (int id : ids) {
			Fragment fragment = getSupportFragmentManager().findFragmentById(id);
			if (fragment != null && !fragment.isRemoving() && fragment instanceof BaseSettingsFragment
					&& ((BaseSettingsFragment) fragment).getStatusBarColorId() != -1) {
				return (BaseSettingsFragment) fragment;
			}
		}
		return null;
	}

	@NonNull
	public List<Fragment> getActiveTalkbackFragments() {
		List<Fragment> allFragments = getSupportFragmentManager().getFragments();
		List<Fragment> fragmentForTalkBack = new ArrayList<>();
		for (Fragment fragment : allFragments) {
			if (!(fragment instanceof DashBaseFragment)) {
				fragmentForTalkBack.add(fragment);
			}
		}
		return fragmentForTalkBack;
	}

	public void updateFragments() {
		FragmentManager manager = getSupportFragmentManager();
		for (Fragment fragment : manager.getFragments()) {
			updateFragment(manager, fragment);
		}
		DashboardOnMap dashboard = activity.getDashboard();
		if (dashboard.isVisible() && !dashboard.isCurrentTypeHasIndividualFragment()) {
			dashboard.refreshContent(true);
		}
	}

	public void updateFragment(@NonNull FragmentManager manager, @NonNull Fragment fragment) {
		try {
			manager.beginTransaction().detach(fragment).commitAllowingStateLoss();
			manager.beginTransaction().attach(fragment).commitAllowingStateLoss();
		} catch (IllegalStateException e) {
			LOG.error("Error updating fragment " + fragment.getClass().getSimpleName(), e);
		}
	}

	public void closeAllFragments() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		for (Fragment fragment : fragmentManager.getFragments()) {
			if (fragment instanceof DialogFragment) {
				((DialogFragment) fragment).dismiss();
			}
		}
		for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
			fragmentManager.popBackStack();
		}
	}

	public void onStop() {
		QuickSearchDialogFragment quickSearchFragment = getQuickSearchDialogFragment();
		if (quickSearchFragment != null && quickSearchFragment.isSearchHidden()) {
			quickSearchFragment.closeSearch();
		}
	}

	@Nullable
	public QuickSearchDialogFragment getQuickSearchDialogFragment() {
		return getFragment(QuickSearchDialogFragment.TAG);
	}

	@Nullable
	public ExplorePlacesFragment getExplorePlacesFragment() {
		return getFragment(ExplorePlacesFragment.Companion.getTAG());
	}

	@Nullable
	public PlanRouteFragment getPlanRouteFragment() {
		return getFragment(PlanRouteFragment.TAG);
	}

	@Nullable
	public MeasurementToolFragment getMeasurementToolFragment() {
		return getFragment(MeasurementToolFragment.TAG);
	}

	@Nullable
	public ChooseRouteFragment getChooseRouteFragment() {
		return getFragment(ChooseRouteFragment.TAG);
	}

	@Nullable
	public GpxApproximationFragment getGpxApproximationFragment() {
		return getFragment(GpxApproximationFragment.TAG);
	}

	@Nullable
	public SnapTrackWarningFragment getSnapTrackWarningBottomSheet() {
		return getFragment(SnapTrackWarningFragment.TAG);
	}

	@Nullable
	public TrackMenuFragment getTrackMenuFragment() {
		return getFragment(TrackMenuFragment.TAG);
	}

	@Nullable
	public TrackAppearanceFragment getTrackAppearanceFragment() {
		return getFragment(TrackAppearanceFragment.TAG);
	}

	@Nullable
	public GpsFilterFragment getGpsFilterFragment() {
		return getFragment(GpsFilterFragment.TAG);
	}

	@Nullable
	public DownloadTilesFragment getDownloadTilesFragment() {
		return getFragment(DownloadTilesFragment.TAG);
	}

	@Nullable
	public SelectLocationFragment getSelectMapLocationFragment() {
		if (getConfigureMapOptionFragment() instanceof SelectLocationFragment fragment) {
			return fragment;
		}
		return null;
	}

	@Nullable
	public ConfigureMapOptionFragment getConfigureMapOptionFragment() {
		return getFragment(ConfigureMapOptionFragment.TAG);
	}

	@Nullable
	public WeatherForecastFragment getWeatherForecastFragment() {
		return getFragment(WeatherForecastFragment.TAG);
	}

	public void dismissFragment(@Nullable String name) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void backToConfigureProfileFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		int backStackEntryCount = fragmentManager.getBackStackEntryCount();
		if (backStackEntryCount > 0 && !fragmentManager.isStateSaved()) {
			BackStackEntry entry = fragmentManager.getBackStackEntryAt(backStackEntryCount - 1);
			if (ConfigureProfileFragment.TAG.equals(entry.getName())) {
				fragmentManager.popBackStack();
			}
		}
	}

	@Nullable
	public FirstUsageWizardFragment getFirstUsageWizardFragment() {
		FirstUsageWizardFragment fragment = (FirstUsageWizardFragment) getSupportFragmentManager()
				.findFragmentByTag(FirstUsageWizardFragment.TAG);
		return fragment != null && !fragment.isDetached() ? fragment : null;
	}

	public void disableFirstUsageFragment() {
		FirstUsageWizardFragment wizardFragment = getFirstUsageWizardFragment();
		if (wizardFragment != null) {
			wizardFragment.closeWizard();
		}
	}

	@MainThread
	public boolean removeFragment(String tag) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(tag);
		if (fragment != null) {
			fm.beginTransaction()
					.remove(fragment)
					.commitNowAllowingStateLoss();
			return true;
		}
		return false;
	}

	public void dismissSettingsScreens() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(DRAWER_SETTINGS_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		try {
			FragmentManager manager = getSupportFragmentManager();
			String fragmentName = pref.getFragment();
			Fragment fragment = manager.getFragmentFactory().instantiate(activity.getClassLoader(), fragmentName);
			if (caller instanceof BaseSettingsFragment) {
				fragment.setArguments(((BaseSettingsFragment) caller).buildArguments());
			}
			String tag = fragment.getClass().getName();
			if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
				manager.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(DRAWER_SETTINGS_ID)
						.commitAllowingStateLoss();
				return true;
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}

	public boolean isFragmentVisible() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (!(fragment instanceof DashBaseFragment) && fragment.isVisible()
					|| activity.getDashboard().isVisible()) {
				return true;
			}
		}
		return false;
	}

	public void dismissCardDialog() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(ContextMenuCardDialogFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void showXMasDialog() {
		SecondSplashScreenFragment.SHOW = false;
		dismissSecondSplashScreen();
		XMasDialogFragment.showInstance(getSupportFragmentManager());
	}

	public void dismissSecondSplashScreen() {
		if (SecondSplashScreenFragment.VISIBLE) {
			SecondSplashScreenFragment.VISIBLE = false;
			SecondSplashScreenFragment.SHOW = false;
			removeFragment(SecondSplashScreenFragment.TAG);
			activity.applyScreenOrientation();
		}
	}

	public boolean isFirstScreenShowing() {
		return getFirstUsageWizardFragment() != null;
	}

	public void closeQuickSearch() {
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.closeSearch();
			activity.refreshMap();
		}
	}

	public void closeExplore() {
		ExplorePlacesFragment fragment = getExplorePlacesFragment();
		if (fragment != null) {
			fragment.closeFragment();
		}
	}

	public void showQuickSearch(double latitude, double longitude) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			activity.refreshMap();
		}
		QuickSearchDialogFragment.showInstance(activity, "", null,
				REGULAR, CATEGORIES, new LatLon(latitude, longitude));
	}

	public void showQuickSearch(String searchQuery) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			activity.refreshMap();
		}
		QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
				REGULAR, CATEGORIES, null);
	}

	public void showQuickSearch(Object object) {
		showQuickSearch(object, null);
	}

	public void showQuickSearch(Object object, @Nullable LatLon latLon) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			activity.refreshMap();
		}
		QuickSearchDialogFragment.showInstance(activity, "", object,
				REGULAR, CATEGORIES, latLon);
	}

	public void showQuickSearch(ShowQuickSearchMode mode, boolean showCategories) {
		showQuickSearch(mode, showCategories, "", null);
	}

	public void showQuickSearch(ShowQuickSearchMode mode, QuickSearchTab showSearchTab) {
		showQuickSearch(mode, showSearchTab, "", null);
	}

	public void showQuickSearch(@NonNull ShowQuickSearchMode mode, boolean showCategories,
	                            @NonNull String searchQuery, @Nullable LatLon searchLocation) {
		if (mode == ShowQuickSearchMode.CURRENT) {
			activity.getContextMenu().close();
		} else {
			hideVisibleMenu();
		}
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (mode.isPointSelection()) {
			if (fragment != null) {
				fragment.dismiss();
			}
			QuickSearchType searchType = null;
			switch (mode) {
				case START_POINT_SELECTION:
					searchType = QuickSearchType.START_POINT;
					break;
				case DESTINATION_SELECTION:
					searchType = QuickSearchType.DESTINATION;
					break;
				case DESTINATION_SELECTION_AND_START:
					searchType = QuickSearchType.DESTINATION_AND_START;
					break;
				case INTERMEDIATE_SELECTION:
					searchType = QuickSearchType.INTERMEDIATE;
					break;
				case HOME_POINT_SELECTION:
					searchType = QuickSearchType.HOME_POINT;
					break;
				case WORK_POINT_SELECTION:
					searchType = QuickSearchType.WORK_POINT;
					break;
			}
			if (searchType != null) {
				QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
						searchType, showCategories ? CATEGORIES : ADDRESS, searchLocation);
			}
		} else if (fragment != null) {
			if (mode == ShowQuickSearchMode.NEW
					|| (mode == ShowQuickSearchMode.NEW_IF_EXPIRED && fragment.isExpired())) {
				fragment.dismiss();
				QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
						REGULAR, showCategories ? CATEGORIES : HISTORY, searchLocation);
			} else {
				fragment.show();
			}
			activity.refreshMap();
		} else {
			QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
					REGULAR, showCategories ? CATEGORIES : HISTORY, searchLocation);
		}
	}

	public void showQuickSearch(@NonNull ShowQuickSearchMode mode, QuickSearchTab showSearchTab,
	                            @NonNull String searchQuery, @Nullable LatLon searchLocation) {
		if (mode == ShowQuickSearchMode.CURRENT) {
			activity.getContextMenu().close();
		} else {
			hideVisibleMenu();
		}
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (mode.isPointSelection()) {
			if (fragment != null) {
				fragment.dismiss();
			}
			QuickSearchType searchType = null;
			switch (mode) {
				case START_POINT_SELECTION:
					searchType = QuickSearchType.START_POINT;
					break;
				case DESTINATION_SELECTION:
				case DESTINATION_SELECTION_AND_START:
					searchType = QuickSearchType.DESTINATION;
					break;
				case INTERMEDIATE_SELECTION:
					searchType = QuickSearchType.INTERMEDIATE;
					break;
				case HOME_POINT_SELECTION:
					searchType = QuickSearchType.HOME_POINT;
					break;
				case WORK_POINT_SELECTION:
					searchType = QuickSearchType.WORK_POINT;
					break;
			}
			QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
					searchType, showSearchTab, searchLocation);
		} else if (fragment != null) {
			if (mode == ShowQuickSearchMode.NEW
					|| (mode == ShowQuickSearchMode.NEW_IF_EXPIRED && fragment.isExpired())) {
				fragment.dismiss();
				QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
						REGULAR, showSearchTab, searchLocation);
			} else {
				fragment.show();
			}
			activity.refreshMap();
		} else {
			QuickSearchDialogFragment.showInstance(activity, searchQuery, null,
					REGULAR, showSearchTab, searchLocation);
		}
	}

	public void showSettings() {
		dismissSettingsScreens();
		BaseSettingsFragment.showInstance(activity, SettingsScreenType.MAIN_SETTINGS);
	}

	private void hideVisibleMenu() {
		MapContextMenu contextMenu = activity.getContextMenu();
		if (contextMenu.isVisible()) {
			contextMenu.hide();
		} else {
			MapMultiSelectionMenu multiSelectionMenu = contextMenu.getMultiSelectionMenu();
			if (multiSelectionMenu != null && multiSelectionMenu.isVisible()) {
				multiSelectionMenu.hide();
			} else if (getTrackMenuFragment() != null) {
				dismissFragment(TrackMenuFragment.TAG);
			}
		}
	}
}