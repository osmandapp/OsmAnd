package net.osmand.plus.settings;

public interface OnApplyPreference {

	boolean onApplyPreference(String prefId, Object newValue, ApplyQueryType applyQueryType);

}
