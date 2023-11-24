package net.osmand.plus.keyevent.callbacks;

import net.osmand.util.Algorithms;

public enum EventType {

	SELECT_DEVICE,
	RENAME_DEVICE,
	ADD_NEW_DEVICE,
	DELETE_DEVICE,
	UPDATE_KEY_BINDING,
	RESET_ALL_KEY_BINDINGS;

	public boolean isDeviceRelated() {
		return Algorithms.equalsToAny(this, SELECT_DEVICE, RENAME_DEVICE, ADD_NEW_DEVICE, DELETE_DEVICE);
	}

	public boolean isKeyBindingRelated() {
		return Algorithms.equalsToAny(this, UPDATE_KEY_BINDING, RESET_ALL_KEY_BINDINGS);
	}

	public boolean isCustomPreferenceRelated() {
		return this != SELECT_DEVICE;
	}
}
