package net.osmand.plus.settings.fragments.search;

// FK-TODO: remove?
class SearchDatabaseStatusHandler {

	private final SetStringPreference pluginsCoveredBySettingsSearch;

	public SearchDatabaseStatusHandler(final SetStringPreference pluginsCoveredBySettingsSearch) {
		this.pluginsCoveredBySettingsSearch = pluginsCoveredBySettingsSearch;
	}

	public boolean isSearchDatabaseUpToDate() {
		return pluginsCoveredBySettingsSearch.get().equals(ConfigurationProvider.getEnabledPlugins());
	}

	public void setSearchDatabaseUpToDate() {
		pluginsCoveredBySettingsSearch.set(ConfigurationProvider.getEnabledPlugins());
	}
}
