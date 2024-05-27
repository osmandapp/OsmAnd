package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.assignment.KeyAssignmentCategory;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyAssignmentsCollection {

	private final List<KeyAssignment> assignments;
	private Map<String, KeyAssignment> assignmentById = new HashMap<>();
	private Map<Integer, KeyAssignment> assignmentByKeyCode = new HashMap<>();

	public KeyAssignmentsCollection(@NonNull List<KeyAssignment> assignments) {
		this.assignments = assignments;
		syncCache();
	}

	@NonNull
	public List<KeyAssignment> getAssignments() {
		return assignments;
	}

	@NonNull
	public List<KeyAssignment> getAssignmentsCopy() {
		List<KeyAssignment> copy = new ArrayList<>();
		for (KeyAssignment assignment : getAssignments()) {
			copy.add(new KeyAssignment(assignment));
		}
		return copy;
	}

	@NonNull
	public Map<KeyAssignmentCategory, List<KeyAssignment>> getCategorizedAssignments(@NonNull OsmandApplication app) {
		Map<KeyAssignmentCategory, List<KeyAssignment>> result = new HashMap<>();
		for (KeyAssignment assignment : getAssignments()) {
			KeyEventCommand command = assignment.getCommand(app);
			if (command != null) {
				KeyAssignmentCategory category = command.getCategory();
				List<KeyAssignment> assignments = result.get(category);
				if (assignments == null) {
					assignments = new ArrayList<>();
					result.put(category, assignments);
				}
				assignments.add(assignment);
			}
		}
		return result;
	}

	public boolean hasNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		for (KeyAssignment assignment : getAssignments()) {
			if (Objects.equals(assignment.getName(context), newName)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public KeyAssignment findById(@NonNull String id) {
		return assignmentById.get(id);
	}

	@Nullable
	public KeyAssignment findByKeyCode(int keyCode) {
		return assignmentByKeyCode.get(keyCode);
	}

	public void syncCache() {
		Map<String, KeyAssignment> newAssignmentById = new HashMap<>();
		Map<Integer, KeyAssignment> newAssignmentByKeyCode = new HashMap<>();
		for (KeyAssignment assignment : getAssignments()) {
			newAssignmentById.put(assignment.getId(), assignment);
			for (int keyCode : assignment.getKeyCodes()) {
				newAssignmentByKeyCode.put(keyCode, assignment);
			}
		}
		this.assignmentById = newAssignmentById;
		this.assignmentByKeyCode = newAssignmentByKeyCode;
	}
}
