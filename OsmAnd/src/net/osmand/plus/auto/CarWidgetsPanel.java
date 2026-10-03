package net.osmand.plus.auto;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.View.MeasureSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 *  Widgets are drawn on the right side of the surface to keep the left part
 *  free for the navigation card of the head unit. When more widgets are enabled than fit on the screen,
 *  a click on the panel shows the next page.
 * <p>
 * Widget views are created and laid out by this class only and are never attached to a window,
 * so the phone UI is not affected.
 */
public class CarWidgetsPanel {
	private static final float MIN_SURFACE_WIDTH_DP = 400f;
	private static final float CORNER_RADIUS_DP = 8f;
	private static final float BORDER_WIDTH_DP = 2f;
	private static final float DIVIDER_WIDTH_DP = 1f;
	private static final float PANEL_PADDING_DP = 0f;
	private static final float WIDGET_WIDTH_DP = 130f;
	private static final float PANEL_WIDTH_CAR_DP = 200f;
	private static final float MAX_PANEL_WIDTH_RATIO = 0.22f;
	private static final float MAX_PANEL_HEIGHT_RATIO = 0.7f;

	private final OsmandApplication app;
	private final WidgetsPanel panel;

	private int firstVisibleWidget;
	private int lastVisibleCount;

	private final Paint borderPaint = new Paint();
	private final Paint backgroundPaint = new Paint();
	private final Paint dividerPaint = new Paint();

	private List<MapWidgetInfo> widgetInfos = new ArrayList<>();
	private Set<String> visibleWidgetIds = new HashSet<>();
	private final RectF lastPanelBounds = new RectF();

	private volatile DrawSettings lastDrawSettings;

	public CarWidgetsPanel(@NonNull OsmandApplication app) {
		this(app, WidgetsPanel.ANDROID_AUTO);
	}

