package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BottomSheetItemWithTitleAndButton extends SimpleBottomSheetItem {

	private View.OnClickListener onClickListener;
	private Drawable leftCompoundDrawable;
	private Drawable rightCompoundDrawable;
	private String ButtonTitle;
	private TextView textButtonTV;
	@ColorRes
	private int buttonTextColor = INVALID_ID;

	public BottomSheetItemWithTitleAndButton(View customView,
	                                         @LayoutRes int layoutId,
	                                         Object tag,
	                                         boolean disabled,
	                                         View.OnClickListener onClickListener,
	                                         int position,
	                                         Drawable leftCompoundDrawable,
	                                         Drawable rightCompoundDrawable,
	                                         Drawable icon,
	                                         String title,
	                                         String ButtonTitle,
	                                         @ColorRes int titleColorId,
	                                         @ColorRes int buttonTextColor) {
		super(customView, layoutId, tag, disabled, null, position, icon, title, titleColorId);
		this.leftCompoundDrawable = leftCompoundDrawable;
		this.rightCompoundDrawable = rightCompoundDrawable;
		this.ButtonTitle = ButtonTitle;
		this.onClickListener = onClickListener;
		this.buttonTextColor = buttonTextColor;
	}

	public void setButtonIcon(Drawable leftCompoundDrawable, Drawable rightCompoundDrawable) {
		textButtonTV.setCompoundDrawablesWithIntrinsicBounds(leftCompoundDrawable, null, rightCompoundDrawable, null);
	}

	public void setButtonText(String text) {
		textButtonTV.setText(text);
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		textButtonTV = (TextView) view.findViewById(R.id.text_button);
		textButtonTV.setOnClickListener(onClickListener);
		setButtonIcon(leftCompoundDrawable, rightCompoundDrawable);
		setButtonText(ButtonTitle);
		if (buttonTextColor != INVALID_ID) {
			textButtonTV.setTextColor(ContextCompat.getColor(app, buttonTextColor));
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected String title;
		private String buttonTitle;
		protected View.OnClickListener onClickListener;
		private Drawable leftCompoundDrawable;
		private Drawable rightCompoundDrawable;
		@ColorRes
		protected int buttonTextColor = INVALID_ID;

		public BottomSheetItemWithTitleAndButton.Builder setButtonIcon(Drawable leftCompoundDrawable, Drawable rightCompoundDrawable) {
			this.leftCompoundDrawable = leftCompoundDrawable;
			this.rightCompoundDrawable = rightCompoundDrawable;
			return this;
		}

		public BottomSheetItemWithTitleAndButton.Builder setOnClickListener(View.OnClickListener onClickListener) {
			this.onClickListener = onClickListener;
			return this;
		}

		public BottomSheetItemWithTitleAndButton.Builder setButtonTitle(String buttonTitle) {
			this.buttonTitle = buttonTitle;
			return this;
		}

		public BottomSheetItemWithTitleAndButton create() {
			return new BottomSheetItemWithTitleAndButton(
					customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					leftCompoundDrawable,
					rightCompoundDrawable,
					icon,
					title,
					buttonTitle,
					titleColorId,
					buttonTextColor);
		}
	}
}
