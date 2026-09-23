package net.osmand.plus.auto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;
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
import java.util.function.Consumer;

/**
 * Prototype of a generic widgets panel drawn over the Android Auto map surface.
 * <p>
 *  Widgets are drawn on the right side of the surface to keep the left part
 *  free for the navigation card of the head unit. When more widgets are enabled than fit on the screen,
 *  a click on the panel shows the next page.
 * <p>
 * Widget views are created and laid out by this class only and are never attached to a window,
 * so the phone UI is not affected.
 */
public class CarWidgetsPanel {

	/**
	 * Panel is drawn only when the visible area is at least that wide.
	 */
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
	/**
	 * Safety net for narrow head units (800x480), the panel never takes more than this.
	 */
	private static final float MAX_PANEL_WIDTH_RATIO = 0.22f;
	/**
	 * Fraction of the visible area height the panel is allowed to occupy.
	 */
	private static final float MAX_PANEL_HEIGHT_RATIO = 0.7f;

	private final CarContext carContext;
	private final OsmandApplication app;
	private final WidgetsPanel panel;

	private final Object widgetsLock = new Object();

	private int firstVisibleWidget;
	private int lastVisibleCount;
	private boolean needsRebake = true;

	private final Paint borderPaint = new Paint();
	private final Paint backgroundPaint = new Paint();
	private final Paint dividerPaint = new Paint();

	private volatile WidgetsBuffer widgetsBuffer = null;
	private volatile List<MapWidgetInfo> widgetInfos = new ArrayList<>();
	private volatile Set<String> visibleWidgetIds = new HashSet<>();

	private volatile DrawSettings lastDrawSettings;

	public CarWidgetsPanel(@NonNull OsmandApplication app, @NonNull CarContext carContext) {
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

		backgroundPaint.setDither(true);
		backgroundPaint.setAntiAlias(true);
		backgroundPaint.setStyle(Paint.Style.FILL);
	}

	public void applyDrawSettingsAndUpdateWidgets(@NonNull DrawSettings drawSettings) {
		synchronized (widgetsLock) {
			boolean shouldUpdateAppearance = drawSettingsDiffer(drawSettings);
			List<MapWidgetInfo> infos = this.widgetInfos;
			if (shouldUpdateAppearance) {
				boolean nightMode = drawSettings.isNightMode();
				float density = drawSettings.getDensity();
				ResolvedPanelAppearance appearance = app.getPanelAppearanceSettingsManager()
						.resolveCommitted(panel, null, nightMode, false, density, true);
				applyPanelAppearance(appearance);
				for (MapWidgetInfo info : infos) {
					info.widget.applyPanelAppearanceForAndroidAuto(appearance);
				}
				this.lastDrawSettings = drawSettings;
				needsRebake = true;
			}
			for (MapWidgetInfo info : infos) {
				info.widget.updateInfo(drawSettings);
			}
		}
	}

	public void reloadWidgets() {
		synchronized (widgetsLock) {
			clearWidgets();
			List<MapWidgetInfo> newWidgetInfos = getWidgetInfos();
			Set<String> newVisibleIds =  new HashSet<>();
			if (!Algorithms.isEmpty(newWidgetInfos)) {
				ApplicationMode applicationMode = app.getSettings().getApplicationMode();
				List<String> visibilities = MapWidgetInfo.getAndroidAutoWidgetsVisibility(app, applicationMode);
				for (MapWidgetInfo info : newWidgetInfos) {
					if (info.isEnabledForAppMode(applicationMode, visibilities)) {
						newVisibleIds.add(info.key);
					}
				}
			}
			widgetInfos = newWidgetInfos;
			visibleWidgetIds = newVisibleIds;
		}
	}

