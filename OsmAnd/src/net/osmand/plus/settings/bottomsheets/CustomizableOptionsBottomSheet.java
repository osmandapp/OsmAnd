package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.utils.UiUtilities;


public class CustomizableOptionsBottomSheet extends CustomizableBottomSheet implements IDialog {

	public static final String TAG = CustomizableOptionsBottomSheet.class.getSimpleName();

	public CustomizableOptionsBottomSheet(@NonNull String processId) {
		super(processId);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null || displayData == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);

		for (DisplayItem displayItem : displayData.getDisplayItems()) {
			items.add(new BottomSheetItemWithDescription.Builder()
					.setDescription(displayItem.getDescription())
					.setIcon(displayItem.getNormalIcon())
					.setTitle(displayItem.getTitle())
					.setBackground(createSelectableBackground(displayItem))
					.setLayoutId(displayItem.getLayoutId())
					.setOnClickListener(displayItem.isClickable() ? v -> onItemClicked(displayItem) : null)
					.create()
			);

			DividerItem divider = createDividerIfNeeded(ctx, displayItem);
			if (divider != null) {
				items.add(divider);
			}
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId, boolean usedOnMap) {
		try {
			CustomizableOptionsBottomSheet fragment = new CustomizableOptionsBottomSheet(processId);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
