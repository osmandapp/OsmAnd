package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.base.dialog.data.DialogExtra.DESCRIPTION;
import static net.osmand.plus.base.dialog.data.DialogExtra.DIALOG_BUTTONS;
import static net.osmand.plus.base.dialog.data.DialogExtra.DRAWABLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.dialog.data.DisplayDialogButtonItem;
import net.osmand.plus.base.dialog.data.DialogExtra;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.Objects;

public class CustomizableQuestionBottomSheet extends CustomizableBottomSheet {

	public static final String TAG = CustomizableQuestionBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null || displayData == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);
		
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View contentView = inflater.inflate(R.layout.bottom_sheet_icon_title_description, (ViewGroup) getView(), false);
		ImageView ivIcon = contentView.findViewById(R.id.icon);
		TextView tvTitle = contentView.findViewById(R.id.title);
		TextView tvDescription = contentView.findViewById(R.id.description);

		Drawable drawable = (Drawable) getExtra(DRAWABLE);
		AndroidUiHelper.updateVisibility(tvTitle, drawable != null);
		if (drawable != null) {
			ivIcon.setImageDrawable(drawable);
		}

		String title = (String) getExtra(TITLE);
		AndroidUiHelper.updateVisibility(tvTitle, title != null);
		tvTitle.setText(title);

		String description = (String) getExtra(DESCRIPTION);
		AndroidUiHelper.updateVisibility(tvDescription, description != null);
		tvDescription.setText(description);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(contentView).create());
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return getDialogButton(1).getTitleId();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return getDialogButton(1).getButtonType();
	}

	@Override
	protected void onRightBottomButtonClick() {
		getDialogButton(1).onButtonClick(super::onRightBottomButtonClick);
	}

	@Override
	protected int getDismissButtonTextId() {
		return getDialogButton(2).getTitleId();
	}

	@Override
	protected DialogButtonType getDismissButtonType() {
		return getDialogButton(2).getButtonType();
	}

	@Override
	protected void onDismissButtonClickAction() {
		getDialogButton(2).onButtonClick(super::onDismissButtonClickAction);
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		return getDialogButton(0).getTitleId();
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return getDialogButton(0).getButtonType();
	}

	@Override
	protected void onThirdBottomButtonClick() {
		getDialogButton(0).onButtonClick(super::onThirdBottomButtonClick);
	}

	@Override
	public int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
	}

	@NonNull
	private DisplayDialogButtonItem getDialogButton(int index) {
		DisplayDialogButtonItem[] buttons = (DisplayDialogButtonItem[]) getExtra(DIALOG_BUTTONS);
		return Objects.requireNonNull(buttons)[index];
	}

	@Nullable
	private Object getExtra(@NonNull DialogExtra parameterKey) {
		return displayData == null ? null : displayData.getExtra(parameterKey);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			CustomizableQuestionBottomSheet fragment = new CustomizableQuestionBottomSheet();
			fragment.setProcessId(processId);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
		}
	}
}
