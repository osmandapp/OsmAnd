package net.osmand.plus.settings.fragments.search;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.dao.SearchablePreferenceScreenTreeDAO;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;

// FK-TODO: remove?
class ConfigurationFromSearchDatabaseProvider {

	private final SearchablePreferenceScreenTreeDAO searchablePreferenceScreenTreeDAO;
	private final Locale locale;
	private final ConfigurationBundleConverter configurationBundleConverter;

	public ConfigurationFromSearchDatabaseProvider(final SearchablePreferenceScreenTreeDAO searchablePreferenceScreenTreeDAO,
												   final Locale locale,
												   final ConfigurationBundleConverter configurationBundleConverter) {
		this.searchablePreferenceScreenTreeDAO = searchablePreferenceScreenTreeDAO;
		this.locale = locale;
		this.configurationBundleConverter = configurationBundleConverter;
	}

	public Optional<Configuration> getConfigurationFromSearchDatabase() {
		return searchablePreferenceScreenTreeDAO
				.findTreeById(locale)
				.map(SearchablePreferenceScreenTree::configuration)
				.map(configurationBundleConverter::convertBackward);
	}
}
