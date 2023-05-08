package net.osmand.plus.settings.controllers;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.uidata.DialogDisplayData;
import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class CompassModeDisplayDataCreator {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ApplicationMode appMode;
	private final boolean usedOnMap;

	public CompassModeDisplayDataCreator(@NonNull OsmandApplication app,
	                                     @NonNull ApplicationMode appMode,
	                                     boolean usedOnMap) {
		this.app = app;
		this.appMode = appMode;
		this.usedOnMap = usedOnMap;
		this.settings = app.getSettings();
	}

	@NonNull
	public DialogDisplayData createDisplayData() {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = isNightMode();
		int controlsColor = appMode.getProfileColor(nightMode);
		UiUtilities iconsCache = app.getUIUtilities();
		DialogDisplayData displayData = new DialogDisplayData();
		List<DialogDisplayItem> items = new ArrayList<>();

		displayData.setTitle(getString(R.string.rotate_map_to));
		displayData.setDescription(getString(R.string.compass_click_desc));
		for (CompassMode compassMode : CompassMode.values()) {
			DialogDisplayItem item = new DialogDisplayItem();
			item.title = getString(compassMode.getTitleId());
			item.layoutId = R.layout.bottom_sheet_item_with_descr_and_radio_btn;
			item.normalIcon = item.selectedIcon = iconsCache.getIcon(compassMode.getIconId(nightMode));
			item.customControlsColor = controlsColor;
			item.tag = compassMode;
			items.add(item);
		}
		displayData.setDisplayItems(items);

		CompassMode compassMode = settings.getCompassMode(appMode);
		displayData.setSelectedItemIndex(compassMode.ordinal());
		return displayData;
	}

	private String getString(int stringId) {
		return app.getString(stringId);
	}

	private boolean isNightMode() {
		return usedOnMap ?
				app.getDaynightHelper().isNightModeForMapControlsForProfile(appMode) :
				!settings.isLightContentForMode(appMode);
	}

}
