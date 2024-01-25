package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.DESCRIPTION;
import static net.osmand.plus.base.dialog.data.DialogExtra.DRAWABLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.PRIMARY_BUTTON_TITLE_ID;
import static net.osmand.plus.base.dialog.data.DialogExtra.SECONDARY_BUTTON_TITLE_ID;
import static net.osmand.plus.base.dialog.data.DialogExtra.TERTIARY_BUTTON_TITLE_ID;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableThreeOptionsBottomSheet;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class DisableBatteryOptimizationController extends BaseDialogController implements IDisplayDataProvider {

	public static final String PROCESS_ID = "disable_battery_optimization";

	private final ApplicationMode appMode;
	private final OsmandSettings settings;

	public DisableBatteryOptimizationController(@NonNull OsmandApplication app,
	                                            @NonNull ApplicationMode appMode) {
		super(app);
		this.appMode = appMode;
		this.settings = app.getSettings();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		UiUtilities iconsCache = app.getUIUtilities();

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.battery_optimization));
		displayData.putExtra(DESCRIPTION, getString(R.string.battery_optimization_desc));
		displayData.putExtra(PRIMARY_BUTTON_TITLE_ID, R.string.battery_optimization_settings);
		displayData.putExtra(SECONDARY_BUTTON_TITLE_ID, R.string.dont_ask_anymore);
		displayData.putExtra(TERTIARY_BUTTON_TITLE_ID, R.string.shared_string_cancel);

		int warningColor = ColorUtilities.getWarningColor(app, nightMode);
		Drawable brokenTrackIcon = iconsCache.getPaintedIcon(R.drawable.ic_action_track_broken, warningColor);
		displayData.putExtra(DRAWABLE, brokenTrackIcon);
		return displayData;
	}

	public static void askShowDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode, boolean usedOnMap) {
		// check is feature still enabled or if user disabled the display of the dialog
		boolean shouldShowDialog = true;
		if (shouldShowDialog) {
			showDialog(mapActivity, appMode, usedOnMap);
		}
	}

	public static void showDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode, boolean usedOnMap) {
		OsmandApplication app = mapActivity.getMyApplication();
		DisableBatteryOptimizationController controller = new DisableBatteryOptimizationController(app, appMode);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableThreeOptionsBottomSheet.showInstance(manager, PROCESS_ID, usedOnMap);
	}
}
