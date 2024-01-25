package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.base.dialog.data.DialogExtra.DESCRIPTION;
import static net.osmand.plus.base.dialog.data.DialogExtra.DRAWABLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.PRIMARY_BUTTON_TITLE_ID;
import static net.osmand.plus.base.dialog.data.DialogExtra.SECONDARY_BUTTON_TITLE_ID;
import static net.osmand.plus.base.dialog.data.DialogExtra.TERTIARY_BUTTON_TITLE_ID;
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
import net.osmand.plus.base.dialog.data.DialogExtra;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class CustomizableThreeOptionsBottomSheet extends CustomizableBottomSheet {

	public static final String TAG = CustomizableThreeOptionsBottomSheet.class.getSimpleName();

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
		Integer titleId = (Integer) getExtra(SECONDARY_BUTTON_TITLE_ID);
		return titleId != null ? titleId : super.getRightBottomButtonTextId();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected int getDismissButtonTextId() {
		Integer titleId = (Integer) getExtra(TERTIARY_BUTTON_TITLE_ID);
		return titleId != null ? titleId : super.getDismissButtonTextId();
	}

	@Override
	protected int getThirdBottomButtonTextId() {
		Integer titleId = (Integer) getExtra(PRIMARY_BUTTON_TITLE_ID);
		return titleId != null ? titleId : super.getThirdBottomButtonTextId();
	}

	@Override
	protected DialogButtonType getThirdBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	public int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
	}

	@Nullable
	private Object getExtra(@NonNull DialogExtra parameterKey) {
		return displayData == null ? null : displayData.getExtra(parameterKey);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId, boolean usedOnMap) {
		try {
			CustomizableThreeOptionsBottomSheet fragment = new CustomizableThreeOptionsBottomSheet();
			fragment.setProcessId(processId);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
