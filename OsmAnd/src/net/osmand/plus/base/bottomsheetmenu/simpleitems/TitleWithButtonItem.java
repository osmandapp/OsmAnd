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
	private Drawable icon;
	private String textOnButton;
	private TextView textButtonTV;

	public TitleWithButtonItem(View.OnClickListener onClickListener,
	                           Drawable icon,
	                           String title,
	                           String textOnButton,
	                           String iconPosition) {
		this.title = title;
		this.layoutId = R.layout.bottom_sheet_item_title_with_button;
		this.icon = icon;
		this.textOnButton = textOnButton;
		this.iconPosition = iconPosition;
		this.onClickListener = onClickListener;
	}

	public void setButtonIcon(Drawable icon, String iconPosition) {
		this.icon = icon;
		if (this.icon != null) {
			switch (iconPosition) {
				case textOnLeft:
					textButtonTV.setCompoundDrawablesWithIntrinsicBounds(this.icon, null, null, null);
					break;
				case textOnTop:
					textButtonTV.setCompoundDrawablesWithIntrinsicBounds(null, this.icon, null, null);
					break;
				case textOnRight:
					textButtonTV.setCompoundDrawablesWithIntrinsicBounds(null, null, this.icon, null);
					break;
				case textOnBottom:
					textButtonTV.setCompoundDrawablesWithIntrinsicBounds(null, null, null, this.icon);
					break;
			}
		}
	}

	public void setButtonText(String text) {
		textOnButton = text;
		textButtonTV.setText(textOnButton);

	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);

		if (textOnButton != null) {
			textButtonTV = (TextView) view.findViewById(R.id.text_button);
			textButtonTV.setOnClickListener(onClickListener);
			setButtonIcon(icon, iconPosition);
			setButtonText(textOnButton);
		}
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
}
