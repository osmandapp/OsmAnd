package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.PreferenceMarker.isPreferenceConnectedToPlugin;

import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class IncludePreferenceInSearchResultsPredicate implements de.KnollFrank.lib.settingssearch.provider.IncludePreferenceInSearchResultsPredicate {

	@Override
	public boolean includePreferenceOfHostInSearchResults(final SearchablePreferencePOJO preference, final Class<? extends PreferenceFragmentCompat> host) {
		// FK-TODO: handle also all the other plugins besides OsmandDevelopmentPlugin
		if (isDevelopmentSettingsPreference(preference, host) && !PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
			return false;
		}
		return true;
	}

	private static boolean isDevelopmentSettingsPreference(final SearchablePreferencePOJO preference,
														   final Class<? extends PreferenceFragmentCompat> host) {
		return DevelopmentSettingsFragment.class.equals(host) || isPreferenceConnectedToPlugin(preference, OsmandDevelopmentPlugin.class);
	}
}
