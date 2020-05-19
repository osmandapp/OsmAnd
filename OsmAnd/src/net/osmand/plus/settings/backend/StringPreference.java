package net.osmand.plus.settings.backend;

public class StringPreference extends CommonPreference<String> {

	private OsmandSettings osmandSettings;

	StringPreference(OsmandSettings osmandSettings, String id, String defaultValue) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
	}

	@Override
	protected String getValue(Object prefs, String defaultValue) {
		return osmandSettings.settingsAPI.getString(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, String val) {
		return osmandSettings.settingsAPI.edit(prefs).putString(getId(), (val != null) ? val.trim() : val).commit();
	}

	@Override
	public String parseString(String s) {
		return s;
	}
}
