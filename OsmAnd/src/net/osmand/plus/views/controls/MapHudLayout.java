package net.osmand.plus.views.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class MapHudLayout extends FrameLayout {

	private static final Log LOG = PlatformUtil.getLog(MapHudLayout.class);

	private static final double[][] DIRECTIONS = getAvailableDirections();

	private final Paint gridPaint;
	private final int gridSize;

	public MapHudLayout(@NonNull Context context) {
		this(context, null);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapHudLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.gridSize = AndroidUtils.dpToPx(context, 48);

		this.gridPaint = new Paint();
		gridPaint.setColor(Color.BLACK);
		gridPaint.setStrokeWidth(1f);
		setWillNotDraw(false);
	}

	public void updateButtons() {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof MapButton button && button.getVisibility() == VISIBLE) {
				updateButton(button, false);
			}
		}
	}

	public void updateButton(@NonNull MapButton button, boolean save) {
		QuadRect currentBounds = getRect(button);
		QuadTree<QuadRect> intersections = initBoundIntersections(button);
		LayoutParams params = (LayoutParams) button.getLayoutParams();

//		if (OsmandMapLayer.intersects(intersections, currentBounds, false)) {
//			params = updateButtonPosition(button, intersections);
//		}
//		if (!isSnappedToGrid(params.rightMargin) || !isSnappedToGrid(params.bottomMargin)) {
//			params.rightMargin = snapToGrid(params.rightMargin);
//			params.bottomMargin = snapToGrid(params.bottomMargin);
//		}

		int width = button.getMeasuredWidth();
		int height = button.getMeasuredHeight();

		if (width > 0 && height > 0) {
			int[] a = transformCoordinates(params.rightMargin, params.bottomMargin, width, height, gridSize);
			if ((params.rightMargin != a[0]) || (params.bottomMargin != a[1])) {
				params.rightMargin = a[0];
				params.bottomMargin = a[1];
				button.setLayoutParams(params);
			}
		}

		if (save) {
			button.saveMargins();
		}
	}

	public static int[] transformCoordinates(int x, int y, int w, int h, int cellSize) {
		int nx = roundCoordinate(x, w, cellSize);
		int ny = roundCoordinate(y, h, cellSize);
		System.out.printf("%d %d (%d, %d)-> %d %d\n", x, y, w, h, nx, ny);
		LOG.debug(String.format(Locale.US, "%d %d (%d, %d)-> %d %d\n", x, y, w, h, nx, ny));

		int[] a = new int[2];
		a[0] = nx;
		a[1] = ny;

		return a;
	}

	public static int roundCoordinate(int coord, int screenSize, int cellSize) {
		int start = coord - coord % cellSize;
		if (2 * start < screenSize) {
			return start;
		}
		int end = (screenSize - coord);
		end = end - end % cellSize;
		int ret = screenSize - end - cellSize;
		if (ret * 2 < screenSize) {
			ret += cellSize;
		}
		return ret;
	}

	@Override
	protected void onDraw(@NotNull Canvas canvas) {
		super.onDraw(canvas);
//		drawGridLines(canvas);
	}

	private void drawGridLines(Canvas canvas) {
		int width = getWidth();
		int height = getHeight();

		// Draw vertical grid lines
		for (int x = 0; x < width; x += gridSize) {
			int nx = roundCoordinate(x, width, gridSize);  // Round to nearest grid point
			canvas.drawLine(nx, 0, nx, height, gridPaint); // Draw vertical line
		}

		// Draw horizontal grid lines
		for (int y = 0; y < height; y += gridSize) {
			int ny = roundCoordinate(y, height, gridSize);  // Round to nearest grid point
			canvas.drawLine(0, ny, width, ny, gridPaint);   // Draw horizontal line
		}
	}

	@NonNull
	private LayoutParams updateButtonPosition(@NonNull MapButton button, @NonNull QuadTree<QuadRect> intersections) {
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		int buttonWidth = button.getMeasuredWidth();
		int buttonHeight = button.getMeasuredHeight();

		int maxRightMargin = width - buttonWidth;
		int maxBottomMargin = height - buttonHeight;

		int maxStepsX = width / gridSize;
		int maxStepsY = height / gridSize;
		int maxSteps = Math.max(maxStepsX, maxStepsY);

		LayoutParams params = (LayoutParams) button.getLayoutParams();
		for (int step = 1; step <= maxSteps; step++) {
			for (double[] direction : DIRECTIONS) {
				int newRightMargin = (int) (params.rightMargin + direction[0] * step * gridSize);
				int newBottomMargin = (int) (params.bottomMargin + direction[1] * step * gridSize);

				newRightMargin = Math.max(0, Math.min(newRightMargin, maxRightMargin));
				newBottomMargin = Math.max(0, Math.min(newBottomMargin, maxBottomMargin));

				int newLeft = width - newRightMargin - buttonWidth;
				int newTop = height - newBottomMargin - buttonHeight;

				QuadRect newRect = new QuadRect(newLeft, newTop, newLeft + buttonWidth, newTop + buttonHeight);
				if (!OsmandMapLayer.intersects(intersections, newRect, false)) {
					params.rightMargin = newRightMargin;
					params.bottomMargin = newBottomMargin;
					return params;
				}
			}
		}
		return params;
	}

	@NonNull
	private QuadRect getRect(@NonNull View view) {
		Rect rect = AndroidUtils.getViewBoundOnWindow(view);
		if (view instanceof MapButton button) {
			int radius = button.getShadowRadius();
			return new QuadRect(rect.left - radius, rect.top - radius, rect.right + radius, rect.bottom + radius);
		} else {
			return new QuadRect(rect.left, rect.top, rect.right, rect.bottom);
		}
	}

	@NonNull
	private QuadTree<QuadRect> initBoundIntersections(@NonNull MapButton button) {
		QuadTree<QuadRect> intersections = OsmandMapLayer.initBoundIntersections(getMeasuredWidth(), getMeasuredHeight());
		initBoundIntersections(this, button, intersections, false);
		initBoundIntersections(findViewById(R.id.MapHudButtonsOverlayTop), button, intersections, true);
		initBoundIntersections(findViewById(R.id.MapHudButtonsOverlayBottom), button, intersections, true);
		return intersections;
	}

	private void initBoundIntersections(@NonNull ViewGroup parent, @NonNull MapButton button, @NonNull QuadTree<QuadRect> intersections, boolean recursive) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			if (child != button && child.getVisibility() == VISIBLE) {
				if (child instanceof MapButton || child instanceof SideWidgetsPanel || child instanceof VerticalWidgetPanel) {
					QuadRect rect = getRect(child);
					intersections.insert(rect, new QuadRect(rect));
				} else if (child instanceof ViewGroup && recursive) {
					initBoundIntersections((ViewGroup) child, button, intersections, true);
				}
			}
		}
	}

	private boolean isSnappedToGrid(int margin) {
		return margin % gridSize == 0;
	}

	private int snapToGrid(int margin) {
		return Math.round((float) margin / gridSize) * gridSize;
	}

	private static double[][] getAvailableDirections() {
		double[][] directions = new double[16][2];
		for (int i = 0; i < 16; i++) {
			double angle = Math.toRadians(i * 22.5);
			directions[i][0] = Math.cos(angle);
			directions[i][1] = Math.sin(angle);
		}
		return directions;
	}
}