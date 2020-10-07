package net.osmand.plus.settings.backend;

class LongPreference extends CommonPreference<Long> {

	LongPreference(OsmandSettings osmandSettings, String id, long defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public Long getValue(Object prefs, Long defaultValue) {
		return getSettingsAPI().getLong(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Long val) {
		return getSettingsAPI().edit(prefs).putLong(getId(), val).commit();
	}

	@Override
	public Long parseString(String s) {
		return Long.parseLong(s);
	}
}
