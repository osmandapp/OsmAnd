package net.osmand.plus.settings.fragments.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
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
		scrollToSetting(mapActivity, setting);
		highlightSetting(
				getFragment(mapActivity, ConfigureMapFragment.TAG),
				setting);
	}

	private static void scrollToSetting(final MapActivity mapActivity, final Setting setting) {
		mapActivity
				.getDashboard()
				.applyScrollPosition(
						mapActivity.getDashboard().getMainScrollView(),
						// FK-TODO: remove constant 3380
						3380);
	}

	private Fragment getFragment(final MapActivity mapActivity, final String tag) {
		return mapActivity.getSupportFragmentManager().findFragmentByTag(tag);
	}

	private static void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		if (settingsFragment instanceof final SettingHighlighterProvider settingHighlighterProvider) {
			settingHighlighterProvider
					.getSettingHighlighter()
					.highlightSetting(settingsFragment, setting);
		}
	}
}
