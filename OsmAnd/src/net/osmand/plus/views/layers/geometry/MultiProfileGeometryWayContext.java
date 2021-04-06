package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class MultiProfileGeometryWayContext extends GeometryWayContext {

	private final UiUtilities iconsCache;

	public final float minIconMargin;
	public final float circleSize;
	public final float pointIconSize;

	private RenderingLineAttributes multiProfileAttrs;

	private final Map<String, Bitmap> profileIconsBitmapCache;

	public MultiProfileGeometryWayContext(Context ctx, UiUtilities iconsCache, float density) {
		super(ctx, density);
		this.iconsCache = iconsCache;
		profileIconsBitmapCache = new HashMap<>();
		minIconMargin = density * 30;
		circleSize = density * 70;
		pointIconSize = density * 22f;
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes multiProfileAttrs) {
		this.multiProfileAttrs = multiProfileAttrs;
		super.updatePaints(nightMode, multiProfileAttrs);
	}

	@NonNull
	public Bitmap getProfileIconBitmap(@DrawableRes int iconRes, @ColorInt int color) {
		String key = iconRes + "_" + color;
		Bitmap bitmap = profileIconsBitmapCache.get(key);
		if (bitmap == null) {
			bitmap = Bitmap.createBitmap((int) circleSize, (int) circleSize, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			float center = bitmap.getWidth() / 2f;

			canvas.drawCircle(center, center, center / 2, multiProfileAttrs.paint_1);
			multiProfileAttrs.paint3.setColor(color);
			canvas.drawCircle(center, center, center / 2, multiProfileAttrs.paint3);

			float iconSize = center - getDensity() * 10;
			Bitmap profileIconBitmap = AndroidUtils.createScaledBitmap(
					iconsCache.getPaintedIcon(iconRes, color), (int) iconSize, (int) iconSize);
			canvas.drawBitmap(profileIconBitmap, center - iconSize / 2, center - iconSize / 2, multiProfileAttrs.paint3);

			profileIconsBitmapCache.put(key, bitmap);
		}
		return bitmap;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_split_interval;
	}
}