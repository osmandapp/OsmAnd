package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;

public class ClearAllHistoryBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ClearAllHistoryBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View titleView = inflate(R.layout.backup_delete_data);
		BaseBottomSheetItem item = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.clear_all_history_warning))
				.setTitle(getString(R.string.clear_all_history))
				.setIcon(getIcon(R.drawable.ic_action_alert, R.color.color_osm_edit_delete))
				.setCustomView(titleView)
				.create();
		items.add(item);
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
		return R.string.shared_string_clear_all;
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
			ClearAllHistoryBottomSheet fragment = new ClearAllHistoryBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
