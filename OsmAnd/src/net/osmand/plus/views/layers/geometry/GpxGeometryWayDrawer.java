package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import net.osmand.plus.views.layers.geometry.GpxGeometryWay.GeometryArrowsStyle;

public class GpxGeometryWayDrawer extends GeometryWayDrawer<GpxGeometryWayContext> {

	public GpxGeometryWayDrawer(GpxGeometryWayContext context) {
		super(context);
	}

	@Override
	protected PathPoint getArrowPathPoint(float iconx, float icony, GeometryWayStyle<?> style, double angle) {
		return new ArrowPathPoint(iconx, icony, angle, style);
	}

	private static class ArrowPathPoint extends PathPoint {

		ArrowPathPoint(float x, float y, double angle, GeometryWayStyle<?> style) {
			super(x, y, angle, style);
		}

		@Override
		void draw(Canvas canvas, GeometryWayContext context) {
			if (style instanceof GeometryArrowsStyle) {
				GeometryArrowsStyle arrowsWayStyle = (GeometryArrowsStyle) style;
				Bitmap bitmap = style.getPointBitmap();

				float newWidth = arrowsWayStyle.getTrackWidth() / 2f;
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = newWidth / 2f;

				Matrix matrix = getMatrix();
				matrix.reset();
				matrix.postScale(newWidth / bitmap.getWidth(), 1);
				matrix.postRotate((float) angle, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);

				Paint paint = context.getPaintIconCustom();
				Integer pointColor = style.getPointColor();
				paint.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.SRC_IN));
				canvas.drawBitmap(bitmap, matrix, paint);
			}
		}
	}
}