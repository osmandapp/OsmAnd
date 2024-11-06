package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.PreferenceMarker.isPreferenceConnectedToPlugin;

import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class IncludePreferenceInSearchResultsPredicate implements de.KnollFrank.lib.settingssearch.provider.IncludePreferenceInSearchResultsPredicate {

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferencePOJO preference, final Class<? extends PreferenceFragmentCompat> hostOfPreference) {
		return !isPreferenceConnectedToAnInactivePlugin(preference, hostOfPreference);
	}

	private static boolean isPreferenceConnectedToAnInactivePlugin(final SearchablePreferencePOJO preference, final Class<? extends PreferenceFragmentCompat> hostOfPreference) {
		// FK-TODO: handle also all the other plugins besides OsmandDevelopmentPlugin
		return isPreferenceConnectedToInactivePlugin(
				preference,
				hostOfPreference,
				DevelopmentSettingsFragment.class,
				OsmandDevelopmentPlugin.class);
	}

	private static boolean isPreferenceConnectedToInactivePlugin(
			final SearchablePreferencePOJO preference,
			final Class<? extends PreferenceFragmentCompat> hostOfPreference,
			final Class<? extends PreferenceFragmentCompat> preferenceFragment,
			final Class<? extends OsmandPlugin> plugin) {
		return (preferenceFragment.equals(hostOfPreference) || isPreferenceConnectedToPlugin(preference, plugin)) &&
				!PluginsHelper.isActive(plugin);
	}
}
