package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.*;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
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

	private View view;

	public MapWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
			@Nullable String customId, @Nullable WidgetsPanel panel) {
		this.app = mapActivity.getApp();
		this.settings = app.getSettings();
		this.mapActivity = mapActivity;
		this.customId = customId;
		this.widgetType = widgetType;
		this.iconsCache = app.getUIUtilities();
		this.locationProvider = app.getLocationProvider();
		this.routingHelper = app.getRoutingHelper();
		this.nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		this.visibilityHelper = mapActivity.getWidgetsVisibilityHelper();

		String id = customId != null ? customId : widgetType.id;
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
		WidgetsPanel selectedPanel = panel != null ? panel : widgetType.getPanel(id, settings, layoutMode);
		setPanel(selectedPanel);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	public void initView() {
		if (view == null) {
			view = getView();
		}
	}

	@NonNull
	public View getView() {
		if (view == null) {
			view = UiUtilities.getInflater(mapActivity, nightMode).inflate(getLayoutId(), null);
			setupView(view);
		}
		return view;
	}

	protected void setupView(@NonNull View view) {

	}

	@NonNull
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	/**
	 * @return preference that needs to be reset after deleting widget
	 */
	@Nullable
	public CommonPreference<?> getWidgetSettingsPrefToReset(@NonNull ApplicationMode appMode, @Nullable ScreenLayoutMode layoutMode) {
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

	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel panel, @NonNull List<MapWidget> followingWidgets) {
		container.addView(getView());
	}

	public void detachView(@NonNull WidgetsPanel widgetsPanel, @NonNull List<MapWidgetInfo> widgets, @NonNull ApplicationMode mode) {
		View view = getView();
		if (view.getParent() instanceof ViewGroup viewGroup) {
			viewGroup.removeView(view);
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
		updateInfo(getView(), drawSettings);
	}

	protected abstract void updateInfo(@NonNull View view, @Nullable DrawSettings drawSettings);

	public void updateColors(@NonNull TextState textState) {
		nightMode = textState.night;
	}

	protected boolean updateVisibility(boolean visible) {
		return AndroidUiHelper.updateVisibility(getView(), visible);
	}

	public boolean isViewVisible() {
		return getView().getVisibility() == View.VISIBLE;
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

		updateTextShadow(textShadow, textShadowColor, shadowRadius, typefaceStyle);

		text.setTextColor(textColor);
		text.setTypeface(Typeface.DEFAULT, typefaceStyle);
	}

	public static void updateTextColor(@NonNull OutlinedTextContainer text, @Nullable TextView textShadow,
			@ColorInt int textColor, @ColorInt int textShadowColor,
			boolean boldText, int shadowRadius) {
		int typefaceStyle = boldText ? Typeface.BOLD : Typeface.NORMAL;

		updateTextShadow(textShadow, textShadowColor, shadowRadius, typefaceStyle);

		text.setTextColor(textColor);
		text.setTypeface(Typeface.DEFAULT, typefaceStyle);
		text.showOutline(false);
	}

	private static void updateTextShadow(@Nullable TextView textShadow, @ColorInt int textShadowColor, int shadowRadius, int typefaceStyle){
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
	}

	public static void updateTextOutline(@Nullable OutlinedTextContainer textContainer, @NonNull TextState textState) {
		if (textContainer == null) {
			return;
		}

		if (textState.textShadowRadius > 0) {
			textContainer.setStrokeWidth(textState.textShadowRadius);
			int color = textState.textShadowColor;
			if (color != 0) {
				textContainer.setStrokeColor(textState.textShadowColor);
			}
			textContainer.showOutline(true);
		} else {
			textContainer.showOutline(false);
		}
		textContainer.invalidateTextViews();
	}

	public static void updateTextContainer(@Nullable OutlinedTextContainer textContainer, @NonNull TextState textState) {
		if (textContainer == null) {
			return;
		}

		int typefaceStyle = textState.textBold ? Typeface.BOLD : Typeface.NORMAL;
		textContainer.setTextColor(textState.textColor);
		textContainer.setTypeface(Typeface.DEFAULT, typefaceStyle);
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