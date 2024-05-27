package net.osmand.plus.keyevent.fragments.keyassignments;

import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.EDIT_KEY_ASSIGNMENT_ITEM;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.EMPTY_STATE;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.KEY_ASSIGNMENT_ITEM;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.SPACE;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentController;
import net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.ArrayList;
import java.util.List;

class KeyAssignmentsController implements IDialogController {

	public static final String PROCESS_ID = "key_assignments";

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final InputDeviceProfile inputDevice;
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
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		if (inputDevice == null) {
			return new ArrayList<>();
		}
		boolean editMode = isInEditMode();
		List<ScreenItem> screenItems = new ArrayList<>();
		screenItems.add(new ScreenItem(CARD_TOP_DIVIDER));
		if (inputDevice.hasActiveAssignments()) {
			screenItems.add(new ScreenItem(HEADER));
			for (KeyAssignment assignment : inputDevice.getAssignments()) {
				int itemType = editMode ? EDIT_KEY_ASSIGNMENT_ITEM : KEY_ASSIGNMENT_ITEM;
				screenItems.add(new ScreenItem(itemType, assignment));
			}
		} else if (!editMode) {
			screenItems.add(new ScreenItem(EMPTY_STATE));
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void enterEditMode() {
		editBundle = new EditingBundle();
		editBundle.assignments = inputDevice.getAssignmentsCopy();
	}

	public void exitEditMode() {
		editBundle = null;
	}

	public boolean isInEditMode() {
		return editBundle != null;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
	}

	public void askRemoveAssignment() {

	}

	public void askRemoveAllAssignments() {
		if (!inputDevice.hasActiveAssignments()) {
			app.showShortToastMessage(R.string.key_assignments_already_cleared_message);
			return;
		}
		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.reset_key_assignments)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_reset_all, (dialog, which) -> {
					deviceHelper.resetAllAssignments(appMode, inputDevice.getId());
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.reset_key_assignments_desc);
	}

	public boolean isDeviceTypeEditable() {
		return inputDevice != null && inputDevice.isCustom();
	}

	public boolean hasAssignments() {
		return inputDevice.hasActiveAssignments();
	}

	public void askAddAssignment() {
		if (inputDevice != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			EditKeyAssignmentFragment.showInstance(app, fm, appMode, inputDevice.getId(), null);
		}
	}

	public void askEditAssignment(@NonNull KeyAssignment assignment) {
		if (inputDevice != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			EditKeyAssignmentFragment.showInstance(app, fm, appMode, inputDevice.getId(), assignment.getId());
		}
	}

	public void saveChanges() {

	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap, new RequestMapThemeParams().setAppMode(appMode));
	}

	private static class EditingBundle {
		List<KeyAssignment> assignments;
	}

	public static KeyAssignmentsController getInstance(@NonNull OsmandApplication app) {
		return (KeyAssignmentsController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void registerInstance(@NonNull OsmandApplication app,
	                                    @NonNull ApplicationMode appMode,
	                                    boolean usedOnMap) {
		app.getDialogManager().register(PROCESS_ID, new KeyAssignmentsController(app, appMode, usedOnMap));
	}
}
