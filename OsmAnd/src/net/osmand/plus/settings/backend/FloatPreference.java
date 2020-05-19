package net.osmand.plus.settings.backend;

public class FloatPreference extends CommonPreference<Float> {


	private OsmandSettings osmandSettings;

	FloatPreference(OsmandSettings osmandSettings, String id, float defaultValue) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
	}

	@Override
	protected Float getValue(Object prefs, Float defaultValue) {
		return osmandSettings.settingsAPI.getFloat(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Float val) {
		return osmandSettings.settingsAPI.edit(prefs).putFloat(getId(), val).commit();
	}

	@Override
	public Float parseString(String s) {
		return Float.parseFloat(s);
	}
}
