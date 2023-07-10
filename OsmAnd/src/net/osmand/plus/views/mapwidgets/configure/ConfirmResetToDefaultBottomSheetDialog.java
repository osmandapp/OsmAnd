package net.osmand.plus.views.mapwidgets.configure;

import android.os.Bundle;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ConfirmResetToDefaultBottomSheetDialog extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmResetToDefaultBottomSheetDialog.class.getSimpleName();

	@StringRes
	private int dialogTitleId;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.reset_all_settings_desc))
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setTitle(getString(dialogTitleId))
				.setTitleColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_plain_title_with_description)
				.create());
		items.add(new DividerSpaceItem(requireContext(), getDimen(R.dimen.content_padding_small)));
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_reset;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof ResetToDefaultListener) {
			((ResetToDefaultListener) target).onResetToDefaultConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getFirstDividerHeight() {
		return getDimen(R.dimen.dialog_content_margin);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected DialogButtonType getDismissButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected void onDismissButtonClickAction() {
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull Fragment target,
	                                @StringRes int dialogTitleId) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfirmResetToDefaultBottomSheetDialog fragment = new ConfirmResetToDefaultBottomSheetDialog();
			fragment.usedOnMap = false;
			fragment.dialogTitleId = dialogTitleId;
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface ResetToDefaultListener {

		void onResetToDefaultConfirmed();
	}
}