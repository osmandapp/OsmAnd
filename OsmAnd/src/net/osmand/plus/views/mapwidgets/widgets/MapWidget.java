package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OutlineTextView;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.List;

public abstract class MapWidget {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final MapActivity mapActivity;
	protected final UiUtilities iconsCache;
	protected final OsmAndLocationProvider locationProvider;
	protected final RoutingHelper routingHelper;
	protected final WidgetsVisibilityHelper visibilityHelper;

	protected final WidgetType widgetType;
	protected boolean nightMode;

	protected WidgetsPanel panel;
	@Nullable
	protected String customId;

	protected final View view;

	public MapWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
			@Nullable String customId, @Nullable WidgetsPanel panel) {
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.customId = customId;
		this.widgetType = widgetType;
		this.iconsCache = app.getUIUtilities();
		this.locationProvider = app.getLocationProvider();
		this.routingHelper = app.getRoutingHelper();
		this.nightMode = app.getDaynightHelper().isNightMode();
		this.visibilityHelper = mapActivity.getWidgetsVisibilityHelper();
		this.view = UiUtilities.getInflater(mapActivity, nightMode).inflate(getLayoutId(), null);

		String id = customId != null ? customId : widgetType.id;
		WidgetsPanel selectedPanel = panel != null ? panel : widgetType.getPanel(id, settings);
		setPanel(selectedPanel);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	@NonNull
	public View getView() {
		return view;
	}

	/**
	 * @return preference that needs to be reset after deleting widget
	 */
	@Nullable
	public OsmandPreference<?> getWidgetSettingsPrefToReset(@NonNull ApplicationMode appMode) {
		return null;
	}

	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		WidgetState widgetState = getWidgetState();
		if (widgetState != null) {
			widgetState.copyPrefs(appMode, customId);
		}
	}

	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode,
			@NonNull ApplicationMode appMode, @Nullable String customId) {
	}

	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel panel,
			@NonNull List<MapWidget> followingWidgets) {
		container.addView(view);
	}

	public void detachView(@NonNull WidgetsPanel widgetsPanel) {
		ViewParent parent = view.getParent();
		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).removeView(view);
		}
	}

	public boolean isNightMode() {
		return nightMode;
	}

	@Nullable
	public WidgetState getWidgetState() {
		return null;
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	public boolean isExternal() {
		return getWidgetType() == WidgetType.AIDL_WIDGET;
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

	protected void setPanel(@NonNull WidgetsPanel panel) {
		this.panel = panel;
	}

	public boolean isVerticalWidget() {
		return panel.isPanelVertical();
	}

	public static void updateTextColor(@NonNull TextView text, @Nullable TextView textShadow,
			@ColorInt int textColor, @ColorInt int textShadowColor,
			boolean boldText, int shadowRadius) {
		int typefaceStyle = boldText ? Typeface.BOLD : Typeface.NORMAL;

		if (textShadow != null) {
			if (shadowRadius > 0) {
				AndroidUiHelper.updateVisibility(textShadow, true);
				textShadow.setTypeface(Typeface.DEFAULT, typefaceStyle);
				textShadow.getPaint().setStrokeWidth(shadowRadius);
				textShadow.getPaint().setStyle(Style.STROKE);
				textShadow.setTextColor(textShadowColor);
				textShadow.invalidate();
			} else {
				AndroidUiHelper.updateVisibility(textShadow, false);
			}
		}
		text.setTextColor(textColor);
		text.setTypeface(Typeface.DEFAULT, typefaceStyle);
	}

	public static void updateTextOutline(@Nullable TextView textView, @NonNull TextState textState) {
		updateTextOutline(textView, textState, null);
	}

	public static void updateTextOutline(@Nullable TextView textView, @NonNull TextState textState, @Nullable Integer typeFace) {
		if (textView instanceof OutlineTextView outlineTextView) {
			if (textState.textShadowRadius > 0) {
				outlineTextView.setStrokeWidth(textState.textShadowRadius);
				int color = textState.textShadowColor;
				if (color != 0) {
					outlineTextView.setStrokeColor(textState.textShadowColor);
				}
				outlineTextView.showOutline(true);

				if (typeFace != null) {
					outlineTextView.setTypeface(Typeface.DEFAULT, typeFace);
				}
			} else {
				outlineTextView.showOutline(false);
			}
			outlineTextView.invalidate();
		}
	}

	public static void updateTextOutlineTextView(@Nullable TextView newTextView, @Nullable TextView oldTextView) {
		if (newTextView instanceof OutlineTextView newOutlineTextView && oldTextView instanceof OutlineTextView oldOutlineTextView) {
			newOutlineTextView.setStrokeColor(oldOutlineTextView.getStrokeColor());
			newOutlineTextView.setStrokeWidth(oldOutlineTextView.getStrokeWidth());
			newOutlineTextView.showOutline(oldOutlineTextView.shouldShowOutline());
		}
	}

	@NonNull
	protected String getString(@StringRes int stringId, Object... args) {
		if (args.length > 0) {
			return app.getString(stringId, args);
		} else {
			return app.getString(stringId);
		}
	}

	@Dimension
	protected int getDimensionPixelSize(@DimenRes int resId) {
		return getMyApplication().getResources().getDimensionPixelSize(resId);
	}

	@NonNull
	public OsmandApplication getMyApplication() {
		return app;
	}
}