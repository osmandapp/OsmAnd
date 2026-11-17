package net.osmand.plus.importfiles.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.importfiles.ui.ExitImportBottomSheet.OnExitConfirmedListener;
import net.osmand.plus.utils.AndroidUtils;

public class SkipPointsSelectionBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SkipPointsSelectionBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.points_selection_descr))
				.setTitle(getString(R.string.are_you_sure))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);

		Context context = requireContext();
		items.add(new DividerSpaceItem(context, AndroidUtils.dpToPx(context, 12)));
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnExitConfirmedListener) {
			((OnExitConfirmedListener) fragment).onExitConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SkipPointsSelectionBottomSheet fragment = new SkipPointsSelectionBottomSheet();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}