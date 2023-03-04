package net.osmand.plus.settings.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.uidata.DialogDisplayData;
import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;
import net.osmand.plus.base.dialog.interfaces.IDialogDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.IDialogItemSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.CompassMode;

public class CompassModeWidgetDialogController implements IDialogDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "select_compass_mode_on_map";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public CompassModeWidgetDialogController(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@Nullable
	@Override
	public DialogDisplayData getDialogDisplayData(@NonNull String processId) {
		if (processId.equals(PROCESS_ID)) {
			ApplicationMode appMode = settings.getApplicationMode();
			return new CompassModeDisplayDataCreator(app, appMode, true).createDisplayData();
		}
		return null;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DialogDisplayItem selected) {
		CompassMode compassMode = (CompassMode) selected.tag;
		settings.ROTATE_MAP.set(compassMode.getValue());
		app.getMapViewTrackingUtilities().onRotateMapModeChanged();
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
