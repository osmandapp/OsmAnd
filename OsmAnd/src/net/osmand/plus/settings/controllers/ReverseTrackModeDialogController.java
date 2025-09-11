package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.SHOW_BOTTOM_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.enums.ReverseTrackStrategy;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.ColorUtilities;

public class ReverseTrackModeDialogController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "select_track_reverse_mode";

	private final ApplicationMode appMode;
	private final OsmandSettings settings;
	private OnResultCallback<ReverseTrackStrategy> onResultCallback;

	public ReverseTrackModeDialogController(@NonNull OsmandApplication app,
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
		boolean nightMode = app.getDaynightHelper().isNightMode(appMode, ThemeUsageContext.OVER_MAP);
		int profileColor = appMode.getProfileColor(nightMode);
		int profileColorAlpha = ColorUtilities.getColorWithAlpha(profileColor, 0.3f);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.reverse_mode));
		displayData.putExtra(BACKGROUND_COLOR, profileColorAlpha);
		displayData.putExtra(SHOW_BOTTOM_BUTTONS, true);

		int selectedItemIndex = -1;
		ReverseTrackStrategy selectedStrategy = settings.GPX_REVERSE_STRATEGY.get();

		for (ReverseTrackStrategy strategy : ReverseTrackStrategy.values()) {
			if (strategy.getTitleId() == -1) continue;
			DisplayItem item = new DisplayItem()
					.setTitle(getString(strategy.getTitleId()))
					.setDescription(getString(strategy.getSummaryId()))
					.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
					.setControlsColor(profileColor)
					.setShowBottomDivider(false)
					.setTag(strategy);
			displayData.addDisplayItem(item);

			if (selectedStrategy == strategy) {
				selectedItemIndex = displayData.getItemsSize() - 1;
			}
		}

		displayData.putExtra(SELECTED_INDEX, selectedItemIndex);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		if (selected.getTag() instanceof ReverseTrackStrategy strategy) {
			if (onResultCallback != null) onResultCallback.onResult(strategy);
		}
	}

	public void setOnResultCallback(@NonNull OnResultCallback<ReverseTrackStrategy> callback) {
		this.onResultCallback = callback;
	}

	@Nullable
	public static ReverseTrackModeDialogController getExistedInstance(@NonNull OsmandApplication app) {
		return (ReverseTrackModeDialogController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                              @NonNull OnResultCallback<ReverseTrackStrategy> onResultCallback) {
		OsmandApplication app = mapActivity.getApp();
		ReverseTrackModeDialogController controller = new ReverseTrackModeDialogController(app, appMode);
		controller.setOnResultCallback(onResultCallback);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableSingleSelectionBottomSheet.showInstance(manager, PROCESS_ID, appMode, true);
	}
}
