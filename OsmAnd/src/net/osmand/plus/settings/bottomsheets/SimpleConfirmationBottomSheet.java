package net.osmand.plus.settings.bottomsheets;

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

public class SimpleConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SimpleConfirmationBottomSheet.class.getSimpleName();

	private String dialogTitle;
	private String dialogDescription;
	@StringRes
	private int actionButtonTitleId;
	private DialogButtonType actionButtonType;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(dialogDescription)
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setTitle(dialogTitle)
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
		return actionButtonTitleId;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return actionButtonType;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof ConfirmationDialogListener) {
			((ConfirmationDialogListener) target).onActionConfirmed();
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

	public static void showResetSettingsDialog(@NonNull FragmentManager fragmentManager,
	                                           @NonNull Fragment target,
	                                           @StringRes int dialogTitleId) {
		SimpleConfirmationBottomSheet fragment = new SimpleConfirmationBottomSheet();
		fragment.usedOnMap = false;
		fragment.dialogTitle = target.getString(dialogTitleId);
		fragment.dialogDescription = target.getString(R.string.reset_all_settings_desc);
		fragment.actionButtonTitleId = R.string.shared_string_reset;
		fragment.actionButtonType = DialogButtonType.SECONDARY;
		fragment.setTargetFragment(target, 0);
		showInstance(fragment, fragmentManager, target);
	}

	public static void showConfirmDeleteDialog(@NonNull FragmentManager fragmentManager, @NonNull Fragment target,
	                                           @NonNull String dialogTitle, @NonNull String dialogDescription) {
		SimpleConfirmationBottomSheet fragment = new SimpleConfirmationBottomSheet();
		fragment.usedOnMap = false;
		fragment.dialogTitle = dialogTitle;
		fragment.dialogDescription = dialogDescription;
		fragment.actionButtonTitleId = R.string.shared_string_delete;
		fragment.actionButtonType = DialogButtonType.SECONDARY_HARMFUL;
		showInstance(fragment, fragmentManager, target);
	}

	private static void showInstance(@NonNull SimpleConfirmationBottomSheet fragment,
	                                 @NonNull FragmentManager fragmentManager,
	                                 @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface ConfirmationDialogListener {
		void onActionConfirmed();
	}
}