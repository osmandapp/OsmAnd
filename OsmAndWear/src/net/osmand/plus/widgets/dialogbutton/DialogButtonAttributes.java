package net.osmand.plus.widgets.dialogbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;

public class DialogButtonAttributes {

	public final static int INVALID_ID = -1;

	private int buttonHeight;
	private int buttonTouchableHeight;
	private int buttonTopMargin;
	private int buttonBottomMargin;

	private int iconId = INVALID_ID;

	private int titleId = INVALID_ID;
	private String title;
	private boolean useUppercase;

	private DialogButtonType buttonType = DialogButtonType.PRIMARY;


	public static DialogButtonAttributes createDefaultInstance(
			Context context, @Nullable AttributeSet attrs, int defStyleAttr
	) {
		DialogButtonAttributes defAttrs = new DialogButtonAttributes();

		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.DialogButton, defStyleAttr, 0
		);

		defAttrs.buttonHeight = a.getDimensionPixelSize(
				R.styleable.DialogButton_dialogButtonHeight,
				getDimension(context, R.dimen.dialog_button_height)
		);

		defAttrs.buttonTouchableHeight = a.getDimensionPixelSize(
				R.styleable.DialogButton_dialogButtonTouchableHeight,
				getDimension(context, R.dimen.acceptable_touch_radius)
		);

		defAttrs.buttonTopMargin = a.getDimensionPixelSize(
				R.styleable.DialogButton_dialogButtonTopMargin, 0
		);

		defAttrs.buttonBottomMargin = a.getDimensionPixelSize(
				R.styleable.DialogButton_dialogButtonBottomMargin, 0
		);

		defAttrs.title = a.getString(R.styleable.DialogButton_dialogButtonTitle);
		defAttrs.iconId = a.getResourceId(R.styleable.DialogButton_dialogButtonIcon, INVALID_ID);
		defAttrs.useUppercase = a.getBoolean(R.styleable.DialogButton_dialogButtonUseUppercaseTitle, false);

		int buttonTypeId = a.getInteger(R.styleable.DialogButton_dialogButtonType, 0);
		defAttrs.buttonType = DialogButtonType.getById(buttonTypeId);

		return defAttrs;
	}

	public void setIconId(int iconId) {
		this.iconId = iconId;
	}

	public void setTitleId(int titleId) {
		this.titleId = titleId;
		this.title = null;
	}

	public void setTitle(String title) {
		this.title = title;
		this.titleId = INVALID_ID;
	}

	public void setButtonHeight(int buttonHeight) {
		this.buttonHeight = buttonHeight;
	}

	public void setButtonType(@NonNull DialogButtonType buttonType) {
		this.buttonType = buttonType;
	}

	public int getIconId() {
		return iconId;
	}

	public int getTitleId() {
		return titleId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@LayoutRes
	public DialogButtonType getButtonType() {
		return buttonType;
	}

	public int getButtonHeight() {
		return buttonHeight;
	}

	public int getButtonTouchableHeight() {
		return buttonTouchableHeight;
	}

	public int getButtonTopMargin() {
		return buttonTopMargin;
	}

	public int getButtonBottomMargin() {
		return buttonBottomMargin;
	}

	public boolean shouldUseUppercase() {
		return useUppercase;
	}

	private static int getDimension(@NonNull Context ctx, @DimenRes int resId) {
		return ctx.getResources().getDimensionPixelSize(resId);
	}
}
