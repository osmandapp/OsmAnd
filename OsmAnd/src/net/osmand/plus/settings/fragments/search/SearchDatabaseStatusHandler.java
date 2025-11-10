package net.osmand.plus.settings.fragments.search;

import java.util.Optional;

class SearchDatabaseStatusHandler {

	private final ConfigurationFromSearchDatabaseProvider configurationFromSearchDatabaseProvider;
	private final ActualConfigurationProvider actualConfigurationProvider;

	public SearchDatabaseStatusHandler(final ConfigurationFromSearchDatabaseProvider configurationFromSearchDatabaseProvider,
									   final ActualConfigurationProvider actualConfigurationProvider) {
		this.configurationFromSearchDatabaseProvider = configurationFromSearchDatabaseProvider;
		this.actualConfigurationProvider = actualConfigurationProvider;
	}

	public boolean isSearchDatabaseUpToDate() {
		final Optional<Configuration> configurationFromSearchDatabase = configurationFromSearchDatabaseProvider.getConfigurationFromSearchDatabase();
		final Configuration actualConfiguration = actualConfigurationProvider.getActualConfiguration();
		return configurationFromSearchDatabase.equals(Optional.of(actualConfiguration));
	}
}
