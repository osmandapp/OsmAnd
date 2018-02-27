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

	protected String description;
	@ColorRes
	private int descriptionColorId = INVALID_ID;

	public BottomSheetItemWithDescription(View customView,
										  @LayoutRes int layoutId,
										  boolean clickable,
										  View.OnClickListener onClickListener,
										  int position,
										  Drawable icon,
										  String title,
										  @ColorRes int titleColorId,
										  String description,
										  @ColorRes int descriptionColorId) {
		super(customView, layoutId, clickable, onClickListener, position, icon, title, titleColorId);
		this.description = description;
		this.descriptionColorId = descriptionColorId;
	}

	protected BottomSheetItemWithDescription() {

	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		if (description != null) {
			TextView descriptionTv = (TextView) view.findViewById(R.id.description);
			descriptionTv.setText(description);
			if (descriptionColorId != INVALID_ID) {
				descriptionTv.setTextColor(ContextCompat.getColor(app, descriptionColorId));
			}
		}
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected String description;
		@ColorRes
		protected int descriptionColorId = INVALID_ID;

		public Builder setDescription(String description) {
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
