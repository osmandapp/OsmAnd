package net.osmand.plus.settings.fragments.search;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.dao.SearchablePreferenceScreenTreeDao;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;

// FK-TODO: remove?
class ConfigurationFromSearchDatabaseProvider {

	private final SearchablePreferenceScreenTreeDao searchablePreferenceScreenTreeDao;
	private final Locale locale;
	private final ConfigurationBundleConverter configurationBundleConverter;

	public ConfigurationFromSearchDatabaseProvider(final SearchablePreferenceScreenTreeDao searchablePreferenceScreenTreeDao,
												   final Locale locale,
												   final ConfigurationBundleConverter configurationBundleConverter) {
		this.searchablePreferenceScreenTreeDao = searchablePreferenceScreenTreeDao;
		this.locale = locale;
		this.configurationBundleConverter = configurationBundleConverter;
	}

	public Optional<Configuration> getConfigurationFromSearchDatabase() {
		return searchablePreferenceScreenTreeDao
				.findTreeById(locale)
				.map(SearchablePreferenceScreenTree::configuration)
				.map(configurationBundleConverter::convertBackward);
	}
}
