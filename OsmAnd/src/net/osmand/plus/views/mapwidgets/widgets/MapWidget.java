package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
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
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceConsumer;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
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

	@Nullable
	protected ResolvedPanelAppearance androidAutoPanelAppearance;
	private volatile boolean isWidgetAALayoutNeeded = true;
	protected float measuredAAHeight = 0f;
	protected float measuredAAWidth = 0f;
	protected Bitmap androidAutoBitmap;
	protected Canvas androidAutoCanvas;
	protected final Paint androidAutoBitmapPaint = new Paint();

	protected volatile boolean isAndroidAuto;

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

	public MapWidget(@NonNull OsmandApplication app, @NonNull WidgetType widgetType,
	                 @Nullable String customId, @Nullable WidgetsPanel panel) {
		this.app = app;
		this.settings = app.getSettings();
		this.customId = customId;
		this.widgetType = widgetType;
		this.iconsCache = app.getUIUtilities();
		this.locationProvider = app.getLocationProvider();
		this.routingHelper = app.getRoutingHelper();
		this.nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		this.mapActivity = null;
		this.visibilityHelper = null;
		String id = customId != null ? customId : widgetType.id;
		WidgetsPanel selectedPanel = panel != null ? panel : widgetType.getPanel(id, settings, null);
		setPanel(selectedPanel);
	}

	@LayoutRes
	protected abstract int getLayoutId();

	public void initView() {
		if (view == null) {
			view = getView();
		}
	}

	public void initAndroidAuto() {

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
		if (isAndroidAuto()) {
			recreateInternalForAndroidAuto();
			ResolvedPanelAppearance appearance = androidAutoPanelAppearance;
			if (appearance != null && appearance.getPanel() == panel) {
				onAndroidAutoPanelAppearanceChanged(appearance);
			}
		} else {
			recreateViewInternal();
			ResolvedPanelAppearance appearance = panelAppearance;
			if (appearance != null && appearance.getPanel() == panel) {
				onPanelAppearanceChanged(appearance);
			}
		}
	}

	protected void recreateViewInternal() {

	}

	protected void recreateInternalForAndroidAuto() {
		initAndroidAuto();
	}

	protected void setupView(@NonNull View view) {

	}

	// region android auto

	public void applyPanelAppearanceForAndroidAuto(@NonNull ResolvedPanelAppearance appearance) {
		androidAutoPanelAppearance = appearance;
		onAndroidAutoPanelAppearanceChanged(appearance);
		markAndroidAutoLayoutNeeded();
	}

	public void onAndroidAutoPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {

	}

	public boolean shouldDrawForAndroidAuto() {
		return widgetType.supportsAndroidAuto;
	}

	public float getMeasuredAAHeight() {
		return measuredAAHeight;
	}

	public float getMeasuredAAWidth() {
		return measuredAAWidth;
	}

	public void updateAndroidAutoBitmap(@NonNull DrawSettings drawSettings, boolean isRtl) {
		int height = (int) measuredAAHeight;
		int width = (int) measuredAAWidth;
		boolean isValidSize = width > 0 && height > 0;
		boolean isDifferentSize = androidAutoBitmap == null
				|| height != androidAutoBitmap.getHeight()
				|| width != androidAutoBitmap.getWidth();
		if (isDifferentSize) {
			if (androidAutoBitmap != null) {
				androidAutoBitmap.recycle();
				androidAutoBitmap = null;
				androidAutoCanvas = null;
			}
			if (isValidSize) {
				androidAutoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				androidAutoCanvas = new Canvas(androidAutoBitmap);
			}
		} else {
			clearAndroidAutoBitmap();
		}
		if (androidAutoCanvas != null) {
			drawForAndroidAuto(androidAutoCanvas, drawSettings, width, height, isRtl);
		}
	}

	@Nullable
	public Bitmap getAndroidAutoBitmap() {
		return androidAutoBitmap;
	}

	private void clearAndroidAutoBitmap() {
		if (androidAutoCanvas != null) {
			androidAutoCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.SRC);
		}
	}

	public void drawForAndroidAuto(@NonNull Canvas canvas, @NonNull DrawSettings drawSettings,
	                               float widgetWidthPx, float widgetHeightPx, boolean isRtl) {

	}

	public void drawAndroidAutoBitmap(@NonNull Canvas canvas) {
		Bitmap bitmap = getAndroidAutoBitmap();
		if (bitmap != null) {
			canvas.drawBitmap(androidAutoBitmap, 0, 0, androidAutoBitmapPaint);
		}
	}

	public final void markAndroidAutoLayoutNeeded() {
		isWidgetAALayoutNeeded = true;
	}

	public boolean isAndroidAutoLayoutNeeded() {
		return isWidgetAALayoutNeeded;
	}

	public final boolean layoutAAIfNeeded(Context context, int desiredWidthPx, boolean isRtl) {
		if (!isWidgetAALayoutNeeded) {
			return false;
		}
		isWidgetAALayoutNeeded = false;
		doLayoutAAWidget(context, desiredWidthPx, isRtl);
		return true;
	}

	protected void doLayoutAAWidget(Context context, int desiredWidthPx, boolean isRtl) {
		// layout and set measuredAAHeight and measuredAAWidth in subclasses
	}
	// endregion

	// null in case of android auto widge
	@Nullable
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
		if (isAndroidAuto()) {
			updateInfoForAndroidAuto(drawSettings);
		} else if (mapActivity != null) {
			updateInfo(getView(), drawSettings);
		}
	}

	protected abstract void updateInfo(@NonNull View view, @Nullable DrawSettings drawSettings);

	protected void updateInfoForAndroidAuto(@Nullable DrawSettings drawSettings) {
	}

	@Override
	public final void applyPanelAppearance(@NonNull ResolvedPanelAppearance appearance) {
		if (isAndroidAuto()) {
			applyPanelAppearanceForAndroidAuto(appearance);
		} else {
			panelAppearance = appearance;
			nightMode = appearance.getNightMode();
			if (mapActivity != null) {
				getView();
				onPanelAppearanceChanged(appearance);
			}
		}
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

	private static void updateTextShadow(@Nullable TextView textShadow, @ColorInt int textShadowColor, int shadowRadius, int typefaceStyle) {
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

	public boolean isAndroidAuto() {
		return isAndroidAuto;
	}

	public void setAndroidAuto(boolean androidAuto) {
		isAndroidAuto = androidAuto;
	}
}
