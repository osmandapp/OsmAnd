package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;

import net.osmand.util.Algorithms;

public abstract class GeometryWayStyle<T extends GeometryWayContext> {

	private T context;
	protected Integer color;
	protected Float width;

	public GeometryWayStyle(T context) {
		this.context = context;
	}

	public GeometryWayStyle(T context, Integer color) {
		this.context = context;
		this.color = color;
	}

	public GeometryWayStyle(T context, Integer color, Float width) {
		this.context = context;
		this.color = color;
		this.width = width;
	}

	public T getContext() {
		return context;
	}

	public Context getCtx() {
		return context.getCtx();
	}

	public Integer getColor() {
		return color;
	}

	public Float getWidth() {
		return width;
	}

	public Integer getStrokeColor() {
		return context.getStrokeColor(color);
	}

	public Integer getPointColor() {
		return null;
	}

	public boolean isNightMode() {
		return context.isNightMode();
	}

	public boolean hasPathLine() {
		return true;
	}

	public boolean isVisibleWhileZooming() {
		return false;
	}

	public boolean isUnique() {
		return false;
	}

	public double getPointStepPx(double zoomCoef) {
		Bitmap arrow = context.getArrowBitmap();
		int arrowHeight = arrow.getHeight();
		return arrowHeight * 4f * zoomCoef;
	}

	public abstract Bitmap getPointBitmap();

	public boolean hasPaintedPointBitmap() {
		return false;
	}

	@Override
	public int hashCode() {
		return (color != null ? color.hashCode() : 0) + (context.isNightMode() ? 1231 : 1237);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof GeometryWayStyle)) {
			return false;
		}
		GeometryWayStyle<?> o = (GeometryWayStyle<?>) other;
		return Algorithms.objectEquals(color, o.color) && Algorithms.objectEquals(width, o.width);
	}
}
