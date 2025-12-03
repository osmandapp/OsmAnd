package net.osmand.plus.settings.fragments.search;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.List;

public class SettingsSearchInitializer {

	private final OsmandApplication app;
	private final StateChangedListener<String> rebuildSearchDatabaseListener = s -> rebuildSearchDatabase();

	public SettingsSearchInitializer(final OsmandApplication app) {
		this.app = app;
	}

	public void rebuildSearchDatabaseOnAppProfileChanged() {
		for (var appProfilePreference : getAppProfilePreferences()) {
			appProfilePreference.addListener(rebuildSearchDatabaseListener);
		}
	}

	private List<OsmandPreference<String>> getAppProfilePreferences() {
		return List.of(
				app.getSettings().CUSTOM_APP_MODES_KEYS,
				app.getSettings().USER_PROFILE_NAME);
	}

	private void rebuildSearchDatabase() {
		app
				.preferencesDatabaseManager
				.getPreferencesDatabase()
				.searchablePreferenceScreenGraphRepository()
				.addGraphCreator(new SearchDatabaseRebuilder(app.getTileSourceTemplatesProvider()));
	}
}
