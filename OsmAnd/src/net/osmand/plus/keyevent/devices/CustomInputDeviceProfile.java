package net.osmand.plus.keyevent.devices;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.keyevent.assignment.KeyAssignment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CustomInputDeviceProfile extends InputDeviceProfile {

	private final String customId;
	private String customName;

	public CustomInputDeviceProfile(@NonNull String customId, @NonNull String customName,
	                                @NonNull InputDeviceProfile parentDevice) {
		this.customId = customId;
		this.customName = customName;
		setAssignments(parentDevice.getAssignmentsCopy());
	}

	public CustomInputDeviceProfile(@NonNull JSONObject object) throws JSONException {
		customId = object.getString("id");
		customName = object.getString("name");

		JSONArray jsonArray = object.has("assignments")
				? object.getJSONArray("assignments")
				// For previous version compatibility
				: object.getJSONArray("keybindings");

		List<KeyAssignment> assignments = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			assignments.add(new KeyAssignment(jsonObject));
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

	public void addAssignmentKeyCode(@NonNull String assignmentId, int keyCode) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			removeKeyCodeFromPreviousAssignment(keyCode);
			assignment.addKeyCode(keyCode);
			assignmentsCollection.syncCache();
		}
	}

	public void updateAssignmentKeyCode(@NonNull String assignmentId, int oldKeyCode, int newKeyCode) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			removeKeyCodeFromPreviousAssignment(newKeyCode);
			assignment.updateKeyCode(oldKeyCode, newKeyCode);
			assignmentsCollection.syncCache();
		}
	}

	private void removeKeyCodeFromPreviousAssignment(int keyCode) {
		KeyAssignment previousAssignment = assignmentsCollection.findByKeyCode(keyCode);
		if (previousAssignment != null) {
			previousAssignment.removeKeyCode(keyCode);
		}
	}

	public void clearAssignmentKeyCodes(@NonNull String assignmentId) {
		KeyAssignment assignment = assignmentsCollection.findById(assignmentId);
		if (assignment != null) {
			assignment.clearKeyCodes();
			assignmentsCollection.syncCache();
		}
	}

	public void resetAllAssignments() {
		for (KeyAssignment assignment : assignmentsCollection.getAllAssignments()) {
			assignment.clearKeyCodes();
		}
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
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", customId);
		jsonObject.put("name", customName);
		JSONArray jsonArray = new JSONArray();
		for (KeyAssignment assignment : getAssignments()) {
			jsonArray.put(assignment.toJson());
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
