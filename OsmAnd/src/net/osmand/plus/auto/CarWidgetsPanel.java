package net.osmand.plus.auto;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.View.MeasureSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetsInitializer;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	/** Widgets are stacked without gaps, their own dividers separate the rows. */
	private static final float WIDGET_SPACING_DP = 0f;
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

	public static final String WIDGETS_SEPARATOR = ";";

	private final OsmandApplication app;
	private final WidgetsPanel panel;

	private final List<MapWidget> widgets = new ArrayList<>();
	private final RectF lastPanelBounds = new RectF();

	private MapActivity cachedMapActivity;
	private ApplicationMode cachedAppMode;
	private Boolean cachedNightMode;
	private String cachedWidgetIds;
	private int firstVisibleWidget;
	private int lastVisibleCount;

	public CarWidgetsPanel(@NonNull OsmandApplication app) {
		this(app, WidgetsPanel.RIGHT);
	}

	public CarWidgetsPanel(@NonNull OsmandApplication app, @NonNull WidgetsPanel panel) {
		this.app = app;
		this.panel = panel;
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
		List<MapWidget> widgets = getWidgets(drawSettings.isNightMode());
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
		float scale = panelWidth / widgetWidth;

		// The panel is flush with the right edge and keeps a fixed top, so that it does not jump
		// when the speedometer or the alarm widget appear or change size.
		float right = visibleArea.right;
		float left = right - panelWidth;
		float top = visibleArea.top + topOffset;
		float maxBottom = top + visibleArea.height() * MAX_PANEL_HEIGHT_RATIO;

		List<View> views = new ArrayList<>();
		List<Float> tops = new ArrayList<>();
		List<Float> bottoms = new ArrayList<>();
		float y = top;
		for (int i = firstVisibleWidget; i < widgets.size(); i++) {
			MapWidget widget = widgets.get(i);
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
			float height = measuredHeight * scale;
			if (y + height > maxBottom) {
				break;
			}
			view.layout(0, 0, measuredWidth, measuredHeight);
			// A row is dropped only when the transient widget really covers it, a small overlap
			// on the edge is not worth losing a whole row for.
			boolean covered = hiddenArea != null
					&& Math.min(hiddenArea.bottom, y + height) > Math.max(hiddenArea.top, y)
					&& Math.min(hiddenArea.right, right) - Math.max(hiddenArea.left, left)
					> panelWidth / 2;
			if (!covered) {
				views.add(view);
				tops.add(y);
				bottoms.add(y + height);
			}
			// The slot is kept even for a hidden widget, the panel must not shift.
			y += height + WIDGET_SPACING_DP * carDensity;
			lastVisibleCount++;
		}
		if (views.isEmpty()) {
			return 0;
		}
		float corner = CORNER_RADIUS_DP * carDensity;
		// Rounded on the left, flush square on the right. Rows hidden by the reserved area split
		// the panel into several blocks, each of them gets its own rounded outline.
		int blockStart = 0;
		for (int i = 1; i <= views.size(); i++) {
			boolean endOfBlock = i == views.size()
					|| bottoms.get(i - 1) + 1 < tops.get(i);
			if (endOfBlock) {
				drawBlock(canvas, views.subList(blockStart, i), tops.subList(blockStart, i),
						left, right, bottoms.get(i - 1), corner, scale);
				lastPanelBounds.union(left, tops.get(blockStart), right, bottoms.get(i - 1));
				blockStart = i;
			}
		}
		return lastPanelBounds.height();
	}

	private void drawBlock(@NonNull Canvas canvas, @NonNull List<View> views,
			@NonNull List<Float> tops, float left, float right, float bottom, float corner,
			float scale) {
		Path path = new Path();
		float top = tops.get(0);
		path.addRoundRect(new RectF(left, top, right, bottom),
				new float[] {corner, corner, 0, 0, 0, 0, corner, corner}, Path.Direction.CW);

		canvas.save();
		canvas.clipPath(path);
		for (int i = 0; i < views.size(); i++) {
			canvas.save();
			canvas.translate(left, tops.get(i));
			canvas.scale(scale, scale);
			views.get(i).draw(canvas);
			canvas.restore();
		}
		canvas.restore();
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
		MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		if (mapActivity == null) {
			// Widgets can only be created together with a map activity. Once created they stay
			// valid and keep being updated even after the activity is gone.
			return widgets;
		}
		String widgetIds = app.getSettings().AA_WIDGETS.getModeValue(appMode);
		if (mapActivity != cachedMapActivity || appMode != cachedAppMode
				|| !Boolean.valueOf(nightMode).equals(cachedNightMode)
				|| !Algorithms.stringsEqual(widgetIds, cachedWidgetIds)) {
			cachedMapActivity = mapActivity;
			cachedAppMode = appMode;
			cachedNightMode = nightMode;
			cachedWidgetIds = widgetIds;
			recreateWidgets(mapActivity, appMode, nightMode, widgetIds);
		}
		return widgets;
	}

	private void recreateWidgets(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
			boolean nightMode, @Nullable String widgetIds) {
		widgets.clear();
		firstVisibleWidget = 0;

		List<String> selectedIds = getSelectedWidgetIds(widgetIds);
		if (selectedIds.isEmpty()) {
			return;
		}
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
		Map<String, MapWidgetInfo> available = new HashMap<>();
		for (MapWidgetInfo info : WidgetsInitializer.createAllControls(mapActivity, appMode, layoutMode)) {
			// Custom copies of a widget share the type id, the first one is enough for the car.
			available.putIfAbsent(info.getWidgetType().id, info);
		}
		float density = app.getResources().getDisplayMetrics().density;
		ResolvedPanelAppearance appearance = app.getPanelAppearanceSettingsManager()
				.resolveCommitted(panel, layoutMode, nightMode, false, density, true);
		for (String id : selectedIds) {
			MapWidgetInfo info = available.get(id);
			if (info != null) {
				MapWidget widget = info.widget;
				widget.applyPanelAppearance(appearance);
				widgets.add(widget);
			}
		}
	}

	/**
	 * @return ids of the widgets selected for the car screen, in the order they are shown.
	 */
	@NonNull
	public static List<String> getSelectedWidgetIds(@Nullable String widgetIds) {
		if (Algorithms.isEmpty(widgetIds)) {
			return Collections.emptyList();
		}
		List<String> ids = new ArrayList<>();
		for (String id : widgetIds.split(WIDGETS_SEPARATOR)) {
			if (!Algorithms.isEmpty(id) && !ids.contains(id)) {
				ids.add(id);
			}
		}
		return ids;
	}

	public void clearWidgets() {
		widgets.clear();
		lastPanelBounds.setEmpty();
		lastVisibleCount = 0;
		firstVisibleWidget = 0;
		cachedMapActivity = null;
		cachedAppMode = null;
		cachedNightMode = null;
	}
}
