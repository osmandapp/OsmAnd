package net.osmand.plus.settings.fragments.search;

import androidx.annotation.Nullable;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import java.util.stream.Stream;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;

// FK-TODO: merge into SearchResultsFilter
class IncludePreferenceInSearchResultsPredicate {

	public boolean includePreferenceInSearchResults(final SearchablePreferenceOfHostWithinTree preference) {
		return !isPreferenceConnectedToAnyInactivePlugin(preference);
	}

	private static boolean isPreferenceConnectedToAnyInactivePlugin(final SearchablePreferenceOfHostWithinTree preference) {
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

	private static boolean isPreferenceConnectedToPlugin(final SearchablePreferenceOfHostWithinTree preference,
														 final OsmandPlugin plugin) {
		return isPreferenceOnSettingsScreen(preference, plugin.getSettingsScreenType()) ||
				PreferenceMarker.isPreferenceConnectedToPlugin(preference, plugin.getClass());
	}

	private static boolean isPreferenceOnSettingsScreen(final SearchablePreferenceOfHostWithinTree preference,
														final @Nullable SettingsScreenType settingsScreenType) {
		return settingsScreenType != null && settingsScreenType.fragmentClass.equals(preference.hostOfPreference().host());
	}
}
