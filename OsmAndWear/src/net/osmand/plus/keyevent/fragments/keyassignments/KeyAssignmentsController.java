package net.osmand.plus.keyevent.fragments.keyassignments;

import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.EDIT_KEY_ASSIGNMENT_ITEM;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.EMPTY_STATE;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.KEY_ASSIGNMENT_ITEM;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.SPACE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentController;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyAssignmentsController implements IDialogController {

	public static final String PROCESS_ID = "key_assignments";

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final InputDeviceProfile inputDevice;
	private final DialogManager dialogManager;
	private EditingBundle initialBundle;
	private EditingBundle editBundle;
	private FragmentActivity activity;
	private final boolean usedOnMap;

	public KeyAssignmentsController(@NonNull OsmandApplication app,
	                                @NonNull ApplicationMode appMode,
	                                boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.deviceHelper = app.getInputDeviceHelper();
		this.inputDevice = deviceHelper.getSelectedDevice(appMode);
		this.dialogManager = app.getDialogManager();
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		if (inputDevice == null) {
			return new ArrayList<>();
		}
		boolean editMode = isInEditMode();
		List<ScreenItem> screenItems = new ArrayList<>();
		int itemType = editMode ? EDIT_KEY_ASSIGNMENT_ITEM : KEY_ASSIGNMENT_ITEM;
		List<KeyAssignment> assignments = editMode ? editBundle.getFilledAssignments() : inputDevice.getFilledAssignments();
		if (!Algorithms.isEmpty(assignments)) {
			screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
			screenItems.add(new ScreenItem(HEADER));
			for (KeyAssignment assignment : assignments) {
				screenItems.add(new ScreenItem(itemType, assignment));
			}
		} else if (!editMode) {
			screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
			screenItems.add(new ScreenItem(EMPTY_STATE));
		}
		if (!Algorithms.isEmpty(screenItems)) {
			screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
			screenItems.add(new ScreenItem(SPACE));
		}
		return screenItems;
	}

	public void enterEditMode() {
		editBundle = new EditingBundle();
		editBundle.assignments = inputDevice.getAssignmentsCopy();
		initialBundle = new EditingBundle();
		initialBundle.assignments = new ArrayList<>(editBundle.assignments);
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

	public void exitEditMode() {
		editBundle = null;
		initialBundle = null;
		askRefreshDialog();
	}

	public boolean isInEditMode() {
		return editBundle != null;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	public void askRemoveAssignment(@NonNull KeyAssignment assignment) {
		editBundle.assignments.remove(assignment);
		askRefreshDialog();
	}

	public void askRemoveAllAssignments() {
		if (!inputDevice.hasActiveAssignments()) {
			app.showShortToastMessage(R.string.key_assignments_already_cleared_message);
			return;
		}
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.clear_all_key_shortcuts)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_remove, (dialog, which) -> removeAllAssignments());
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_all_key_shortcuts_summary);
	}

	private void removeAllAssignments() {
		deviceHelper.clearAllAssignments(appMode, inputDevice.getId());
		exitEditMode();
	}

	public boolean isDeviceTypeEditable() {
		return inputDevice != null && inputDevice.isCustom();
	}

	public boolean hasAssignments() {
		return inputDevice.hasActiveAssignments();
	}

	public void askAddAssignment(@Nullable View sharedElement) {
		if (inputDevice != null) {
			EditKeyAssignmentController.showAddAssignmentDialog(activity, appMode, inputDevice.getId(), sharedElement);
		}
	}

	public void askEditAssignment(@NonNull KeyAssignment assignment, @NonNull View anchorView) {
		if (inputDevice != null) {
			EditKeyAssignmentController.showEditAssignmentDialog(activity, appMode, inputDevice.getId(), assignment.getId(), anchorView);
		}
	}

	public boolean hasChanges() {
		return !Objects.equals(initialBundle, editBundle);
	}

	public void askSaveChanges() {
		deviceHelper.saveUpdatedAssignmentsList(appMode, inputDevice.getId(), editBundle.assignments);
		exitEditMode();
	}

	public void registerDialog(@NonNull IDialog dialog) {
		dialogManager.register(PROCESS_ID, dialog);
	}

	public void unregisterDialogIfNeeded(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			unregisterDialog();
		}
	}

	private void unregisterDialog() {
		dialogManager.unregister(PROCESS_ID);
	}

	private void askRefreshDialog() {
		dialogManager.askRefreshDialogCompletely(PROCESS_ID);
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap, new RequestMapThemeParams().setAppMode(appMode));
	}

	private static class EditingBundle {
		private List<KeyAssignment> assignments;

		@NonNull
		public List<KeyAssignment> getFilledAssignments() {
			List<KeyAssignment> result = new ArrayList<>();
			for (KeyAssignment assignment : assignments) {
				if (assignment.hasRequiredParameters()) {
					result.add(assignment);
				}
			}
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof EditingBundle)) return false;

			EditingBundle that = (EditingBundle) o;
			return Objects.equals(assignments, that.assignments);
		}

		@Override
		public int hashCode() {
			return assignments != null ? assignments.hashCode() : 0;
		}
	}

	public static void showKeyAssignmentsDialog(@NonNull OsmandApplication app,
	                                            @NonNull FragmentManager fragmentManager,
	                                            @NonNull ApplicationMode appMode,
	                                            boolean usedOnMap) {
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new KeyAssignmentsController(app, appMode, usedOnMap));
		if (!KeyAssignmentsFragment.showInstance(fragmentManager, appMode)) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	@Nullable
	public static KeyAssignmentsController getExistedInstance(@NonNull OsmandApplication app) {
		return (KeyAssignmentsController) app.getDialogManager().findController(PROCESS_ID);
	}
}
