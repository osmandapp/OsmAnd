package net.osmand.plus.settings.backend;

public class BooleanPreference extends CommonPreference<Boolean> {

	BooleanPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	protected Boolean getValue(Object prefs, Boolean defaultValue) {
		return osmandSettings.settingsAPI.getBoolean(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Boolean val) {
		return osmandSettings.settingsAPI.edit(prefs).putBoolean(getId(), val).commit();
	}

	@Override
	public Boolean parseString(String s) {
		return Boolean.parseBoolean(s);
	}
}
