package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.YOffsetOfChildWithinContainerProvider.getYOffsetOfChildWithinContainer;

import android.view.View;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.MapModeFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.widgets.alert.SelectionDialogFragment;

import java.util.Objects;
import java.util.function.Consumer;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;
import de.KnollFrank.lib.settingssearch.results.DefaultShowSettingsFragmentAndHighlightSetting;
import de.KnollFrank.lib.settingssearch.results.Setting;

class ShowSettingsFragmentAndHighlightSetting implements de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting {

	private final de.KnollFrank.lib.settingssearch.results.ShowSettingsFragmentAndHighlightSetting delegate;

	public ShowSettingsFragmentAndHighlightSetting(final @IdRes int fragmentContainerViewId) {
		delegate = new DefaultShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId);
	}

	@Override
	public void showSettingsFragmentAndHighlightSetting(final FragmentActivity activity,
														final Fragment settingsFragment,
														final SearchablePreferenceOfHostWithinTree settingToHighlight) {
		if (activity instanceof final MapActivity mapActivity) {
			final boolean handled =
					showSettingsFragmentOfMapActivityAndHighlightSetting(
							mapActivity,
							settingsFragment,
							asSetting(settingToHighlight.searchablePreference()));
			if (handled) {
				return;
			}
		}
		delegate.showSettingsFragmentAndHighlightSetting(activity, settingsFragment, settingToHighlight);
	}

	private static boolean showSettingsFragmentOfMapActivityAndHighlightSetting(final MapActivity mapActivity,
																				final Fragment settingsFragment,
																				final Setting setting) {
		if (settingsFragment instanceof ConfigureMapFragment) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			final ConfigureMapFragment configureMapFragment = Objects.requireNonNull(ConfigureMapFragment.getVisibleInstance(mapActivity));
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
		if (settingsFragment instanceof final SelectMapStyleBottomSheetDialogFragment selectMapStyleBottomSheetDialogFragment) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			selectMapStyleBottomSheetDialogFragment.showNow(mapActivity.getSupportFragmentManager());
			selectMapStyleBottomSheetDialogFragment
					.getSettingHighlighter()
					.highlightSetting(selectMapStyleBottomSheetDialogFragment, setting);
			return true;
		}
		if (settingsFragment instanceof final SelectionDialogFragment selectionDialogFragment) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			selectionDialogFragment.showNow(mapActivity.getSupportFragmentManager());
			execute(
					selectionDialogFragment.getListView(),
					listView -> listView.setSelection(selectionDialogFragment.getIndexedOf(setting)),
					listView -> selectionDialogFragment.getSettingHighlighter().highlightSetting(selectionDialogFragment, setting));
			return true;
		}
		if (settingsFragment instanceof final ConfigureMapDialogs.MapLanguageDialog mapLanguageDialog) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			mapLanguageDialog.showNow(mapActivity.getSupportFragmentManager(), null);
			execute(
					mapLanguageDialog.getListView(),
					listView -> listView.setSelection(mapLanguageDialog.getIndexedOf(setting)),
					listView -> mapLanguageDialog.getSettingHighlighter().highlightSetting(mapLanguageDialog, setting));
			return true;
		}
		if (settingsFragment instanceof final MapModeFragment mapModeFragment) {
			IntentHelper.showConfigureMapDashboard(mapActivity.getDashboard());
			mapModeFragment.show(mapActivity, true);
			mapActivity.getDashboard().hideDashboard();
			return true;
		}
		return false;
	}

	private static void execute(final ListView listView,
								final Consumer<ListView> doWithListView1,
								final Consumer<ListView> doWithListView2) {
		listView.addOnLayoutChangeListener(
				new View.OnLayoutChangeListener() {

					@Override
					public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom, final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
						listView.removeOnLayoutChangeListener(this);
						doWithListView2.accept(listView);
					}
				});
		doWithListView1.accept(listView);
		listView.requestLayout();
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
