package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class SaveGroupConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SaveGroupConfirmationBottomSheet.class.getSimpleName();

	public static final String EDITOR_TAG_KEY = "editor_tag_key";
	public static final String POINTS_SIZE_KEY = "points_size_key";

	private String editorTag;
	private int pointsSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getArguments();
		if (bundle != null) {
			editorTag = bundle.getString(EDITOR_TAG_KEY);
			pointsSize = bundle.getInt(POINTS_SIZE_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		String description = getString(isWptEditor() ? R.string.apply_to_existing_points_descr : R.string.apply_to_existing_favorites_descr);
		items.add(new ShortDescriptionItem.Builder()
				.setDescription(description)
				.setDescriptionColorId(ColorUtilities.getSecondaryTextColorId(nightMode))
				.setTitle(getString(R.string.shared_string_save))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());
		items.add(new DividerSpaceItem(getContext(), getResources().getDimensionPixelSize(R.dimen.bottom_sheet_exit_button_margin)));
	}

	private boolean isWptEditor() {
		return WptPtEditor.TAG.equals(editorTag);
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		int textId = isWptEditor() ? R.string.apply_only_to_new_points : R.string.apply_only_to_new_favorites;
		rightButton.setButtonType(getRightBottomButtonType());
		rightButton.setTitleId(textId);
	}

	@Override
	protected void setupThirdButton() {
		super.setupThirdButton();
		String applyExisting = getString(R.string.apply_to_existing);
		String text = getString(R.string.ltr_or_rtl_combine_via_space, applyExisting, "(" + pointsSize + ")");
		thirdButton.setButtonType(getThirdBottomButtonType());
		thirdButton.setTitle(text);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.apply_only_to_new_favorites;
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return R.string.apply_to_existing;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GroupEditorFragment) {
			((GroupEditorFragment) fragment).editPointsGroup(false);
		}
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GroupEditorFragment) {
			((GroupEditorFragment) fragment).editPointsGroup(true);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                @NonNull String editorTag, int pointsSize) {
		if (!manager.isStateSaved()) {
			Bundle bundle = new Bundle();
			bundle.putString(EDITOR_TAG_KEY, editorTag);
			bundle.putInt(POINTS_SIZE_KEY, pointsSize);

			SaveGroupConfirmationBottomSheet fragment = new SaveGroupConfirmationBottomSheet();
			fragment.setArguments(bundle);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
