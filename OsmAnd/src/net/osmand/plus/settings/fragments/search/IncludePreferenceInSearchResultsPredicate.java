package net.osmand.plus.settings.fragments.search;

import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class IncludePreferenceInSearchResultsPredicate implements de.KnollFrank.lib.settingssearch.provider.IncludePreferenceInSearchResultsPredicate {

	@Override
	public boolean includePreferenceOfHostInSearchResults(final SearchablePreferencePOJO preference, final Class<? extends PreferenceFragmentCompat> host) {
		if (isDevelopmentSettingsPreference(preference, host) && !PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
			return false;
		}
		return true;
	}

	private static final Set<Optional<String>> DEVELOPMENT_PREFERENCE_KEYS =
			// FK-TODO: use constants from RouteParametersFragment
			Set
					.of("development", "osmand.development", "pt_safe_mode")
					.stream()
					.map(Optional::of)
					.collect(Collectors.toSet());

	private static boolean isDevelopmentSettingsPreference(final SearchablePreferencePOJO preference, final Class<? extends PreferenceFragmentCompat> host) {
		return DEVELOPMENT_PREFERENCE_KEYS.contains(preference.key()) || DevelopmentSettingsFragment.class.equals(host);
	}
}
