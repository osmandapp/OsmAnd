package net.osmand.plus.routepreparationmenu.cards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public abstract class BaseCard {

	protected OsmandApplication app;
	protected MapActivity mapActivity;

	protected View view;

	boolean showTopShadow;
	boolean showBottomShadow;
	boolean showDivider = true;
	boolean transparentBackground;
	protected boolean nightMode;

	private CardListener listener;

	public interface CardListener {
		void onCardLayoutNeeded(@NonNull BaseCard card);
		void onCardPressed(@NonNull BaseCard card);
		void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex);
	}

	public BaseCard(@NonNull MapActivity mapActivity) {
		this(mapActivity, true);
	}

	public BaseCard(@NonNull MapActivity mapActivity, boolean usedOnMap) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		nightMode = usedOnMap ? app.getDaynightHelper().isNightModeForMapControls() : !app.getSettings().isLightContent();
	}

	public abstract int getCardLayoutId();

	@Nullable
	public View getView() {
		return view;
	}

	public int getViewHeight() {
		return view != null ? view.getHeight() : 0;
	}

	public int getTopViewHeight() {
		return getViewHeight();
	}

	public void update() {
		if (view != null) {
			updateContent();
		}
	}

	public void applyState(@NonNull BaseCard card) {
		// non implemented
	}

	public CardListener getListener() {
		return listener;
	}

	public void setListener(CardListener listener) {
		this.listener = listener;
	}

	public void setLayoutNeeded() {
		CardListener listener = this.listener;
		if (listener != null) {
			listener.onCardLayoutNeeded(this);
		}
	}

	protected abstract void updateContent();

	public View build(Context ctx) {
		ContextThemeWrapper context =
				new ContextThemeWrapper(ctx, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		view = LayoutInflater.from(context).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public OsmandApplication getMyApplication() {
		return app;
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	@ColorInt
	protected int getActiveColor() {
		return getResolvedColor(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
	}

	@ColorInt
	protected int getMainFontColor() {
		return getResolvedColor(nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light);
	}

	@ColorInt
	protected int getSecondaryColor() {
		return getResolvedColor(R.color.description_font_and_bottom_sheet_icons);
	}

	protected Drawable getContentIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.description_font_and_bottom_sheet_icons);
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

	public void setShowTopShadow(boolean showTopShadow) {
		this.showTopShadow = showTopShadow;
	}

	public void setShowBottomShadow(boolean showBottomShadow) {
		this.showBottomShadow = showBottomShadow;
	}

	public boolean isShowDivider() {
		return showDivider;
	}

	public void setShowDivider(boolean showDivider) {
		this.showDivider = showDivider;
	}

	public boolean isTransparentBackground() {
		return transparentBackground;
	}

	public void setTransparentBackground(boolean transparentBackground) {
		this.transparentBackground = transparentBackground;
	}
}