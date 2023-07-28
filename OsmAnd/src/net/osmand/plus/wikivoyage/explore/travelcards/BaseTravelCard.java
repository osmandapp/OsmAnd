package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class BaseTravelCard {

	protected OsmandApplication app;
	protected boolean nightMode;

	public BaseTravelCard(@NonNull OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public abstract void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder);

	public abstract int getCardType();

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.active_color_primary_light, R.color.active_color_primary_dark);
	}

	protected Drawable getColoredIcon(@DrawableRes int icon, @ColorRes int colorLight, @ColorRes int colorDark) {
		return getColoredIcon(icon, nightMode ? colorDark : colorLight);
	}

	protected Drawable getColoredIcon(@DrawableRes int icon, @ColorRes int color) {
		return app.getUIUtilities().getIcon(icon, color);
	}

	protected boolean isInternetAvailable() {
		return app.getSettings().isInternetConnectionAvailable();
	}

	@DrawableRes
	protected int getPrimaryBtnBgRes(boolean enabled) {
		if (enabled) {
			return nightMode ? R.drawable.wikivoyage_primary_btn_bg_dark : R.drawable.wikivoyage_primary_btn_bg_light;
		}
		return nightMode ? R.drawable.wikivoyage_secondary_btn_bg_dark : R.drawable.wikivoyage_secondary_btn_bg_light;
	}

	@ColorRes
	protected int getPrimaryBtnTextColorRes(boolean enabled) {
		if (enabled) {
			return ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		}
		return R.color.text_color_secondary_light;
	}
}