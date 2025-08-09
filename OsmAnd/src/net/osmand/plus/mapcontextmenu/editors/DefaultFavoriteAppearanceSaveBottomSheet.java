package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.plus.myplaces.favorites.SaveOption.APPLY_TO_ALL;
import static net.osmand.plus.myplaces.favorites.SaveOption.APPLY_TO_EXISTING;
import static net.osmand.plus.myplaces.favorites.SaveOption.APPLY_TO_NEW;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DefaultFavoriteAppearanceSaveBottomSheet extends SaveGroupConfirmationBottomSheet {

	public static final String TAG = DefaultFavoriteAppearanceSaveBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		String description = getString(isWptEditor() ? R.string.apply_to_existing_points_descr : R.string.save_favorite_default_appearance);
		items.add(new ShortDescriptionItem.Builder()
				.setDescription(description)
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setTitle(getString(R.string.shared_string_save))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());

		items.add(new DividerSpaceItem(getContext(), getDimensionPixelSize(R.dimen.bottom_sheet_exit_button_margin)));

		String applyExisting = getString(R.string.apply_to_existing);
		String text = getString(R.string.ltr_or_rtl_combine_via_space, applyExisting, "(" + pointsSize + ")");
		items.add(new BottomSheetItemButton.Builder()
				.setButtonType(DialogButtonType.SECONDARY)
				.setTitle(text)
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(v -> {
					Fragment fragment = getTargetFragment();
					if (fragment instanceof FavoriteAppearanceFragment) {
						((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_EXISTING);
					}
					dismiss();
				})
				.create());

		items.add(new DividerSpaceItem(getContext(), getDimensionPixelSize(R.dimen.context_menu_buttons_padding_bottom)));
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		int textId = R.string.apply_to_all_points;
		rightButton.setButtonType(getRightBottomButtonType());
		rightButton.setTitleId(textId);
	}

	@Override
	protected void setupThirdButton() {
		super.setupThirdButton();
		String newPoints = getString(isWptEditor() ? R.string.apply_only_to_new_points : R.string.apply_only_to_new_favorites);
		thirdButton.setButtonType(getThirdBottomButtonType());
		thirdButton.setTitle(newPoints);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.apply_to_all_points;
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return isWptEditor() ? R.string.apply_only_to_new_points : R.string.apply_only_to_new_favorites;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteAppearanceFragment) {
			((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_ALL);
		}
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteAppearanceFragment) {
			((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_NEW);
		}
		dismiss();
	}

	@Override
	public int getSecondDividerHeight() {
		return getDimensionPixelSize(R.dimen.horizontal_divider_height);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target, int pointsSize) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putInt(POINTS_SIZE_KEY, pointsSize);

			DefaultFavoriteAppearanceSaveBottomSheet fragment = new DefaultFavoriteAppearanceSaveBottomSheet();
			fragment.setArguments(bundle);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
