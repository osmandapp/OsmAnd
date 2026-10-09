package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.OsmandSettings;

public class BooleanStringPreference extends BooleanPreference {

	public BooleanStringPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public Boolean getValue(Object prefs, Boolean defaultValue) {
		Boolean value;
		try {
			value = parseString(getSettingsAPI().getString(prefs, getId(), defaultValue.toString()));
		} catch (ClassCastException e) {
			value = getSettingsAPI().getBoolean(prefs, getId(), defaultValue);
			setValue(prefs, value);
		}
		return value;
	}

	@Override
	protected boolean setValue(Object prefs, Boolean val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putString(getId(), val != null ? val.toString() : null).commit();
	}
}