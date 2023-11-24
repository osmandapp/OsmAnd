package net.osmand.plus.plugins.osmedit.dialogs;

import android.os.Bundle;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class AreYouSureBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = AreYouSureBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.unsaved_changes_will_be_lost))
				.setTitle(getString(R.string.are_you_sure))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());

		int spaceHeight = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_exit_button_margin);
		items.add(new DividerSpaceItem(getContext(), spaceHeight));
	}

	@Override
	protected void onThirdBottomButtonClick() {
		if (getTargetFragment() instanceof EditPoiDialogFragment) {
			((EditPoiDialogFragment) getTargetFragment()).dismiss();
		}
		dismiss();
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return (DialogButtonType.SECONDARY);
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.shared_string_exit;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (getTargetFragment() instanceof EditPoiDialogFragment) {
			((EditPoiDialogFragment) getTargetFragment()).trySave();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	@Override
	protected void onDismissButtonClickAction() {
		dismiss();
	}

	@Override
	protected int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AreYouSureBottomSheetDialogFragment fragment = new AreYouSureBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(false);
			fragment.show(fragmentManager, TAG);
		}
	}
}