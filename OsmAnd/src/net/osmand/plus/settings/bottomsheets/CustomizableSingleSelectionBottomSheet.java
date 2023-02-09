package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.displaydata.DialogDisplayDataProvider;
import net.osmand.plus.settings.bottomsheets.displaydata.DialogDisplayItem;
import net.osmand.plus.settings.bottomsheets.displaydata.DialogDisplayData;
import net.osmand.plus.settings.bottomsheets.displaydata.OnDialogItemSelectedListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

/**
 * Implementation of Single Selection Bottom Sheet.
 * Only displays the data passed to it in the form of DialogDisplayData.
 * When choosing one of the options, the selected option is passed to the target fragment
 * and dialog automatically closed without the need for confirmation by the user.
 *
 * To use this dialog you should implement two interfaces in your class:
 * 1. DisplayDataProvider to collect and prepare the data for display.
 * 2. OnDialogItemSelectedListener to process selected result.
 *
 * "Dialog id" need to recognize the process for which the dialog was called,
 * to provide the correct data for display and to correctly process selected
 * result in target fragment.
 */
public class CustomizableSingleSelectionBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = CustomizableSingleSelectionBottomSheet.class.getSimpleName();

	private static final String DIALOG_ID = "dialog_id";
	private static final String SELECTED_ITEM_INDEX = "selected_item_index";

	private String dialogId;
	private int selectedItemIndex;
	private DialogDisplayData dataToDisplay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			dialogId = args.getString(DIALOG_ID);
			selectedItemIndex = args.getInt(SELECTED_ITEM_INDEX, -1);
			refreshDataToDisplay();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null || dataToDisplay == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);

		TitleItem titleItem = new TitleItem(dataToDisplay.getTitle());
		items.add(titleItem);

		int dividerStartPadding = getDimen(R.dimen.bottom_sheet_divider_margin_start);
		List<DialogDisplayItem> displayItems = dataToDisplay.getDisplayItems();
		for (int i = 0; i < displayItems.size(); i++) {
			DialogDisplayItem displayItem = displayItems.get(i);
			boolean isChecked = i == selectedItemIndex;
			Drawable icon = isChecked ? displayItem.selectedIcon : displayItem.normalIcon;
			BaseBottomSheetItem[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(isChecked)
					.setButtonTintList(AndroidUtils.createCheckedColorIntStateList(
							ColorUtilities.getColor(ctx, R.color.icon_color_default_light),
							isProfileDependent() ?
									getAppMode().getProfileColor(nightMode) :
									ColorUtilities.getActiveColor(ctx, nightMode)))
					.setDescription(displayItem.description)
					.setTitle(displayItem.title)
					.setIcon(icon)
					.setTag(i)
					.setLayoutId(displayItem.layoutId)
					.setOnClickListener(v -> {
						selectedItemIndex = (int) preferenceItem[0].getTag();
						onItemSelected(displayItem);
						dismiss();
					})
					.create();
			items.add(preferenceItem[0]);
			if (i != displayItems.size() - 1) {
				DividerItem divider = new DividerItem(ctx);
				divider.setMargins(dividerStartPadding, 0, 0, 0);
				items.add(divider);
			}
		}
	}

	private void refreshDataToDisplay() {
		Fragment target = getTargetFragment();
		if (target instanceof DialogDisplayDataProvider) {
			DialogDisplayDataProvider dataProvider = (DialogDisplayDataProvider) target;
			dataToDisplay = dataProvider.provideDialogDisplayData(dialogId);
		}
	}

	private void onItemSelected(@NonNull DialogDisplayItem selectedItem) {
		Fragment target = getTargetFragment();
		if (target instanceof OnDialogItemSelectedListener) {
			OnDialogItemSelectedListener listener = (OnDialogItemSelectedListener) target;
			listener.onDialogItemSelected(dialogId, selectedItem);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(DIALOG_ID, dialogId);
		outState.putInt(SELECTED_ITEM_INDEX, selectedItemIndex);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull Fragment target, boolean usedOnMap,
	                                   @NonNull String dialogId, int selectedItemIndex,
	                                   @NonNull ApplicationMode appMode, boolean profileDependent) {
		try {
			CustomizableSingleSelectionBottomSheet fragment = new CustomizableSingleSelectionBottomSheet();
			Bundle args = new Bundle();
			args.putString(DIALOG_ID, dialogId);
			args.putInt(SELECTED_ITEM_INDEX, selectedItemIndex);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setProfileDependent(profileDependent);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

}
