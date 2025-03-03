package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.OsmandSettings;

public class LongPreference extends CommonPreference<Long> {

	public LongPreference(OsmandSettings settings, String id, long defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public Long getValue(Object prefs, Long defaultValue) {
		return getSettingsAPI().getLong(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Long val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putLong(getId(), val).commit();
	}

	@Override
	public Long parseString(String s) {
		return Long.parseLong(s);
	}
}