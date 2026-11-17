package net.osmand.plus.views.layers.geometry;

import android.graphics.Color;

import androidx.annotation.NonNull;

public class GeometryGradientWayStyle<C extends MultiColoringGeometryWayContext> extends GeometrySolidWayStyle<C> {

	public int currColor;
	public int nextColor;

	public GeometryGradientWayStyle(@NonNull C context, int color, float width) {
		super(context, color, width, Color.BLACK, true);
	}

	@Override
	public boolean isUnique() {
		return true;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		if (!(other instanceof GeometryGradientWayStyle)) {
			return false;
		}
		GeometryGradientWayStyle<?> o = (GeometryGradientWayStyle<?>) other;
		return currColor == o.currColor && nextColor == o.nextColor;
	}

	@Override
	public int getColorizationScheme() {
		return COLORIZATION_GRADIENT;
	}
}