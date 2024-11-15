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
import androidx.fragment.app.FragmentManager;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.selectkeycode.OnKeyCodeSelectedCallback;
import net.osmand.plus.keyevent.fragments.selectkeycode.SelectKeyCodeFragment;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
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

	public static final String TRANSITION_NAME = "shared_element_container";

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final DialogManager dialogManager;
	private final InputDevicesHelper deviceHelper;
	private final String deviceId;
	private final String assignmentId;
	private EditingBundle initialBundle;
	private EditingBundle editBundle;

	public EditKeyAssignmentController(@NonNull OsmandApplication app,
	                                   @NonNull ApplicationMode appMode,
									   @NonNull String deviceId,
									   @Nullable String assignmentId) {
		this.app = app;
		this.appMode = appMode;
		this.dialogManager = app.getDialogManager();
		this.deviceHelper = app.getInputDeviceHelper();
		this.deviceId = deviceId;
		this.assignmentId = assignmentId;
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

	public void registerDialog(@NonNull IDialog dialog) {
		dialogManager.register(PROCESS_ID, dialog);
	}

	public void finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(PROCESS_ID);
			dialogManager.unregister(AddQuickActionController.PROCESS_ID);
		}
	}

	public void showOverflowMenu(@NonNull FragmentActivity activity, @Nullable View actionView) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_rename)
				.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
				.setOnClickListener(v -> askRenameAssignment(activity))
				.create()
		);
		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> askRemoveAssignment(activity))
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = actionView;
		displayData.menuItems = menuItems;
		displayData.nightMode = isNightMode();
		PopUpMenu.show(displayData);
	}

	public void askRenameAssignment(@NonNull FragmentActivity activity) {
		String oldName = getKeyAssignmentName();
		showEnterNameDialog(activity, oldName, this::onNameEntered);
	}

	private void showEnterNameDialog(@NonNull FragmentActivity activity, @Nullable String oldName,
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

	public void askRemoveAssignment(@NonNull FragmentActivity activity) {
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.remove_key_assignment)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_remove, (dialog, which) -> {
					deviceHelper.removeKeyAssignmentCompletely(appMode, deviceId, assignmentId);
					askDismissDialog();
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.remove_key_assignment_summary);
	}

	@Nullable
	private String getKeyAssignmentName() {
		KeyAssignment assignment = getAssignment();
		return assignment != null ? assignment.getName(app) : null;
	}

	@Nullable
	public String getDialogTitle() {
		KeyAssignment assignment = getAssignment();
		return assignment != null? assignment.getName(app) : app.getString(R.string.new_key_assignment);
	}

	public void askAddAction(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		AddQuickActionController controller = new AddKeyEventQuickActionController(app);
		AddQuickActionController.showAddQuickActionDialog(app, manager, controller);
	}

	public void askDeleteAction() {
		editBundle.action = null;
		askRefreshDialog();
	}

	public void askEditAction(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		AddQuickActionController controller = new AddKeyEventQuickActionController(app);
		AddQuickActionController.showCreateEditActionDialog(app, manager, controller, editBundle.action);
	}

	public void askDeleteKeyCode(@NonNull Integer keyCode) {
		editBundle.keyCodes.remove(keyCode);
		askRefreshDialog();
	}

	public void askAddKeyCode(@NonNull FragmentActivity activity) {
		showKeyCodeSelectionScreen(activity, KeyEvent.KEYCODE_UNKNOWN);
	}

	public void askChangeKeyCode(@NonNull FragmentActivity activity, int keyCode) {
		showKeyCodeSelectionScreen(activity, keyCode);
	}

	private void showKeyCodeSelectionScreen(@NonNull FragmentActivity activity, int keyCode) {
		SelectKeyCodeFragment.showInstance(
				activity.getSupportFragmentManager(),
				appMode, deviceId, assignmentId, keyCode);
	}

	@Override
	public void onKeyCodeSelected(@Nullable Integer oldKeyCode, @NonNull Integer newKeyCode) {
		if (editBundle != null) {
			if (oldKeyCode != null && oldKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
				editBundle.keyCodes.remove(oldKeyCode);
			}
			editBundle.keyCodes.add(newKeyCode);
		}
		askRefreshDialog();
	}

	public void enterEditMode() {
		KeyAssignment assignment = getAssignment();
		editBundle = new EditingBundle();
		initialBundle = new EditingBundle();
		if (assignment != null) {
			editBundle.action = assignment.getAction();
			editBundle.keyCodes = new ArrayList<>(assignment.getKeyCodes());
			initialBundle.action = editBundle.action;
			initialBundle.keyCodes = new ArrayList<>(editBundle.keyCodes);
		}
		askRefreshDialog();
	}

	public void askExitEditMode(@Nullable FragmentActivity activity) {
		if (activity != null && hasChanges()) {
			AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
					.setTitle(R.string.discard_changes_prompt)
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_continue, (dialog, which) -> exitEditMode());
			CustomAlert.showSimpleMessage(dialogData, R.string.unsaved_changes_will_be_lost);
		} else {
			exitEditMode();
		}
	}

	private void exitEditMode() {
		editBundle = null;
		initialBundle = null;
		if (isNewAssignment()) {
			askDismissDialog();
		} else {
			askRefreshDialog();
		}
	}

	public boolean hasChanges() {
		return !Objects.equals(initialBundle, editBundle);
	}

	public boolean hasChangesToSave() {
		return editBundle != null && editBundle.action != null && !Algorithms.isEmpty(editBundle.keyCodes);
	}

	public boolean isKeyCodeAlreadyAssignedToThisAction(@NonNull Integer keyCode) {
		return editBundle != null && editBundle.keyCodes.contains(keyCode);
	}

	@Nullable
	public QuickAction getSelectedAction() {
		return editBundle != null ? editBundle.action : null;
	}

	public void askSaveChanges() {
		if (isNewAssignment()) {
			Integer[] keyCodesArray = new Integer[editBundle.keyCodes.size()];
			KeyAssignment assignment = new KeyAssignment(editBundle.action, editBundle.keyCodes.toArray(keyCodesArray));
			deviceHelper.addAssignment(appMode, deviceId, assignment);
		} else {
			deviceHelper.updateAssignment(appMode, deviceId, assignmentId, editBundle.action, editBundle.keyCodes);
		}
		askDismissDialog();
	}

	@Nullable
	public KeyAssignment getAssignment() {
		return deviceHelper.findAssignment(appMode, deviceId, assignmentId);
	}

	public boolean isInEditMode() {
		return editBundle != null;
	}

	public boolean isNewAssignment() {
		return assignmentId == null;
	}

	private void askRefreshDialog() {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID);
	}

	private void askDismissDialog() {
		dialogManager.askDismissDialog(PROCESS_ID);
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(false);
	}

	@Nullable
	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
	}

	private static class EditingBundle {
		QuickAction action;
		List<Integer> keyCodes = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof EditingBundle)) return false;

			EditingBundle that = (EditingBundle) o;

			if (!Objects.equals(action, that.action)) return false;
			return Objects.equals(keyCodes, that.keyCodes);
		}

		@Override
		public int hashCode() {
			int result = action != null ? action.hashCode() : 0;
			result = 31 * result + (keyCodes != null ? keyCodes.hashCode() : 0);
			return result;
		}
	}

	public static void showEditAssignmentDialog(@NonNull FragmentActivity activity,
	                                            @NonNull ApplicationMode appMode,
	                                            @NonNull String deviceId,
	                                            @NonNull String assignmentId,
	                                            @Nullable View anchorView) {
		showDialog(activity, appMode, deviceId, assignmentId, anchorView);
	}

	public static void showAddAssignmentDialog(@NonNull FragmentActivity activity,
	                                           @NonNull ApplicationMode appMode,
	                                           @NonNull String deviceId,
	                                           @Nullable View anchorView) {
		showDialog(activity, appMode, deviceId, null, anchorView);
	}

	private static void showDialog(@NonNull FragmentActivity activity,
	                               @NonNull ApplicationMode appMode,
	                               @NonNull String deviceId,
	                               @Nullable String assignmentId,
	                               @Nullable View anchorView) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		EditKeyAssignmentController controller =
				new EditKeyAssignmentController(app, appMode, deviceId, assignmentId);
		if (assignmentId == null) {
			controller.enterEditMode();
		}
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);
		if (!EditKeyAssignmentFragment.showInstance(activity, appMode, anchorView)) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	@Nullable
	public static EditKeyAssignmentController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (EditKeyAssignmentController) dialogManager.findController(PROCESS_ID);
	}

	public class AddKeyEventQuickActionController extends AddQuickActionController {

		public AddKeyEventQuickActionController(@NonNull OsmandApplication app) {
			super(app);
		}

		@NonNull
		@Override
		public QuickAction produceQuickAction(boolean isNew, int type, long actionId) {
			return isNew ? mapButtonsHelper.newActionByType(type) : createActionCopy(editBundle.action);
		}

		@Nullable
		private QuickAction createActionCopy(@Nullable QuickAction action) {
			return action != null ? MapButtonsHelper.produceAction(action) : null;
		}

		@Override
		public boolean isNameUnique(@NonNull QuickAction action) {
			return true;
		}

		@Override
		public QuickAction generateUniqueActionName(@NonNull QuickAction action) {
			return null;
		}

		@Override
		public void askSaveAction(boolean isNew, @NonNull QuickAction action) {
			editBundle.action = action;
			askRefreshDialog();
		}

		@Override
		public void askRemoveAction(@NonNull QuickAction action) {
			askDeleteAction();
		}
	}
}
