package net.osmand.plus.routepreparationmenu.routeCards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public abstract class BaseRouteCard {

	protected OsmandApplication app;
	protected MapActivity mapActivity;

	protected View view;

	boolean showTopShadow;
	boolean showBottomShadow;
	protected boolean nightMode;

	public BaseRouteCard(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
	}

	public abstract int getCardLayoutId();

	public abstract void update();

	public View build(Context ctx) {
		view = LayoutInflater.from(ctx).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public OsmandApplication getMyApplication() {
		return app;
	}

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

	public void setShowTopShadow(boolean showTopShadow) {
		this.showTopShadow = showTopShadow;
	}

	public void setShowBottomShadow(boolean showBottomShadow) {
		this.showBottomShadow = showBottomShadow;
	}
}