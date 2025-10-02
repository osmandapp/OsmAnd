package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

public class DeleteAllDataConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = DeleteAllDataConfirmationBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View titleView = inflate(R.layout.backup_delete_all_confirmation);
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnConfirmDeletionListener) {
			((OnConfirmDeletionListener) fragment).onDeletionConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.delete_all_confirmation;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY_HARMFUL;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	public int getFirstDividerHeight() {
		return getDimensionPixelSize(R.dimen.dialog_content_margin);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DeleteAllDataConfirmationBottomSheet fragment = new DeleteAllDataConfirmationBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface OnConfirmDeletionListener {

		void onDeletionConfirmed();

	}
}