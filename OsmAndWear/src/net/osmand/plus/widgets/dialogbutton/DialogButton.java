package net.osmand.plus.widgets.dialogbutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public class DialogButton extends LinearLayout {

	private final DialogButtonAttributes attrs;
	private final DialogButtonViewHolder viewHolder;

	public DialogButton(Context context) {
		this(context, null);
	}

	public DialogButton(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DialogButton(Context context, @Nullable AttributeSet attrSet, int defStyleAttr) {
		super(context, attrSet, defStyleAttr);
		attrs = DialogButtonAttributes.createDefaultInstance(context, attrSet, defStyleAttr);
		viewHolder = new DialogButtonViewHolder(this, attrs);
	}

	public void setTitleId(@StringRes int titleId) {
		attrs.setTitleId(titleId);
		viewHolder.updateTitle();
	}

	public void setTitle(@Nullable String title) {
		attrs.setTitle(title);
		viewHolder.updateTitle();
	}

	public void setIconId(@DrawableRes int iconId) {
		attrs.setIconId(iconId);
		viewHolder.updateIcon();
	}

	public void setButtonType(@NonNull DialogButtonType buttonType) {
		attrs.setButtonType(buttonType);
		viewHolder.updateButtonAppearance();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		viewHolder.updateEnabled(enabled);
	}

	@Override
	public void setOnClickListener(@Nullable OnClickListener onClickListener) {
		findViewById(R.id.button_wrapper).setOnClickListener(onClickListener);
	}

	public View getButtonView() {
		return findViewById(R.id.button_body);
	}

	public void setButtonHeight(int buttonHeight) {
		attrs.setButtonHeight(buttonHeight);
		viewHolder.fitSizes();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		viewHolder.fitSizes();
	}
}
