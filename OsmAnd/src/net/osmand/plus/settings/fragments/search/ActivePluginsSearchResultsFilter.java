package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;
import de.KnollFrank.lib.settingssearch.results.SearchResultsFilter;

class ActivePluginsSearchResultsFilter implements SearchResultsFilter {

	private final Map<SearchablePreferenceOfHostWithinTree, PreferencePath> preferencePathByPreference = new HashMap<>();

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferenceOfHostWithinTree preference) {
		return !isPreferenceConnectedToAnyInactivePlugin(preference);
	}

	private boolean isPreferenceConnectedToAnyInactivePlugin(final SearchablePreferenceOfHostWithinTree preference) {
		return ActivePluginsSearchResultsFilter
				.getInactivePlugins()
				.stream()
				.anyMatch(inactivePlugin -> isPreferenceConnectedToPlugin(preference, inactivePlugin));
	}

	private static List<OsmandPlugin> getInactivePlugins() {
		return PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin -> !plugin.isActive())
				.toList();
	}

	private boolean isPreferenceConnectedToPlugin(final SearchablePreferenceOfHostWithinTree preference,
												  final OsmandPlugin plugin) {
		return isPreferenceOnSettingsScreen(preference, Optional.ofNullable(plugin.getSettingsScreenType())) ||
				isPreferencePathConnectedToPlugin(getPreferencePath(preference), plugin) ||
				isMapSourcePreferenceConnectedToPlugin(preference, plugin);
	}

	private static boolean isPreferenceOnSettingsScreen(final SearchablePreferenceOfHostWithinTree preference,
														final Optional<SettingsScreenType> settingsScreenType) {
		return settingsScreenType
				.map(_settingsScreenType -> isPreferenceOnSettingsScreen(preference, _settingsScreenType))
				.orElse(false);
	}

	private static boolean isPreferenceOnSettingsScreen(final SearchablePreferenceOfHostWithinTree preference,
														final SettingsScreenType settingsScreenType) {
		return settingsScreenType.fragmentClass.equals(preference.hostOfPreference().host().fragment());
	}

	private static boolean isPreferencePathConnectedToPlugin(final PreferencePath preferencePath,
															 final OsmandPlugin plugin) {
		return preferencePath
				.preferences()
				.stream()
				.anyMatch(preference -> PreferenceMarker.isPreferenceConnectedToPlugin(preference, plugin.getClass()));
	}

	private boolean isMapSourcePreferenceConnectedToPlugin(final SearchablePreferenceOfHostWithinTree preference,
														   final OsmandPlugin plugin) {
		return plugin instanceof OsmandRasterMapsPlugin && isMapSourcePreference(preference);
	}

	private boolean isMapSourcePreference(final SearchablePreferenceOfHostWithinTree preference) {
		return this
				.getPreferencePath(preference)
				.preferences()
				.stream()
				// FK-FIXME: "en-" ist zu speziell
				.anyMatch(_preference -> _preference.hostOfPreference().id().startsWith("en-net.osmand.plus.widgets.alert.MapLayerSelectionDialogFragment$MapLayerSelectionDialogFragmentProxy"));
	}

	private PreferencePath getPreferencePath(final SearchablePreferenceOfHostWithinTree preference) {
		return preferencePathByPreference.computeIfAbsent(
				preference,
				SearchablePreferenceOfHostWithinTree::getPreferencePath);
	}
}
