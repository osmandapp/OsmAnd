package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;

import java.util.Set;
import java.util.stream.Collectors;

class SearchDatabaseStatusHandler {

	private final SetStringPreference pluginsCoveredBySettingsSearch;

	public SearchDatabaseStatusHandler(final SetStringPreference pluginsCoveredBySettingsSearch) {
		this.pluginsCoveredBySettingsSearch = pluginsCoveredBySettingsSearch;
	}

	public boolean isSearchDatabaseUpToDate() {
		return pluginsCoveredBySettingsSearch.get().equals(getEnabledPlugins());
	}

	public void setSearchDatabaseUpToDate() {
		pluginsCoveredBySettingsSearch.set(getEnabledPlugins());
	}

	private static Set<String> getEnabledPlugins() {
		return PluginsHelper
				.getEnabledPlugins()
				.stream()
				.map(OsmandPlugin::getId)
				.collect(Collectors.toSet());
	}
}
