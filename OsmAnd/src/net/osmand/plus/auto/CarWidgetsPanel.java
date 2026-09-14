package net.osmand.plus.auto;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prototype of a generic widgets panel drawn over the Android Auto map surface.
 * <p>
 * The panel reuses the regular map widgets of the current profile ({@link WidgetsPanel#RIGHT}
 * by default), so any widget - including the OBD II ones of the Vehicle metrics plugin - shows
 * up in the car without a car-specific implementation. Widgets are drawn on the right side of
 * the surface to keep the left part free for the navigation card of the head unit. When more
 * widgets are enabled than fit on the screen, a click on the panel shows the next page.
 * <p>
 * Widget views are created and laid out by this class only and are never attached to a window,
 * so the phone UI is not affected.
 */
public class CarWidgetsPanel {

	/** Panel is drawn only when the visible area is at least that wide. */
	private static final float MIN_SURFACE_WIDTH_DP = 400f;
	private static final float PANEL_MARGIN_DP = 10f;
	private static final float CORNER_RADIUS_DP = 8f;
	private static final float BORDER_WIDTH_DP = 2f;
	private static final float DIVIDER_WIDTH_DP = 1f;
	private static final float PANEL_PADDING_DP = 4f;
	private static final float WIDGET_WIDTH_DP = 130f;
	/**
	 * Width of the panel in car dp. The car screen is viewed from about twice the distance of a
	 * phone, so the widgets are drawn ~1.5 times bigger than the {@link #WIDGET_WIDTH_DP} used on
	 * the phone, which keeps them slightly larger than a phone widget in angular size.
	 */
	private static final float PANEL_WIDTH_CAR_DP = 200f;
	/** Safety net for narrow head units (800x480), the panel never takes more than this. */
	private static final float MAX_PANEL_WIDTH_RATIO = 0.22f;
	/** Fraction of the visible area height the panel is allowed to occupy. */
	private static final float MAX_PANEL_HEIGHT_RATIO = 0.7f;

	private final CarContext carContext;
	private final OsmandApplication app;
	private final WidgetsPanel panel;

	private final List<MapWidget> widgets = new ArrayList<>();
	private final RectF lastPanelBounds = new RectF();

	private ApplicationMode cachedAppMode;
	private Boolean cachedNightMode;
	private List<String> cachedWidgetIds = new ArrayList<>();
	private int firstVisibleWidget;
	private int lastVisibleCount;

	private final Paint borderPaint = new Paint();
	private final Paint backgroundPaint = new Paint();
	private final Paint dividerPaint = new Paint();

	public CarWidgetsPanel(@NonNull OsmandApplication app,  @NonNull CarContext carContext) {
		this(app, carContext, WidgetsPanel.ANDROID_AUTO);
	}

	public CarWidgetsPanel(@NonNull OsmandApplication app, @NonNull CarContext carContext, @NonNull WidgetsPanel panel) {
		this.app = app;
		this.panel = panel;
		this.carContext = carContext;

		borderPaint.setDither(true);
		borderPaint.setAntiAlias(true);
		borderPaint.setStyle(Paint.Style.STROKE);

		dividerPaint.setDither(true);
		dividerPaint.setAntiAlias(true);
		dividerPaint.setStyle(Paint.Style.STROKE);

		dividerPaint.setDither(true);
		dividerPaint.setAntiAlias(true);
		backgroundPaint.setStyle(Paint.Style.FILL);
	}

	public void updateWidgetsInfo(DrawSettings drawSettings) {
		for (MapWidget w : widgets) {
			w.updateInfo(drawSettings);
		}
	}

	public void reloadWidgets(DrawSettings drawSettings) {
		clearWidgets();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		List<MapWidgetInfo> widgetInfos = getWidgetInfos(appMode);
		List<String> widgetIds = widgetInfos.stream().map(v -> v.key).collect(Collectors.toList());
		boolean nightMode = drawSettings.isNightMode();
		cachedAppMode = appMode;
		cachedNightMode = nightMode;
		cachedWidgetIds = widgetIds;
		recreateWidgets(nightMode, widgetInfos);
	}

	/**
	 * @param topOffset constant offset from the top of the visible area, it only depends on the
	 *                  speedometer, which does not appear and disappear while driving.
	 * @param hiddenArea area covered by a transient widget (the alarm). Rows that fall into it are
	 *                   hidden and their slots are kept, so that the panel never shifts.
	 * @return height occupied by the panel in surface pixels.
	 */
	public float drawWidgets(@NonNull Canvas canvas, @NonNull Rect visibleArea,
			@NonNull DrawSettings drawSettings, float carDensity, float topOffset,
			@Nullable Rect hiddenArea) {
		lastPanelBounds.setEmpty();
		lastVisibleCount = 0;
		if (!app.getSettings().AA_SHOW_WIDGETS_PANEL.get()
				|| visibleArea.width() < MIN_SURFACE_WIDTH_DP * carDensity) {
			return 0;
		}
		if (widgets.isEmpty()) {
			return 0;
		}
		if (firstVisibleWidget >= widgets.size()) {
			firstVisibleWidget = 0;
		}
		// Widget views are inflated with the application resources, so they are measured in phone
		// pixels and scaled while drawing. The panel is sized in car dp, the car screen is looked
		// at from farther away than a phone, so the widgets are drawn bigger than on the phone.
		float appDensity = app.getResources().getDisplayMetrics().density;
		int widgetWidth = (int) (WIDGET_WIDTH_DP * appDensity);
		float panelWidth = Math.min(PANEL_WIDTH_CAR_DP * carDensity,
				visibleArea.width() * MAX_PANEL_WIDTH_RATIO);
		float panelPadding = PANEL_PADDING_DP * carDensity;
		float borderWidth = BORDER_WIDTH_DP * carDensity;
		float panelContentWidth = panelWidth - panelPadding * 2 - borderWidth * 2;
		float corner = CORNER_RADIUS_DP * carDensity;
		float dividerWidth = DIVIDER_WIDTH_DP * carDensity;

		float scale = panelContentWidth / widgetWidth;
		boolean isRtl = carContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

		// The panel is flush with the right edge and keeps a fixed top, so that it does not jump
		// when the speedometer or the alarm widget appear or change size.
		float right = visibleArea.right;
		float left = right - panelWidth;
		float top = visibleArea.top + topOffset;
		float maxHeight = Math.min(visibleArea.bottom - top, visibleArea.height() * MAX_PANEL_HEIGHT_RATIO);
		float maxBottom = top + maxHeight;

		float contentRight = right - panelPadding - borderWidth;
		float contentLeft = left + panelPadding + borderWidth;
		float contentTop = top + borderWidth;
		float maxContentBottom = maxBottom - borderWidth;

		List<Float> tops = new ArrayList<>();
		List<Float> bottoms = new ArrayList<>();
		List<MapWidget> drawnWidgets = new ArrayList<>();

		float y = contentTop;
		for (int i = firstVisibleWidget; i < widgets.size(); i++) {
			MapWidget widget = widgets.get(i);
			if (!widget.shouldDrawForAndroidAuto()) {
				lastVisibleCount++;
				continue;
			}
			float measuredHeight = widget.measureHeightForAndroidAuto(widgetWidth);
			if (measuredHeight <= 0) {
				lastVisibleCount++;
				continue;
			}
			float height = measuredHeight * scale;
			if (y + height > maxContentBottom) {
				break;
			}
			// A row is dropped only when the transient widget really covers it, a small overlap
			// on the edge is not worth losing a whole row for.
			boolean covered = hiddenArea != null
					&& Math.min(hiddenArea.bottom, y + height) > Math.max(hiddenArea.top, y)
					&& Math.min(hiddenArea.right, contentRight) - Math.max(hiddenArea.left, contentLeft)
					> panelContentWidth / 2;
			if (!covered) {
				drawnWidgets.add(widget);
				tops.add(y);
				bottoms.add(y + height);
			}
			// The slot is kept even for a hidden widget, the panel must not shift.
			y += height + dividerWidth;
			lastVisibleCount++;
		}
		if (drawnWidgets.isEmpty()) {
			return 0;
		}

		//  Rows hidden by the reserved area split the panel into several blocks, each of them gets its own outline.
		int blockStart = 0;
		for (int i = 1; i <= drawnWidgets.size(); i++) {
			boolean endOfBlock = i == drawnWidgets.size()
					|| bottoms.get(i - 1) + 1 < tops.get(i);
			if (endOfBlock) {
				drawBlock(canvas, drawSettings,
						drawnWidgets.subList(blockStart, i),
						tops.subList(blockStart, i), bottoms.subList(blockStart, i),
						contentLeft, contentRight,
						panelPadding, corner,
						borderWidth, dividerWidth, scale, isRtl);
				lastPanelBounds.union(left, tops.get(blockStart), right, bottoms.get(i - 1));
				blockStart = i;
			}
		}
		return lastPanelBounds.height();
	}

	private void drawBlock(@NonNull Canvas canvas,
						   @NonNull DrawSettings drawSettings,
						   @NonNull List<MapWidget> widgets,
	                       @NonNull List<Float> tops, @NonNull List<Float> bottoms,
	                       float contentLeft, float contentRight, float padding,
	                       float corner, float borderWidth, float dividerWidth,
	                       float scale, boolean isRtl) {
		Path backgroundPath = new Path();
		float blockTop = tops.get(0);
		float blockBottom = bottoms.get(bottoms.size() - 1);

		float blockLeft = contentLeft - padding;
		float blockRight = contentRight + padding;

		backgroundPath.addRoundRect(
				new RectF(blockLeft, blockTop, blockRight, blockBottom),
				new float[]{
						corner, corner,
						corner, corner,
						corner, corner,
						corner, corner
				},
				Path.Direction.CW
		);

		canvas.save();
		canvas.clipPath(backgroundPath);

		drawBackground(canvas, backgroundPath);
		drawBorder(canvas, backgroundPath, borderWidth);

		float widgetWidth = (contentRight - contentLeft) / scale;
//		float widgetHeight;
		float widgetContentTop, widgetContentBottom;
		for (int i = 0; i < widgets.size(); i++) {
			widgetContentTop = tops.get(i);
			widgetContentBottom = bottoms.get(i);
			drawWidget(canvas, drawSettings, widgets.get(i),
					contentLeft, contentRight,
					widgetContentTop, widgetContentBottom,
					scale, widgetWidth, isRtl);
//			widgetHeight = (widgetContentBottom - widgetContentTop) / scale;
//			canvas.save();
//			canvas.translate(contentLeft, tops.get(i));
//			canvas.scale(scale, scale);
//			widgets.get(i).drawForAndroidAuto(canvas, drawSettings,
//					widgetWidth, widgetHeight, isRtl);
//			canvas.restore();
			if (i < widgets.size() - 1) {
				drawSeparator(canvas, widgetContentBottom, dividerWidth, blockRight, blockLeft);
			}
		}

		canvas.restore();
	}

	private void drawWidget(@NonNull Canvas canvas,
							@NonNull DrawSettings drawSettings,
							@NonNull MapWidget widget,
							float contentLeft, float contentRight,
							float widgetContentTop, float widgetContentBottom,
							float scale,
							float widgetWidth,
							boolean isRtl) {
		float widgetHeight = (widgetContentTop - widgetContentBottom) / scale;
		canvas.save();
		canvas.translate(contentLeft, widgetContentTop);
		canvas.scale(scale, scale);
		widget.drawForAndroidAuto(canvas, drawSettings,
				widgetWidth, widgetHeight, isRtl);
		canvas.restore();
	}

	private void drawBackground(@NonNull Canvas canvas, @NonNull Path backgroundPath) {
		canvas.drawPath(backgroundPath, backgroundPaint);
	}
	private void drawBorder(@NonNull Canvas canvas, @NonNull Path backgroundPath, float borderWidth) {
		borderPaint.setStrokeWidth(borderWidth*2);
		canvas.drawPath(backgroundPath, borderPaint);
	}

	private void drawSeparator(@NonNull Canvas canvas, @NonNull Float widgetBottom, float dividerWidth, float blockRight, float blockLeft) {
		dividerPaint.setStrokeWidth(dividerWidth);
		canvas.drawLine(
				blockLeft, widgetBottom + dividerWidth / 2,
				blockRight, widgetBottom + dividerWidth / 2,
				dividerPaint
		);
	}


	/**
	 * @return true if the click was handled by the panel.
	 */
	public boolean onSurfaceClick(float x, float y) {
		if (lastPanelBounds.isEmpty() || !lastPanelBounds.contains(x, y)) {
			return false;
		}
		int next = firstVisibleWidget + Math.max(lastVisibleCount, 1);
		firstVisibleWidget = next < widgets.size() ? next : 0;
		return true;
	}

	@NonNull
	private List<MapWidget> getWidgets(boolean nightMode) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		List<MapWidgetInfo> widgetInfos = getWidgetInfos(appMode);
		List<String> widgetIds = widgetInfos.stream().map(v -> v.key).collect(Collectors.toList());

		if (appMode != cachedAppMode
				|| !Boolean.valueOf(nightMode).equals(cachedNightMode)
				|| !Algorithms.objectEquals(widgetIds, cachedWidgetIds)) {
			cachedAppMode = appMode;
			cachedNightMode = nightMode;
			cachedWidgetIds = widgetIds;
			recreateWidgets(nightMode, widgetInfos);
		}
		return widgets;
	}

	private List<MapWidgetInfo> getWidgetInfos(@NonNull ApplicationMode appMode) {
		int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE;
		MapWidgetRegistry widgetRegistry = app.getMapWidgetRegistry();
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode, enabledWidgetsFilter, List.of(panel));
		return new ArrayList<>(widgetInfos);
	}

	private void recreateWidgets(boolean nightMode, @Nullable List<MapWidgetInfo> widgetInfos) {
		widgets.clear();
		firstVisibleWidget = 0;

		if (Algorithms.isEmpty(widgetInfos)) {
			return;
		}

		float density = app.getResources().getDisplayMetrics().density;
		ResolvedPanelAppearance appearance = app.getPanelAppearanceSettingsManager()
				.resolveCommitted(panel, null, nightMode, false, density, true);
		applyPanelAppearance(appearance);

		for (MapWidgetInfo info : widgetInfos) {
			MapWidget widget = info.widget;
			widget.applyPanelAppearance(appearance);
			widgets.add(widget);
		}
	}

	private void applyPanelAppearance(@NonNull ResolvedPanelAppearance appearance) {
		int baseBorderColor = ColorUtilities.removeAlpha(appearance.getPanelBorderColor());
		int borderColor = ColorUtilities.getColorWithAlpha(baseBorderColor, 0.7f);
		borderPaint.setColor(borderColor);
		backgroundPaint.setColor(appearance.getBackground().getColor());
		dividerPaint.setColor(appearance.getDividerColor());
	}

	public void clearWidgets() {
		widgets.clear();
		lastPanelBounds.setEmpty();
		lastVisibleCount = 0;
		firstVisibleWidget = 0;
		cachedAppMode = null;
		cachedNightMode = null;
	}
}
