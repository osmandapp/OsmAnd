package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandSettings;

public class IntPreference extends CommonPreference<Integer> {

	public IntPreference(@NonNull OsmandSettings settings, @NonNull String id, int defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public Integer getValue(@NonNull Object prefs, Integer defaultValue) {
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