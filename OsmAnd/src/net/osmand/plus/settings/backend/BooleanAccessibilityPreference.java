package net.osmand.plus.settings.backend;

import net.osmand.plus.ApplicationMode;

class BooleanAccessibilityPreference extends BooleanPreference {

	private OsmandSettings osmandSettings;

	BooleanAccessibilityPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
	}

	@Override
	public Boolean get() {
		return osmandSettings.ctx.accessibilityEnabled() ? super.get() : getDefaultValue();
	}

	@Override
	public Boolean getModeValue(ApplicationMode mode) {
		return osmandSettings.ctx.accessibilityEnabledForMode(mode) ? super.getModeValue(mode) : getDefaultValue();
	}

	@Override
	public boolean set(Boolean obj) {
		return osmandSettings.ctx.accessibilityEnabled() && super.set(obj);
	}

	@Override
	public boolean setModeValue(ApplicationMode mode, Boolean obj) {
		return osmandSettings.ctx.accessibilityEnabledForMode(mode) && super.setModeValue(mode, obj);
	}
}
