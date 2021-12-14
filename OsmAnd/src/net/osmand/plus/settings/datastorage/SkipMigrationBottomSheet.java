package net.osmand.plus.settings.datastorage;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class SkipMigrationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SkipMigrationBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_skip)));

		BaseBottomSheetItem item = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.shared_string_skip_warning))
				.setLayoutId(R.layout.bottom_sheet_item_primary_descr)
				.create();
		items.add(item);
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnConfirmMigrationSkipListener) {
			((OnConfirmMigrationSkipListener) fragment).onMigrationSkipConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_back;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.skip_confirmation;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SkipMigrationBottomSheet fragment = new SkipMigrationBottomSheet();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface OnConfirmMigrationSkipListener {

		void onMigrationSkipConfirmed();

	}
}