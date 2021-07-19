package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;

import net.osmand.plus.R;

import androidx.annotation.NonNull;

public class RouteGeometryWayContext extends MultiColoringGeometryWayContext {

	public RouteGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
	}

	@NonNull
	@Override
	public Paint getStrokePaint() {
		return getAttrs().customColorPaint;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}
}