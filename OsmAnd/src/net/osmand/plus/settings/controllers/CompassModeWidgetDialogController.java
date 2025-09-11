package net.osmand.plus.settings.controllers;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.CompassMode;

public class CompassModeWidgetDialogController extends BaseCompassModeDialogController {

	public static final String PROCESS_ID = "select_compass_mode_on_map";

	public CompassModeWidgetDialogController(OsmandApplication app) {
		super(app, app.getSettings().getApplicationMode());
	}

	@NonNull @Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		Object newValue = selected.getTag();
		if (newValue instanceof CompassMode) {
			CompassMode compassMode = (CompassMode) newValue;
			app.getMapViewTrackingUtilities().switchCompassModeTo(compassMode);
		}
	}

	public static void showDialog(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		CompassModeWidgetDialogController controller = new CompassModeWidgetDialogController(app);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableSingleSelectionBottomSheet.showInstance(manager, PROCESS_ID, null, true);
	}
}
