package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;

public class GeometryGradient3DWayStyle<C extends MultiColoringGeometryWayContext> extends GeometryGradientWayStyle<C> {

	public int currOutlineColor;
	public int nextOutlineColor;

	public GeometryGradient3DWayStyle(@NonNull C context, int color, float width) {
		super(context, color, width);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		if (!(other instanceof GeometryGradient3DWayStyle)) {
			return false;
		}
		GeometryGradient3DWayStyle<?> o = (GeometryGradient3DWayStyle<?>) other;
		return currColor == o.currColor && nextColor == o.nextColor
				&& currOutlineColor == o.currOutlineColor && nextOutlineColor == o.nextOutlineColor;
	}
}