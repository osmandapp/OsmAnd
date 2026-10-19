package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY_HARMFUL;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.quickaction.ConfirmationBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DeleteConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmationBottomSheet.class.getSimpleName();

	private BaseLocalItem localItem;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.delete_map_description, localItem.getName(requireContext())))
				.setTitle(getString(R.string.delete_map))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof ConfirmDeletionListener) {
			((ConfirmDeletionListener) target).onDeletionConfirmed(localItem);
		}
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
		OsmandApplication app = requiredMyApplication();
		TextView textView = rightButton.findViewById(R.id.button_text);
		textView.setTextColor(ContextCompat.getColor(app, R.color.deletion_color_warning));
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, @NonNull BaseLocalItem localItem) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DeleteConfirmationBottomSheet fragment = new DeleteConfirmationBottomSheet();
			fragment.localItem = localItem;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface ConfirmDeletionListener {
		void onDeletionConfirmed(@NonNull BaseLocalItem localItem);
	}
}
