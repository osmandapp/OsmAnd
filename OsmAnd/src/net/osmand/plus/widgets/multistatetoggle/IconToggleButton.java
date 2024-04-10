package net.osmand.plus.widgets.multistatetoggle;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
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

	@Override
	protected void initView() {
		buttons.clear();
		dividers.clear();
		container.removeAllViews();
		container.setBackground(null);
		for (int i = 0; i < items.size(); i++) {
			createBtn(items.get(i));
		}
		updateView();
	}

	@Override
	public void updateView() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int textColorPrimary = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int textColorSecondary = ColorUtilities.getSecondaryTextColor(app, nightMode);

		int selectedItemIndex = getSelectedItemIndex();
		int radius = AndroidUtils.dpToPx(app, 4);
		float[] leftBtnRadii = {radius, radius, 0, 0, 0, 0, radius, radius};
		float[] rightBtnRadii = {0, 0, radius, radius, radius, radius, 0, 0};
		float[] internalBtnRadii = {0, 0, 0, 0, 0, 0, 0, 0};
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);

		for (int i = 0; i < items.size(); i++) {
			IconRadioItem item = items.get(i);
			ViewGroup button = buttons.get(i);
			View selectablePart = button.findViewById(R.id.selectable_part);

			boolean enabled = item.isEnabled();
			button.setEnabled(enabled);
			int textColor = enabled ? textColorPrimary : textColorSecondary;
			int selectedBgColor = enabled ? activeColor : defaultColor;
			int contentColor = item.getCustomColor() != null ? item.getCustomColor() : activeColor;
			int itemColor = enabled ? contentColor : defaultColor;
			GradientDrawable background = new GradientDrawable();

			if (i == 0) {
				background.setCornerRadii(isLayoutRtl ? rightBtnRadii : leftBtnRadii);
			} else if (isLastItem(i)) {
				background.setCornerRadii(isLayoutRtl ? leftBtnRadii : rightBtnRadii);
			} else {
				background.setCornerRadii(internalBtnRadii);
			}
			if (selectedItem == item) {
				background.setColor(ColorUtilities.getColorWithAlpha(selectedBgColor, BACKGROUND_ALPHA));
				background.setStroke(AndroidUtils.dpToPx(app, 1.5f), ColorUtilities.getColorWithAlpha(selectedBgColor, BACKGROUND_STROKE_ALPHA));
				selectablePart.setBackground(background);
				updateItemView(button, item, textColor);
			} else {
				LayerDrawable layerDrawable = new LayerDrawable(new GradientDrawable[]{background});
				setupLayerInset(i, selectedItemIndex, radius, layerDrawable);
				background.setStroke(AndroidUtils.dpToPx(app, 1.5f), ColorUtilities.getStrokedButtonsOutlineColor(app, nightMode));
				selectablePart.setBackground(layerDrawable);
				updateItemView(button, item, itemColor);
			}
			Drawable selectedDrawable = UiUtilities.getColoredSelectableDrawable(app, selectedBgColor, BACKGROUND_ALPHA);
			selectablePart.setForeground(selectedDrawable);
		}
	}

	private void setupLayerInset(int currentItemIndex, int selectedItemIndex, int radius, @NonNull LayerDrawable layerDrawable) {
		int hideSide = radius * -1;
		if (currentItemIndex == 0) {
			layerDrawable.setLayerInset(0, 0, 0, selectedItemIndex == currentItemIndex + 1 ? hideSide : 0, 0);
		} else if (isLastItem(currentItemIndex)) {
			layerDrawable.setLayerInset(0, hideSide, 0, 0, 0);
		} else {
			layerDrawable.setLayerInset(0, hideSide, 0, selectedItemIndex == currentItemIndex + 1 ? hideSide : 0, 0);
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
