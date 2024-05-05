package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyAssignmentsCollection;
import net.osmand.plus.keyevent.assignment.KeyAssignmentCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;

import java.util.List;
import java.util.Map;

public abstract class InputDeviceProfile {

	protected OsmandApplication app;
	protected KeyAssignmentsCollection assignmentsCollection;

	@NonNull
	public InputDeviceProfile initialize(@NonNull OsmandApplication app) {
		this.app = app;
		return this;
	}

	protected void setAssignments(@NonNull List<KeyAssignment> assignments) {
		assignmentsCollection = new KeyAssignmentsCollection(assignments);
	}

	@NonNull
	public Map<KeyAssignmentCategory, List<KeyAssignment>> getCategorizedAssignments() {
		return assignmentsCollection.getCategorizedAssignments(app);
	}

	@NonNull
	public List<KeyAssignment> getAssignments() {
		return assignmentsCollection.getAllAssignments();
	}

	@NonNull
	public List<KeyAssignment> getAssignmentsCopy() {
		return assignmentsCollection.getAssignmentsCopy();
	}

	public boolean hasAssignmentNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		return assignmentsCollection.hasNameDuplicate(context, newName);
	}

	@Nullable
	public KeyEventCommand findCommand(int keyCode) {
		KeyAssignment assignment = findAssignment(keyCode);
		return assignment != null ? assignment.getCommand(app) : null;
	}

	@Nullable
	public KeyAssignment findAssignment(int keyCode) {
		return assignmentsCollection.findByKeyCode(keyCode);
	}

	@Nullable
	public KeyAssignment findAssignment(@NonNull String assignmentId) {
		return assignmentsCollection.findById(assignmentId);
	}

	public int getAssignmentsCount() {
		return assignmentsCollection.getAllAssignments().size();
	}

	public boolean hasActiveAssignments() {
		return getActiveAssignmentsCount() > 0;
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
