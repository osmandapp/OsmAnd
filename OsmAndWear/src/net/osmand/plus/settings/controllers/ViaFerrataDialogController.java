package net.osmand.plus.settings.controllers;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.SHOW_BOTTOM_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.SUBTITLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;
import static net.osmand.plus.settings.fragments.ApplyQueryType.SNACK_BAR;

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
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.router.GeneralRouter.RoutingParameter;

public class ViaFerrataDialogController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "via_ferrata";

	private final ApplicationMode appMode;
	private final OsmandSettings settings;
	private final RoutingParameter parameter;
	private OnConfirmPreferenceChange preferenceChangeCallback;

	public ViaFerrataDialogController(@NonNull OsmandApplication app,
	                                  @NonNull ApplicationMode appMode,
	                                  @NonNull RoutingParameter parameter) {
		super(app);
		this.appMode = appMode;
		this.settings = app.getSettings();
		this.parameter = parameter;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setCallback(@NonNull OnConfirmPreferenceChange preferenceChangeCallback) {
		this.preferenceChangeCallback = preferenceChangeCallback;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		int profileColor = appMode.getProfileColor(nightMode);
		int profileColorAlpha = ColorUtilities.getColorWithAlpha(profileColor, 0.3f);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.routing_attr_allow_via_ferrata_name));
		displayData.putExtra(SUBTITLE, getString(R.string.routing_attr_allow_via_ferrata_description));
		displayData.putExtra(BACKGROUND_COLOR, profileColorAlpha);
		displayData.putExtra(SHOW_BOTTOM_BUTTONS, true);

		String[] titles = new String[] {
				getString(R.string.shared_string_avoid),
				getString(R.string.shared_string_allow)
		};
		String[] descriptions = new String[] {
				null,
				getString(R.string.may_be_used_if_there_is_shortcut)
		};

		for (int i = 0; i < titles.length; i++) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(titles[i])
					.setDescription(descriptions[i])
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_left_radio_btn)
					.setControlsColor(profileColor)
					.setTag(i == 1)
			);
		}

		boolean isAllowed = getPreference().getModeValue(appMode);
		displayData.putExtra(SELECTED_INDEX, isAllowed ? 1 : 0);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		Object newValue = selected.getTag();
		if (newValue instanceof Boolean) {
			Boolean value = (Boolean) newValue;
			String prefId = getPreference().getId();
			preferenceChangeCallback.onConfirmPreferenceChange(prefId, value, SNACK_BAR);
		}
	}

	@NonNull
	private CommonPreference<Boolean> getPreference() {
		return settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
	}
}
