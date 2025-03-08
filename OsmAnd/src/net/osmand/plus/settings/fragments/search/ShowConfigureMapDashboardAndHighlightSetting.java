package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.YOffsetOfChildWithinContainerProvider.getYOffsetOfChildWithinContainer;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.IntentHelper;

import java.util.Objects;

import de.KnollFrank.lib.settingssearch.results.Setting;

class ShowConfigureMapDashboardAndHighlightSetting {

	private final DashboardOnMap dashboardOnMap;
	private final ConfigureMapFragment configureMapFragment;

	public static ShowConfigureMapDashboardAndHighlightSetting from(final MapActivity mapActivity) {
		return new ShowConfigureMapDashboardAndHighlightSetting(
				mapActivity.getDashboard(),
				Objects.requireNonNull((ConfigureMapFragment) mapActivity.getSupportFragmentManager().findFragmentByTag(ConfigureMapFragment.TAG)));
	}

	private ShowConfigureMapDashboardAndHighlightSetting(final DashboardOnMap dashboardOnMap,
														 final ConfigureMapFragment configureMapFragment) {
		this.dashboardOnMap = dashboardOnMap;
		this.configureMapFragment = configureMapFragment;
	}

	public void showConfigureMapDashboardAndHighlightSetting(final Setting setting) {
		IntentHelper.showConfigureMapDashboard(dashboardOnMap);
		highlightSetting(setting);
	}

	private void highlightSetting(final Setting setting) {
		scrollToSetting(setting);
		configureMapFragment
				.getSettingHighlighter()
				.highlightSetting(configureMapFragment, setting);
		// showDialog(setting);
	}

	private void scrollToSetting(final Setting setting) {
		dashboardOnMap.applyScrollPosition(
				dashboardOnMap.getMainScrollView(),
				getYOffsetOfChildWithinContainer(
						configureMapFragment.getView(setting),
						dashboardOnMap.getMainScrollView()));
	}

	private void showDialog(final Setting setting) {
		if (setting.hasPreferenceMatchWithinSearchableInfo()) {
			configureMapFragment.getView(setting).performClick();
		}
	}
}
