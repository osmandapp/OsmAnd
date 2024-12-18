package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import java.util.Optional;

public class PreferenceDialogs {

	public static <T extends Fragment & ShowableSearchablePreferenceDialogProvider> boolean showDialogForPreference(
			final Preference preference,
			final T target) {
		final Optional<ShowableSearchablePreferenceDialog<?>> preferenceDialog = target.getShowableSearchablePreferenceDialog(preference, target);
		if (preferenceDialog.isPresent()) {
			preferenceDialog.get().show();
			return true;
		}
		return false;
	}
}