	private WidgetsBuffer syncWidgetsBufferSize(int width, int height, boolean hasWidgets) {
		WidgetsBuffer buffer = widgetsBuffer;
		boolean shouldResetBuffer = !hasWidgets
				|| buffer == null
				|| buffer.offscreenBitmap.getWidth() != width
				|| buffer.offscreenBitmap.getHeight() != height;
		if (shouldResetBuffer) {
			if (buffer != null) {
				buffer.recycle();
			}
			if (width > 0 && height > 0 && hasWidgets) {
				widgetsBuffer = new WidgetsBuffer(width, height);
			} else {
				widgetsBuffer = null;
			}
		}
		return widgetsBuffer;
	}

	private boolean hasWidgetsWithDirtyLayout(@NonNull List<MapWidgetInfo> widgetInfos) {
		return widgetInfos.stream().anyMatch(w -> w.widget.isAndroidAutoLayoutNeeded());
	}

	private boolean isAndroidAutoWidgetPanelEnabled() {
		return app.getSettings().AA_SHOW_WIDGETS_PANEL.get();
	}

	public boolean bakeWidgets(@NonNull Rect visibleArea,
	                        @NonNull DrawSettings drawSettings, float carDensity, float topOffset,
	                        @Nullable Rect hiddenArea, boolean forceRedraw) {

		RectF maxPanelRect;
		List<MapWidgetInfo> infos;
		Set<String> ids;
		int lastVisibleWidgetCount, firstWidgetIndex;
		boolean baked;
		synchronized (widgetsLock) {
			if (firstVisibleWidget >= widgetInfos.size()) {
				firstVisibleWidget = 0;
			}
			infos = this.widgetInfos;
			ids = this.visibleWidgetIds;
			lastVisibleWidgetCount = lastVisibleCount;
			firstWidgetIndex = firstVisibleWidget;

			maxPanelRect = calculateAvailablePanelRect(visibleArea, carDensity, topOffset);
			boolean hasWidgets = Algorithms.isNotEmpty(infos);
			WidgetsBuffer buffer = syncWidgetsBufferSize((int) maxPanelRect.width(), (int) maxPanelRect.height(), hasWidgets);
			if (buffer == null) {
				return false;
			}
			Pair<Boolean, Integer> result = doBakeWidgets(
					buffer,
					infos, ids,
					firstWidgetIndex, lastVisibleWidgetCount,
					maxPanelRect,
					drawSettings, carDensity, hiddenArea,
					forceRedraw);
			baked = result.first;
			lastVisibleCount = result.second;
		}
		return baked;
	}

	private boolean drawSettingsDiffer(DrawSettings other) {
		if (lastDrawSettings == other) {
			return false;
		}
		if (lastDrawSettings == null) {
			return true;
		}
		return lastDrawSettings.isNightMode() != other.isNightMode()
				|| lastDrawSettings.getDensity() != other.getDensity();
	}

	private Pair<Boolean, Integer> doBakeWidgets(
			@NonNull WidgetsBuffer buffer,
			@NonNull List<MapWidgetInfo> widgetInfos,
			@NonNull Set<String> visibleIds,
			int firstVisibleWidgetIndex, int prevLastVisibleCount,
			@NonNull RectF maxPanelRect,
			@NonNull DrawSettings drawSettings, float carDensity,
			@Nullable Rect hiddenArea, boolean forceRedraw
	) {
		boolean willBake = needsRebake
				|| buffer.maxBoundsSizeDiffer(maxPanelRect)
				|| hasWidgetsWithDirtyLayout(widgetInfos)
				|| forceRedraw;
		needsRebake = false;

		if (!willBake) {
			return new Pair<>(false, prevLastVisibleCount);
		}

		int lastVisibleWidgetCount = 0;
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

		Rect localHiddenArea = null;
		if (hiddenArea != null) {
			localHiddenArea = new Rect(hiddenArea);
			localHiddenArea.offset((int) -maxPanelRect.left, (int) -maxPanelRect.top);
		}
		float y = contentTop;
		for (int i = firstVisibleWidgetIndex; i < widgetInfos.size(); i++) {
			MapWidgetInfo widgetInfo = widgetInfos.get(i);
			if (!shouldDrawWidget(widgetInfo, visibleIds)) {
				lastVisibleWidgetCount++;
				continue;
			}
			MapWidget widget = widgetInfo.widget;
			if (widget.layoutAAIfNeeded(app, widgetWidth, isRtl)) {
				widget.updateAndroidAutoBitmap(drawSettings, isRtl);
			}
			float measuredHeight = widget.getMeasuredAAHeight();
			if (measuredHeight <= 0) {
				lastVisibleWidgetCount++;
				continue;
			}
			float height = measuredHeight * scale;
			if (y + height > maxContentBottom) {
				break;
			}
			// A row is dropped only when the transient widget really covers it, a small overlap
			// on the edge is not worth losing a whole row for.
			boolean covered = localHiddenArea != null
					&& Math.min(localHiddenArea.bottom, y + height) > Math.max(localHiddenArea.top, y)
					&& Math.min(localHiddenArea.right, contentRight) - Math.max(localHiddenArea.left, contentLeft)
					> panelContentWidth / 2;
			if (!covered) {
				drawnWidgets.add(widget);
				tops.add(y);
				bottoms.add(y + height);
			}
			// The slot is kept even for a hidden widget, the panel must not shift.
			y += height;
			lastVisibleWidgetCount++;
		}

		RectF localDirtyRect = new RectF();
		buffer.clearBackCanvas();
		buffer.drawToBack((offCanvas -> {
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
		}));

		buffer.finalizeBitmap(localDirtyRect, maxPanelRect);
		return new Pair<>(true, lastVisibleWidgetCount);
	}

