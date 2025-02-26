package net.osmand.plus.settings.fragments.search;

import android.view.View;

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
	}

	private void scrollToSetting(final Setting setting) {
		dashboardOnMap.applyScrollPosition(
				dashboardOnMap.getMainScrollView(),
				getYOffsetOfChildWithinContainer(
						getViewForSetting(setting),
						dashboardOnMap.getMainScrollView()));
	}

	private View getViewForSetting(final Setting setting) {
		return configureMapFragment
				.getViewAtPosition(configureMapFragment.getPositionOfSetting(setting).orElseThrow())
				.orElseThrow();
	}

	private static int getYOffsetOfChildWithinContainer(final View child, final View container) {
		return child == container ?
				0 :
				child.getTop() + getYOffsetOfChildWithinContainer((View) child.getParent(), container);
	}
}
