package net.osmand.plus.widgets.dialogbutton;

import static androidx.core.content.ContextCompat.getColorStateList;
import static net.osmand.plus.helpers.AndroidUiHelper.setEnabled;
import static net.osmand.plus.utils.AndroidUtils.createEnabledColorStateList;
import static net.osmand.plus.utils.AndroidUtils.setMargins;
import static net.osmand.plus.utils.UiUtilities.createTintedDrawable;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonAttributes.INVALID_ID;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.TextViewEx;

public class DialogButtonViewHolder {

	private final LinearLayout view;
	private final DialogButtonAttributes attrs;

	public DialogButtonViewHolder(@NonNull LinearLayout view, @NonNull DialogButtonAttributes attrs) {
		this.view = view;
		this.attrs = attrs;
		setupViewComplete();
	}

	private void setupViewComplete() {
		Context context = view.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		view.removeAllViews();
		view.setOrientation(LinearLayout.VERTICAL);

		View rootView = inflater.inflate(R.layout.custom_dialog_button, view, false);
		view.addView(rootView);

		updateButtonHeight();
		updateButtonAppearance();
		updateTitle();
		fitSizes();
	}

	public void updateButtonHeight() {
		int buttonHeight = attrs.getButtonHeight();
		View buttonBody = view.findViewById(R.id.button_body);
		buttonBody.getLayoutParams().height = buttonHeight;
	}

	public void updateButtonAppearance() {
		updateBackground();
		updateRippleBackground();
		updateTitleColor();
		updateIcon();
	}

	public void updateBackground() {
		DialogButtonType buttonType = attrs.getButtonType();
		int backgroundAttr = buttonType.getBackgroundAttr();
		if (backgroundAttr != INVALID_ID) {
			Context ctx = view.getContext();
			View buttonBody = view.findViewById(R.id.button_body);
			int backgroundResId = resolveAttribute(ctx, buttonType.getBackgroundAttr());
			AndroidUtils.setBackground(ctx, buttonBody, backgroundResId);
		}
	}

	public void updateRippleBackground() {
		Context ctx = view.getContext();
		View buttonContainer = view.findViewById(R.id.button_container);
		DialogButtonType buttonType = attrs.getButtonType();
		int rippleResId = AndroidUtils.resolveAttribute(ctx, buttonType.getRippleAttr());
		AndroidUtils.setBackground(ctx, buttonContainer, rippleResId);
	}

	public void updateTitleColor() {
		Context ctx = view.getContext();
		DialogButtonType buttonType = attrs.getButtonType();
		TextViewEx tvTitle = view.findViewById(R.id.button_text);

		ColorStateList colorStateList;
		int contentColorId = resolveAttribute(ctx, buttonType.getContentColorAttr());
		if (buttonType == DialogButtonType.TERTIARY) {
			int disabledColorId = ColorUtilities.getSecondaryTextColorId();
			colorStateList = createEnabledColorStateList(ctx, disabledColorId, contentColorId);
		} else {
			colorStateList = getColorStateList(ctx, contentColorId);
		}
		tvTitle.setTextColor(colorStateList);
	}

	public void updateIcon() {
		int iconResId = attrs.getIconId();
		if (iconResId != INVALID_ID) {
			Context ctx = view.getContext();
			DialogButtonType buttonType = attrs.getButtonType();
			TextViewEx tvTitle = view.findViewById(R.id.button_text);
			int contentColor = getColorFromAttr(ctx, buttonType.getContentColorAttr());
			Drawable icon = createTintedDrawable(ctx, iconResId, contentColor);
			tvTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			tvTitle.setCompoundDrawablePadding(ctx.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
		}
	}

	public void updateTitle() {
		TextViewEx tvTitle = view.findViewById(R.id.button_text);
		String title = attrs.getTitle();
		if (title == null && attrs.getTitleId() != INVALID_ID) {
			title = view.getContext().getString(attrs.getTitleId());
		}
		if (title == null) {
			title = "";
		}
		if (attrs.shouldUseUppercase()) {
			title = title.toUpperCase();
		}
		tvTitle.setText(title);
	}

	public void updateEnabled(boolean enabled) {
		setEnabled(view, enabled, R.id.button_wrapper, R.id.button_body, R.id.button_text);
	}

	public void fitSizes() {
		int minTouchableHeight = attrs.getButtonTouchableHeight();
		int buttonBodyHeight = attrs.getButtonHeight();

		int buttonHeight = attrs.getButtonHeight();
		View buttonBody = view.findViewById(R.id.button_body);
		buttonBody.getLayoutParams().height = buttonHeight;

		int buttonWrapperHeight = Math.max(minTouchableHeight, buttonBodyHeight);
		View buttonWrapper = view.findViewById(R.id.button_wrapper);
		buttonWrapper.getLayoutParams().height = buttonWrapperHeight;

		if (view.getLayoutParams() instanceof MarginLayoutParams) {
			int compensationMargin = 0;
			if (buttonBodyHeight < buttonWrapperHeight) {
				int extraHeight = buttonWrapperHeight - buttonBodyHeight;
				int extraPadding = extraHeight / 2;
				compensationMargin = -extraPadding;
			}
			MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
			int start = lp.getMarginStart();
			int top = attrs.getButtonTopMargin() + compensationMargin;
			int end = lp.getMarginEnd();
			int bottom = attrs.getButtonBottomMargin() + compensationMargin;
			setMargins(lp, start, top, end, bottom);
		}
	}

	@ColorInt
	private static int getColorFromAttr(@NonNull Context ctx, int attr) {
		return getColor(ctx, resolveAttribute(ctx, attr));
	}

	private static int resolveAttribute(@NonNull Context ctx, int attr) {
		return AndroidUtils.resolveAttribute(ctx, attr);
	}

	@ColorInt
	private static int getColor(@NonNull Context ctx, @ColorRes int colorId) {
		return ColorUtilities.getColor(ctx, colorId);
	}
}
