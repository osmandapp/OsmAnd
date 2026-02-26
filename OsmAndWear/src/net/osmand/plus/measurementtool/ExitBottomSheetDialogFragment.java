package net.osmand.plus.measurementtool;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;

public class ExitBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final int REQUEST_CODE = 1001;
	public static final int SAVE_RESULT_CODE = 2;
	public static final int EXIT_RESULT_CODE = 3;
	private static final String DESCRIPTION = "description";
	public static final String TAG = ExitBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String description = "";
		if (args != null) {
			description = args.getString(DESCRIPTION);
		}
		items.add(new ShortDescriptionItem.Builder()
				.setDescription(description)
				.setTitle(getString(R.string.exit_without_saving))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());
		items.add(new DividerSpaceItem(getContext(), getResources().getDimensionPixelSize(R.dimen.bottom_sheet_exit_button_margin)));
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_save;
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.shared_string_exit;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment != null) {
			targetFragment.onActivityResult(REQUEST_CODE, SAVE_RESULT_CODE, null);
		}
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment != null) {
			targetFragment.onActivityResult(REQUEST_CODE, EXIT_RESULT_CODE, null);
		}
		dismiss();
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return (DialogButtonType.SECONDARY);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment, @NonNull String description) {
		if (!fragmentManager.isStateSaved()) {
			ExitBottomSheetDialogFragment fragment = new ExitBottomSheetDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString(DESCRIPTION, description);
			fragment.setArguments(bundle);
			fragment.setTargetFragment(targetFragment, REQUEST_CODE);
			fragment.show(fragmentManager, TAG);
		}
	}
}