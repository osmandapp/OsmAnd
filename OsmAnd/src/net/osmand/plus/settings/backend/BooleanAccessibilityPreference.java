package net.osmand.plus.settings.backend;

class BooleanAccessibilityPreference extends BooleanPreference {

	BooleanAccessibilityPreference(OsmandSettings osmandSettings, String id, boolean defaultValue) {
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
