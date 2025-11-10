package net.osmand.plus.settings.fragments.search;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.dao.SearchablePreferenceScreenGraphDAO;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;

class ConfigurationFromSearchDatabaseProvider {

	private final SearchablePreferenceScreenGraphDAO searchablePreferenceScreenGraphDAO;
	private final Locale locale;
	private final ConfigurationBundleConverter configurationBundleConverter;

	public ConfigurationFromSearchDatabaseProvider(final SearchablePreferenceScreenGraphDAO searchablePreferenceScreenGraphDAO,
												   final Locale locale,
												   final ConfigurationBundleConverter configurationBundleConverter) {
		this.searchablePreferenceScreenGraphDAO = searchablePreferenceScreenGraphDAO;
		this.locale = locale;
		this.configurationBundleConverter = configurationBundleConverter;
	}

	public Optional<Configuration> getConfigurationFromSearchDatabase() {
		return searchablePreferenceScreenGraphDAO
				.findGraphById(locale)
				.map(SearchablePreferenceScreenGraph::configuration)
				.map(configurationBundleConverter::doBackward);
	}
}
