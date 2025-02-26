package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.IntentHelper;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.results.*;

class ShowSettingsFragmentAndHighlightSetting implements de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting {

	private final de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting delegate;

	public ShowSettingsFragmentAndHighlightSetting(final @IdRes int fragmentContainerViewId) {
		this.delegate = new DefaultShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId);
	}

	@Override
	public void showSettingsFragmentAndHighlightSetting(final FragmentActivity activity, final Fragment settingsFragment, final SearchablePreference setting2Highlight) {
		if (activity instanceof final MapActivity mapActivity && settingsFragment instanceof ConfigureMapFragment) {
			showConfigureMapDashboardAndHighlightSetting(mapActivity, asSetting(setting2Highlight));
		} else {
			delegate.showSettingsFragmentAndHighlightSetting(activity, settingsFragment, setting2Highlight);
		}
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

	private void showConfigureMapDashboardAndHighlightSetting(final MapActivity mapActivity,
															  final Setting setting) {
		IntentHelper.showConfigureMapDashboard(mapActivity);
		final ConfigureMapFragment configureMapFragment = getConfigureMapFragment(mapActivity);
		scrollToSetting(mapActivity.getDashboard(), configureMapFragment, setting);
		highlightSetting(configureMapFragment, setting);
	}

	private static void scrollToSetting(final DashboardOnMap dashboard,
										final ConfigureMapFragment configureMapFragment,
										final Setting setting) {
		dashboard.applyScrollPosition(
				dashboard.getMainScrollView(),
				getYOffsetOfChildWithinContainer(
						getViewForSetting(configureMapFragment, setting),
						dashboard.getMainScrollView()));
	}

	private static View getViewForSetting(final ConfigureMapFragment configureMapFragment, final Setting setting) {
		return configureMapFragment
				.getViewAtPosition(configureMapFragment.getPositionOfSetting(setting).orElseThrow())
				.orElseThrow();
	}

	private ConfigureMapFragment getConfigureMapFragment(final MapActivity mapActivity) {
		return (ConfigureMapFragment) mapActivity.getSupportFragmentManager().findFragmentByTag(ConfigureMapFragment.TAG);
	}

	private static void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		if (settingsFragment instanceof final SettingHighlighterProvider settingHighlighterProvider) {
			settingHighlighterProvider
					.getSettingHighlighter()
					.highlightSetting(settingsFragment, setting);
		}
	}

	private static int getYOffsetOfChildWithinContainer(final View child, final View container) {
		return child == container ?
				0 :
				child.getTop() + getYOffsetOfChildWithinContainer((View) child.getParent(), container);
	}
}
