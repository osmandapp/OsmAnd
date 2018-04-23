package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;

public abstract class BaseTravelCard {

	protected static final int INVALID_POSITION = -1;

	protected OsmandApplication app;

	protected int position = INVALID_POSITION;
	protected boolean nightMode;

	public abstract void inflate(OsmandApplication app, ViewGroup container, boolean nightMode);

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return app.getIconsCache().getIcon(drawableRes, color);
	}

	protected Drawable getIcon(@DrawableRes int drawableRes) {
		return app.getIconsCache().getIcon(drawableRes);
	}

	protected Drawable getIcon(int iconId, int colorLightId, int colorDarkId) {
		return app.getIconsCache().getIcon(iconId, nightMode ? colorLightId : colorDarkId);
	}

	protected void onLeftButtonClickAction() {

	}

	protected void onRightButtonClickAction() {

	}
}
