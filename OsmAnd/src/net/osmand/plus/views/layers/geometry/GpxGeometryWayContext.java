package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;

import net.osmand.plus.R;

public class GpxGeometryWayContext extends GeometryWayContext {

	public GpxGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
		Paint paint = getPaintIcon();
		paint.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_direction_arrow;
	}
}
