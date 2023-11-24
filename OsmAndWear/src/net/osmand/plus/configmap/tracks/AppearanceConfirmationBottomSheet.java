package net.osmand.plus.configmap.tracks;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class AppearanceConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = AppearanceConfirmationBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_apply_changes)));

		ItemsSelectionHelper<TrackItem> selectionHelper = getItemsSelectionHelper();
		if (selectionHelper != null) {
			int count = selectionHelper.getSelectedItemsSize();
			items.add(new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(R.string.change_default_tracks_appearance_confirmation_description, String.valueOf(count)))
					.setDescriptionColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
					.setLayoutId(R.layout.bottom_sheet_item_descr)
					.create());
		}
	}

	@Nullable
	private ItemsSelectionHelper<TrackItem> getItemsSelectionHelper() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof TracksAppearanceFragment) {
			return ((TracksAppearanceFragment) fragment).getItemsSelectionHelper();
		}
		return null;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof TracksAppearanceFragment) {
			((TracksAppearanceFragment) fragment).saveTracksAppearance();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AppearanceConfirmationBottomSheet fragment = new AppearanceConfirmationBottomSheet();
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}