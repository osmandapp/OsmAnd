package net.osmand.plus.settings.fragments.search;

import java.util.Locale;

import de.KnollFrank.lib.settingssearch.db.preference.dao.SearchablePreferenceScreenGraphDAO;

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

	public Configuration getConfigurationFromSearchDatabase() {
		return configurationBundleConverter.doBackward(
				searchablePreferenceScreenGraphDAO
						.findGraphById(locale)
						.orElseThrow()
						.configuration());
	}
}
