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
import net.osmand.plus.settings.enums.PanelIconMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceConsumer;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.configure.appearance.PanelAppearanceSettings;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

import java.util.List;

public abstract class MapWidget implements PanelAppearanceConsumer {

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
	@Nullable
	private ResolvedPanelAppearance panelAppearance;

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

	@NonNull
	protected WidgetSize resolveWidgetSize(@NonNull WidgetSize individualSize) {
		ResolvedPanelAppearance appearance = panelAppearance;
		if (appearance != null && appearance.getPanel() == panel) {
			WidgetSize resolvedSize = appearance.getSizeMode().getWidgetSize();
			return resolvedSize != null ? resolvedSize : individualSize;
		}
		return PanelAppearanceSettings.resolveWidgetSize(app, panel, individualSize, mapActivity);
	}

	@NonNull
	protected PanelIconMode resolvePanelIconMode() {
		ResolvedPanelAppearance appearance = panelAppearance;
		if (appearance != null && appearance.getPanel() == panel) {
			return appearance.getIconMode();
		}
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
		return app.getPanelAppearanceSettingsManager().get(panel).getIconModePref(layoutMode).get();
	}

	protected boolean resolveIconVisibility(boolean individualShowIcon) {
		return switch (resolvePanelIconMode()) {
			case ON -> true;
			case OFF -> false;
			case ORIGINAL -> individualShowIcon;
		};
	}

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

	public final void recreateView() {
		recreateViewInternal();
		ResolvedPanelAppearance appearance = panelAppearance;
		if (appearance != null && appearance.getPanel() == panel) {
			onPanelAppearanceChanged(appearance);
		}
	}

	protected void recreateViewInternal() {

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

	@Override
	public final void applyPanelAppearance(@NonNull ResolvedPanelAppearance appearance) {
		panelAppearance = appearance;
		nightMode = appearance.getNightMode();
		getView();
		onPanelAppearanceChanged(appearance);
	}

	protected void onPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {

	}

	@Nullable
	protected final ResolvedPanelAppearance getPanelAppearance() {
		return panelAppearance;
	}

	protected boolean updateVisibility(boolean visible) {
		return AndroidUiHelper.updateVisibility(getView(), visible);
	}

	public boolean isViewVisible() {
		return getView().getVisibility() == View.VISIBLE;
	}

	public boolean supportsPanelRowDivider() {
		return true;
	}

	public boolean isAttached() {
		return view != null && view.getParent() != null;
	}

	protected void setPanel(@NonNull WidgetsPanel panel) {
		this.panel = panel;
	}

	public boolean isVerticalWidget() {
		return panel.isPanelVertical();
	}

	public static void updateTextColor(@Nullable TextView text, @Nullable TextView textShadow,
			@ColorInt int textColor, @ColorInt int textShadowColor, boolean boldText, int shadowRadius) {
		int typefaceStyle = boldText ? Typeface.BOLD : Typeface.NORMAL;

		updateTextShadow(textShadow, textShadowColor, shadowRadius, typefaceStyle);

		if (text != null) {
			text.setTextColor(textColor);
			text.setTypeface(Typeface.DEFAULT, typefaceStyle);
		}
	}

	public static void updateTextColor(@Nullable OutlinedTextContainer text, @Nullable TextView textShadow,
			@ColorInt int textColor, @ColorInt int textShadowColor, boolean boldText, int shadowRadius) {
		int typefaceStyle = boldText ? Typeface.BOLD : Typeface.NORMAL;

		updateTextShadow(textShadow, textShadowColor, shadowRadius, typefaceStyle);

		if (text != null) {
			text.setTextColor(textColor);
			text.setTypeface(Typeface.DEFAULT, typefaceStyle);
			text.showOutline(false);
		}
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