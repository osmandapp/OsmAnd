package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandSettings;

public class LongPreference extends CommonPreference<Long> {

	public LongPreference(@NonNull OsmandSettings settings, @NonNull String id, long defaultValue) {
		super(settings, id, defaultValue);
	}

	@Override
	public Long getValue(@NonNull Object prefs, Long defaultValue) {
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