	public void drawWidgetsBuffer(@NonNull Canvas canvas) {
		WidgetsBuffer buffer = this.widgetsBuffer;
		if (buffer == null) {
			return;
		}
		if (!isAndroidAutoWidgetPanelEnabled()) {
			return;
		}
		buffer.draw(canvas);
	}

	private void drawBlock(@NonNull Canvas canvas,
	                       @NonNull List<MapWidget> widgets,
	                       @NonNull List<Float> tops, @NonNull List<Float> bottoms,
	                       float contentLeft, float contentRight, float padding,
	                       float corner, float borderWidth, float dividerWidth,
	                       float scale, RectF outBlockRect) {
		Path backgroundPath = new Path();
		float blockTop = tops.get(0) - borderWidth;
		float blockBottom = bottoms.get(bottoms.size() - 1) + borderWidth;

		float blockLeft = contentLeft - padding - borderWidth;
		float blockRight = contentRight + padding + borderWidth;

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
		borderPaint.setStrokeWidth(borderWidth * 2);
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
		if (!isAndroidAutoWidgetPanelEnabled()) {
			return false;
		}
		RectF bounds = new RectF();
		WidgetsBuffer buffer = widgetsBuffer;
		if (buffer != null) {
			buffer.getGlobalBounds(bounds);
		}
		if (bounds.isEmpty() || !bounds.contains(x, y)) {
			return false;
		}
		synchronized (widgetsLock) {
			int next = firstVisibleWidget + Math.max(lastVisibleCount, 1);
			firstVisibleWidget = next < widgetInfos.size() ? next : 0;
			needsRebake = true;
		}
		return true;
	}

	private List<MapWidgetInfo> getWidgetInfos() {
		MapWidgetRegistry widgetRegistry = app.getMapWidgetRegistry();
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getAndroidAutoWidgetsToShowInAA(app, panel);
		return new ArrayList<>(widgetInfos);
	}

	public void onWidgetVisibilityChanged(MapWidgetInfo widgetInfo) {
		synchronized (widgetsLock) {
			Set<String> updatedIds = new HashSet<>(visibleWidgetIds);
			boolean isEnabled = widgetInfo.isEnabledForAndroidAutoMode(app.getSettings().getApplicationMode());
			if (isEnabled) {
				updatedIds.add(widgetInfo.key);
			} else {
				updatedIds.remove(widgetInfo.key);
			}
			visibleWidgetIds = updatedIds;
		}
	}

