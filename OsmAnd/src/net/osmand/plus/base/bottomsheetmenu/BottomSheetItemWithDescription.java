package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;

public class BottomSheetItemWithDescription extends SimpleBottomSheetItem {

	protected CharSequence description;
	@ColorRes
	private int descriptionColorId = INVALID_ID;
	private int descriptionMaxLines = INVALID_VALUE;
	private boolean descriptionLinksClickable = false;

	private TextView descriptionTv;

	public BottomSheetItemWithDescription(View customView,
										  @LayoutRes int layoutId,
										  Object tag,
										  boolean disabled,
										  View.OnClickListener onClickListener,
										  int position,
										  Drawable icon,
										  Drawable background,
										  String title,
										  @ColorRes int titleColorId,
										  CharSequence description,
										  @ColorRes int descriptionColorId,
										  int descriptionMaxLines,
										  boolean descriptionLinksClickable) {
		super(customView, layoutId, tag, disabled, onClickListener, position, icon, background, title, titleColorId);
		this.description = description;
		this.descriptionColorId = descriptionColorId;
		this.descriptionMaxLines = descriptionMaxLines;
		this.descriptionLinksClickable = descriptionLinksClickable;
	}

	protected BottomSheetItemWithDescription() {

	}

	public void setDescription(CharSequence description) {
		this.description = description;
		descriptionTv.setText(description);
	}

	public void setDescriptionMaxLines(int maxLines) {
		this.descriptionMaxLines = maxLines;
		descriptionTv.setMaxLines(maxLines);
	}

	public void setDescriptionLinksClickable(boolean descriptionLinksClickable) {
		this.descriptionLinksClickable = descriptionLinksClickable;
		if (descriptionTv != null) {
			if (descriptionLinksClickable) {
				descriptionTv.setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				descriptionTv.setMovementMethod(null);
			}
		}
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		descriptionTv = (TextView) view.findViewById(R.id.description);
		if (descriptionTv != null) {
			descriptionTv.setText(description);
			if (descriptionColorId != INVALID_ID) {
				descriptionTv.setTextColor(ContextCompat.getColor(context, descriptionColorId));
			}
			if (descriptionMaxLines != INVALID_VALUE) {
				descriptionTv.setMaxLines(descriptionMaxLines);
			}
			if (descriptionLinksClickable) {
				descriptionTv.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected CharSequence description;
		@ColorRes
		protected int descriptionColorId = INVALID_ID;
		protected int descriptionMaxLines = INVALID_POSITION;
		protected boolean descriptionLinksClickable = false;

		public Builder setDescription(CharSequence description) {
			this.description = description;
			return this;
		}

		public Builder setDescriptionColorId(@ColorRes int descriptionColorId) {
			this.descriptionColorId = descriptionColorId;
			return this;
		}

		public Builder setDescriptionMaxLines(int maxLines) {
			this.descriptionMaxLines = maxLines;
			return this;
		}

		public void setDescriptionLinksClickable(boolean descriptionLinksClickable) {
			this.descriptionLinksClickable = descriptionLinksClickable;
		}

		public BottomSheetItemWithDescription create() {
			return new BottomSheetItemWithDescription(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					background,
					title,
					titleColorId,
					description,
					descriptionColorId,
					descriptionMaxLines,
					descriptionLinksClickable);
		}
	}
}
