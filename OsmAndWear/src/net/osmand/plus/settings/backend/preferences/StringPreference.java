package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.OsmandSettings;

public class StringPreference extends CommonPreference<String> {

	public StringPreference(OsmandSettings settings, String id, String defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public String getValue(Object prefs, String defaultValue) {
		return getSettingsAPI().getString(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, String val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putString(getId(), (val != null) ? val.trim() : val).commit();
	}

	@Override
	public String parseString(String s) {
		return s;
	}
}