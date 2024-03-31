package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColorId;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;

public class AppearanceConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = AppearanceConfirmationBottomSheet.class.getSimpleName();

	private static final String ITEMS_COUNT_KEY = "items_count";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_apply_changes)));

		int count = getItemsCount();
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.change_default_tracks_appearance_confirmation_description, String.valueOf(count)))
				.setDescriptionColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_descr)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof OnAppearanceChangeConfirmedListener) {
			((OnAppearanceChangeConfirmedListener) fragment).onAppearanceChangeConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	private int getItemsCount() {
		Bundle arguments = getArguments();
		return arguments != null ? arguments.getInt(ITEMS_COUNT_KEY, 0) : 0;
	}

	public interface OnAppearanceChangeConfirmedListener {
		void onAppearanceChangeConfirmed();
	}

	public static void showInstance(@NonNull FragmentManager manager, int itemsCount) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AppearanceConfirmationBottomSheet fragment = new AppearanceConfirmationBottomSheet();
			Bundle arguments = new Bundle();
			arguments.putInt(ITEMS_COUNT_KEY, itemsCount);
			fragment.setArguments(arguments);
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}