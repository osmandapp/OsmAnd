package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColorId;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;

public class ConfirmChangesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ConfirmChangesBottomSheet.class.getSimpleName();

	private static final String DESCRIPTION_KEY = "description_key";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_apply_changes)));

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription())
				.setDescriptionColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_descr)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof ChangesConfirmationListener) {
			((ChangesConfirmationListener) fragment).onChangesConfirmed();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Nullable
	private String getDescription() {
		Bundle bundle = getArguments();
		return bundle != null ? bundle.getString(DESCRIPTION_KEY) : null;
	}

	public interface ChangesConfirmationListener {
		void onChangesConfirmed();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String description) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(DESCRIPTION_KEY, description);

			ConfirmChangesBottomSheet fragment = new ConfirmChangesBottomSheet();
			fragment.setArguments(bundle);
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}