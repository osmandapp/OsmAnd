package net.osmand.plus.settings.fragments.search;

import android.view.View;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.IntentHelper;

import java.util.Objects;

import de.KnollFrank.lib.settingssearch.results.Setting;

class ShowConfigureMapDashboardAndHighlightSetting {

	private final MapActivity mapActivity;
	private final ConfigureMapFragment configureMapFragment;

	public static ShowConfigureMapDashboardAndHighlightSetting from(final MapActivity mapActivity) {
		return new ShowConfigureMapDashboardAndHighlightSetting(
				mapActivity,
				Objects.requireNonNull((ConfigureMapFragment) mapActivity.getSupportFragmentManager().findFragmentByTag(ConfigureMapFragment.TAG)));
	}

	private ShowConfigureMapDashboardAndHighlightSetting(final MapActivity mapActivity, final ConfigureMapFragment configureMapFragment) {
		this.mapActivity = mapActivity;
		this.configureMapFragment = configureMapFragment;
	}

	public void showConfigureMapDashboardAndHighlightSetting(final Setting setting) {
		IntentHelper.showConfigureMapDashboard(mapActivity);
		highlightSetting(setting);
	}

	private void highlightSetting(final Setting setting) {
		scrollToSetting(setting);
		configureMapFragment
				.getSettingHighlighter()
				.highlightSetting(configureMapFragment, setting);
	}

	private void scrollToSetting(final Setting setting) {
		final DashboardOnMap dashboard = mapActivity.getDashboard();
		dashboard.applyScrollPosition(
				dashboard.getMainScrollView(),
				getYOffsetOfChildWithinContainer(
						getViewForSetting(setting),
						dashboard.getMainScrollView()));
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
