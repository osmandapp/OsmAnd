package net.osmand.plus.views.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class MapButtonsLayout extends FrameLayout {

	private static final double[][] DIRECTIONS = getAvailableDirections();

	private final int gridSize;

	public MapButtonsLayout(@NonNull Context context) {
		this(context, null);
	}

	public MapButtonsLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapButtonsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MapButtonsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.gridSize = AndroidUtils.dpToPx(context, 8);
	}

	public void updateButtons() {
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child instanceof MapButton button) {
				updateButton(button);
			}
		}
	}

	public void updateButton(@NonNull MapButton button) {
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
		button.saveMargins();
	}

	@NonNull
	private QuadTree<QuadRect> initBoundIntersections(@NonNull MapButton button) {
		QuadTree<QuadRect> intersections = OsmandMapLayer.initBoundIntersections(getWidth(), getHeight());
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			if (child != button && child instanceof MapButton) {
				QuadRect rect = getRect((MapButton) child);
				intersections.insert(rect, new QuadRect(rect));
			}
		}
		return intersections;
	}

	@NonNull
	private LayoutParams updateButtonPosition(@NonNull MapButton button, @NonNull QuadTree<QuadRect> intersections) {
		LayoutParams originalParams = (LayoutParams) button.getLayoutParams();
		LayoutParams params = new LayoutParams(originalParams);

		int size = button.getSize();
		int maxStepsX = getWidth() / gridSize;
		int maxStepsY = getHeight() / gridSize;
		int maxSteps = Math.max(maxStepsX, maxStepsY);
		int minRightMargin = getWidth() - size;
		int minBottomMargin = getHeight() - size;

		for (int step = 1; step <= maxSteps; step++) {
			for (double[] direction : DIRECTIONS) {
				int newRightMargin = (int) (originalParams.rightMargin + direction[0] * step * gridSize);
				int newBottomMargin = (int) (originalParams.bottomMargin + direction[1] * step * gridSize);

				params.rightMargin = Math.max(0, Math.min(newRightMargin, minRightMargin));
				params.bottomMargin = Math.max(0, Math.min(newBottomMargin, minBottomMargin));

				QuadRect rect = getRect(button, params);
				if (!OsmandMapLayer.intersects(intersections, rect, false)) {
					return params;
				}
			}
		}
		return originalParams;
	}

	@NonNull
	private static QuadRect getRect(@NonNull MapButton button) {
		return getRect(button, (LayoutParams) button.getLayoutParams());
	}

	@NonNull
	private static QuadRect getRect(@NonNull MapButton button, @NonNull LayoutParams params) {
		int size = button.getSize();
		int radius = button.getShadowRadius();
		return new QuadRect(params.rightMargin + size - radius, params.bottomMargin + size - radius,
				params.rightMargin + radius, params.bottomMargin + radius);
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