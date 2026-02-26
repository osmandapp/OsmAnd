package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.configmap.tracks.appearance.DefaultAppearanceFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class ConfirmDefaultAppearanceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmDefaultAppearanceBottomSheet.class.getSimpleName();

	private static final String TRACKS_COUNT_KEY = "description_key";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.change_default_tracks_appearance_confirmation))
				.setTitle(getString(R.string.shared_string_save))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		confirmChanges(false);
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		confirmChanges(true);
		dismiss();
	}

	private void confirmChanges(boolean updateExisting) {
		Fragment fragment = getParentFragment();
		if (fragment instanceof DefaultAppearanceFragment) {
			((DefaultAppearanceFragment) fragment).saveChanges(updateExisting);
		}
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.apply_only_to_new;
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.apply_to_existing;
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return SECONDARY;
	}

	@Override
	protected void setupThirdButton() {
		super.setupThirdButton();

		Bundle bundle = getArguments();
		int count = bundle != null ? bundle.getInt(TRACKS_COUNT_KEY) : 0;
		if (count > 0) {
			String text = getString(R.string.apply_to_existing);
			thirdButton.setButtonType(getThirdBottomButtonType());
			thirdButton.setTitle(getString(R.string.ltr_or_rtl_combine_via_space, text, "(" + count + ")"));
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, int tracksCount) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TRACKS_COUNT_KEY, tracksCount);

			ConfirmDefaultAppearanceBottomSheet fragment = new ConfirmDefaultAppearanceBottomSheet();
			fragment.setArguments(bundle);
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}