	public CarWidgetsPanel(@NonNull OsmandApplication app, @NonNull WidgetsPanel panel) {
		this.app = app;
		this.panel = panel;

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

	public void reloadWidgets() {
		clearWidgets();
		List<MapWidgetInfo> newWidgetInfos = getWidgetInfos();
		Set<String> newVisibleIds = new HashSet<>();
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

	private boolean isAndroidAutoWidgetPanelEnabled() {
		return app.getSettings().AA_SHOW_WIDGETS_PANEL.get();
	}

	public void drawWidgetViews(
			@NonNull Canvas canvas,
			@NonNull Rect visibleArea,
			@Nullable Rect hiddenArea,
			@NonNull DrawSettings drawSettings, float carDensity, float topOffset) {
		RectF maxPanelRect;
		List<MapWidgetInfo> infos;
		Set<String> ids;
		int firstWidgetIndex;
		lastPanelBounds.setEmpty();
		if (firstVisibleWidget >= widgetInfos.size()) {
			firstVisibleWidget = 0;
		}
		infos = this.widgetInfos;
		boolean hasWidgets = Algorithms.isNotEmpty(infos);
		if (!hasWidgets) {
			return;
		}
		ids = this.visibleWidgetIds;
		firstWidgetIndex = firstVisibleWidget;

		maxPanelRect = calculateAvailablePanelRect(visibleArea, carDensity, topOffset);

		lastVisibleCount = doDrawWidgets(canvas, infos, ids,
				firstWidgetIndex,
				maxPanelRect,
				drawSettings, carDensity, hiddenArea);
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

	private Integer doDrawWidgets(
			@NonNull Canvas canvas,
			@NonNull List<MapWidgetInfo> widgetInfos,
			@NonNull Set<String> visibleIds,
			int firstVisibleWidgetIndex,
			@NonNull RectF maxPanelRect,
			@NonNull DrawSettings drawSettings, float carDensity,
			@Nullable Rect hiddenArea
	) {
		ResolvedPanelAppearance panelAppearance = null;
		boolean shouldUpdateAppearance = drawSettingsDiffer(drawSettings);
		if (shouldUpdateAppearance) {
			boolean nightMode = drawSettings.isNightMode();
			float density = drawSettings.getDensity();
			panelAppearance = app.getPanelAppearanceSettingsManager()
					.resolveCommitted(panel, null, nightMode, false, density, true);
			applyPanelAppearance(panelAppearance);
			this.lastDrawSettings = drawSettings;
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

		float panelTop = maxPanelRect.top;
		float panelLeft = maxPanelRect.left;
		float panelRight = maxPanelRect.right;
		float maxPanelBottom = maxPanelRect.bottom;

		float panelContentRight = panelRight - panelPadding - borderWidth;
		float panelContentLeft = panelLeft + panelPadding + borderWidth;
		float panelContentTop = panelTop + borderWidth;
		float maxPanelContentBottom = maxPanelBottom - borderWidth;

		List<Float> tops = new ArrayList<>();
		List<Float> bottoms = new ArrayList<>();
		List<MapWidget> drawnWidgets = new ArrayList<>();

		float y = panelContentTop;
		for (int i = firstVisibleWidgetIndex; i < widgetInfos.size(); i++) {
			MapWidgetInfo widgetInfo = widgetInfos.get(i);
			if (!shouldDrawWidget(widgetInfo, visibleIds)) {
				lastVisibleWidgetCount++;
				continue;
			}
			MapWidget widget = widgetInfo.widget;
			if (shouldUpdateAppearance) {
				widget.applyPanelAppearance(panelAppearance);
			}
			widget.updateInfo(drawSettings);
			View view = widget.getView();
			if (view.getVisibility() != View.VISIBLE) {
				lastVisibleCount++;
				continue;
			}
			view.measure(MeasureSpec.makeMeasureSpec(widgetWidth, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int measuredWidth = view.getMeasuredWidth();
			int measuredHeight = view.getMeasuredHeight();
			if (measuredWidth <= 0 || measuredHeight <= 0) {
				lastVisibleCount++;
				continue;
			}
			view.layout(0, 0, measuredWidth, measuredHeight);
			float height = measuredHeight * scale;
			if (y + height > maxPanelContentBottom) {
				break;
			}

			boolean covered = hiddenArea != null
					&& Math.min(hiddenArea.bottom, y + height) > Math.max(hiddenArea.top, y)
					&& Math.min(hiddenArea.right, panelContentRight) - Math.max(hiddenArea.left, panelContentLeft)
					> panelContentWidth / 2;
			if (!covered) {
				drawnWidgets.add(widget);
				tops.add(y);
				bottoms.add(y + height);
			}
			y += height;
			lastVisibleWidgetCount++;
		}

		RectF localDirtyRect = new RectF();
		//  Rows hidden by the reserved area split the panel into several blocks, each of them gets its own outline.
		int blockStart = 0;
		RectF blockRect = new RectF();
		for (int i = 1; i <= drawnWidgets.size(); i++) {
			boolean endOfBlock = i == drawnWidgets.size()
					|| bottoms.get(i - 1) + 1 < tops.get(i);
			if (endOfBlock) {
				drawBlock(canvas,
						drawnWidgets.subList(blockStart, i),
						tops.subList(blockStart, i), bottoms.subList(blockStart, i),
						panelContentLeft, panelContentRight,
						panelPadding, corner,
						borderWidth, dividerWidth, scale, blockRect);
				localDirtyRect.union(blockRect);
				blockStart = i;
			}
		}
		lastPanelBounds.set(localDirtyRect);
		return lastVisibleWidgetCount;
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

		drawBorder(canvas, backgroundPath, borderWidth);
		canvas.restore();
	}

	private void drawWidget(@NonNull Canvas canvas,
	                        @NonNull MapWidget widget,
	                        float contentLeft, float widgetContentTop,
	                        float scale) {
		canvas.save();
		canvas.translate(contentLeft, widgetContentTop);
		canvas.scale(scale, scale);
		widget.getView().draw(canvas);
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
				blockLeft, widgetBottom - dividerWidth,
				blockRight, widgetBottom - dividerWidth,
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
		if (lastPanelBounds.isEmpty() || !lastPanelBounds.contains(x, y)) {
			return false;
		}
		int next = firstVisibleWidget + Math.max(lastVisibleCount, 1);
		firstVisibleWidget = next < widgetInfos.size() ? next : 0;
		return true;
	}

	private List<MapWidgetInfo> getWidgetInfos() {
		MapWidgetRegistry widgetRegistry = app.getMapWidgetRegistry();
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getAndroidAutoWidgetsToShowInAA(app, panel);
		return new ArrayList<>(widgetInfos);
	}

	public void onWidgetVisibilityChanged(MapWidgetInfo widgetInfo) {
		Set<String> updatedIds = new HashSet<>(visibleWidgetIds);
		boolean isEnabled = widgetInfo.isEnabledForAndroidAutoMode(app.getSettings().getApplicationMode());
		if (isEnabled) {
			updatedIds.add(widgetInfo.key);
		} else {
			updatedIds.remove(widgetInfo.key);
		}
		visibleWidgetIds = updatedIds;
	}

	private boolean shouldDrawWidget(MapWidgetInfo widgetInfo, Set<String> visibleIds) {
		return visibleIds.contains(widgetInfo.key);
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
		widgetInfos = new ArrayList<>();
		visibleWidgetIds = new HashSet<>();
		lastVisibleCount = 0;
		firstVisibleWidget = 0;
		lastDrawSettings = null;
	}
}
