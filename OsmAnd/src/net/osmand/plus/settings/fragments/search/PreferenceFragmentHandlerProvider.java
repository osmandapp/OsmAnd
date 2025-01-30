package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import java.util.Optional;

@FunctionalInterface
public interface PreferenceFragmentHandlerProvider {

	Optional<PreferenceFragmentHandler> getPreferenceFragmentHandler(Preference preference);
}
