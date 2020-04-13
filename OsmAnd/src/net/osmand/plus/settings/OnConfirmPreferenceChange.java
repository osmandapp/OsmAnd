package net.osmand.plus.settings;

public interface OnConfirmPreferenceChange {

	boolean onConfirmPreferenceChange(String prefId, Object newValue, ApplyQueryType applyQueryType);

}
