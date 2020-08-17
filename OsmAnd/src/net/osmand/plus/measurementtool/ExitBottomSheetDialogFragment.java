package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;

public class ExitBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final int REQUEST_CODE = 1001;
	public static final int SAVE_RESULT_CODE = 2;
	public static final int EXIT_RESULT_CODE = 3;

	public static final String TAG = ExitBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		items.add(new ShortDescriptionItem.Builder()
				.setDescription(getString(R.string.plan_route_exit_dialog_descr))
				.setDescriptionColorId(nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light)
				.setTitle(getString(R.string.exit_without_saving))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());

		items.add(new DividerSpaceItem(getContext(),
				getResources().getDimensionPixelSize(R.dimen.bottom_sheet_exit_button_margin)));

		items.add(new BottomSheetItemButton.Builder()
				.setButtonType(UiUtilities.DialogButtonType.SECONDARY)
				.setTitle(getString(R.string.shared_string_exit))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment != null) {
							targetFragment.onActivityResult(REQUEST_CODE, EXIT_RESULT_CODE, null);
						}
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(getContext(),
				getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin)));

		items.add(new BottomSheetItemButton.Builder()
				.setTitle(getString(R.string.shared_string_save))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Fragment targetFragment = getTargetFragment();
						if (targetFragment != null) {
							targetFragment.onActivityResult(REQUEST_CODE, SAVE_RESULT_CODE, null);
						}
						dismiss();
					}
				})
				.create());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment) {
		if (!fragmentManager.isStateSaved()) {
			ExitBottomSheetDialogFragment fragment = new ExitBottomSheetDialogFragment();
			fragment.setTargetFragment(targetFragment, REQUEST_CODE);
			fragment.show(fragmentManager, TAG);
		}
	}
}
