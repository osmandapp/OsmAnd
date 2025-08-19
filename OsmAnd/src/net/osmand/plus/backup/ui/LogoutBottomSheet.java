package net.osmand.plus.backup.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class LogoutBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = LogoutBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.logout_from_osmand_cloud)));

		BaseBottomSheetItem item = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.logout_from_osmand_cloud_decsr))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(item);
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();

		Fragment target = getTargetFragment();
		if (target instanceof BackupSettingsFragment) {
			((BackupSettingsFragment) target).logout();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_logout;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	public int getFirstDividerHeight() {
		return getDimensionPixelSize(R.dimen.dialog_content_margin);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			LogoutBottomSheet fragment = new LogoutBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}