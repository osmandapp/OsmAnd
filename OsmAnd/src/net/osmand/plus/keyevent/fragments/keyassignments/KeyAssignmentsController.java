package net.osmand.plus.keyevent.fragments.keyassignments;

import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_BOTTOM_SHADOW;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.CARD_TOP_DIVIDER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.HEADER;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.KEY_ASSIGNMENT_ITEM;
import static net.osmand.plus.keyevent.fragments.keyassignments.KeyAssignmentsAdapter.SPACE;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignmentCategory;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentFragment;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class KeyAssignmentsController {

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final InputDevicesHelper deviceHelper;
	private final InputDeviceProfile inputDevice;
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
		if (inputDevice == null || inputDevice.getAssignmentsCount() == 0) {
			return new ArrayList<>();
		}
		List<ScreenItem> screenItems = new ArrayList<>();
		Map<KeyAssignmentCategory, List<KeyAssignment>> categorizedAssignments = inputDevice.getCategorizedAssignments();
		for (KeyAssignmentCategory category : KeyAssignmentCategory.values()) {
			List<KeyAssignment> assignments = categorizedAssignments.get(category);
			if (Algorithms.isEmpty(assignments)) continue;

			String categoryName = app.getString(category.getTitleId());
			screenItems.add(new ScreenItem(category.ordinal() > 0 ? CARD_DIVIDER : CARD_TOP_DIVIDER, categoryName));
			screenItems.add(new ScreenItem(HEADER, categoryName));
			for (KeyAssignment assignment : assignments) {
				screenItems.add(new ScreenItem(KEY_ASSIGNMENT_ITEM, assignment));
			}
		}
		screenItems.add(new ScreenItem(CARD_BOTTOM_SHADOW));
		screenItems.add(new ScreenItem(SPACE));
		return screenItems;
	}

	public void setActivity(FragmentActivity activity) {
		this.activity = activity;
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

	public void askEditAssignment(@NonNull KeyAssignment assignment) {
		if (inputDevice != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			EditKeyAssignmentFragment.showInstance(fm, appMode, inputDevice.getId(), assignment.getId());
		}
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}
