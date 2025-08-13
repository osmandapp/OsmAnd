package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer.RenderingLineAttributes;

import java.util.HashMap;
import java.util.Map;

public class PublicTransportGeometryWayContext extends CommonGeometryWayContext {

	private RenderingLineAttributes attrsPT;
	private Bitmap anchorBitmap;
	private Map<Pair<Integer, Drawable>, Bitmap> stopBitmapsCache = new HashMap<>();
	private final Map<Integer, Bitmap> stopSmallBitmapsCache = new HashMap<>();

	public PublicTransportGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
	}

	public RenderingLineAttributes getAttrsPT() {
		return attrsPT;
	}

	@Override
	protected boolean hasAttrs() {
		return super.hasAttrs() && attrsPT != null;
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes attrs,
	                         @NonNull RenderingLineAttributes attrsPT, @NonNull RenderingLineAttributes attrsW) {
		this.attrsPT = attrsPT;
		updatePaints(nightMode, attrs, attrsW);
	}

	public Bitmap getAnchorBitmap() {
		return anchorBitmap;
	}

	@Override
	protected void recreateBitmaps() {
		super.recreateBitmaps();
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
		stopBitmapsCache = new HashMap<>();
	}

	@Override
	protected float getArrowMargin() {
		return 2f * getDensity();
	}

	@Override
	public void clearCustomColor() {
		super.clearCustomColor();
		if (attrsPT != null) {
			attrsPT.customColor = 0;
		}
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
