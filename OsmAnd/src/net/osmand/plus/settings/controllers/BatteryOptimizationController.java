package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.DESCRIPTION;
import static net.osmand.plus.base.dialog.data.DialogExtra.DIALOG_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.DRAWABLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayDialogButtonItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IOnDialogDismissed;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableQuestionBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class BatteryOptimizationController extends BaseDialogController
		implements IDisplayDataProvider, IOnDialogDismissed {

	public static final String PROCESS_ID = "disable_battery_optimization";

	private final OsmandSettings settings;
	private final IOnDialogDismissed callback;

	public BatteryOptimizationController(@NonNull OsmandApplication app,
	                                     @Nullable IOnDialogDismissed callback) {
		super(app);
		this.settings = app.getSettings();
		this.callback = callback;
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
		openBatteryOptimizationSettings();
		dialogManager.askDismissDialog(getProcessId());
	}

	private void onDoNotAskAnymoreClicked() {
		settings.SHOW_BATTERY_OPTIMIZATION_DIALOG.set(false);
		dialogManager.askDismissDialog(getProcessId());
	}

	public void openBatteryOptimizationSettings() {
		Intent intent = new Intent();
		intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
		String packageName = app.getPackageName();
		intent.setData(Uri.parse("package:" + packageName));
		AndroidUtils.startActivityIfSafe(app, intent);
	}

	@Override
	public void onDialogDismissed(@NonNull FragmentActivity activity) {
		askResumePreviousProcess(callback, activity);
	}

	private static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		return app.getSettings().SHOW_BATTERY_OPTIMIZATION_DIALOG.get() && !isIgnoringBatteryOptimizations(app);
	}

	public static boolean isIgnoringBatteryOptimizations(@NonNull OsmandApplication app) {
		String packageName = app.getPackageName();
		PowerManager powerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		return powerManager.isIgnoringBatteryOptimizations(packageName);
	}

	public static void askShowDialog(@NonNull FragmentActivity activity, boolean usedOnMap,
	                                 @Nullable IOnDialogDismissed callback) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (shouldShowDialog(app)) {
			showDialog(activity, usedOnMap, callback);
		} else {
			askResumePreviousProcess(callback, activity);
		}
	}

	public static void showDialog(@NonNull FragmentActivity activity, boolean usedOnMap,
	                              @Nullable IOnDialogDismissed callback) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		BatteryOptimizationController controller = new BatteryOptimizationController(app, callback);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = activity.getSupportFragmentManager();
		CustomizableQuestionBottomSheet.showInstance(manager, PROCESS_ID, usedOnMap);
	}

	private static void askResumePreviousProcess(@Nullable IOnDialogDismissed callback,
	                                             @NonNull FragmentActivity activity) {
		if (callback != null) {
			callback.onDialogDismissed(activity);
		}
	}
}
