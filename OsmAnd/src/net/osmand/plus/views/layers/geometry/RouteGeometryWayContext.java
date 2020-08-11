package net.osmand.plus.views.layers.geometry;

import android.content.Context;

import net.osmand.plus.R;

public class RouteGeometryWayContext extends GeometryWayContext {

	public RouteGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}
}
