package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.YOffsetOfChildWithinContainerProvider.getYOffsetOfChildWithinContainer;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.transport.TransportLinesFragment;

import java.util.Objects;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.results.*;

class ShowSettingsFragmentAndHighlightSetting implements de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting {

	private final de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting delegate;

	public ShowSettingsFragmentAndHighlightSetting(final @IdRes int fragmentContainerViewId) {
		delegate = new DefaultShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId);
	}

	@Override
	public void showSettingsFragmentAndHighlightSetting(final FragmentActivity activity,
														final Fragment settingsFragment,
														final SearchablePreference setting2Highlight) {
		if (activity instanceof final MapActivity mapActivity) {
			final boolean handled =
					showSettingsFragmentOfMapActivityAndHighlightSetting(
							mapActivity,
							settingsFragment,
							asSetting(setting2Highlight));
			if (handled) {
				return;
			}
		}
		delegate.showSettingsFragmentAndHighlightSetting(activity, settingsFragment, setting2Highlight);
	}

	private static boolean showSettingsFragmentOfMapActivityAndHighlightSetting(final MapActivity mapActivity,
																				final Fragment settingsFragment,
																				final Setting setting) {
		if (settingsFragment instanceof ConfigureMapFragment) {
			final ConfigureMapFragment configureMapFragment = getConfigureMapFragment(mapActivity);
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			scrollDashboardOnMapToSettingOfConfigureMapFragment(
					mapActivity.getDashboard(),
					setting,
					configureMapFragment);
			configureMapFragment
					.getSettingHighlighter()
					.highlightSetting(configureMapFragment, setting);
			return true;
		}
		if (settingsFragment instanceof final DetailsBottomSheet detailsBottomSheet) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			detailsBottomSheet.showNow(mapActivity.getSupportFragmentManager());
			detailsBottomSheet
					.getSettingHighlighter()
					.highlightSetting(detailsBottomSheet, setting);
			return true;
		}
		if (settingsFragment instanceof final TransportLinesFragment transportLinesFragment) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			transportLinesFragment.showNow(mapActivity.getSupportFragmentManager());
			transportLinesFragment
					.getSettingHighlighter()
					.highlightSetting(transportLinesFragment, setting);
			return true;
		}
		return false;
	}

	private static Setting asSetting(final SearchablePreference preference) {
		return new Setting() {

			@Override
			public String getKey() {
				return preference.getKey();
			}

			@Override
			public boolean hasPreferenceMatchWithinSearchableInfo() {
				return preference.hasPreferenceMatchWithinSearchableInfo();
			}
		};
	}

	private static ConfigureMapFragment getConfigureMapFragment(final MapActivity mapActivity) {
		return (ConfigureMapFragment) Objects.requireNonNull(mapActivity.getSupportFragmentManager().findFragmentByTag(ConfigureMapFragment.TAG));
	}

	private static void scrollDashboardOnMapToSettingOfConfigureMapFragment(final DashboardOnMap dashboardOnMap,
																			final Setting setting,
																			final ConfigureMapFragment configureMapFragment) {
		dashboardOnMap.applyScrollPosition(
				dashboardOnMap.getMainScrollView(),
				getYOffsetOfChildWithinContainer(
						configureMapFragment.getView(setting),
						dashboardOnMap.getMainScrollView()));
	}
}
