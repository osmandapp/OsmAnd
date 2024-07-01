package net.osmand.plus.settings.fragments;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.SHOW_BOTTOM_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.SUBTITLE;
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
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.utils.ColorUtilities;

public class ProfileOptionsController extends BaseDialogController implements IDisplayDataProvider, IDialogItemSelected {

	public static final String PROCESS_ID = "profile_appearance_options_controller";

	private final ApplicationMode appMode;
	private String title;
	private String description;
	private CommonPreference<MarkerDisplayOption> preference;

	public ProfileOptionsController(@NonNull OsmandApplication app,
	                                @NonNull ApplicationMode appMode) {
		super(app);
		this.appMode = appMode;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		Object newValue = selected.getTag();
		if (newValue instanceof MarkerDisplayOption) {
			onItemSelected((MarkerDisplayOption) newValue, preference);
		}
	}

	public MarkerDisplayOption getSelectedItem(@NonNull CommonPreference<MarkerDisplayOption> preference) {
		return preference.getModeValue(appMode);
	}

	public void onItemSelected(@NonNull MarkerDisplayOption displayOption, @NonNull CommonPreference<MarkerDisplayOption> preference) {
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		int profileColor = appMode.getProfileColor(nightMode);
		int profileColorAlpha = ColorUtilities.getColorWithAlpha(profileColor, 0.3f);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, title);
		displayData.putExtra(SUBTITLE, description);
		displayData.putExtra(BACKGROUND_COLOR, profileColorAlpha);
		displayData.putExtra(SHOW_BOTTOM_BUTTONS, true);
		for (MarkerDisplayOption displayOption : MarkerDisplayOption.values()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(displayOption.getNameRes()))
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn_left)
					.setControlsColor(profileColor)
					.setTag(displayOption));
		}
		displayData.putExtra(SELECTED_INDEX, getSelectedItem(preference).ordinal());
		return displayData;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void showDialog(@NonNull MapActivity mapActivity, @NonNull String title, @NonNull String description, @NonNull CommonPreference<MarkerDisplayOption> preference) {
		this.title = title;
		this.description = description;
		this.preference = preference;

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, this);

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		CustomizableSingleSelectionBottomSheet.showInstance(manager, PROCESS_ID, true);
	}
}


