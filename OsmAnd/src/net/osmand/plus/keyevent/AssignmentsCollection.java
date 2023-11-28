package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AssignmentsCollection {

	private final List<KeyBinding> assignments;
	private Map<String, KeyBinding> keyBindingById = new HashMap<>();
	private Map<Integer, KeyBinding> keyBindingByKeyCode = new HashMap<>();

	public AssignmentsCollection(@NonNull List<KeyBinding> assignments) {
		this.assignments = assignments;
		syncCache();
	}

	@NonNull
	public List<KeyBinding> getAssignments() {
		return assignments;
	}

	@NonNull
	public Map<AssignmentsCategory, List<KeyBinding>> getCategorizedAssignments(@NonNull OsmandApplication app) {
		Map<AssignmentsCategory, List<KeyBinding>> result = new HashMap<>();
		for (KeyBinding assignment : getAssignments()) {
			KeyEventCommand command = assignment.getCommand(app);
			if (command != null) {
				AssignmentsCategory category = command.getCategory();
				List<KeyBinding> assignments = result.get(category);
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
		for (KeyBinding assignment : getAssignments()) {
			if (!Algorithms.isEmpty(assignment.getKeyCodes())) {
				count++;
			}
		}
		return count;
	}

	public boolean hasNameDuplicate(@NonNull OsmandApplication context, @NonNull String newName) {
		for (KeyBinding keyBinding : getAssignments()) {
			if (Objects.equals(keyBinding.getName(context), newName)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public KeyBinding findById(@NonNull String id) {
		return keyBindingById.get(id);
	}

	@Nullable
	public KeyBinding findByKeyCode(int keyCode) {
		return keyBindingByKeyCode.get(keyCode);
	}

	public void syncCache() {
		Map<String, KeyBinding> newKeyBindingById = new HashMap<>();
		Map<Integer, KeyBinding> newKeyBindingByKeyCode = new HashMap<>();
		for (KeyBinding keyBinding : assignments) {
			newKeyBindingById.put(keyBinding.getId(), keyBinding);
			for (int keyCode : keyBinding.getKeyCodes()) {
				newKeyBindingByKeyCode.put(keyCode, keyBinding);
			}
		}
		this.keyBindingById = newKeyBindingById;
		this.keyBindingByKeyCode = newKeyBindingByKeyCode;
	}
}
