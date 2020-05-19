package net.osmand.plus.settings.backend;

class IntPreference extends CommonPreference<Integer> {


	private OsmandSettings osmandSettings;

	IntPreference(OsmandSettings osmandSettings, String id, int defaultValue) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
	}

	@Override
	protected Integer getValue(Object prefs, Integer defaultValue) {
		return osmandSettings.settingsAPI.getInt(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Integer val) {
		return osmandSettings.settingsAPI.edit(prefs).putInt(getId(), val).commit();
	}

	@Override
	public Integer parseString(String s) {
		return Integer.parseInt(s);
	}
}
