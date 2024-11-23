package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.MapFocus;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class MapFocusDialogController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "select_map_focus";

	private final ApplicationMode appMode;
	private final OsmandSettings settings;

	public MapFocusDialogController(@NonNull OsmandApplication app,
	                                @NonNull ApplicationMode appMode) {
		super(app);
		this.appMode = appMode;
		this.settings = app.getSettings();
	}

	@NonNull @Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		int dividerStartPadding = app.getResources()
				.getDimensionPixelSize(R.dimen.bottom_sheet_divider_margin_start);
		UiUtilities iconsCache = app.getUIUtilities();
		boolean nightMode = !settings.isLightContentForMode(appMode);
		int profileColor = appMode.getProfileColor(nightMode);
		int profileColorAlpha = ColorUtilities.getColorWithAlpha(profileColor, 0.3f);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.display_position));
		displayData.putExtra(BACKGROUND_COLOR, profileColorAlpha);
		for (MapFocus mapFocus : MapFocus.values()) {
			DisplayItem item = new DisplayItem()
					.setTitle(getString(mapFocus.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_with_bottom_descr_and_radio_btn)
					.setNormalIcon(iconsCache.getThemedIcon(mapFocus.getIconId()))
					.setSelectedIcon(iconsCache.getPaintedIcon(mapFocus.getIconId(), profileColor))
					.setControlsColor(profileColor)
					.setTag(mapFocus);
			if (mapFocus == MapFocus.AUTOMATIC) {
				item.setDescription(getString(R.string.display_position_automatic_descr));
			}
			if (mapFocus != MapFocus.AUTOMATIC) {
				item.setShowBottomDivider(true, dividerStartPadding);
			}
			displayData.addDisplayItem(item);
		}

		int value = settings.POSITION_PLACEMENT_ON_MAP.getModeValue(appMode);
		int selectedItemIndex = MapFocus.valueOf(value).ordinal();
		displayData.putExtra(SELECTED_INDEX, selectedItemIndex);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		Object newValue = selected.getTag();
		if (newValue instanceof MapFocus) {
			MapFocus mapFocus = (MapFocus) newValue;
			settings.POSITION_PLACEMENT_ON_MAP.setModeValue(appMode, mapFocus.getValue());
		}
	}

	public static void showDialog(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode) {
		OsmandApplication app = mapActivity.getMyApplication();
		MapFocusDialogController controller = new MapFocusDialogController(app, appMode);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableSingleSelectionBottomSheet.showInstance(manager, PROCESS_ID, true);
	}
}
