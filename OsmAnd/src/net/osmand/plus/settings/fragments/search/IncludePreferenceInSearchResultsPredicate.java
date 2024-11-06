package net.osmand.plus.settings.fragments.search;

import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import javax.annotation.Nullable;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class IncludePreferenceInSearchResultsPredicate implements de.KnollFrank.lib.settingssearch.provider.IncludePreferenceInSearchResultsPredicate {

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferencePOJO preference,
													final Class<? extends PreferenceFragmentCompat> hostOfPreference) {
		return !isPreferenceConnectedToAnInactivePlugin(preference, hostOfPreference);
	}

	private static boolean isPreferenceConnectedToAnInactivePlugin(final SearchablePreferencePOJO preference,
																   final Class<? extends PreferenceFragmentCompat> hostOfPreference) {
		return PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin -> !plugin.isActive())
				.anyMatch(inactivePlugin -> isPreferenceConnectedToPlugin(preference, hostOfPreference, inactivePlugin));
	}

	private static boolean isPreferenceConnectedToPlugin(
			final SearchablePreferencePOJO preference,
			final Class<? extends PreferenceFragmentCompat> hostOfPreference,
			final OsmandPlugin plugin) {
		return isPreferenceOnSettingsScreen(hostOfPreference, plugin.getSettingsScreenType()) ||
				PreferenceMarker.isPreferenceConnectedToPlugin(preference, plugin.getClass());
	}

	private static boolean isPreferenceOnSettingsScreen(
			final Class<? extends PreferenceFragmentCompat> preferenceFragment,
			final @Nullable SettingsScreenType settingsScreenType) {
		return settingsScreenType != null && settingsScreenType.fragmentName.equals(preferenceFragment.getName());
	}
}
