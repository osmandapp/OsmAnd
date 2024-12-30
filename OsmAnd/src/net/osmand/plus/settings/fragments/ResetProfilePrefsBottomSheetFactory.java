package net.osmand.plus.settings.fragments;

import androidx.fragment.app.Fragment;

import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.fragments.search.ShowableSearchablePreferenceDialog;

import java.util.Optional;

public class ResetProfilePrefsBottomSheetFactory {

	public static ShowableSearchablePreferenceDialog<ResetProfilePrefsBottomSheet> createResetProfilePrefsBottomSheet(
			final Optional<Fragment> target,
			final BaseSettingsFragment baseSettingsFragment) {
		return new ShowableSearchablePreferenceDialog<>(
				ResetProfilePrefsBottomSheet.createInstance(
						baseSettingsFragment.getSelectedAppMode(),
						target)) {

			@Override
			protected void show(final ResetProfilePrefsBottomSheet resetProfilePrefsBottomSheet) {
				resetProfilePrefsBottomSheet.show(baseSettingsFragment.getFragmentManager());
			}
		};
	}
}
