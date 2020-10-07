package net.osmand.plus.settings.backend;

class IntPreference extends CommonPreference<Integer> {

	IntPreference(OsmandSettings osmandSettings, String id, int defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public Integer getValue(Object prefs, Integer defaultValue) {
		return getSettingsAPI().getInt(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Integer val) {
		return getSettingsAPI().edit(prefs).putInt(getId(), val).commit();
	}

	@Override
	public Integer parseString(String s) {
		return Integer.parseInt(s);
	}
}
