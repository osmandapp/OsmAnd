package net.osmand.plus.keyevent.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.assignment.KeyAssignment;

import java.util.List;

public abstract class PredefinedInputDeviceProfile extends InputDeviceProfile {

	@Override
	@NonNull
	public InputDeviceProfile initialize(@NonNull OsmandApplication app) {
		super.initialize(app);
		setAssignments(collectAssignments());
		return this;
	}

	/**
	 * Override this method to add or update bindings between
	 * keycodes and commands for a specific predefined input device profile.
	 *
	 * @return list of predefined assignments for the particular device type.
	 */
	@NonNull
	protected abstract List<KeyAssignment> collectAssignments();

	public void addAssignment(@NonNull List<KeyAssignment> assignments,
	                          @NonNull String commandId, @NonNull Integer ... keyCodes) {
		assignments.add(new KeyAssignment(commandId, keyCodes));
	}
}
