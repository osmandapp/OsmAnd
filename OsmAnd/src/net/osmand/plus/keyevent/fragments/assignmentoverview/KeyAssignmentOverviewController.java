package net.osmand.plus.keyevent.fragments.assignmentoverview;

import static net.osmand.plus.utils.ColorUtilities.getActiveColor;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyAssignmentOverviewController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final String deviceId;
	private final String assignmentId;
	private FragmentActivity activity;
	private final Fragment targetFragment;
	private final boolean usedOnMap;

	public KeyAssignmentOverviewController(@NonNull OsmandApplication app,
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

	public void showOverflowMenu(@Nullable View actionView) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_rename)
				.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
				.setOnClickListener(v -> askRenameAssignment())
				.create()
		);
		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> askRemoveAssignment())
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = actionView;
		displayData.menuItems = menuItems;
		displayData.nightMode = isNightMode();
		PopUpMenu.show(displayData);
	}

	public void askRenameAssignment() {
		String oldName = getCustomNameSummary();
		showEnterNameDialog(oldName, newName -> {
			onNameEntered(newName);
			return true;
		});
	}

	public void askEditAssignment() {
		// TODO show Editing dialog
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

	public void askRemoveAssignment() {
		KeyAssignment assignment = getAssignment();
		if (assignment != null && !assignment.hasKeyCodes()) {
			app.showShortToastMessage(R.string.key_assignments_already_cleared_message);
			return;
		}
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.clear_key_assignment)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_remove, (dialog, which) -> {
					deviceHelper.removeKeyAssignment(appMode, deviceId, assignmentId);
					// TODO dismiss dialog
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_key_assignment_desc);
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	@Nullable
	public String getCustomNameSummary() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getName(app) : null;
	}

	public int getActionIconId() {
		// TODO use suitable icon
		return R.drawable.ic_action_my_location;
	}

	@Nullable
	public String getActionNameSummary() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getCommandTitle(app) : null;
	}

	@NonNull
	public List<Integer> getKeyCodes() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getKeyCodes() : new ArrayList<>();
	}

	@Nullable
	public KeyAssignment getAssignment() {
		return deviceHelper.findAssignment(appMode, deviceId, assignmentId);
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
