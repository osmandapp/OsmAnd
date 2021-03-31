package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class MultiProfileGeometryWayContext extends GeometryWayContext {

	private RenderingLineAttributes multiProfileAttrs;

	private Bitmap pointIcon;

	private final Map<String, Bitmap> profileIconsBitmapCache;

	public MultiProfileGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
		profileIconsBitmapCache = new HashMap<>();
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes multiProfileAttrs) {
		this.multiProfileAttrs = multiProfileAttrs;
		super.updatePaints(nightMode, multiProfileAttrs);
	}

	@Override
	protected void recreateBitmaps() {
		float density = getDensity();
		float size = density * 12.5f;
		float outerRadius = density * 6.25f;
		float centerRadius = density * 6;
		float innerRadius = density * 4;
		float centerXY = size / 2;

		pointIcon = Bitmap.createBitmap((int) size, (int) size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(pointIcon);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		paint.setColor(Color.BLACK);
		canvas.drawCircle(centerXY, centerXY, outerRadius, paint);

		paint.setColor(Color.WHITE);
		canvas.drawCircle(centerXY, centerXY, centerRadius, paint);

		paint.setColor(Algorithms.parseColor("#637EFB"));
		canvas.drawCircle(centerXY, centerXY, innerRadius, paint);
	}

	@Override
	protected int getArrowBitmapResId() {
		return 0;
	}

	@NonNull
	public Bitmap getProfileIconBitmap(@NonNull String profileKey, int profileColor) {
		String key = profileKey + "_" + profileColor;
		Bitmap bitmap = profileIconsBitmapCache.get(key);
		if (bitmap == null) {
			float density = getDensity();
			float diameter = density * 18;
			bitmap = Bitmap.createBitmap((int) diameter, (int) diameter, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(bitmap);
			canvas.drawCircle(diameter / 2, diameter / 2, diameter / 2, multiProfileAttrs.paint_1);
			multiProfileAttrs.paint3.setColor(profileColor);
			canvas.drawCircle(diameter / 2, diameter / 2, diameter / 2, multiProfileAttrs.paint3);
		}
		return bitmap;
	}

	@NonNull
	public Bitmap getPointIcon() {
		return pointIcon;
	}
}