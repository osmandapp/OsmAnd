package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

public class GeometrySolidWayStyle<C extends MultiColoringGeometryWayContext> extends GeometryWayStyle<C> {

	private static final float LINE_WIDTH_THRESHOLD_DP = 8f;
	private static final float ARROW_DISTANCE_MULTIPLIER = 1.5f;
	private static final float SPECIAL_ARROW_DISTANCE_MULTIPLIER = 10f;

	public static final int OUTER_CIRCLE_COLOR = 0x33000000;

	private final int directionArrowColor;

	private final boolean hasPathLine;

	private final float lineWidthThresholdPix;
	private final float outerCircleRadius;
	private final float innerCircleRadius;

	GeometrySolidWayStyle(@NonNull C context, int lineColor, float lineWidth, int directionArrowColor,
	                      boolean hasPathLine) {
		super(context, lineColor, lineWidth);
		this.directionArrowColor = directionArrowColor;
		this.hasPathLine = hasPathLine;

		this.innerCircleRadius = AndroidUtils.dpToPxAuto(context.getCtx(), 7);
		this.outerCircleRadius = AndroidUtils.dpToPxAuto(context.getCtx(), 8);
		this.lineWidthThresholdPix = AndroidUtils.dpToPxAuto(context.getCtx(), LINE_WIDTH_THRESHOLD_DP);
	}

	@Override
	public Bitmap getPointBitmap() {
		return useSpecialArrow() ? getContext().getSpecialArrowBitmap() : getContext().getArrowBitmap();
	}

	@NonNull
	@Override
	public Integer getPointColor() {
		return directionArrowColor;
	}

	public boolean hasPathLine() {
		return hasPathLine;
	}

	public float getInnerCircleRadius() {
		return innerCircleRadius;
	}

	public float getOuterCircleRadius() {
		return outerCircleRadius;
	}

	@Override
	public double getPointStepPx(double zoomCoef) {
		return useSpecialArrow() ? getSpecialPointStepPx() : getRegularPointStepPx();
	}

	public double getSpecialPointStepPx() {
		Bitmap bitmap = getContext().getSpecialArrowBitmap();
		return bitmap.getHeight() * SPECIAL_ARROW_DISTANCE_MULTIPLIER;
	}

	public double getRegularPointStepPx() {
		Bitmap bitmap = getContext().getArrowBitmap();
		return bitmap.getHeight() + getWidth(0) * ARROW_DISTANCE_MULTIPLIER;
	}

	public boolean useSpecialArrow() {
		return getWidth(0) <= lineWidthThresholdPix;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		if (!(other instanceof GeometrySolidWayStyle)) {
			return false;
		}
		GeometrySolidWayStyle<?> o = (GeometrySolidWayStyle<?>) other;
		return Algorithms.objectEquals(directionArrowColor, o.directionArrowColor);
	}

	@Override
	public int getColorizationScheme() {
		return COLORIZATION_SOLID;
	}
}
