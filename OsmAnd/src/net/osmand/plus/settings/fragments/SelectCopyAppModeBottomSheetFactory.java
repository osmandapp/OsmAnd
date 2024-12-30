package net.osmand.plus.settings.fragments;

import androidx.fragment.app.Fragment;

import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.fragments.search.ShowableSearchablePreferenceDialog;

import java.util.Optional;

public class SelectCopyAppModeBottomSheetFactory {

	public static ShowableSearchablePreferenceDialog<SelectCopyAppModeBottomSheet> createSelectCopyAppModeBottomSheet(
			final Optional<Fragment> target,
			final BaseSettingsFragment baseSettingsFragment) {
		return new ShowableSearchablePreferenceDialog<>(
				SelectCopyAppModeBottomSheet.createInstance(
						target.orElse(null),
						baseSettingsFragment.getSelectedAppMode())) {

			@Override
			protected void show(final SelectCopyAppModeBottomSheet selectCopyAppModeBottomSheet) {
				selectCopyAppModeBottomSheet.show(baseSettingsFragment.getFragmentManager());
			}
		};
	}
}
