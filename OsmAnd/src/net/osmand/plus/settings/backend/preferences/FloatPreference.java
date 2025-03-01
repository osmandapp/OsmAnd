package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandSettings;

public class FloatPreference extends CommonPreference<Float> {

	public FloatPreference(@NonNull OsmandSettings settings, @NonNull String id,
			float defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public Float getValue(@NonNull Object prefs, Float defaultValue) {
		return getSettingsAPI().getFloat(prefs, getId(), defaultValue);
	}

	@Override
	protected boolean setValue(Object prefs, Float val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putFloat(getId(), val).commit();
	}

	@Override
	public Float parseString(String s) {
		return Float.parseFloat(s);
	}
}