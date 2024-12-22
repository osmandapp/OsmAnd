package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import java.util.Optional;

public class PreferenceDialogs {

	public static <T extends Fragment & ShowableSearchablePreferenceDialogProvider> boolean showDialogForPreference(
			final Preference preference,
			final T preferenceDialogProviderAndTarget) {
		final Optional<ShowableSearchablePreferenceDialog<?>> preferenceDialog =
				preferenceDialogProviderAndTarget.getShowableSearchablePreferenceDialog(
						preference,
						Optional.of(preferenceDialogProviderAndTarget));
		if (preferenceDialog.isPresent()) {
			preferenceDialog.get().show();
			return true;
		}
		return false;
	}
}
