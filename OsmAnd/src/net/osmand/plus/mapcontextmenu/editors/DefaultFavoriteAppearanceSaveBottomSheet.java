package net.osmand.plus.mapcontextmenu.editors;

import static net.osmand.plus.mapcontextmenu.editors.DefaultFavoriteAppearanceSaveBottomSheet.SaveOption.APPLY_TO_ALL;
import static net.osmand.plus.mapcontextmenu.editors.DefaultFavoriteAppearanceSaveBottomSheet.SaveOption.APPLY_TO_EXISTING;
import static net.osmand.plus.mapcontextmenu.editors.DefaultFavoriteAppearanceSaveBottomSheet.SaveOption.APPLY_TO_NEW;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class DefaultFavoriteAppearanceSaveBottomSheet extends SaveGroupConfirmationBottomSheet {

	protected DialogButton applyToAll;

	@Override
	protected void setupBottomButtons(ViewGroup view) {
		if (isWptEditor()) {
			super.setupBottomButtons(view);
		} else {
			Activity activity = requireActivity();
			LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
			buttonsContainer = (LinearLayout) themedInflater.inflate(R.layout.favorite_save_bottom_sheet_buttons, view);
			setupThirdButton();
			setupRightButton();
			setupDismissButton();
			setupApplyToAllButton();
			updateBottomButtons();
		}
	}

	private void setupApplyToAllButton() {
		applyToAll = buttonsContainer.findViewById(R.id.apply_to_all);
		applyToAll.setButtonHeight(getRightButtonHeight());

		applyToAll.setTitleId(R.string.apply_to_all_points);
		applyToAll.setButtonType(DialogButtonType.PRIMARY);
		applyToAll.setOnClickListener(v -> onApplyToAllBottomButtonClick());
		View divider = buttonsContainer.findViewById(R.id.buttons_divider);
		divider.getLayoutParams().height = getFirstDividerHeight();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	protected void onApplyToAllBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteAppearanceFragment) {
			((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_ALL);
		}
		dismiss();
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteAppearanceFragment) {
			((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_NEW);
		}
		dismiss();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteAppearanceFragment) {
			((FavoriteAppearanceFragment) fragment).editPointsGroup(APPLY_TO_EXISTING);
		}
		dismiss();
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.content_padding);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                @NonNull String editorTag, int pointsSize) {
		if (!manager.isStateSaved()) {
			Bundle bundle = new Bundle();
			bundle.putString(EDITOR_TAG_KEY, editorTag);
			bundle.putInt(POINTS_SIZE_KEY, pointsSize);

			DefaultFavoriteAppearanceSaveBottomSheet fragment = new DefaultFavoriteAppearanceSaveBottomSheet();
			fragment.setArguments(bundle);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public enum SaveOption {
		APPLY_TO_EXISTING,
		APPLY_TO_NEW,
		APPLY_TO_ALL
	}
}
