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
import net.osmand.plus.activities.OsmandActionBarActivity;

public abstract class BaseTravelCard {

	protected static final int INVALID_POSITION = -1;
	protected static final int DEFAULT_VALUE = -1;

	protected View view;
	protected OsmandActionBarActivity activity;

	protected int position = INVALID_POSITION;

	public abstract void inflate(OsmandApplication app, ViewGroup container, boolean nightMode);

	protected abstract View getView(OsmandApplication app, ViewGroup parent, boolean nightMode);

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(activity, colorId);
	}

	protected Drawable getIcon(@DrawableRes int drawableRes, @ColorRes int color) {
		return activity.getMyApplication().getIconsCache().getIcon(drawableRes, color);
	}

	protected Drawable getBackgroundIcon(@DrawableRes int drawableRes) {
		return activity.getMyApplication().getIconsCache().getIcon(drawableRes);
	}

	@StringRes
	protected int getTitleId() {
		return DEFAULT_VALUE;
	}

	@StringRes
	protected int getDescriptionId() {
		return DEFAULT_VALUE;
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
		return !activity.getMyApplication().getSettings().isLightContent();
	}
}
