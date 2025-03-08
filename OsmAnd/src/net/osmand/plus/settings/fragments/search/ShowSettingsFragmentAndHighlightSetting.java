package net.osmand.plus.settings.fragments.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dialogs.DetailsBottomSheet;
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
		if (activity instanceof final MapActivity mapActivity) {
			if (settingsFragment instanceof ConfigureMapFragment) {
				ShowConfigureMapDashboardAndHighlightSetting.from(mapActivity).showConfigureMapDashboardAndHighlightSetting(asSetting(setting2Highlight));
			} else if (settingsFragment instanceof final DetailsBottomSheet detailsBottomSheet) {
				IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
				detailsBottomSheet.show(mapActivity.getSupportFragmentManager());
				// FK-TODO: highlightSetting()
			}
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
}
