package net.osmand.plus.auto;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private final List<MapWidgetInfo> widgetInfos = new ArrayList<>();
	private final Set<String> visibleWidgetIds = new HashSet<>();
	private final RectF lastPanelBounds = new RectF();

	private int firstVisibleWidget;
	private int lastVisibleCount;

	private final Paint borderPaint = new Paint();
	private final Paint backgroundPaint = new Paint();
	private final Paint dividerPaint = new Paint();

	private WidgetsBuffer widgetsBuffer = null;
	private final Paint blitPaint = new Paint();

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
		backgroundPaint.setAntiAlias(true);
	}

	public void reloadWidgets(DrawSettings drawSettings) {
		clearWidgets();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		List<MapWidgetInfo> newWidgetInfos = getWidgetInfos(appMode);

		if (Algorithms.isEmpty(newWidgetInfos)) {
			return;
		}

		boolean nightMode = drawSettings.isNightMode();
		float density = app.getResources().getDisplayMetrics().density;
		ResolvedPanelAppearance appearance = app.getPanelAppearanceSettingsManager()
				.resolveCommitted(panel, null, nightMode, false, density, true);
		applyPanelAppearance(appearance);

		ApplicationMode applicationMode = app.getSettings().getApplicationMode();
		List<String> visibilities = MapWidgetInfo.getAndroidAutoWidgetsVisibility(app, applicationMode);
		for (MapWidgetInfo info : newWidgetInfos) {
			MapWidget widget = info.widget;
			widget.applyPanelAppearanceForAndroidAuto(appearance);
			widgetInfos.add(info);
			if (info.isEnabledForAppMode(applicationMode, visibilities)) {
				visibleWidgetIds.add(info.key);
			}
		}
	}

	private void recycleWidgetsBufferIfNeeded(int width, int height) {
		if (widgetsBuffer == null || widgetsBuffer.offscreenBitmap.getWidth() != width
				|| widgetsBuffer.offscreenBitmap.getHeight() != height) {
			if (widgetsBuffer != null) {
				widgetsBuffer.offscreenBitmap.recycle();
			}
			widgetsBuffer = new WidgetsBuffer(width, height);
		}
	}

	public boolean hasWidgetsWithDirtyLayout() {
		return widgetInfos.stream().anyMatch(w -> w.widget.isAndroidAutoLayoutNeeded());
	}

	public void bakeWidgets(@NonNull Rect visibleArea,
	                        @NonNull DrawSettings drawSettings, float carDensity, float topOffset,
	                        @Nullable Rect hiddenArea) {
		bakeWidgets(visibleArea, drawSettings, carDensity, topOffset, hiddenArea, false);
	}

	public void bakeWidgets(@NonNull Rect visibleArea,
	                        @NonNull DrawSettings drawSettings, float carDensity, float topOffset,
	                        @Nullable Rect hiddenArea, boolean forceRedraw) {
		lastPanelBounds.setEmpty();
		lastVisibleCount = 0;
		if (!app.getSettings().AA_SHOW_WIDGETS_PANEL.get()
				|| visibleArea.width() < MIN_SURFACE_WIDTH_DP * carDensity) {
			return;
		}
		if (widgetInfos.isEmpty()) {
			return;
		}
		if (firstVisibleWidget >= widgetInfos.size()) {
			firstVisibleWidget = 0;
		}

		RectF maxPanelRect = calculateAvailablePanelRect(visibleArea, carDensity, topOffset);
		recycleWidgetsBufferIfNeeded((int) maxPanelRect.width(), (int) maxPanelRect.height());

		RectF localDirtyRect = new RectF();
		if (widgetsBuffer.maxBoundsDiffer(maxPanelRect) || hasWidgetsWithDirtyLayout() || forceRedraw) {
			localDirtyRect.set(0, 0, maxPanelRect.width(), maxPanelRect.height());
		}

		if (localDirtyRect.isEmpty()) return;

		float panelWidth = maxPanelRect.width();

		float appDensity = app.getResources().getDisplayMetrics().density;
		int widgetWidth = (int) (WIDGET_WIDTH_DP * appDensity);
		float panelPadding = PANEL_PADDING_DP * carDensity;
		float borderWidth = BORDER_WIDTH_DP * carDensity;
		float panelContentWidth = panelWidth - panelPadding * 2 - borderWidth * 2;

		float corner = CORNER_RADIUS_DP * carDensity;
		float dividerWidth = DIVIDER_WIDTH_DP * carDensity;

		float scale = panelContentWidth / widgetWidth;
		boolean isRtl = carContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

		float left = maxPanelRect.left;
		float top = maxPanelRect.top;

		float localLeft = 0;
		float localTop = 0;
		float localRight = maxPanelRect.width();
		float localMaxBottom = maxPanelRect.height();

		float contentRight = localRight - panelPadding - borderWidth;
		float contentLeft = localLeft + panelPadding + borderWidth;
		float contentTop = localTop + borderWidth;
		float maxContentBottom = localMaxBottom - borderWidth;

		List<Float> tops = new ArrayList<>();
		List<Float> bottoms = new ArrayList<>();
		List<MapWidget> drawnWidgets = new ArrayList<>();

		float y = contentTop;
		for (int i = firstVisibleWidget; i < widgetInfos.size(); i++) {
			MapWidgetInfo widgetInfo = widgetInfos.get(i);
			if (!shouldDrawWidget(widgetInfo)) {
				lastVisibleCount++;
				continue;
			}
			MapWidget widget = widgetInfo.widget;
			widget.updateInfo(drawSettings);
			if (widget.layoutAAIfNeeded(app, widgetWidth, isRtl)) {
				widget.updateAndroidAutoBitmap(drawSettings, isRtl);
			}
			float measuredHeight = widget.getMeasuredAAHeight();
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
			y += height;
			lastVisibleCount++;
		}


		widgetsBuffer.clearCanvas();
		Canvas offCanvas = widgetsBuffer.offscreenCanvas;
		offCanvas.save();
		//  Rows hidden by the reserved area split the panel into several blocks, each of them gets its own outline.
		int blockStart = 0;
		RectF blockRect = new RectF();
		for (int i = 1; i <= drawnWidgets.size(); i++) {
			boolean endOfBlock = i == drawnWidgets.size()
					|| bottoms.get(i - 1) + 1 < tops.get(i);
			if (endOfBlock) {
				drawBlock(offCanvas,
						drawnWidgets.subList(blockStart, i),
						tops.subList(blockStart, i), bottoms.subList(blockStart, i),
						contentLeft, contentRight,
						panelPadding, corner,
						borderWidth, dividerWidth, scale, blockRect);
				localDirtyRect.union(blockRect);
				blockStart = i;
			}
		}
		offCanvas.restore();

		RectF frameDirtyRect = new RectF(localDirtyRect);
		frameDirtyRect.offset(left, top);
		widgetsBuffer.previousBounds.set(widgetsBuffer.currentBounds);
		widgetsBuffer.currentBounds.set(frameDirtyRect);
		widgetsBuffer.lastMaxPanelBounds.set(maxPanelRect);
		widgetsBuffer.markFrameReady(localDirtyRect);
	}

	public void drawWidgets2(@NonNull Canvas canvas) {
		if (widgetsBuffer == null) {
			return;
		}

		canvas.save();
		RectF localDirtyRect = new RectF();
		boolean hasNewFrame = widgetsBuffer.consumeFrame(localDirtyRect);
		if (hasNewFrame) {
			RectF absoluteDirtyRect = new RectF(localDirtyRect);
			absoluteDirtyRect.offset(widgetsBuffer.currentBounds.left, widgetsBuffer.currentBounds.top);

			canvas.clipRect(absoluteDirtyRect);
			Rect localIntRect = new Rect();
			localDirtyRect.round(localIntRect);

			canvas.drawBitmap(
					widgetsBuffer.offscreenBitmap,
					localIntRect,
					absoluteDirtyRect,
					blitPaint
			);
		} else {
			canvas.drawBitmap(
					widgetsBuffer.offscreenBitmap,
					widgetsBuffer.currentBounds.left,
					widgetsBuffer.currentBounds.top,
					blitPaint
			);
		}
		canvas.restore();
	}

	private void drawBlock(@NonNull Canvas canvas,
	                       @NonNull List<MapWidget> widgets,
	                       @NonNull List<Float> tops, @NonNull List<Float> bottoms,
	                       float contentLeft, float contentRight, float padding,
	                       float corner, float borderWidth, float dividerWidth,
	                       float scale, RectF outBlockRect) {
		Path backgroundPath = new Path();
		float blockTop = tops.get(0);
		float blockBottom = bottoms.get(bottoms.size() - 1);

		float blockLeft = contentLeft - padding;
		float blockRight = contentRight + padding;

		outBlockRect.set(blockLeft, blockTop, blockRight, blockBottom);

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

		float widgetContentTop, widgetContentBottom;
		for (int i = 0; i < widgets.size(); i++) {
			widgetContentTop = tops.get(i);
			drawWidget(canvas, widgets.get(i),
					contentLeft,
					widgetContentTop,
					scale);
			if (i < widgets.size() - 1) {
				widgetContentBottom = bottoms.get(i);
				drawSeparator(canvas, widgetContentBottom, dividerWidth, blockRight, blockLeft);
			}
		}

		canvas.restore();
	}

	private void drawWidget(@NonNull Canvas canvas,
	                        @NonNull MapWidget widget,
							float contentLeft, float widgetContentTop,
							float scale) {
        canvas.save();
		canvas.translate(contentLeft, widgetContentTop);
		canvas.scale(scale, scale);
		widget.drawAndroidAutoBitmap(canvas);
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
		firstVisibleWidget = next < widgetInfos.size() ? next : 0;
		return true;
	}

	private List<MapWidgetInfo> getWidgetInfos(@NonNull ApplicationMode appMode) {
		int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE;
		MapWidgetRegistry widgetRegistry = app.getMapWidgetRegistry();
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getAndroidAutoWidgetsForPanel(app, appMode, enabledWidgetsFilter, List.of(panel));
		return new ArrayList<>(widgetInfos);
	}

	public void onWidgetVisibilityChanged(MapWidgetInfo widgetInfo) {
		boolean isEnabled = widgetInfo.isEnabledForAndroidAutoMode(app.getSettings().getApplicationMode());
		if (isEnabled) {
			visibleWidgetIds.add(widgetInfo.key);
		} else {
			visibleWidgetIds.remove(widgetInfo.key);
		}
	}

	public void onWidgetRegistered(DrawSettings drawSettings, MapWidgetInfo widgetInfo) {
		List<String> widgetsOrder = panel.getAndroidAutoWidgetsOrder(app.getSettings().getApplicationMode(), app.getSettings());
		int index = widgetsOrder.indexOf(widgetInfo.key);
		if (index == -1) {
			if (widgetInfos.isEmpty()) {
				index = 0;
			} else {
				index = widgetInfos.size() - 1;
			}
		}
		widgetInfos.add(index, widgetInfo);

		boolean nightMode = drawSettings.isNightMode();
		float density = app.getResources().getDisplayMetrics().density;
		ResolvedPanelAppearance appearance = app.getPanelAppearanceSettingsManager()
				.resolveCommitted(panel, null, nightMode, false, density, true);
		applyPanelAppearance(appearance);
		MapWidget widget = widgetInfo.widget;
		widget.applyPanelAppearance(appearance);
	}

	private boolean shouldDrawWidget(MapWidgetInfo widgetInfo) {
		return widgetInfo.widget.shouldDrawForAndroidAuto() && visibleWidgetIds.contains(widgetInfo.key);
	}

	private void applyPanelAppearance(@NonNull ResolvedPanelAppearance appearance) {
		int baseBorderColor = ColorUtilities.removeAlpha(appearance.getPanelBorderColor());
		int borderColor = ColorUtilities.getColorWithAlpha(baseBorderColor, 0.7f);
		borderPaint.setColor(borderColor);
		backgroundPaint.setColor(appearance.getBackground().getColor());
		dividerPaint.setColor(appearance.getDividerColor());
	}


	private RectF calculateAvailablePanelRect(@NonNull Rect visibleArea,
	                                          float carDensity, float topOffset) {
		float panelWidth = Math.min(PANEL_WIDTH_CAR_DP * carDensity,
				visibleArea.width() * MAX_PANEL_WIDTH_RATIO);

		// The panel is flush with the right edge and keeps a fixed top, so that it does not jump
		// when the speedometer or the alarm widget appear or change size.
		float top = visibleArea.top + topOffset;
		float right = visibleArea.right;
		float left = right - panelWidth;
		float maxHeight = Math.min(visibleArea.bottom - top, visibleArea.height() * MAX_PANEL_HEIGHT_RATIO);
		float bottom = top + maxHeight;

		return new RectF(left, top, right, bottom);
	}

	public void clearWidgets() {
		widgetInfos.clear();
		visibleWidgetIds.clear();
		lastPanelBounds.setEmpty();
		lastVisibleCount = 0;
		firstVisibleWidget = 0;
	}

	private static class WidgetsBuffer {
		final Bitmap offscreenBitmap;
		final Canvas offscreenCanvas;
		final RectF currentBounds = new RectF();
		final RectF previousBounds = new RectF();
		private final Object renderLock = new Object();
		private final RectF accumulatedLocalDirtyRect = new RectF();
		private boolean isFrameReady = false;
		final RectF lastMaxPanelBounds = new RectF();

		public WidgetsBuffer(int width, int height) {
			this.offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.offscreenCanvas = new Canvas(offscreenBitmap);
		}

		void clearCanvas() {
			offscreenCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.SRC);
		}

		void markFrameReady(@NonNull RectF newLocalDirtyRect) {
			synchronized (renderLock) {
				accumulatedLocalDirtyRect.union(newLocalDirtyRect);
				isFrameReady = true;
			}
		}

		public boolean consumeFrame(@NonNull RectF outLocalDirtyRect) {
			synchronized (renderLock) {
				if (!isFrameReady) {
					return false;
				}
				outLocalDirtyRect.set(accumulatedLocalDirtyRect);
				accumulatedLocalDirtyRect.setEmpty();
				isFrameReady = false;
				return true;
			}
		}

		public boolean maxBoundsDiffer(RectF targetBounds) {
			return !lastMaxPanelBounds.equals(targetBounds);
		}
	}
}
