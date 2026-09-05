package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;


public class CustomizableOptionsBottomSheet extends CustomizableBottomSheet {

	public static final String TAG = CustomizableOptionsBottomSheet.class.getSimpleName();

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
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String processId, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, processId)) {
			CustomizableOptionsBottomSheet fragment = new CustomizableOptionsBottomSheet();
			fragment.setProcessId(processId);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(manager, TAG);
		}
	}
}
