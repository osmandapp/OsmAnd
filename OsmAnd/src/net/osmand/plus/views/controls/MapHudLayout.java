package net.osmand.plus.views.controls;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class MapHudLayout extends FrameLayout {

	private static final double[][] DIRECTIONS = getAvailableDirections();

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

		this.gridSize = AndroidUtils.dpToPx(context, 8);
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

		if (OsmandMapLayer.intersects(intersections, currentBounds, false)) {
			params = updateButtonPosition(button, intersections);
		}
		if (!isSnappedToGrid(params.rightMargin) || !isSnappedToGrid(params.bottomMargin)) {
			params.rightMargin = snapToGrid(params.rightMargin);
			params.bottomMargin = snapToGrid(params.bottomMargin);
		}
		button.setLayoutParams(params);

		if (save) {
			button.saveMargins();
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
			return new QuadRect(rect.left - radius, rect.top - radius,
					rect.right + radius, rect.bottom + radius);
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

	private void initBoundIntersections(@NonNull ViewGroup parent, @NonNull MapButton button,
	                                    @NonNull QuadTree<QuadRect> intersections, boolean recursive) {
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