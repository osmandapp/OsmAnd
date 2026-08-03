package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;

public class GeometryWalkWayStyle extends GeometryWayStyle<CommonGeometryWayContext> {

	GeometryWalkWayStyle(CommonGeometryWayContext context) {
		super(context);
	}

	@Override
	public boolean hasPathLine() {
		return false;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		return other instanceof GeometryWalkWayStyle;
	}

	public Bitmap getPointBitmap() {
		return getContext().getWalkArrowBitmap();
	}

	@Override
	public boolean hasPaintedPointBitmap() {
		return true;
	}

	@Override
	public double getPointStepPx(double zoomCoef) {
		return getPointBitmap().getHeight() * 1.2f * zoomCoef;
	}

	@Override
	public boolean isVisibleWhileZooming() {
		return true;
	}
}
