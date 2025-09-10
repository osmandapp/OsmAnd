package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

public class GlobalAccessibilityPreference extends BooleanPreference {

	public GlobalAccessibilityPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public Boolean get() {
		return super.get();
	}

	@Override
	public Boolean getModeValue(ApplicationMode mode) {
		return super.getModeValue(mode);
	}

	@Override
	public boolean set(Boolean obj) {
		return super.set(obj);
	}

	@Override
	public boolean setModeValue(ApplicationMode mode, Boolean obj) {
		return super.setModeValue(mode, obj);
	}
}
