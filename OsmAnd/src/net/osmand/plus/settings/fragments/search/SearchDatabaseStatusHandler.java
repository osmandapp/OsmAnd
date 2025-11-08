package net.osmand.plus.settings.fragments.search;

class SearchDatabaseStatusHandler {

	private final ConfigurationFromSearchDatabaseProvider configurationFromSearchDatabaseProvider;
	private final ActualConfigurationProvider actualConfigurationProvider;

	public SearchDatabaseStatusHandler(final ConfigurationFromSearchDatabaseProvider configurationFromSearchDatabaseProvider,
									   final ActualConfigurationProvider actualConfigurationProvider) {
		this.configurationFromSearchDatabaseProvider = configurationFromSearchDatabaseProvider;
		this.actualConfigurationProvider = actualConfigurationProvider;
	}

	public boolean isSearchDatabaseUpToDate() {
		return configurationFromSearchDatabaseProvider.getConfigurationFromSearchDatabase().equals(actualConfigurationProvider.getActualConfiguration());
	}
}
