package net.osmand.plus.keyevent.listener;

import net.osmand.util.CollectionUtils;

public enum EventType {

	SELECT_DEVICE,
	RENAME_DEVICE,
	ADD_NEW_DEVICE,
	DELETE_DEVICE,

	RENAME_ASSIGNMENT,
	UPDATE_ASSIGNMENT_KEYCODE,
	ADD_ASSIGNMENT_KEYCODE,
	REMOVE_ASSIGNMENT_KEYCODE,
	CLEAR_ASSIGNMENT_KEYCODES,
	RESET_ASSIGNMENTS;

	public boolean isDeviceRelated() {
		return CollectionUtils.equalsToAny(this, SELECT_DEVICE,
				RENAME_DEVICE, ADD_NEW_DEVICE, DELETE_DEVICE);
	}

	public boolean isAssignmentRelated() {
		return CollectionUtils.equalsToAny(this, RENAME_ASSIGNMENT,
				UPDATE_ASSIGNMENT_KEYCODE, ADD_ASSIGNMENT_KEYCODE,
				REMOVE_ASSIGNMENT_KEYCODE, RESET_ASSIGNMENTS,
				CLEAR_ASSIGNMENT_KEYCODES);
	}

	public boolean isCustomPreferenceRelated() {
		return this != SELECT_DEVICE;
	}
}
