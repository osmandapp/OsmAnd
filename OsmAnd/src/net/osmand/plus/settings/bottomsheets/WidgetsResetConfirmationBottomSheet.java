package net.osmand.plus.settings.bottomsheets;

import androidx.fragment.app.Fragment;

public class WidgetsResetConfirmationBottomSheet extends ConfirmationBottomSheet{

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
		Fragment target = getTargetFragment();
		if (target instanceof ConfirmationDialogListener) {
			((ConfirmationDialogListener) target).onActionConfirmed(actionId);
		}
	}
}
