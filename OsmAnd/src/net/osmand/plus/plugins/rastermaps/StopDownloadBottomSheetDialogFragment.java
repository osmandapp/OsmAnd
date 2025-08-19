package net.osmand.plus.plugins.rastermaps;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class StopDownloadBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = StopDownloadBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.stop_download_desc))
				.setTitle(getString(R.string.stop_download))
				.setLayoutId(R.layout.bottom_sheet_plain_title_with_description)
				.create());

		items.add(new DividerSpaceItem(context, AndroidUtils.dpToPx(context, 16)));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.stop_and_exit;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof TilesDownloadProgressFragment) {
			((TilesDownloadProgressFragment) target).dismiss(false);
		}
		dismiss();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected int getFirstDividerHeight() {
		return dpToPx(24);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_back;
	}

	@Override
	protected void onDismissButtonClickAction() {
		dismiss();
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected void setupBottomButtons(ViewGroup view) {
		super.setupBottomButtons(view);
		Context context = view.getContext();
		View space = new View(context);
		space.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(context, 4)));
		view.addView(space);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			StopDownloadBottomSheetDialogFragment fragment = new StopDownloadBottomSheetDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}