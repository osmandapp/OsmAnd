package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

public class BooleanAccessibilityPreference extends BooleanPreference {

	public BooleanAccessibilityPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
		super(osmandSettings, id, defaultValue);
	}

	@Override
	public Boolean get() {
		return getContext().accessibilityEnabled() ? super.get() : getDefaultValue();
	}

	@Override
	public Boolean getModeValue(ApplicationMode mode) {
		return getContext().accessibilityEnabledForMode(mode) ? super.getModeValue(mode) : getDefaultValue();
	}

	@Override
	public boolean set(Boolean obj) {
		return getContext().accessibilityEnabled() && super.set(obj);
	}

	@Override
	public boolean setModeValue(ApplicationMode mode, Boolean obj) {
		return getContext().accessibilityEnabledForMode(mode) && super.setModeValue(mode, obj);
	}
}
