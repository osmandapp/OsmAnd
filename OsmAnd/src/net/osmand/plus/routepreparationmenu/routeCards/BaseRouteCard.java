package net.osmand.plus.routepreparationmenu.routeCards;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class BaseRouteCard {

	protected OsmandApplication app;

	protected boolean isLastItem;
	protected boolean nightMode;

	public BaseRouteCard(OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public abstract View createCardView();

	protected abstract void applyDayNightMode();

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.icon_color);
	}

	protected Drawable getActiveIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.active_buttons_and_links_light, R.color.active_buttons_and_links_dark);
	}

	protected Drawable getColoredIcon(@DrawableRes int icon, @ColorRes int colorLight, @ColorRes int colorDark) {
		return getColoredIcon(icon, nightMode ? colorDark : colorLight);
	}

	protected Drawable getColoredIcon(@DrawableRes int icon, @ColorRes int color) {
		return app.getUIUtilities().getIcon(icon, color);
	}
	public void setLastItem(boolean lastItem) {
		isLastItem = lastItem;
	}
}