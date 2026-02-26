package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.OsmandSettings;

public class IntPreference extends CommonPreference<Integer> {

	public IntPreference(OsmandSettings settings, String id, int defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public Integer getValue(Object prefs, Integer defaultValue) {
		return getSettingsAPI().getInt(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Integer val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putInt(getId(), val).commit();
	}

	@Override
	public Integer parseString(String s) {
		return Integer.parseInt(s);
	}
}