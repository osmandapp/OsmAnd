package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.AssignmentsCollection;
import net.osmand.plus.keyevent.AssignmentsCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.keybinding.KeyBinding;

import java.util.List;
import java.util.Map;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected AssignmentsCollection assignmentsCollection;

	@NonNull
	public InputDeviceProfile initialize(@NonNull OsmandApplication app) {
		this.app = app;
		return this;
	}

	protected void setAssignments(@NonNull List<KeyBinding> keyBindings) {
		assignmentsCollection = new AssignmentsCollection(keyBindings);
	}

	@NonNull
	public Map<AssignmentsCategory, List<KeyBinding>> getCategorizedAssignments() {
		return assignmentsCollection.getCategorizedAssignments(app);
	}

	@NonNull
	public List<KeyBinding> getAssignments() {
		return assignmentsCollection.getAssignments();
	}

	public boolean hasAssignmentNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		return assignmentsCollection.hasNameDuplicate(context, newName);
	}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		KeyBinding keyBinding = findAssignment(keyCode);
		return keyBinding != null ? keyBinding.getCommand(app) : null;
	}

	@Nullable
	public KeyBinding findAssignment(int keyCode) {
		return assignmentsCollection.findByKeyCode(keyCode);
	}

	@Nullable
	public KeyBinding findAssignment(@NonNull String assignmentId) {
		return assignmentsCollection.findById(assignmentId);
	}

	public int getAssignmentsCount() {
		return assignmentsCollection.getAssignments().size();
	}

	public int getActiveAssignmentsCount() {
		return assignmentsCollection.getActiveAssignmentsCount();
	}

	@NonNull
	public abstract String getId();

	public boolean isCustom() {
		return false;
	}

	@NonNull
	public abstract String toHumanString(@NonNull Context context);

}
