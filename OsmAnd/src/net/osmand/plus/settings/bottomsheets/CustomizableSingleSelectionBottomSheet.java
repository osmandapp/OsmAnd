package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;
import net.osmand.plus.base.dialog.uidata.DialogDisplayData;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

/**
 * Implementation of Single Selection Bottom Sheet.
 * Only displays the data passed to it in the form of DialogDisplayData.
 * When choosing one of the options, the selected option is passed to the controller
 * and dialog automatically closed without the need for confirmation by the user.
 */
public class CustomizableSingleSelectionBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = CustomizableSingleSelectionBottomSheet.class.getSimpleName();

	private static final String PROCESS_ID = "process_id";

	private OsmandApplication app;
	private String processId;
	private DialogDisplayData dataToDisplay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = requiredMyApplication();
		Bundle args = getArguments();
		if (args != null) {
			processId = args.getString(PROCESS_ID);
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

		String description = dataToDisplay.getDescription();
		if (description != null) {
			LongDescriptionItem descriptionItem = new LongDescriptionItem(description);
			items.add(descriptionItem);
		}

		int dividerStartPadding = getDimen(R.dimen.bottom_sheet_divider_margin_start);
		List<DialogDisplayItem> displayItems = dataToDisplay.getDisplayItems();
		for (int i = 0; i < displayItems.size(); i++) {
			DialogDisplayItem displayItem = displayItems.get(i);
			boolean isChecked = i == dataToDisplay.getSelectedItemIndex();
			Drawable icon = isChecked ? displayItem.selectedIcon : displayItem.normalIcon;
			int controlsColor = displayItem.customControlsColor != null ?
					displayItem.customControlsColor : ColorUtilities.getActiveColor(ctx, nightMode);
			BaseBottomSheetItem[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(isChecked)
					.setButtonTintList(AndroidUtils.createCheckedColorIntStateList(
							ColorUtilities.getColor(ctx, R.color.icon_color_default_light), controlsColor))
					.setDescription(displayItem.description)
					.setTitle(displayItem.title)
					.setIcon(icon)
					.setTag(i)
					.setLayoutId(displayItem.layoutId)
					.setOnClickListener(v -> {
						dataToDisplay.setSelectedItemIndex((int) preferenceItem[0].getTag());
						onItemSelected(displayItem);
						dismiss();
					})
					.create();
			items.add(preferenceItem[0]);
			if (displayItem.addDividerAfter) {
				DividerItem divider = new DividerItem(ctx);
				divider.setMargins(dividerStartPadding, 0, 0, 0);
				items.add(divider);
			}
		}
	}

	private void refreshDataToDisplay() {
		DialogManager dialogManager = app.getDialogManager();
		dataToDisplay = dialogManager.getDialogDisplayData(processId);
	}

	private void onItemSelected(@NonNull DialogDisplayItem selectedItem) {
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.onDialogItemSelected(processId, selectedItem);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			// Automatically unregister controller when close the dialog
			// to avoid any possible memory leaks
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.unregister(processId);
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId, boolean usedOnMap) {
		try {
			CustomizableSingleSelectionBottomSheet fragment = new CustomizableSingleSelectionBottomSheet();
			Bundle args = new Bundle();
			args.putString(PROCESS_ID, processId);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

}
