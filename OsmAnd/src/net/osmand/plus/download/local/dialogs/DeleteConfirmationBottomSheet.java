package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY_HARMFUL;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.quickaction.ConfirmationBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DeleteConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmationBottomSheet.class.getSimpleName();

	private DeleteConfirmationDialogController controller;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = DeleteConfirmationDialogController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.delete_map_description, controller.getItemName()))
				.setTitle(getString(R.string.delete_map))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		controller.onDeleteConfirmed();
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_delete;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return SECONDARY_HARMFUL;
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		TextView textView = rightButton.findViewById(R.id.button_text);
		textView.setTextColor(getColor(R.color.deletion_color_warning));
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			new DeleteConfirmationBottomSheet().show(manager, TAG);
		}
	}
}
