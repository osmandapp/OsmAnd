package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class BottomSheetItemWithDescriptionDifHeight extends BottomSheetItemWithDescription {

	private final int minHeight;

	public BottomSheetItemWithDescriptionDifHeight(View customView,
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
	                                               int minHeight) {
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
				descriptionLinksClickable);

		this.minHeight = minHeight;

	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		if (minHeight != INVALID_VALUE) {
			if (view instanceof TextView) {
				view.setMinimumHeight(minHeight);
				((TextView) view).setMinHeight(minHeight);
			} else {
				view.setMinimumHeight(minHeight);
			}
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
			params.height = WRAP_CONTENT;
			view.setLayoutParams(params);
		}
	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		int minHeight = INVALID_VALUE;

		public Builder setMinHeight(int minHeight) {
			this.minHeight = minHeight;
			return this;
		}

		public BottomSheetItemWithDescriptionDifHeight create() {
			return new BottomSheetItemWithDescriptionDifHeight(customView,
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
					minHeight);
		}
	}
}
