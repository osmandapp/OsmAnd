package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BottomSheetItemTitleWithTwoButtons extends SimpleBottomSheetItem {

	private TextView textButton;
	private View.OnClickListener onButtonClickListener;
	private Drawable leftCompoundDrawable;
	private Drawable rightCompoundDrawable;
	private String buttonTitle;
	@ColorRes
	private int buttonTextColor = INVALID_ID;

	private TextView textButtonSecond;
	private View.OnClickListener onSecondButtonClickListener;
	private Drawable leftCompoundDrawableSecond;
	private Drawable rightCompoundDrawableSecond;

	public BottomSheetItemTitleWithTwoButtons(View customView,
											  @LayoutRes int layoutId,
											  Object tag,
											  boolean disabled,
											  View.OnClickListener onClickListener,
											  int position,
											  Drawable icon,
											  String title,
											  @ColorRes int titleColorId,
											  String buttonTitle,
											  View.OnClickListener onButtonClickListener,
											  Drawable leftCompoundDrawable,
											  Drawable rightCompoundDrawable,
											  Drawable leftCompoundDrawableSecond,
											  Drawable rightCompoundDrawableSecond,
											  View.OnClickListener onSecondButtonClickListener,
											  @ColorRes int buttonTextColor) {
		super(customView,
				layoutId,
				tag,
				disabled,
				onClickListener,
				position,
				icon,
				title,
				titleColorId);
		this.buttonTitle = buttonTitle;
		this.onButtonClickListener = onButtonClickListener;
		this.leftCompoundDrawable = leftCompoundDrawable;
		this.rightCompoundDrawable = rightCompoundDrawable;
		this.onSecondButtonClickListener = onSecondButtonClickListener;
		this.leftCompoundDrawableSecond = leftCompoundDrawableSecond;
		this.rightCompoundDrawableSecond = rightCompoundDrawableSecond;
		this.buttonTextColor = buttonTextColor;
	}

	public void setButtonIcons(@Nullable Drawable leftCompoundDrawable, @Nullable Drawable rightCompoundDrawable) {
		this.leftCompoundDrawable = leftCompoundDrawable;
		this.rightCompoundDrawable = rightCompoundDrawable;
		textButton.setCompoundDrawablesWithIntrinsicBounds(leftCompoundDrawable, null, rightCompoundDrawable, null);
	}

	public void setButtonText(String buttonTitle) {
		this.buttonTitle = buttonTitle;
		textButton.setText(buttonTitle);
	}

	public void setSecondButtonIcons(@Nullable Drawable leftCompoundDrawable, @Nullable Drawable rightCompoundDrawable) {
		this.leftCompoundDrawableSecond = leftCompoundDrawable;
		this.rightCompoundDrawableSecond = rightCompoundDrawable;
		textButtonSecond.setCompoundDrawablesWithIntrinsicBounds(leftCompoundDrawable, null, rightCompoundDrawable, null);
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		textButton = view.findViewById(R.id.text_button);
		textButton.setOnClickListener(onButtonClickListener);
		textButton.setCompoundDrawablesWithIntrinsicBounds(leftCompoundDrawable, null, rightCompoundDrawable, null);
		textButton.setText(buttonTitle);
		if (buttonTextColor != INVALID_ID) {
			textButton.setTextColor(ContextCompat.getColor(app, buttonTextColor));
		}
		textButtonSecond = view.findViewById(R.id.text_button_second);
		textButtonSecond.setOnClickListener(onSecondButtonClickListener);
		textButtonSecond.setCompoundDrawablesWithIntrinsicBounds(leftCompoundDrawableSecond, null, rightCompoundDrawableSecond, null);
		if (buttonTextColor != INVALID_ID) {
			textButtonSecond.setTextColor(ContextCompat.getColor(app, buttonTextColor));
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected String buttonTitle;
		protected View.OnClickListener onButtonClickListener;
		protected Drawable leftCompoundDrawable;
		protected Drawable rightCompoundDrawable;
		@ColorRes
		protected int buttonTextColor = INVALID_ID;
		protected View.OnClickListener onSecondButtonClickListener;
		protected Drawable leftCompoundDrawableSecond;
		protected Drawable rightCompoundDrawableSecond;

		public BottomSheetItemTitleWithTwoButtons.Builder setButtonIcons(Drawable leftCompoundDrawable, Drawable rightCompoundDrawable) {
			this.leftCompoundDrawable = leftCompoundDrawable;
			this.rightCompoundDrawable = rightCompoundDrawable;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons.Builder setOnButtonClickListener(View.OnClickListener onButtonClickListener) {
			this.onButtonClickListener = onButtonClickListener;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons.Builder setButtonTitle(String buttonTitle) {
			this.buttonTitle = buttonTitle;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons.Builder setButtonTextColor(@ColorRes int buttonTextColor) {
			this.buttonTextColor = buttonTextColor;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons.Builder setSecondButtonIcons(Drawable leftCompoundDrawable, Drawable rightCompoundDrawable) {
			this.leftCompoundDrawableSecond = leftCompoundDrawable;
			this.rightCompoundDrawableSecond = rightCompoundDrawable;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons.Builder setOnSecondButtonClickListener(View.OnClickListener onButtonClickListener) {
			this.onSecondButtonClickListener = onButtonClickListener;
			return this;
		}

		public BottomSheetItemTitleWithTwoButtons create() {
			return new BottomSheetItemTitleWithTwoButtons(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					title,
					titleColorId,
					buttonTitle,
					onButtonClickListener,
					leftCompoundDrawable,
					rightCompoundDrawable,
					leftCompoundDrawableSecond,
					rightCompoundDrawableSecond,
					onSecondButtonClickListener,
					buttonTextColor);
		}
	}
}
