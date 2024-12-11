package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.layers.base.OsmandMapLayer.RenderingLineAttributes;

public class CommonGeometryWayContext extends GeometryWayContext {

	protected RenderingLineAttributes attrsW;
	private Bitmap walkArrowBitmap;
	protected Bitmap walkInnerArrowBitmap;

	public CommonGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
		float scale = getApp().getOsmandMap().getCarDensityScaleCoef() * 0.5f;
		walkInnerArrowBitmap = RenderingIcons.getBitmapFromVectorDrawable(ctx, getArrowBitmapResId(), scale);
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}

	public RenderingLineAttributes getAttrsW() {
		return attrsW;
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes attrs,
			@NonNull RenderingLineAttributes attrsW) {
		this.attrsW = attrsW;
		updatePaints(nightMode, attrs);
	}

	public Bitmap getWalkArrowBitmap() {
		return walkArrowBitmap;
	}

	@Override
	protected boolean hasAttrs() {
		return super.hasAttrs() && attrsW != null;
	}

	@Override
	protected void recreateBitmaps() {
		super.recreateBitmaps();
		float walkCircleH = attrsW.paint.getStrokeWidth() * 1.33f;
		float walkCircleW = attrsW.paint.getStrokeWidth();
		float walkCircleRadius = attrsW.paint.getStrokeWidth() / 2f;

		// create anchor bitmap
		float density = getDensity();
		float margin = getArrowMargin();

		// create walk arrow bitmap
		float width = walkCircleW + margin * 2;
		float height = walkCircleH + margin * 2;
		Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1f * density);

		RectF rect = new RectF(margin, margin, width - margin, height - margin);
		paint.setColor(attrsW.paint.getColor());
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);
		paint.setColor(getStrokeColor(paint.getColor()));
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);

		Bitmap arrowBitmap = walkInnerArrowBitmap;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1f * density);
		paint.setAlpha(200);
		canvas.drawBitmap(arrowBitmap, width / 2 - arrowBitmap.getWidth() / 2f, height / 2 - arrowBitmap.getHeight() / 2f, paint);

		walkArrowBitmap = bitmap;
	}

	protected float getArrowMargin() {
		float[] intervals = getDashPattern(attrsW.paint);
		return intervals != null && intervals.length >= 2 ? intervals[1] : 2f * getDensity();
	}

	@Nullable
	private float[] getDashPattern(@NonNull Paint paint) {
		PathEffect pathEffect = paint.getPathEffect();
		if (pathEffect instanceof OsmandDashPathEffect) {
			return ((OsmandDashPathEffect) pathEffect).getIntervals();
		}
		return null;
	}
}
