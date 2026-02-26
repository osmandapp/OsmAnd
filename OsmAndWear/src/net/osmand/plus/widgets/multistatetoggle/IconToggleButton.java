package net.osmand.plus.widgets.multistatetoggle;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

public class IconToggleButton extends MultiStateToggleButton<IconRadioItem> {

	public IconToggleButton(@NonNull OsmandApplication app,
	                        @NonNull LinearLayout container,
	                        boolean nightMode) {
		super(app, container, nightMode);
	}

	@Override
	protected int getRadioItemLayoutId() {
		return R.layout.custom_radio_btn_icon_item;
	}

	@Override
	protected void initItemView(@NonNull ViewGroup view,
	                            @NonNull IconRadioItem item) {
		if (item.isUseDefaultColor()) {
			ImageView ivIcon = view.findViewById(R.id.icon);
			ivIcon.setImageDrawable(uiUtilities.getIcon(item.getIconId(false)));
		}
	}

	@Override
	protected void updateItemView(@NonNull ViewGroup view, @NonNull IconRadioItem item,
								  boolean selected, @ColorInt int color) {
		if (!item.isUseDefaultColor()) {
			ImageView ivIcon = view.findViewById(R.id.icon);
			Drawable icon = uiUtilities.getPaintedIcon(item.getIconId(selected), color);
			ivIcon.setImageDrawable(icon);
		}
	}

	@Override
	protected View getBorderBackgroundView(@NonNull ViewGroup button) {
		return button.findViewById(R.id.background_border);
	}

	public static class IconRadioItem extends RadioItem {

		static final int INVALID_ID = -1;

		private final int iconId;
		private int selectedIconId = INVALID_ID;
		private boolean useDefaultColor;

		public IconRadioItem(int iconId) {
			this.iconId = iconId;
		}

		@NonNull
		public IconRadioItem setSelectedIconId(@DrawableRes int selectedIconId) {
			this.selectedIconId = selectedIconId;
			return this;
		}

		@NonNull
		public IconRadioItem setUseDefaultColor() {
			this.useDefaultColor = true;
			return this;
		}

		@DrawableRes
		public int getIconId(boolean selected) {
			return selected && selectedIconId != INVALID_ID ? selectedIconId : iconId;
		}

		public boolean isUseDefaultColor() {
			return useDefaultColor;
		}
	}
}
