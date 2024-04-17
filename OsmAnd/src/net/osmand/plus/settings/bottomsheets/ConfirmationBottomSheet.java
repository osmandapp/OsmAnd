package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY_HARMFUL;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class ConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmationBottomSheet.class.getSimpleName();

	private String title;
	private String description;
	private DialogButtonType buttonType;
	private int actionId;
	@StringRes
	private int buttonTitleId;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setTitle(title)
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
		return buttonTitleId;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return buttonType;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof ConfirmationDialogListener) {
			((ConfirmationDialogListener) target).onActionConfirmed(actionId);
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
		return SECONDARY;
	}

	@Override
	protected void onDismissButtonClickAction() {
		dismiss();
	}

	public static void showResetSettingsDialog(@NonNull FragmentManager manager, @NonNull Fragment target, @StringRes int titleId) {
		ConfirmationBottomSheet fragment = new ConfirmationBottomSheet();
		fragment.usedOnMap = false;
		fragment.title = target.getString(titleId);
		fragment.description = target.getString(R.string.reset_all_settings_desc);
		fragment.buttonTitleId = R.string.shared_string_reset;
		fragment.buttonType = SECONDARY;
		fragment.setTargetFragment(target, 0);
		showInstance(fragment, manager, target, -1);
	}

	public static void showConfirmDeleteDialog(@NonNull FragmentManager manager, @NonNull Fragment target,
	                                           @NonNull String title, @NonNull String description,
	                                           int actionId) {
		ConfirmationBottomSheet fragment = new ConfirmationBottomSheet();
		fragment.usedOnMap = false;
		fragment.title = title;
		fragment.description = description;
		fragment.buttonTitleId = R.string.shared_string_delete;
		fragment.buttonType = SECONDARY_HARMFUL;
		showInstance(fragment, manager, target, actionId);
	}

	private static void showInstance(@NonNull ConfirmationBottomSheet fragment, @NonNull FragmentManager manager,
	                                 @NonNull Fragment target, int actionId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			fragment.actionId = actionId;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface ConfirmationDialogListener {
		/**
		 * @param actionId indicates about action which has been confirmed
		 */
		void onActionConfirmed(int actionId);
	}
}