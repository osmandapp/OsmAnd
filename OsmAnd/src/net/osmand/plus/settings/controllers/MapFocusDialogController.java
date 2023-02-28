package net.osmand.plus.settings.controllers;

import static net.osmand.plus.settings.fragments.ApplyQueryType.BOTTOM_SHEET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.uidata.DialogDisplayData;
import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;
import net.osmand.plus.base.dialog.interfaces.IDialogDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.IDialogItemSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapFocus;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MapFocusDialogController implements IDialogDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "select_map_focus";

	private final OsmandApplication app;
	private final ApplicationMode appMode;
	private final OsmandSettings settings;
	private OnConfirmPreferenceChange preferenceChangeCallback;

	public MapFocusDialogController(@NonNull OsmandApplication app,
	                                @NonNull ApplicationMode appMode) {
		this.app = app;
		this.appMode = appMode;
		this.settings = app.getSettings();
	}

	public void setCallback(@NonNull OnConfirmPreferenceChange preferenceChangeCallback) {
		this.preferenceChangeCallback = preferenceChangeCallback;
	}

	@Nullable
	@Override
	public DialogDisplayData getDialogDisplayData(@NonNull String processId) {
		UiUtilities iconsCache = app.getUIUtilities();
		boolean nightMode = !settings.isLightContentForMode(appMode);

		DialogDisplayData displayData = new DialogDisplayData();
		List<DialogDisplayItem> items = new ArrayList<>();
		displayData.setTitle(app.getString(R.string.display_position));
		int profileColor = appMode.getProfileColor(nightMode);
		for (MapFocus mapFocus : MapFocus.values()) {
			DialogDisplayItem item = new DialogDisplayItem();
			item.title = app.getString(mapFocus.getTitleId());
			item.layoutId = R.layout.bottom_sheet_item_with_bottom_descr_and_radio_btn;
			if (mapFocus == MapFocus.AUTOMATIC) {
				item.description = app.getString(R.string.display_position_automatic_descr);
			}
			item.normalIcon = iconsCache.getThemedIcon(mapFocus.getIconId());
			item.selectedIcon = iconsCache.getPaintedIcon(mapFocus.getIconId(), profileColor);
			item.customControlsColor = profileColor;
			item.addDividerAfter = mapFocus != MapFocus.AUTOMATIC;
			item.tag = mapFocus;
			items.add(item);
		}
		displayData.setDisplayItems(items);

		int selectedValue = settings.POSITION_PLACEMENT_ON_MAP.getModeValue(appMode);
		int selectedItemIndex = MapFocus.getByValue(selectedValue).ordinal();
		displayData.setSelectedItemIndex(selectedItemIndex);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId,
	                                 @NonNull DialogDisplayItem selected) {
		Object newValue = selected.tag;
		if (newValue instanceof MapFocus) {
			String prefId = settings.POSITION_PLACEMENT_ON_MAP.getId();
			MapFocus mapFocus = (MapFocus) newValue;
			Object value = mapFocus.getValue();
			preferenceChangeCallback.onConfirmPreferenceChange(prefId, value, BOTTOM_SHEET);
		}
	}
}