	private boolean shouldDrawWidget(MapWidgetInfo widgetInfo, Set<String> visibleIds) {
		return widgetInfo.widget.shouldDrawForAndroidAuto() && visibleIds.contains(widgetInfo.key);
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
		// The panel is flush with the right edge and keeps a fixed top, so that it does not jump
		// when the speedometer or the alarm widget appear or change size.
		float top = visibleArea.top + topOffset;
		float right = visibleArea.right;
		if (!isAndroidAutoWidgetPanelEnabled()
				|| visibleArea.width() < MIN_SURFACE_WIDTH_DP * carDensity) {
			return new RectF(right, top, right, top);
		}
		float panelWidth = Math.min(PANEL_WIDTH_CAR_DP * carDensity,
				visibleArea.width() * MAX_PANEL_WIDTH_RATIO);

		float left = right - panelWidth;
		float maxHeight = Math.min(visibleArea.bottom - top, visibleArea.height() * MAX_PANEL_HEIGHT_RATIO);
		if (maxHeight < 0) {
			maxHeight = 0;
		}
		float bottom = top + maxHeight;

		return new RectF(left, top, right, bottom);
	}

	public void clearWidgets() {
		synchronized (widgetsLock) {
			widgetInfos = new ArrayList<>();
			visibleWidgetIds = new HashSet<>();
			lastVisibleCount = 0;
			firstVisibleWidget = 0;
			lastDrawSettings = null;
			if (widgetsBuffer != null) {
				widgetsBuffer.recycle();
				widgetsBuffer = null;
			}
		}
	}

	private static class WidgetsBuffer {
		Bitmap offscreenBitmap;
		Canvas offscreenCanvas;
		Canvas frontCanvas;
		Bitmap frontBitmap;
		final RectF currentBounds = new RectF();
		final RectF localDirtyRect = new RectF();
		private final Object renderLock = new Object();
		final RectF lastMaxPanelBounds = new RectF();

		public WidgetsBuffer(int width, int height) {
			this.offscreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.offscreenCanvas = new Canvas(offscreenBitmap);
			this.frontBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.frontCanvas = new Canvas(frontBitmap);
		}

		void clearBackCanvas() {
			offscreenCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.SRC);
		}

		void clearFrontCanvas() {
			frontCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.SRC);
		}

		void draw(@NonNull Canvas canvas) {
			synchronized (renderLock) {
				RectF panelBounds = new RectF(currentBounds);
				if (!panelBounds.isEmpty() && frontBitmap != null) {
					canvas.save();
					canvas.drawBitmap(frontBitmap, panelBounds.left, panelBounds.top, null);
					canvas.restore();
				}
			}
		}

		void drawToBack(@NonNull Consumer<Canvas> drawFunction) {
			offscreenCanvas.save();
			drawFunction.accept(offscreenCanvas);
			offscreenCanvas.restore();
		}

		void getGlobalBounds(@NonNull RectF outRect) {
			synchronized (renderLock) {
				outRect.set(currentBounds);
			}
		}

		void finalizeBitmap(@NonNull RectF newLocalDirtyRect, @NonNull RectF maxPanelRect) {
			synchronized (renderLock) {
				localDirtyRect.set(newLocalDirtyRect);
				RectF globalBounds = new RectF(localDirtyRect);
				globalBounds.offset(maxPanelRect.left, maxPanelRect.top);
				currentBounds.set(globalBounds);
				lastMaxPanelBounds.set(maxPanelRect);
				clearFrontCanvas();
				frontCanvas.drawBitmap(offscreenBitmap, 0, 0, null);
			}
		}

		public void recycle() {
			synchronized (renderLock) {
				if (offscreenBitmap != null) {
					offscreenBitmap.recycle();
					offscreenBitmap = null;
				}
				offscreenCanvas = null;
				if (frontBitmap != null) {
					frontBitmap.recycle();
					frontBitmap = null;
				}
				frontCanvas = null;
			}
		}

		public boolean maxBoundsSizeDiffer(RectF targetBounds) {
			return lastMaxPanelBounds.width() != targetBounds.width() || lastMaxPanelBounds.height() != targetBounds.height();
		}
	}
}
