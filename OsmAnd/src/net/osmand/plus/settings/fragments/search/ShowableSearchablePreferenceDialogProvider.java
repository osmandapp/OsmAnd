package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import java.util.Optional;

@FunctionalInterface
public interface ShowableSearchablePreferenceDialogProvider {

	Optional<ShowableSearchablePreferenceDialog<?>> getShowableSearchablePreferenceDialog(Preference preference, Optional<Fragment> target);
}
