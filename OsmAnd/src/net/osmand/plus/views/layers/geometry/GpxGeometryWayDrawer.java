package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import net.osmand.AndroidUtils;
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

		private Bitmap circleBitmap;

		ArrowPathPoint(float x, float y, double angle, GeometryWayStyle<?> style) {
			super(x, y, angle, style);
			createCircleBitmap((GeometryArrowsStyle) style);
		}

		@Override
		void draw(Canvas canvas, GeometryWayContext context) {
			if (style instanceof GeometryArrowsStyle) {
				Context ctx = style.getCtx();
				GeometryArrowsStyle arrowsWayStyle = (GeometryArrowsStyle) style;
				Bitmap bitmap = style.getPointBitmap();
				boolean useSpecialArrow = arrowsWayStyle.useSpecialArrow();

				float newWidth = useSpecialArrow ? AndroidUtils.dpToPx(ctx, 12) : arrowsWayStyle.getTrackWidth() / 2f;
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = newWidth / 2f;

				Matrix matrix = getMatrix();
				matrix.reset();
				float sy = useSpecialArrow ? newWidth / bitmap.getHeight() : 1;
				matrix.postScale(newWidth / bitmap.getWidth(), sy);
				matrix.postRotate((float) angle, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);

				if (useSpecialArrow) {
					drawCircle(canvas, arrowsWayStyle);
				}

				Paint paint = context.getPaintIconCustom();
				Integer pointColor = style.getPointColor();
				paint.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.SRC_IN));
				canvas.drawBitmap(bitmap, matrix, paint);
			}
		}

		private void drawCircle(Canvas canvas, GeometryArrowsStyle style) {
			float offset = circleBitmap.getWidth() / 2f;
			float angleOffset = AndroidUtils.dpToPx(style.getCtx(), 1);
			double rad = Math.toRadians(angle + 90);
			float x = (float) (this.x - offset - angleOffset * Math.cos(rad));
			float y = (float) (this.y - offset - angleOffset * Math.sin(rad));
			canvas.drawBitmap(circleBitmap, x, y, null);
		}

		private void createCircleBitmap(GeometryArrowsStyle style) {
			Context ctx = style.getCtx();
			int size = AndroidUtils.dpToPx(ctx, 16);
			circleBitmap = Bitmap.createBitmap(size, size, style.getPointBitmap().getConfig());
			Paint paint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
			paint.setStyle(Paint.Style.FILL);

			Canvas c = new Canvas(circleBitmap);

			paint.setColor(0x33000000);
			Path path = new Path();
			path.addCircle(size / 2f, size / 2f, AndroidUtils.dpToPx(ctx, 8), Path.Direction.CW);
			c.drawPath(path, paint);

			paint.setColor(style.getTrackColor());
			path.reset();
			path.addCircle(size / 2f, size / 2f, AndroidUtils.dpToPx(ctx, 7), Path.Direction.CW);
			c.drawPath(path, paint);
		}
	}
}