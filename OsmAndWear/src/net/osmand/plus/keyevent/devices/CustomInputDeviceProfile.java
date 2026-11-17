package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.quickaction.QuickAction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomInputDeviceProfile extends InputDeviceProfile {

	private final String customId;
	private String customName;

	public CustomInputDeviceProfile(@NonNull String customId, @NonNull String customName,
	                                @NonNull InputDeviceProfile parentDevice) {
		this.customId = customId;
		this.customName = customName;
		setAssignments(parentDevice.getAssignmentsCopy());
	}

	public CustomInputDeviceProfile(@NonNull OsmandApplication app, @NonNull JSONObject object) throws JSONException {
		customId = object.getString("id");
		customName = object.getString("name");

		JSONArray jsonArray = object.has("assignments")
				? object.getJSONArray("assignments")
				// For previous version compatibility
				: object.getJSONArray("keybindings");

		List<KeyAssignment> assignments = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			assignments.add(new KeyAssignment(app, jsonObject));
		}
		setAssignments(assignments);
	}

	public void setCustomName(@NonNull String customName) {
		this.customName = customName;
	}

	public void renameAssignment(@NonNull String assignmentId, @NonNull String newName) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			assignment.setCustomName(newName);
		}
	}

	public void addAssignment(@NonNull KeyAssignment assignment) {
		for (int keyCode : assignment.getKeyCodes()) {
			removeKeyCodeFromPreviousAssignment(assignment, keyCode);
		}
		assignmentsCollection.addAssignment(assignment);
		assignmentsCollection.syncCache();
	}

	public void updateAssignment(@NonNull String assignmentId,
	                             @NonNull QuickAction action, @NonNull List<Integer> keyCodes) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			for (int keyCode: keyCodes) {
				removeKeyCodeFromPreviousAssignment(assignment, keyCode);
			}
			assignment.setAction(action);
			assignment.setKeyCodes(keyCodes);
			assignmentsCollection.syncCache();
		}
	}

	private void removeKeyCodeFromPreviousAssignment(@NonNull KeyAssignment assignment, int keyCode) {
		KeyAssignment previousAssignment = assignmentsCollection.findByKeyCode(keyCode);
		if (previousAssignment != null) {
			previousAssignment.removeKeyCode(keyCode);
			if (!Objects.equals(assignment.getId(), previousAssignment.getId()) && !previousAssignment.hasKeyCodes()) {
				removeKeyAssignmentCompletely(previousAssignment.getId());
			}
		}
	}

	public void removeKeyAssignmentCompletely(@NonNull String assignmentId) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			assignmentsCollection.getAssignments().remove(assignment);
			assignmentsCollection.syncCache();
		}
	}

	public void saveUpdatedAssignmentsList(@NonNull List<KeyAssignment> assignments) {
		assignmentsCollection.setAssignments(assignments);
		assignmentsCollection.syncCache();
	}

	public void clearAllAssignments() {
		assignmentsCollection.getAssignments().clear();
		assignmentsCollection.syncCache();
	}

	@NonNull
	@Override
	public String getId() {
		return customId;
	}

	@Override
	public boolean isCustom() {
		return true;
	}

	@NonNull
	public JSONObject toJson(@NonNull OsmandApplication app) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", customId);
		jsonObject.put("name", customName);
		JSONArray jsonArray = new JSONArray();
		for (KeyAssignment assignment : getAssignments()) {
			jsonArray.put(assignment.toJson(app));
		}
		jsonObject.put("assignments", jsonArray);
		return jsonObject;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return customName;
	}
}
