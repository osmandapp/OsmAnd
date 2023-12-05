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

	private final List<KeyAssignment> allAssignments;
	private Map<String, KeyAssignment> assignmentById = new HashMap<>();
	private Map<Integer, KeyAssignment> assignmentByKeyCode = new HashMap<>();

	public KeyAssignmentsCollection(@NonNull List<KeyAssignment> assignments) {
		this.allAssignments = assignments;
		syncCache();
	}

	@NonNull
	public List<KeyAssignment> getAllAssignments() {
		return allAssignments;
	}

	@NonNull
	public List<KeyAssignment> getAssignmentsCopy() {
		List<KeyAssignment> copy = new ArrayList<>();
		for (KeyAssignment assignment : getAllAssignments()) {
			copy.add(new KeyAssignment(assignment));
		}
		return copy;
	}

	@NonNull
	public Map<KeyAssignmentCategory, List<KeyAssignment>> getCategorizedAssignments(@NonNull OsmandApplication app) {
		Map<KeyAssignmentCategory, List<KeyAssignment>> result = new HashMap<>();
		for (KeyAssignment assignment : getAllAssignments()) {
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

	public int getActiveAssignmentsCount() {
		int count = 0;
		for (KeyAssignment assignment : getAllAssignments()) {
			if (!Algorithms.isEmpty(assignment.getKeyCodes())) {
				count++;
			}
		}
		return count;
	}

	public boolean hasNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		for (KeyAssignment assignment : getAllAssignments()) {
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
		for (KeyAssignment assignment : getAllAssignments()) {
			newAssignmentById.put(assignment.getId(), assignment);
			for (int keyCode : assignment.getKeyCodes()) {
				newAssignmentByKeyCode.put(keyCode, assignment);
			}
		}
		this.assignmentById = newAssignmentById;
		this.assignmentByKeyCode = newAssignmentByKeyCode;
	}
}
