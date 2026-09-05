package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;


public class BottomSheetItemTwoChoicesButton extends BottomSheetItemWithCompoundButton {

	private LinearLayout bottomButtons;
	private FrameLayout leftBtnContainer;
	private FrameLayout rightBtnContainer;
	private TextView leftBtn;
	private TextView rightBtn;

	@StringRes
	private final int leftBtnTitleRes;
	@StringRes
	private final int rightBtnTitleRes;
	private boolean isLeftBtnSelected;
	private final OnBottomBtnClickListener onBottomBtnClickListener;
	private int bottomBtnBgRadius;
	private int bottomBtnTextColor;
	private int activeColor;
	private GradientDrawable bottomBtnBg;

	public BottomSheetItemTwoChoicesButton(View customView,
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
										   CharSequence description,
										   @ColorRes int descriptionColorId,
										   int descriptionMaxLines,
										   boolean descriptionLinksClickable,
										   boolean checked,
										   ColorStateList buttonTintList,
										   CompoundButton.OnCheckedChangeListener onCheckedChangeListener,
										   @ColorRes int compoundButtonColorId,
										   boolean isLeftBtnSelected,
										   int leftBtnTitleRes,
										   int rightBtnTitleRes,
										   OnBottomBtnClickListener onBottomBtnClickListener) {
		super(customView,
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
				description,
				descriptionColorId,
				descriptionMaxLines,
				descriptionLinksClickable,
				checked,
				buttonTintList,
				onCheckedChangeListener,
				compoundButtonColorId);
		this.leftBtnTitleRes = leftBtnTitleRes;
		this.rightBtnTitleRes = rightBtnTitleRes;
		this.isLeftBtnSelected = isLeftBtnSelected;
		this.onBottomBtnClickListener = onBottomBtnClickListener;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		bottomBtnBgRadius = AndroidUtils.dpToPx(context, 4);
		bottomBtnTextColor = ColorUtilities.getPrimaryTextColor(context, nightMode);
		activeColor = ColorUtilities.getActiveColor(context, nightMode);
		bottomBtnBg = new GradientDrawable();
		bottomBtnBg.setColor(ColorUtilities.getColorWithAlpha(activeColor, 0.1f));
		bottomBtnBg.setStroke(AndroidUtils.dpToPx(context, 1), ColorUtilities.getColorWithAlpha(activeColor, 0.5f));

		bottomButtons = view.findViewById(R.id.bottom_btns);
		leftBtnContainer = view.findViewById(R.id.left_btn_container);
		rightBtnContainer = view.findViewById(R.id.right_btn_container);
		leftBtn = view.findViewById(R.id.left_btn);
		rightBtn = view.findViewById(R.id.right_btn);

		AndroidUiHelper.updateVisibility(bottomButtons, isChecked());
		if (leftBtn != null) {
			leftBtn.setText(leftBtnTitleRes);
			leftBtn.setOnClickListener(view -> {
				isLeftBtnSelected = true;
				if (onBottomBtnClickListener != null) {
					onBottomBtnClickListener.onBottomBtnClick(true);
				}
				updateBottomButtons();
			});
		}
		if (rightBtn != null) {
			rightBtn.setText(rightBtnTitleRes);
			rightBtn.setOnClickListener(view -> {
				isLeftBtnSelected = false;
				if (onBottomBtnClickListener != null) {
					onBottomBtnClickListener.onBottomBtnClick(false);
				}
				updateBottomButtons();
			});
		}
		updateBottomButtons();
	}

	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		AndroidUiHelper.updateVisibility(bottomButtons, checked);
		updateBottomButtons();
	}

	public void setIsLeftBtnSelected(boolean isLeftBtnSelected) {
		this.isLeftBtnSelected = isLeftBtnSelected;
		updateBottomButtons();
	}

	private void updateBottomButtons() {
		if (bottomBtnBg == null || rightBtn == null || rightBtnContainer == null || leftBtn == null || leftBtnContainer == null) {
			return;
		}
		if (isLeftBtnSelected) {
			bottomBtnBg.setCornerRadii(new float[]{bottomBtnBgRadius, bottomBtnBgRadius, 0, 0, 0, 0, bottomBtnBgRadius, bottomBtnBgRadius});
			rightBtnContainer.setBackgroundColor(Color.TRANSPARENT);
			rightBtn.setTextColor(activeColor);
			leftBtnContainer.setBackground(bottomBtnBg);
			leftBtn.setTextColor(bottomBtnTextColor);
		} else {
			bottomBtnBg.setCornerRadii(new float[]{0, 0, bottomBtnBgRadius, bottomBtnBgRadius, bottomBtnBgRadius, bottomBtnBgRadius, 0, 0});
			rightBtnContainer.setBackground(bottomBtnBg);
			rightBtn.setTextColor(bottomBtnTextColor);
			leftBtnContainer.setBackgroundColor(Color.TRANSPARENT);
			leftBtn.setTextColor(activeColor);
		}
	}

	public static class Builder extends BottomSheetItemWithCompoundButton.Builder {

		@StringRes
		private int leftBtnTitleRes;
		@StringRes
		private int rightBtnTitleRes;
		private boolean isLeftBtnSelected;
		private OnBottomBtnClickListener onBottomBtnClickListener;

		public Builder setLeftBtnTitleRes(int leftBtnTitleRes) {
			this.leftBtnTitleRes = leftBtnTitleRes;
			return this;
		}

		public Builder setRightBtnTitleRes(int rightBtnTitleRes) {
			this.rightBtnTitleRes = rightBtnTitleRes;
			return this;
		}

		public Builder setLeftBtnSelected(boolean leftBtnSelected) {
			isLeftBtnSelected = leftBtnSelected;
			return this;
		}

		public Builder setOnBottomBtnClickListener(OnBottomBtnClickListener onBottomBtnClickListener) {
			this.onBottomBtnClickListener = onBottomBtnClickListener;
			return this;
		}

		public BottomSheetItemTwoChoicesButton create() {
			return new BottomSheetItemTwoChoicesButton(customView,
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
					description,
					descriptionColorId,
					descriptionMaxLines,
					descriptionLinksClickable,
					checked,
					buttonTintList,
					onCheckedChangeListener,
					compoundButtonColorId,
					isLeftBtnSelected,
					leftBtnTitleRes,
					rightBtnTitleRes,
					onBottomBtnClickListener);
		}
	}

	public interface OnBottomBtnClickListener {
		void onBottomBtnClick(boolean onLeftClick);
	}
}