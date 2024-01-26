package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.DESCRIPTION;
import static net.osmand.plus.base.dialog.data.DialogExtra.DIALOG_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.DRAWABLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayDialogButtonItem;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableQuestionV1BottomSheet;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class BatteryOptimizationController extends BaseDialogController implements IDisplayDataProvider {

	public static final String PROCESS_ID = "disable_battery_optimization";

	private final ApplicationMode appMode;
	private final OsmandSettings settings;

	public BatteryOptimizationController(@NonNull OsmandApplication app,
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

		DisplayDialogButtonItem btnBatterySettings = new DisplayDialogButtonItem()
				.setTitleId(R.string.battery_optimization_settings)
				.setButtonType(DialogButtonType.PRIMARY)
				.setOnClickListener(this::onBatteryOptimizationSettingsClicked);

		DisplayDialogButtonItem btnDoNotAskAnymore = new DisplayDialogButtonItem()
				.setTitleId(R.string.dont_ask_anymore)
				.setButtonType(DialogButtonType.SECONDARY)
				.setOnClickListener(this::onDoNotAskAnymoreClicked);

		DisplayDialogButtonItem btnCloseDialog = new DisplayDialogButtonItem()
				.setTitleId(R.string.shared_string_cancel)
				.setButtonType(DialogButtonType.SECONDARY);

		displayData.putExtra(DIALOG_BUTTONS, new DisplayDialogButtonItem[] {
				btnBatterySettings, btnDoNotAskAnymore, btnCloseDialog
		});

		int warningColor = ColorUtilities.getWarningColor(app, nightMode);
		Drawable brokenTrackIcon = iconsCache.getPaintedIcon(R.drawable.ic_action_track_broken, warningColor);
		displayData.putExtra(DRAWABLE, brokenTrackIcon);
		return displayData;
	}

	private void onBatteryOptimizationSettingsClicked() {
		dialogManager.askDismissDialog(getProcessId());
	}

	private void onDoNotAskAnymoreClicked() {
		dialogManager.askDismissDialog(getProcessId());
	}

	public static boolean isIgnoringBatteryOptimizations(@NonNull OsmandApplication app) {
		String packageName = app.getPackageName();
		PowerManager powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		return powerManager.isIgnoringBatteryOptimizations(packageName);
	}

	public static void askShowDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode, boolean usedOnMap) {
		// check is feature still enabled or if user disabled the display of the dialog
		boolean doNotRequestAgain = false;
		OsmandApplication app = mapActivity.getMyApplication();
		if (!doNotRequestAgain && !isIgnoringBatteryOptimizations(app)) {
			showDialog(mapActivity, appMode, usedOnMap);
		}
	}

	public static void showDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode, boolean usedOnMap) {
		OsmandApplication app = mapActivity.getMyApplication();
		BatteryOptimizationController controller = new BatteryOptimizationController(app, appMode);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableQuestionV1BottomSheet.showInstance(manager, PROCESS_ID, usedOnMap);
	}
}
