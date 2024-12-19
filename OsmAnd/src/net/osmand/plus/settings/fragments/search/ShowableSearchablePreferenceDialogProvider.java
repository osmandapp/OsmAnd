package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import java.util.Optional;

@FunctionalInterface
public interface ShowableSearchablePreferenceDialogProvider {

	// Fk-TODO: make Fragment parameter an Optional<Fragment>
	Optional<ShowableSearchablePreferenceDialog<?>> getShowableSearchablePreferenceDialog(Preference preference, Fragment target);
}
