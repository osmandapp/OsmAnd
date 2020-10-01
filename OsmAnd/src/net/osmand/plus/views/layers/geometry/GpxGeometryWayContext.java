package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;

import net.osmand.plus.R;

public class GpxGeometryWayContext extends GeometryWayContext {

	private float trackWidth;

	public GpxGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
		Paint paint = getPaintIcon();
		paint.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_direction_arrow;
	}

	@Override
	public double getDefaultPxStep(double zoomCoef) {
		return getArrowBitmap().getHeight() + trackWidth * 1.5f;
	}

	public void setTrackWidth(float trackWidth) {
		this.trackWidth = trackWidth;
	}
}