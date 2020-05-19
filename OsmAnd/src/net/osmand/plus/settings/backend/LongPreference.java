package net.osmand.plus.settings.backend;

class LongPreference extends CommonPreference<Long> {


	private OsmandSettings osmandSettings;

	LongPreference(OsmandSettings osmandSettings, String id, long defaultValue) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
	}

	@Override
	protected Long getValue(Object prefs, Long defaultValue) {
		return osmandSettings.settingsAPI.getLong(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Long val) {
		return osmandSettings.settingsAPI.edit(prefs).putLong(getId(), val).commit();
	}

	@Override
	public Long parseString(String s) {
		return Long.parseLong(s);
	}
}
