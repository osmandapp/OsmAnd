package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.KeyAssignmentsCollection;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.quickaction.QuickAction;

import java.util.ArrayList;
import java.util.List;

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
	public List<KeyAssignment> getFilledAssignments() {
		List<KeyAssignment> result = new ArrayList<>();
		for (KeyAssignment assignment : getAssignments()) {
			if (assignment.hasRequiredParameters()) {
				result.add(assignment);
			}
		}
		return result;
	}

	@NonNull
	public List<KeyAssignment> getAssignments() {
		return assignmentsCollection.getAssignments();
	}

	@NonNull
	public List<KeyAssignment> getAssignmentsCopy() {
		return assignmentsCollection.getAssignmentsCopy();
	}

	public boolean hasAssignmentNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		return assignmentsCollection.hasNameDuplicate(context, newName);
	}

	@Nullable
	public QuickAction findAction(int keyCode) {
		KeyAssignment assignment = findAssignment(keyCode);
		return assignment != null ? assignment.getAction() : null;
	}

	@Nullable
	public KeyAssignment findAssignment(int keyCode) {
		return assignmentsCollection.findByKeyCode(keyCode);
	}

	@Nullable
	public KeyAssignment findAssignment(@NonNull String assignmentId) {
		return assignmentsCollection.findById(assignmentId);
	}

	public boolean hasActiveAssignments() {
		return getFilledAssignmentsCount() > 0;
	}

	public int getFilledAssignmentsCount() {
		return getFilledAssignments().size();
	}

	@NonNull
	public abstract String getId();

	public boolean isCustom() {
		return false;
	}

	@NonNull
	public abstract String toHumanString(@NonNull Context context);

}
