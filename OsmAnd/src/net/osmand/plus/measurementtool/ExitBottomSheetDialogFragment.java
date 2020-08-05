package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class ExitBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = ExitBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		items.add(new TitleItem(getString(R.string.exit_without_saving)));

		items.add(new ShortDescriptionItem.Builder()
				.setDescription(getString(R.string.plan_route_exit_dialog_descr))
				.setDescriptionColorId(nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
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
						Fragment target = getTargetFragment();
						if (target instanceof ExitFragmentListener) {
							((ExitFragmentListener) target).exitOnClick();
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
						Fragment target = getTargetFragment();
						if (target instanceof ExitFragmentListener) {
							((ExitFragmentListener) target).saveOnClick();
						}
						dismiss();
					}
				})
				.create());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(FragmentManager fragmentManager, Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			ExitBottomSheetDialogFragment fragment = new ExitBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	interface ExitFragmentListener {

		void exitOnClick();

		void saveOnClick();
	}
}
