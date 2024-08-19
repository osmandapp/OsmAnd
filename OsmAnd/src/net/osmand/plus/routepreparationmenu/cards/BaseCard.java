package net.osmand.plus.routepreparationmenu.cards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.util.Localization;

public abstract class BaseCard {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final FragmentActivity activity;

	protected View view;
	protected LayoutInflater themedInflater;

	boolean showTopShadow;
	boolean showBottomShadow;
	boolean showDivider = true;
	boolean transparentBackground;
	protected boolean usedOnMap;
	protected boolean nightMode;

	private CardListener listener;

	public interface CardListener {
		default void onCardLayoutNeeded(@NonNull BaseCard card) {
		}

		default void onCardPressed(@NonNull BaseCard card) {
		}

		default void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		}
	}

	public BaseCard(@NonNull FragmentActivity activity) {
		this(activity, true);
	}

	public BaseCard(@NonNull FragmentActivity activity, boolean usedOnMap) {
		this.activity = activity;
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.settings = app.getSettings();
		this.usedOnMap = usedOnMap;
		RequestMapThemeParams requestMapThemeParams = new RequestMapThemeParams().markIgnoreExternalProvider();
		nightMode = app.getDaynightHelper().isNightMode(usedOnMap, requestMapThemeParams);
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

	protected void notifyCardPressed() {
		if (listener != null) {
			listener.onCardPressed(this);
		}
	}

	protected void notifyButtonPressed(int buttonIndex) {
		if (listener != null) {
			listener.onCardButtonPressed(this, buttonIndex);
		}
	}

	public void setLayoutNeeded() {
		CardListener listener = this.listener;
		if (listener != null) {
			listener.onCardLayoutNeeded(this);
		}
	}

	protected abstract void updateContent();

	@NonNull
	public View build() {
		return build(activity);
	}

	@NonNull
	public View build(@NonNull Context ctx) {
		themedInflater = UiUtilities.getInflater(ctx, nightMode);
		view = themedInflater.inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public OsmandApplication getMyApplication() {
		return app;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	@ColorInt
	protected int getActiveColor() {
		return ColorUtilities.getActiveColor(app, nightMode);
	}

	@ColorInt
	protected int getMainFontColor() {
		return ColorUtilities.getPrimaryTextColor(app, nightMode);
	}

	@ColorInt
	protected int getSecondaryColor() {
		return getResolvedColor(R.color.icon_color_default_light);
	}

	protected Drawable getContentIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int icon) {
		return getColoredIcon(icon, ColorUtilities.getActiveColorId(nightMode));
	}

	protected Drawable getIcon(@DrawableRes int icon) {
		return app.getUIUtilities().getIcon(icon);
	}

	protected Drawable getColoredIcon(@DrawableRes int icon, @ColorRes int color) {
		return app.getUIUtilities().getIcon(icon, color);
	}

	protected Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return app.getUIUtilities().getPaintedIcon(id, color);
	}

	protected int getDimen(@DimenRes int dimenId) {
		return app.getResources().getDimensionPixelSize(dimenId);
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

	public void setText(int viewId, @NonNull String text) {
		if (view != null) {
			View textView = view.findViewById(viewId);
			if (textView instanceof TextView) {
				((TextView) textView).setText(text);
			}
		}
	}

	public void updateVisibility(boolean show) {
		AndroidUiHelper.updateVisibility(view, show);
	}

	public boolean isVisible() {
		return view != null && view.getVisibility() == View.VISIBLE;
	}

	@NonNull
	public final String getString(String resId) {
		return Localization.INSTANCE.getString(resId);
	}

	@NonNull
	public final String getString(@StringRes int resId) {
		return app.getString(resId);
	}

	@NonNull
	public final String getString(@StringRes int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}
}