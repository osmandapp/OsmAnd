package net.osmand.plus.widgets.alert;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Collection;

public class PreferenceScreenFactory {

	private final PreferenceFragmentCompat preferenceFragment;

	public PreferenceScreenFactory(final PreferenceFragmentCompat preferenceFragment) {
		this.preferenceFragment = preferenceFragment;
	}

	public PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
		final PreferenceScreen screen = preferenceFragment.getPreferenceManager().createPreferenceScreen(preferenceFragment.requireContext());
		screen.setTitle("screen title");
		screen.setSummary("screen summary");
		preferences.forEach(screen::addPreference);
		return screen;
	}
}

