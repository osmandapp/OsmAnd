package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class SimpleBottomSheetItem extends BaseBottomSheetItem {

	private Drawable icon;
	protected String title;
	@ColorRes
	protected int titleColorId = INVALID_ID;

	public SimpleBottomSheetItem(View customView,
								 @LayoutRes int layoutId,
								 Object tag,
								 boolean disabled,
								 View.OnClickListener onClickListener,
								 int position,
								 Drawable icon,
								 String title,
								 @ColorRes int titleColorId) {
		super(customView, layoutId, tag, disabled, onClickListener, position);
		this.icon = icon;
		this.title = title;
		this.titleColorId = titleColorId;
	}

	protected SimpleBottomSheetItem() {

	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		if (icon != null) {
			((ImageView) view.findViewById(R.id.icon)).setImageDrawable(icon);
		}
		if (title != null) {
			TextView titleTv = (TextView) view.findViewById(R.id.title);
			titleTv.setText(title);
			if (titleColorId != INVALID_ID) {
				titleTv.setTextColor(ContextCompat.getColor(app, titleColorId));
			}
		}
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		protected Drawable icon;
		protected String title;
		@ColorRes
		protected int titleColorId = INVALID_ID;

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setTitleColorId(@ColorRes int titleColorId) {
			this.titleColorId = titleColorId;
			return this;
		}

		public SimpleBottomSheetItem create() {
			return new SimpleBottomSheetItem(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					title,
					titleColorId);
		}
	}
}
