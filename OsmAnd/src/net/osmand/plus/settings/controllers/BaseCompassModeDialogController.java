package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.SUBTITLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseCompassModeDialogController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	private final ApplicationMode appMode;

	public BaseCompassModeDialogController(@NonNull OsmandApplication app,
	                                       @NonNull ApplicationMode appMode) {
		super(app);
		this.appMode = appMode;
	}

	@Nullable @Override
	public DisplayData getDisplayData(@NonNull String processId) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = isNightMode();
		int profileColor = appMode.getProfileColor(nightMode);
		int profileColorAlpha = ColorUtilities.getColorWithAlpha(profileColor, 0.3f);
		UiUtilities iconsCache = app.getUIUtilities();

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.rotate_map_to));
		displayData.putExtra(SUBTITLE, getString(R.string.compass_click_desc));
		displayData.putExtra(BACKGROUND_COLOR, profileColorAlpha);
		for (CompassMode compassMode : CompassMode.values()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(compassMode.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_radio_btn)
					.setIcon(iconsCache.getIcon(compassMode.getIconId(nightMode)))
					.setControlsColor(profileColor)
					.setTag(compassMode));
		}
		CompassMode selected = settings.getCompassMode(appMode);
		displayData.putExtra(SELECTED_INDEX, selected.ordinal());
		return displayData;
	}
}
