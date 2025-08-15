package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.SHOW_BOTTOM_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.SUBTITLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton.Builder;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

/**
 * Implementation of Single Selection Bottom Sheet.
 * Only displays the data passed to it in the form of DialogDisplayData.
 * When choosing one of the options, the selected option is passed to the controller
 * and dialog automatically closed without the need for confirmation by the user.
 */
public class CustomizableSingleSelectionBottomSheet extends CustomizableBottomSheet {

	public static final String TAG = CustomizableSingleSelectionBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null || displayData == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);

		String title = (String) displayData.getExtra(TITLE);
		if (title != null) {
			TitleItem titleItem = new TitleItem(title);
			items.add(titleItem);
		}

		String description = (String) displayData.getExtra(SUBTITLE);
		if (description != null) {
			LongDescriptionItem descriptionItem = new LongDescriptionItem(description);
			items.add(descriptionItem);
		}

		List<DisplayItem> displayItems = displayData.getDisplayItems();
		for (int i = 0; i < displayItems.size(); i++) {
			DisplayItem displayItem = displayItems.get(i);
			Integer selectedIndex = (Integer) displayData.getExtra(SELECTED_INDEX);
			boolean isChecked = selectedIndex != null && selectedIndex == i;
			BaseBottomSheetItem[] rowItem = new BottomSheetItemWithCompoundButton[1];
			rowItem[0] = new Builder()
					.setChecked(isChecked)
					.setButtonTintList(createCompoundButtonTintList(displayItem))
					.setDescription(displayItem.getDescription())
					.setTitle(displayItem.getTitle())
					.setIcon(isChecked ? displayItem.getSelectedIcon() : displayItem.getNormalIcon())
					.setBackground(createSelectableBackground(displayItem))
					.setTag(i)
					.setLayoutId(displayItem.getLayoutId())
					.setOnClickListener(displayItem.isClickable() ? v -> {
						displayData.putExtra(SELECTED_INDEX, rowItem[0].getTag());
						onItemSelected(displayItem);
						dismiss();
					} : null)
					.create();
			items.add(rowItem[0]);

			DividerItem divider = createDividerIfNeeded(ctx, displayItem);
			if (divider != null) {
				items.add(divider);
			}
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		Boolean show = displayData != null ? (Boolean) displayData.getExtra(SHOW_BOTTOM_BUTTONS) : null;
		return show == null || !show;
	}

	public static boolean showInstance(@NonNull FragmentManager manager, @NonNull String processId,
	                                   @Nullable ApplicationMode appMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			CustomizableSingleSelectionBottomSheet fragment = new CustomizableSingleSelectionBottomSheet();
			fragment.setProcessId(processId);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(manager, TAG);
			return true;
		}
		return false;
	}
}
