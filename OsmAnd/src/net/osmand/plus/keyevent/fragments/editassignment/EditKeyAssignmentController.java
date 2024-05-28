package net.osmand.plus.keyevent.fragments.editassignment;

import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ADD_ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ADD_KEY_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_ACTION_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_ACTION_OVERVIEW;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_KEYS_OVERVIEW;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.ASSIGNED_KEY_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.HEADER_ITEM;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.LIST_DIVIDER;
import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentAdapter.SPACE;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;

import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.selectkeycode.OnKeyCodeSelectedCallback;
import net.osmand.plus.keyevent.fragments.selectkeycode.SelectKeyCodeFragment;
import net.osmand.plus.quickaction.QuickAction;
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

public class EditKeyAssignmentController implements IDialogController, OnKeyCodeSelectedCallback {

	public static final String PROCESS_ID = "edit_key_assignment";

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final String deviceId;
	private final String assignmentId;
	private EditingBundle editBundle;
	private FragmentActivity activity;
	private final boolean usedOnMap;

	public EditKeyAssignmentController(@NonNull OsmandApplication app,
	                                   @NonNull ApplicationMode appMode,
									   @NonNull String deviceId,
									   @Nullable String assignmentId,
	                                   boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.deviceHelper = app.getInputDeviceHelper();
		this.deviceId = deviceId;
		this.assignmentId = assignmentId;
		if (assignmentId == null) {
			enterEditMode();
		}
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
		screenItems.add(new ScreenItem(HEADER_ITEM, R.string.shared_string_action));
		if (isInEditMode()) {
			if (editBundle.action != null) {
				screenItems.add(new ScreenItem(ASSIGNED_ACTION_ITEM, editBundle.action));
				screenItems.add(new ScreenItem(CARD_DIVIDER));
			} else {
				screenItems.add(new ScreenItem(ADD_ACTION_ITEM));
				screenItems.add(new ScreenItem(LIST_DIVIDER));
			}
		} else {
			KeyAssignment assignment = getAssignment();
			if (assignment != null) {
				screenItems.add(new ScreenItem(ASSIGNED_ACTION_OVERVIEW, assignment.getAction()));
			}
			screenItems.add(new ScreenItem(LIST_DIVIDER));
		}
		screenItems.add(new ScreenItem(HEADER_ITEM, R.string.assigned_keys));
		if (isInEditMode()) {
			for (Integer keyCode : editBundle.keyCodes) {
				screenItems.add(new ScreenItem(ASSIGNED_KEY_ITEM, keyCode));
			}
			screenItems.add(new ScreenItem(ADD_KEY_ITEM));
		} else {
			KeyAssignment assignment = getAssignment();
			if (assignment != null) {
				screenItems.add(new ScreenItem(ASSIGNED_KEYS_OVERVIEW, assignment.getKeyCodes()));
			}
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
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
		showEnterNameDialog(oldName, this::onNameEntered);
	}

	private void showEnterNameDialog(@Nullable String oldName,
	                                 @NonNull OnResultCallback<String> callback) {
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
					callback.onResult(newName.trim());
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
					deviceHelper.removeKeyAssignmentCompletely(appMode, deviceId, assignmentId);
					askDismissDialog();
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_key_assignment_desc);
	}

	@Nullable
	public String getCustomNameSummary() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getName(app) : null;
	}

	@Nullable
	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
	}

	@Nullable
	public String getDialogTitle() {
		KeyAssignment assignment = getAssignment();
		return assignment != null? assignment.getName(app) : app.getString(R.string.new_key_assignment);
	}

	public void askAddAction() {
		// TODO open action menu screen
	}

	public void askDeleteAction() {
		editBundle.action = null;
		askRefreshDialog();
	}

	public void askDeleteKeyCode(Integer keyCode) {
		editBundle.keyCodes.remove(keyCode);
		askRefreshDialog();
	}

	public void askAddKeyCode() {
		askChangeKeyCode(KeyEvent.KEYCODE_UNKNOWN);
	}

	public void askChangeKeyCode(int keyCode) {
		SelectKeyCodeFragment.showInstance(
				activity.getSupportFragmentManager(),
				appMode, deviceId, assignmentId, keyCode);
	}

	@Override
	public void onKeyCodeSelected(int oldKeyCode, int newKeyCode) {
		if (editBundle != null) {
			editBundle.keyCodes.add((Integer) newKeyCode);
		}
		askRefreshDialog();
	}

	public void enterEditMode() {
		KeyAssignment assignment = getAssignment();
		editBundle = new EditingBundle();
		if (assignment != null) {
			editBundle.action = assignment.getAction();
			editBundle.keyCodes = assignment.getKeyCodes();
		} else {
			editBundle.keyCodes = new ArrayList<>();
		}
	}

	public void exitEditMode() {
		editBundle = null;
	}

	public boolean isInEditMode() {
		return editBundle != null;
	}

	public boolean isNewAssignment() {
		return assignmentId == null;
	}

	public boolean hasChangesToSave() {
		return editBundle != null && editBundle.action != null && !Algorithms.isEmpty(editBundle.keyCodes);
	}

	@Nullable
	public QuickAction getSelectedAction() {
		return editBundle != null ? editBundle.action : null;
	}

	public void askSaveChanges() {
		if (isNewAssignment()) {

		} else {

		}
	}

	@Nullable
	public KeyAssignment getAssignment() {
		return deviceHelper.findAssignment(appMode, deviceId, assignmentId);
	}

	private void askRefreshDialog() {
		app.getDialogManager().askRefreshDialogCompletely(PROCESS_ID);
	}

	private void askDismissDialog() {
		app.getDialogManager().askDismissDialog(PROCESS_ID);
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	private static class EditingBundle {
		QuickAction action;
		List<Integer> keyCodes;
	}

	public static void createInstance(@NonNull OsmandApplication app,
	                                  @NonNull ApplicationMode appMode,
	                                  @NonNull String deviceId,
	                                  @Nullable String assignmentId,
	                                  boolean usedOnMap) {
		app.getDialogManager().register(
				PROCESS_ID, new EditKeyAssignmentController(app, appMode, deviceId, assignmentId, usedOnMap)
		);
	}

	@Nullable
	public static EditKeyAssignmentController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (EditKeyAssignmentController) dialogManager.findController(PROCESS_ID);
	}
}
