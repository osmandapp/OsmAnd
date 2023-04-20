package net.osmand.plus.settings.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.CompassMode;

public class CompassModeWidgetDialogController implements IDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "select_compass_mode_on_map";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public CompassModeWidgetDialogController(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		if (processId.equals(PROCESS_ID)) {
			ApplicationMode appMode = settings.getApplicationMode();
			return new CompassModeDisplayDataCreator(app, appMode, true).createDisplayData();
		}
		return null;
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
		OsmandApplication app = mapActivity.getMyApplication();
		CompassModeWidgetDialogController controller = new CompassModeWidgetDialogController(app);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableSingleSelectionBottomSheet.showInstance(manager, PROCESS_ID, true);
	}
}
