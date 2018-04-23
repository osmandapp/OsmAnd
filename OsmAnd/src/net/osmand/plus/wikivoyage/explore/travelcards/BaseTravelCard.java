package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;

import net.osmand.plus.OsmandApplication;

public class BaseTravelCard {

	protected OsmandApplication app;
	protected boolean nightMode;

	public BaseTravelCard(OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	protected Drawable getIcon(@DrawableRes int iconId, @ColorRes int colorLightId, @ColorRes int colorDarkId) {
		return getIcon(iconId, nightMode ? colorLightId : colorDarkId);
	}

	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return app.getIconsCache().getIcon(drawableRes, color);
	}
}
