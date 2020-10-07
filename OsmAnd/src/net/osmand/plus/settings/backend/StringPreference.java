package net.osmand.plus.settings.backend;

public class StringPreference extends CommonPreference<String> {

	StringPreference(OsmandSettings osmandSettings, String id, String defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public String getValue(Object prefs, String defaultValue) {
		return getSettingsAPI().getString(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, String val) {
		return getSettingsAPI().edit(prefs).putString(getId(), (val != null) ? val.trim() : val).commit();
	}

	@Override
	public String parseString(String s) {
		return s;
	}
}
