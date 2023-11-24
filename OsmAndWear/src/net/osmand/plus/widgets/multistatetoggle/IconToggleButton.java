package net.osmand.plus.widgets.multistatetoggle;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
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
			ivIcon.setImageDrawable(uiUtilities.getIcon(item.getIconId()));
		}
	}

	@Override
	protected void updateItemView(@NonNull ViewGroup view,
	                              @NonNull IconRadioItem item,
	                              @ColorInt int color) {
		if (!item.isUseDefaultColor()) {
			ImageView ivIcon = view.findViewById(R.id.icon);
			Drawable icon = uiUtilities.getPaintedIcon(item.getIconId(), color);
			ivIcon.setImageDrawable(icon);
		}
	}

	public static class IconRadioItem extends RadioItem {

		private final int iconId;
		private final boolean useDefaultColor;

		public IconRadioItem(int iconId) {
			this(iconId, false);
		}

		public IconRadioItem(int iconId, boolean useDefaultColor) {
			this.iconId = iconId;
			this.useDefaultColor = useDefaultColor;
		}

		public int getIconId() {
			return iconId;
		}

		public boolean isUseDefaultColor() {
			return useDefaultColor;
		}
	}
}
