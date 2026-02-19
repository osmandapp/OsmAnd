package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.fragments.SettingsScreenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.common.LanguageCode;
import de.KnollFrank.lib.settingssearch.common.Strings;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.results.SearchResultsFilter;

class ActivePluginsSearchResultsFilter implements SearchResultsFilter {

	private final Map<SearchablePreferenceOfHostWithinTree, PreferencePath> preferencePathByPreference = new HashMap<>();

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferenceOfHostWithinTree preference,
													final LanguageCode languageCode) {
		return !isPreferenceConnectedToAnyInactivePlugin(preference, languageCode);
	}

	private boolean isPreferenceConnectedToAnyInactivePlugin(final SearchablePreferenceOfHostWithinTree preference,
															 final LanguageCode languageCode) {
		return ActivePluginsSearchResultsFilter
				.getInactivePlugins()
				.stream()
				.anyMatch(inactivePlugin -> isPreferenceConnectedToPlugin(preference, inactivePlugin, languageCode));
	}

	private static List<OsmandPlugin> getInactivePlugins() {
		return PluginsHelper
				.getAvailablePlugins()
				.stream()
				.filter(plugin -> !plugin.isActive())
				.toList();
	}

	private boolean isPreferenceConnectedToPlugin(final SearchablePreferenceOfHostWithinTree preference,
												  final OsmandPlugin plugin,
												  final LanguageCode languageCode) {
		return isPreferenceOnSettingsScreen(preference, Optional.ofNullable(plugin.getSettingsScreenType())) ||
				isPreferencePathConnectedToPlugin(getPreferencePath(preference), plugin) ||
				isMapSourcePreferenceConnectedToPlugin(preference, plugin, languageCode);
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
														   final OsmandPlugin plugin,
														   final LanguageCode languageCode) {
		return plugin instanceof OsmandRasterMapsPlugin && isMapSourcePreference(preference, languageCode);
	}

	private boolean isMapSourcePreference(final SearchablePreferenceOfHostWithinTree preference,
										  final LanguageCode languageCode) {
		return this
				.getPreferencePath(preference)
				.preferences()
				.stream()
				.map(SearchablePreferenceOfHostWithinTree::hostOfPreference)
				.map(SearchablePreferenceScreen::id)
				.anyMatch(
						idStartsWith(
								Strings.prefixIdWithLanguage(
										"net.osmand.plus.widgets.alert.MapLayerSelectionDialogFragment$MapLayerSelectionDialogFragmentProxy",
										languageCode)));
	}

	private PreferencePath getPreferencePath(final SearchablePreferenceOfHostWithinTree preference) {
		return preferencePathByPreference.computeIfAbsent(
				preference,
				SearchablePreferenceOfHostWithinTree::getPreferencePath);
	}

	private static Predicate<String> idStartsWith(final String idPrefix) {
		return id -> id.startsWith(idPrefix);
	}
}
