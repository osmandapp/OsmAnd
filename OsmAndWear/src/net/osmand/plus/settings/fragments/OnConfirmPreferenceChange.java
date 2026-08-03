package net.osmand.plus.settings.fragments;

public interface OnConfirmPreferenceChange {

	boolean onConfirmPreferenceChange(String prefId, Object newValue, ApplyQueryType applyQueryType);

}
