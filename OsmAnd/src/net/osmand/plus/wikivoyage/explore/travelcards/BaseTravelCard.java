package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;

public abstract class BaseTravelCard {

	protected static final int INVALID_POSITION = -1;
	protected static final int DEFAULT_VALUE = -1;

	protected View view;
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

	protected Drawable getPaintedIcon(int iconId, int color) {
		return app.getIconsCache().getPaintedIcon(iconId, color);
	}

	protected Drawable getIcon(int iconId, int colorLightId, int colorDarkId) {
		return app.getIconsCache().getIcon(iconId, nightMode ? colorLightId : colorDarkId);
	}
	@StringRes
	protected int getLeftButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected void onLeftButtonClickAction() {

	}

	@StringRes
	protected int getRightButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected void onRightButtonClickAction() {

	}

	@ColorRes
	protected int getBottomDividerColorId() {
		return DEFAULT_VALUE;
	}

	@LayoutRes
	protected int getLayoutId() {
		return DEFAULT_VALUE;
	}

	protected boolean isNightMode() {
		return !app.getSettings().isLightContent();
	}
}
