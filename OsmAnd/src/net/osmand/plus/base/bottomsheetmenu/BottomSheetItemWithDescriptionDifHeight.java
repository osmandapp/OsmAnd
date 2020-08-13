package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.core.view.ViewCompat;

public class BottomSheetItemWithDescriptionDifHeight extends BottomSheetItemWithDescription {

	private int minHeight;

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
		if (minHeight == 0) {
			minHeight = ViewCompat.getMinimumHeight(view);
		}
		LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
		params.height = minHeight;
		view.setMinimumHeight(minHeight);

	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		int minHeight;

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
