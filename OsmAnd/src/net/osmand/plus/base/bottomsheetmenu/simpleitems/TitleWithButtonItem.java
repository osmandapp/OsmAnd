package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

public class TitleWithButtonItem extends SimpleBottomSheetItem {

	public static final String textOnLeft = "LEFT";
	public static final String textOnTop = "TOP";
	public static final String textOnRight = "RIGHT";
	public static final String textOnBottom = "BOTTOM";

	private View.OnClickListener onClickListener;
	private String iconPosition;
	private Drawable textIcon;
	private String textButton;

	public TitleWithButtonItem(View.OnClickListener onClickListener,
	                           Drawable textIcon,
	                           String title,
	                           String textButton,
	                           String iconPosition) {
		this.title = title;
		this.layoutId = R.layout.bottom_sheet_item_title_with_button;
		this.textIcon = textIcon;
		this.textButton = textButton;
		this.iconPosition = iconPosition;
		this.onClickListener = onClickListener;
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		private Drawable textIcon;
		protected String title;
		private String textOnRight;
		protected View.OnClickListener onClickListener;

		private String iconPosition;

		public TitleWithButtonItem.Builder setIcon(Drawable textIcon) {
			this.textIcon = textIcon;
			return this;
		}

		public TitleWithButtonItem.Builder setIconPosition(String iconPosition) {
			this.iconPosition = iconPosition;
			return this;
		}

		public TitleWithButtonItem.Builder setOnClickListener(View.OnClickListener onClickListener) {
			this.onClickListener = onClickListener;
			return this;
		}

		public TitleWithButtonItem.Builder setTextOnRight(String textOnRight) {
			this.textOnRight = textOnRight;
			return this;
		}

		public TitleWithButtonItem.Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public TitleWithButtonItem create() {
			return new TitleWithButtonItem(onClickListener,
					textIcon,
					title,
					textOnRight,
					iconPosition);
		}
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);

		if (textButton != null) {
			TextView descriptionTv = (TextView) view.findViewById(R.id.text_button);
			descriptionTv.setText(textButton);
			descriptionTv.setOnClickListener(onClickListener);
			if (textIcon != null) {
				switch (iconPosition) {
					case textOnLeft:
						descriptionTv.setCompoundDrawablesWithIntrinsicBounds(textIcon, null, null, null);
						break;
					case textOnTop:
						descriptionTv.setCompoundDrawablesWithIntrinsicBounds(null, textIcon, null, null);
						break;
					case textOnRight:
						descriptionTv.setCompoundDrawablesWithIntrinsicBounds(null, null, textIcon, null);
						break;
					case textOnBottom:
						descriptionTv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, textIcon);
						break;
				}
			}
		}
	}
}
