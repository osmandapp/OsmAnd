package net.osmand.plus.keyevent.fragments.editassignment;

import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.BUTTON_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.NAME_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.SPACE;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;

import android.view.KeyEvent;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.selectkeycode.SelectKeyCodeFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class EditKeyAssignmentController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final String deviceId;
	private final String assignmentId;
	private FragmentActivity activity;
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
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		KeyAssignment assignment = deviceHelper.findAssignment(appMode, deviceId, assignmentId);
		if (assignment == null) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
		screenItems.add(new ScreenItem(NAME_ITEM, assignment.getName(app)));
		screenItems.add(new ScreenItem(ACTION_ITEM, assignment.getCommandTitle(app)));

		if (assignment.hasKeyCodes()) {
			List<Integer> keyCodes = assignment.getKeyCodes();
			for (int i = 0; i < keyCodes.size(); i++) {
				screenItems.add(new ScreenItem(BUTTON_ITEM, keyCodes.get(i)));
			}
		} else {
			screenItems.add(new ScreenItem(BUTTON_ITEM, KeyEvent.KEYCODE_UNKNOWN));
		}
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

	@Nullable
	public String getActionNameSummary() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getCommandTitle(app) : null;
	}

	public void askRenameAssignment() {
		String oldName = getCustomNameSummary();
		showEnterNameDialog(oldName, newName -> {
			onNameEntered(newName);
			return true;
		});
	}

	private void showEnterNameDialog(@Nullable String oldName,
	                                 @NonNull CallbackWithObject<String> callback) {
		boolean nightMode = isNightMode();

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(R.string.shared_string_name)
				.setControlsColor(getActiveColor(activity, nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null);

		dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				EditText editText = (EditText) extra;
				String newName = editText.getText().toString();
				if (Objects.equals(oldName, newName)) {
					return;
				}
				if (Algorithms.isBlank(newName)) {
					app.showToastMessage(R.string.empty_name);
				} else if (deviceHelper.hasAssignmentNameDuplicate(app, appMode, deviceId, newName)) {
					app.showToastMessage(R.string.message_name_is_already_exists);
				} else {
					callback.processResult(newName.trim());
				}
			}
		});
		String caption = activity.getString(R.string.shared_string_name);
		CustomAlert.showInput(dialogData, activity, oldName, caption);
	}

	private void onNameEntered(@NonNull String newName) {
		deviceHelper.renameAssignment(appMode, deviceId, assignmentId, newName);
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
}
