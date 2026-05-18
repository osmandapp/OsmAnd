package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import static net.osmand.plus.utils.UiUtilities.*;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;

public class BottomSheetItemButton extends SimpleBottomSheetItem {

	protected CharSequence description;

	DialogButtonType buttonType;

	LinearLayout buttonView;

	public BottomSheetItemButton(View customView,
	                             @LayoutRes int layoutId,
	                             Object tag,
	                             boolean disabled,
	                             View.OnClickListener onClickListener,
	                             int position,
	                             Drawable icon,
	                             Drawable background,
	                             CharSequence title,
	                             @ColorRes int titleColorId,
	                             boolean iconHidden,
	                             DialogButtonType buttonType) {
		super(customView, layoutId, tag, disabled, onClickListener, position, icon, background, title,
				titleColorId, iconHidden);
		this.buttonType = buttonType;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		buttonView = view.findViewById(R.id.button);
		if (buttonView != null) {
			setupDialogButton(nightMode, buttonView, buttonType, title);
			buttonView.setOnClickListener(onClickListener);
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected DialogButtonType buttonType = PRIMARY;

		public Builder setButtonType(DialogButtonType buttonType) {
			this.buttonType = buttonType;
			return this;
		}

		public BottomSheetItemButton create() {
			return new BottomSheetItemButton(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					background,
					title,
					titleColorId,
					iconHidden,
					buttonType);
		}
	}
}
