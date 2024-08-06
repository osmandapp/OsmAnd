package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.util.Algorithms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public abstract class GeometryWayStyle<T extends GeometryWayContext> {

	private final T context;
	protected Integer color;
	protected Float width;
	protected float[] dashPattern;

	public static final int COLORIZATION_NONE = 0;
	public static final int COLORIZATION_GRADIENT = 1;
	public static final int COLORIZATION_SOLID = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({COLORIZATION_NONE, COLORIZATION_GRADIENT, COLORIZATION_SOLID})
	public @interface ColorizationType {
	}

	protected Gpx3DVisualizationType trackVisualizationType = Gpx3DVisualizationType.NONE;
	protected Gpx3DWallColorType trackWallColorType = Gpx3DWallColorType.NONE;
	protected Gpx3DLinePositionType trackLinePositionType = Gpx3DLinePositionType.TOP;
	protected float additionalExaggeration = 1f;
	protected float elevationMeters = 1000f;

	public GeometryWayStyle(@NonNull T context) {
		this.context = context;
	}

	public GeometryWayStyle(@NonNull T context, Integer color) {
		this.context = context;
		this.color = color;
	}

	public GeometryWayStyle(@NonNull T context, Integer color, Float width) {
		this.context = context;
		this.color = color;
		this.width = width;
	}

	@NonNull
	public T getContext() {
		return context;
	}

	@NonNull
	public Context getCtx() {
		return context.getCtx();
	}

	@Nullable
	public Integer getColor() {
		return color;
	}

	public Integer getColor(Integer def) {
		return color != null ? color : def;
	}

	@Nullable
	public Float getWidth() {
		return width;
	}

	public Float getWidth(Integer def) {
		return width != null ? width : def;
	}

	@Nullable
	public float[] getDashPattern() {
		return dashPattern;
	}

	@Nullable
	public Integer getStrokeColor() {
		return color != null ? context.getStrokeColor(color) : null;
	}

	public Integer getStrokeColor(Integer def) {
		return color != null ? context.getStrokeColor(color) : def;
	}

	@Nullable
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
		return Algorithms.objectEquals(color, o.color)
				&& Algorithms.objectEquals(width, o.width)
				&& Arrays.equals(dashPattern, o.dashPattern)
				&& o.trackVisualizationType == ((GeometryWayStyle<?>) other).trackVisualizationType
				&& o.trackWallColorType == ((GeometryWayStyle<?>) other).trackWallColorType
				&& o.trackLinePositionType == ((GeometryWayStyle<?>) other).trackLinePositionType
				&& o.additionalExaggeration == ((GeometryWayStyle<?>) other).additionalExaggeration
				&& o.elevationMeters == ((GeometryWayStyle<?>) other).elevationMeters;
	}

	public int getColorizationScheme() {
		return COLORIZATION_NONE;
	}
}
