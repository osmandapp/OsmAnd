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

public class BottomSheetItemWithDescription extends SimpleBottomSheetItem {

	protected CharSequence description;
	@ColorRes
	private int descriptionColorId = INVALID_ID;

	private TextView descriptionTv;

	public BottomSheetItemWithDescription(View customView,
										  @LayoutRes int layoutId,
										  Object tag,
										  boolean disabled,
										  View.OnClickListener onClickListener,
										  int position,
										  Drawable icon,
										  String title,
										  @ColorRes int titleColorId,
										  CharSequence description,
										  @ColorRes int descriptionColorId) {
		super(customView, layoutId, tag, disabled, onClickListener, position, icon, title, titleColorId);
		this.description = description;
		this.descriptionColorId = descriptionColorId;
	}

	protected BottomSheetItemWithDescription() {

	}

	public void setDescription(CharSequence description) {
		this.description = description;
		descriptionTv.setText(description);
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		descriptionTv = (TextView) view.findViewById(R.id.description);
		if (descriptionTv != null) {
			descriptionTv.setText(description);
			if (descriptionColorId != INVALID_ID) {
				descriptionTv.setTextColor(ContextCompat.getColor(app, descriptionColorId));
			}
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected CharSequence description;
		@ColorRes
		protected int descriptionColorId = INVALID_ID;

		public Builder setDescription(CharSequence description) {
			this.description = description;
			return this;
		}

		public Builder setDescriptionColorId(@ColorRes int descriptionColorId) {
			this.descriptionColorId = descriptionColorId;
			return this;
		}

		public BottomSheetItemWithDescription create() {
			return new BottomSheetItemWithDescription(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					title,
					titleColorId,
					description,
					descriptionColorId);
		}
	}
}
