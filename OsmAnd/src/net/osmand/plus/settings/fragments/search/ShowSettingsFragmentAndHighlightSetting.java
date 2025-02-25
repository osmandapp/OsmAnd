package net.osmand.plus.settings.fragments.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.results.DefaultShowSettingsFragmentAndHighlightSetting;

class ShowSettingsFragmentAndHighlightSetting implements de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting {

	private final de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting delegate;

	public ShowSettingsFragmentAndHighlightSetting(final @IdRes int fragmentContainerViewId) {
		this.delegate = new DefaultShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId);
	}

	@Override
	public void showSettingsFragmentAndHighlightSetting(final FragmentActivity activity, final Fragment settingsFragment, final SearchablePreference setting2Highlight) {
		if (activity instanceof final MapActivity mapActivity && settingsFragment instanceof ConfigureMapFragment) {
			mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONFIGURE_MAP, null);
		} else {
			delegate.showSettingsFragmentAndHighlightSetting(activity, settingsFragment, setting2Highlight);
		}
	}
}
