package net.osmand.plus.settings.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class DeleteHistoryConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = DeleteHistoryConfirmationBottomSheet.class.getSimpleName();

	private static final String ITEMS_SIZE_KEY = "items_size_key";

	private int itemsSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			itemsSize = savedInstanceState.getInt(ITEMS_SIZE_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.delete_history_items, itemsSize)));

		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setTitle(getString(R.string.delete_history_items_descr))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(descriptionItem);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(ITEMS_SIZE_KEY, itemsSize);
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
		return R.string.shared_string_delete;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, int itemsSize, @Nullable Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			DeleteHistoryConfirmationBottomSheet fragment = new DeleteHistoryConfirmationBottomSheet();
			fragment.itemsSize = itemsSize;
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
