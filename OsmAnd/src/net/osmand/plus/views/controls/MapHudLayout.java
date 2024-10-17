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
	private final float gridSize;
	private final float shadowSize;

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

		this.gridSize = AndroidUtils.dpToPxF(context, 8);
		this.shadowSize = AndroidUtils.dpToPxF(context, 2);
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
		int width = button.getMeasuredWidth();
		int height = button.getMeasuredHeight();

		if (width > 0 && height > 0) {
			int sw = getWidth();
			int sh = getHeight();
			int x = roundCoordinate(params.rightMargin, sw, gridSize);
			int y = roundCoordinate(params.bottomMargin, sh, gridSize);
			if ((params.rightMargin != x) || (params.bottomMargin != y)) {
				LOG.info(String.format("Correct %d, %d -> %d, %d",
						params.rightMargin, params.bottomMargin, x, y));
				params.rightMargin = x;
				params.bottomMargin = y;
				button.setLayoutParams(params);
			}
		}
		if (save) {
			button.saveMargins();
		}
	}

	private static int margin = 1;
	public int roundCoordinate(int coord, int screenSize, float cellSize) {
		int fullCells = (int) Math.max(((coord + cellSize) / cellSize), margin);
		if (2 * fullCells * cellSize < screenSize) {
			return (int) (fullCells * cellSize - shadowSize);
		}
		float end = (screenSize - (coord - cellSize / 2));
		fullCells = (int) (end / cellSize);
		float ret = screenSize - fullCells * cellSize;
		return (int) (ret);
	}

	@Override
	protected void onDraw(@NotNull Canvas canvas) {
		super.onDraw(canvas);
	}


//	@NonNull
//	private LayoutParams updateButtonPosition(@NonNull MapButton button, @NonNull QuadTree<QuadRect> intersections) {
//		int width = getMeasuredWidth();
//		int height = getMeasuredHeight();
//		int buttonWidth = button.getMeasuredWidth();
//		int buttonHeight = button.getMeasuredHeight();
//
//		int maxRightMargin = width - buttonWidth;
//		int maxBottomMargin = height - buttonHeight;
//
//		int maxStepsX = width / gridSize;
//		int maxStepsY = height / gridSize;
//		int maxSteps = Math.max(maxStepsX, maxStepsY);
//
//		LayoutParams params = (LayoutParams) button.getLayoutParams();
//		for (int step = 1; step <= maxSteps; step++) {
//			for (double[] direction : DIRECTIONS) {
//				int newRightMargin = (int) (params.rightMargin + direction[0] * step * gridSize);
//				int newBottomMargin = (int) (params.bottomMargin + direction[1] * step * gridSize);
//
//				newRightMargin = Math.max(0, Math.min(newRightMargin, maxRightMargin));
//				newBottomMargin = Math.max(0, Math.min(newBottomMargin, maxBottomMargin));
//
//				int newLeft = width - newRightMargin - buttonWidth;
//				int newTop = height - newBottomMargin - buttonHeight;
//
//				QuadRect newRect = new QuadRect(newLeft, newTop, newLeft + buttonWidth, newTop + buttonHeight);
//				if (!OsmandMapLayer.intersects(intersections, newRect, false)) {
//					params.rightMargin = newRightMargin;
//					params.bottomMargin = newBottomMargin;
//					return params;
//				}
//			}
//		}
//		return params;
//	}

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