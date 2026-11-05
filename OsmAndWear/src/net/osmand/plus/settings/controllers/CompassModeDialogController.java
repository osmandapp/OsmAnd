package net.osmand.plus.settings.controllers;

import static net.osmand.plus.settings.fragments.ApplyQueryType.SNACK_BAR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;

public class CompassModeDialogController extends BaseCompassModeDialogController {

	public static final String PROCESS_ID = "select_compass_mode_on_preferences_screen";

	private OnConfirmPreferenceChange preferenceChangeCallback;

	public CompassModeDialogController(@NonNull OsmandApplication app,
	                                   @NonNull ApplicationMode appMode) {
		super(app, appMode);
	}

	@NonNull @Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setCallback(@NonNull OnConfirmPreferenceChange preferenceChangeCallback) {
		this.preferenceChangeCallback = preferenceChangeCallback;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId,
	                                 @NonNull DisplayItem selected) {
		Object newValue = selected.getTag();
		if (newValue instanceof CompassMode) {
			OsmandSettings settings = app.getSettings();
			String prefId = settings.ROTATE_MAP.getId();
			CompassMode compassMode = (CompassMode) newValue;
			Object value = compassMode.getValue();
			preferenceChangeCallback.onConfirmPreferenceChange(prefId, value, SNACK_BAR);
		}
	}
}
