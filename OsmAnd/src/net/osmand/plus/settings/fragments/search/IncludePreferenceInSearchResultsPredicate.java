package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;

class IncludePreferenceInSearchResultsPredicate implements de.KnollFrank.lib.settingssearch.provider.IncludePreferenceInSearchResultsPredicate {

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreference preference) {
		return !isPreferenceConnectedToAnyInactivePlugin(preference);
	}

	private static boolean isPreferenceConnectedToAnyInactivePlugin(final SearchablePreference preference) {
		return IncludePreferenceInSearchResultsPredicate
				.getInactivePlugins()
				.anyMatch(inactivePlugin -> isPreferenceConnectedToPlugin(preference, inactivePlugin));
	}

	private static Stream<OsmandPlugin> getInactivePlugins() {
		return PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin -> !plugin.isActive());
	}

	private static boolean isPreferenceConnectedToPlugin(final SearchablePreference preference,
														 final OsmandPlugin plugin) {
		return isPreferenceOnSettingsScreen(preference, plugin.getSettingsScreenType()) ||
				PreferenceMarker.isPreferenceConnectedToPlugin(preference, plugin.getClass());
	}

	private static boolean isPreferenceOnSettingsScreen(final SearchablePreference preference,
														final @Nullable SettingsScreenType settingsScreenType) {
		return settingsScreenType != null && settingsScreenType.fragmentClass.equals(preference.getHost());
	}
}
