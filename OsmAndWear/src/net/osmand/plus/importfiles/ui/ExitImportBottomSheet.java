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
import net.osmand.plus.utils.AndroidUtils;

public class ExitImportBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ExitImportBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.exit_import_descr))
				.setTitle(getString(R.string.exit_import))
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
		return R.string.shared_string_continue;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ExitImportBottomSheet fragment = new ExitImportBottomSheet();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface OnExitConfirmedListener {

		void onExitConfirmed();

	}
}
