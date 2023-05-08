package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.views.layers.base.OsmandMapLayer.RenderingLineAttributes;

import java.util.HashMap;
import java.util.Map;

public class PublicTransportGeometryWayContext extends GeometryWayContext {

	private RenderingLineAttributes attrsPT;
	private RenderingLineAttributes attrsW;

	private Bitmap walkArrowBitmap;
	private Bitmap anchorBitmap;
	private Map<Pair<Integer, Drawable>, Bitmap> stopBitmapsCache = new HashMap<>();
	private final Map<Integer, Bitmap> stopSmallBitmapsCache = new HashMap<>();

	public PublicTransportGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}

	public RenderingLineAttributes getAttrsPT() {
		return attrsPT;
	}

	public RenderingLineAttributes getAttrsW() {
		return attrsW;
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes attrs,
							 @NonNull RenderingLineAttributes attrsPT, @NonNull RenderingLineAttributes attrsW) {
		this.attrsPT = attrsPT;
		this.attrsW = attrsW;
		updatePaints(nightMode, attrs);
	}

	public Bitmap getWalkArrowBitmap() {
		return walkArrowBitmap;
	}

	public Bitmap getAnchorBitmap() {
		return anchorBitmap;
	}

	@Override
	protected boolean hasAttrs() {
		return super.hasAttrs() && attrsPT != null && attrsW != null;
	}

	@Override
	protected void recreateBitmaps() {
		super.recreateBitmaps();
		float walkCircleH = attrsW.paint.getStrokeWidth() * 1.33f;
		float walkCircleW = attrsW.paint.getStrokeWidth();
		float walkCircleRadius = attrsW.paint.getStrokeWidth() / 2f;
		float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 2f;

		// create anchor bitmap
		float density = getDensity();
		float margin = 2f * density;
		float width = routeShieldRadius * 2 + margin * 2;
		float height = routeShieldRadius * 2 + margin * 2;
		Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1f * density);

		float x = width / 2;
		float y = height / 2;
		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(x, y, routeShieldRadius, paint);
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(x, y, routeShieldRadius, paint);

		anchorBitmap = bitmap;

		// create walk arrow bitmap
		width = walkCircleW + margin * 2;
		height = walkCircleH + margin * 2;
		bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1f * density);

		RectF rect = new RectF(margin, margin, width - margin, height - margin);
		paint.setColor(attrsW.paint.getColor());
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);
		paint.setColor(getStrokeColor(paint.getColor()));
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);

		Bitmap arrowBitmap = getArrowBitmap();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1f * density);
		paint.setAlpha(200);
		canvas.drawBitmap(arrowBitmap, width / 2 - arrowBitmap.getWidth() / 2f, height / 2 - arrowBitmap.getHeight() / 2f, paint);

		walkArrowBitmap = bitmap;
		stopBitmapsCache = new HashMap<>();
	}

	@Override
	public void clearCustomColor() {
		super.clearCustomColor();
		attrsPT.customColor = 0;
	}

	public Bitmap getStopShieldBitmap(int color, Drawable stopDrawable) {
		Bitmap bmp = stopBitmapsCache.get(new Pair<>(color, stopDrawable));
		if (bmp == null) {
			int fillColor = ColorUtilities.getContrastColor(getApp(), color, true);
			int strokeColor = getStrokeColor(color);

			float density = getDensity();
			float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 2;
			float routeShieldCornerRadius = 3 * density;

			float margin = 2f * density;
			float width = routeShieldRadius * 2 + margin * 2;
			float height = routeShieldRadius * 2 + margin * 2;
			bmp = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(bmp);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStrokeWidth(1f * density);

			RectF rect = new RectF(margin, margin, width - margin, height - margin);
			paint.setColor(fillColor);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRoundRect(rect, routeShieldCornerRadius, routeShieldCornerRadius, paint);
			paint.setColor(strokeColor);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawRoundRect(rect, routeShieldCornerRadius, routeShieldCornerRadius, paint);

			if (stopDrawable != null) {
				stopDrawable.setColorFilter(new PorterDuffColorFilter(strokeColor, PorterDuff.Mode.SRC_IN));
				float marginBitmap = 1f * density;
				rect.inset(marginBitmap, marginBitmap);
				stopDrawable.setBounds(0, 0, (int) rect.width(), (int) rect.height());
				canvas.translate(rect.left, rect.top);
				stopDrawable.draw(canvas);
			}
			stopBitmapsCache.put(new Pair<>(color, stopDrawable), bmp);
		}
		return bmp;
	}

	public Bitmap getStopSmallShieldBitmap(int color) {
		Bitmap bmp = stopSmallBitmapsCache.get(color);
		if (bmp == null) {
			int fillColor = ColorUtilities.getContrastColor(getApp(), color, true);
			int strokeColor = getStrokeColor(color);

			float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 4;

			float density = getDensity();
			float margin = 3f * density;
			float width = routeShieldRadius * 2 + margin * 2;
			float height = routeShieldRadius * 2 + margin * 2;
			bmp = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(bmp);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStrokeWidth(1f * density);
			paint.setColor(fillColor);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawCircle(width / 2, height / 2, routeShieldRadius, paint);
			paint.setColor(strokeColor);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawCircle(width / 2, height / 2, routeShieldRadius, paint);

			stopSmallBitmapsCache.put(color, bmp);
		}
		return bmp;
	}
}
