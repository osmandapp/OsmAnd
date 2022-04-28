package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import java.util.List;

public abstract class MapWidget {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final MapActivity mapActivity;
	protected final UiUtilities iconsCache;

	private boolean nightMode;

	protected final View view;

	public MapWidget(@NonNull MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.iconsCache = app.getUIUtilities();
		this.nightMode = app.getDaynightHelper().isNightMode();
		this.view = UiUtilities.getInflater(mapActivity, nightMode).inflate(getLayoutId(), null);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@NonNull
	public View getView() {
		return view;
	}

	@Nullable
	public OsmandPreference<Boolean> getWidgetVisibilityPref() {
		return null;
	}

	public void attachView(@NonNull ViewGroup container, int order, @NonNull List<MapWidget> followingWidgets) {
		container.addView(view);
	}

	public void detachView() {
		ViewParent parent = view.getParent();
		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).removeView(view);
		}
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void updateInfo(@Nullable DrawSettings drawSettings) {
		// Not implemented
	}

	public void updateColors(@NonNull TextState textState) {
		nightMode = textState.night;
	}

	protected boolean updateVisibility(boolean visible) {
		return AndroidUiHelper.updateVisibility(view, visible);
	}

	public boolean isViewVisible() {
		return view.getVisibility() == View.VISIBLE;
	}

	public static void updateTextColor(@NonNull TextView text, @Nullable TextView textShadow,
	                                   @ColorInt int textColor, @ColorInt int textShadowColor,
	                                   boolean boldText, int shadowRadius) {
		int typefaceStyle = boldText ? Typeface.BOLD : Typeface.NORMAL;

		if (textShadow != null) {
			if (shadowRadius > 0) {
				AndroidUiHelper.updateVisibility(textShadow, true);
				textShadow.setVisibility(View.VISIBLE);
				textShadow.setTypeface(Typeface.DEFAULT, typefaceStyle);
				textShadow.getPaint().setStrokeWidth(shadowRadius);
				textShadow.getPaint().setStyle(Style.STROKE);
				textShadow.setTextColor(textShadowColor);
			} else {
				AndroidUiHelper.updateVisibility(textShadow, false);
			}
		}

		text.setTextColor(textColor);
		text.setTypeface(Typeface.DEFAULT, typefaceStyle);
	}

	@NonNull
	protected String getString(@StringRes int stringId, Object... args) {
		return app.getString(stringId, args);
	}
}