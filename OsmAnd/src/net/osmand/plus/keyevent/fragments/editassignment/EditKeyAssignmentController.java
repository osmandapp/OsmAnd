package net.osmand.plus.keyevent.fragments.editassignment;

import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ADD_ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ADD_KEY_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_KEY_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.HEADER_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.SPACE;

import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.fragments.selectkeycode.SelectKeyCodeFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

class EditKeyAssignmentController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final String deviceId;
	private final String assignmentId;
	private FragmentActivity activity;
	private final AssignmentEditBundle editBundle = new AssignmentEditBundle();
	private final Fragment targetFragment;
	private final boolean usedOnMap;

	public EditKeyAssignmentController(@NonNull OsmandApplication app,
	                                   @NonNull ApplicationMode appMode,
									   @NonNull Fragment targetFragment,
									   @NonNull String deviceId,
									   @NonNull String assignmentId,
	                                   boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.targetFragment = targetFragment;
		this.usedOnMap = usedOnMap;
		this.deviceHelper = app.getInputDeviceHelper();
		this.deviceId = deviceId;
		this.assignmentId = assignmentId;
		KeyAssignment keyAssignment = getAssignment();
		if (keyAssignment != null) {
			editBundle.command = keyAssignment.getCommand(app);
			editBundle.keyCodes = keyAssignment.getKeyCodes();
		} else {
			editBundle.keyCodes = new ArrayList<>();
		}
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
		screenItems.add(new ScreenItem(HEADER_ITEM, R.string.shared_string_action));
		if (editBundle.command != null) {
			screenItems.add(new ScreenItem(ASSIGNED_ACTION_ITEM, editBundle.command));
		} else {
			screenItems.add(new ScreenItem(ADD_ACTION_ITEM));
		}
		screenItems.add(new ScreenItem(HEADER_ITEM, R.string.assigned_keys));
		for (Integer keyCode : editBundle.keyCodes) {
			screenItems.add(new ScreenItem(ASSIGNED_KEY_ITEM, keyCode));
		}
		screenItems.add(new ScreenItem(ADD_KEY_ITEM));
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	@Nullable
	public String getCustomNameSummary() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getName(app) : null;
	}

	@NonNull
	public String getDialogTitle() {
		if (editBundle.command != null) {
			return editBundle.command.toHumanString(app);
		}
		return app.getString(getAssignment() != null
				? R.string.edit_key_assignment : R.string.new_key_assignment);
	}

	public void askAddAction() {
		// TODO open action menu screen
	}

	public void askDeleteAction() {
		editBundle.command = null;
		// TODO update menu
	}

	public void askDeleteKeyCode(int keyCode) {
		editBundle.keyCodes.remove(keyCode);
		// TODO update menu
	}

	public void askAddKeyCode() {
		askChangeKeyCode(KeyEvent.KEYCODE_UNKNOWN);
	}

	public void askChangeKeyCode(int keyCode) {
		SelectKeyCodeFragment.showInstance(
				activity.getSupportFragmentManager(),
				targetFragment, appMode, deviceId, assignmentId, keyCode);
	}

	public void askClearKeyCodes() {
		KeyAssignment assignment = getAssignment();
		if (assignment != null && !assignment.hasKeyCodes()) {
			app.showShortToastMessage(R.string.key_assignments_already_cleared_message);
			return;
		}
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.clear_key_assignment)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_remove, (dialog, which) -> {
					deviceHelper.clearAssignmentKeyCodes(appMode, deviceId, assignmentId);
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_key_assignment_desc);
	}

	@Nullable
	public KeyAssignment getAssignment() {
		return deviceHelper.findAssignment(appMode, deviceId, assignmentId);
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	public void addOrUpdateKeyCode(int oldKeyCode, int newKeyCode) {
		if (oldKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
			deviceHelper.addAssignmentKeyCode(appMode, deviceId, assignmentId, newKeyCode);
		} else {
			deviceHelper.updateAssignmentKeyCode(appMode, deviceId, assignmentId, oldKeyCode, newKeyCode);
		}
	}

	public boolean hasChangesToSave() {
		return editBundle.command != null && !Algorithms.isEmpty(editBundle.keyCodes);
	}

	public void saveChanges() {
		// TODO
	}

	private static class AssignmentEditBundle {
		KeyEventCommand command;
		List<Integer> keyCodes;
	}
}
