package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.HashMap;
import java.util.Map;

public class MultiProfileGeometryWayContext extends GeometryWayContext {

	@DrawableRes
	private static final int STRAIGHT_LINE_ICON_RES = R.drawable.ic_action_split_interval;

	private static final float PATH_WIDTH_DP = 4;
	private static final float PATH_BORDER_WIDTH_DP = PATH_WIDTH_DP + 2;
	private static final float PROFILE_ICON_BORDER_WIDTH_DP = 1;

	private static final float MIN_PROFILE_ICON_MARGIN_DP = 30;
	private static final float PROFILE_ICON_FRAME_SIZE_DP = 18;
	private static final float PROFILE_ICON_SIZE_DP = 12;

	private final UiUtilities iconsCache;

	private final Paint profileIconBackgroundPaint;
	private final Paint profileIconBorderPaint;
	private final Paint pathBorderPaint;
	private final Paint pathPaint;

	private final Map<String, Bitmap> profileIconsBitmapCache;

	public MultiProfileGeometryWayContext(Context ctx, UiUtilities iconsCache, float density) {
		super(ctx, density);

		this.iconsCache = iconsCache;
		this.profileIconsBitmapCache = new HashMap<>();

		this.profileIconBackgroundPaint = createPaint(Style.FILL, 0xFFFFFFFF, 0);
		this.profileIconBorderPaint = createPaint(Style.STROKE, 0, PROFILE_ICON_BORDER_WIDTH_DP);
		this.pathBorderPaint = createPaint(Style.STROKE, 0, PATH_BORDER_WIDTH_DP);
		this.pathPaint = createPaint(Style.STROKE, 0, PATH_WIDTH_DP);
	}

	private Paint createPaint(@NonNull Style style, @ColorInt int color, float strokeWidthDp) {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setStyle(style);
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidthDp * getDensity());
		return paint;
	}

	@NonNull
	public Paint getPathPaint() {
		return pathPaint;
	}

	@NonNull
	public Paint getPathBorderPaint() {
		return pathBorderPaint;
	}

	@NonNull
	public Paint getProfileIconBackgroundPaint() {
		return profileIconBackgroundPaint;
	}

	@NonNull
	public Bitmap getProfileIconBitmap(@DrawableRes int iconRes, @ColorInt int color) {
		String key = iconRes + "_" + color;
		Bitmap framedProfileIconBitmap = profileIconsBitmapCache.get(key);
		if (framedProfileIconBitmap == null) {
			int profileIconFrameSizePx = (int) getProfileIconSizePx(getDensity());
			framedProfileIconBitmap = Bitmap.createBitmap(profileIconFrameSizePx,
					profileIconFrameSizePx, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(framedProfileIconBitmap);
			float iconCenter = framedProfileIconBitmap.getWidth() / 2f;

			float frameRadius = iconCenter - PROFILE_ICON_BORDER_WIDTH_DP;
			canvas.drawCircle(iconCenter, iconCenter, frameRadius, profileIconBackgroundPaint);
			profileIconBorderPaint.setColor(color);
			canvas.drawCircle(iconCenter, iconCenter, frameRadius, profileIconBorderPaint);

			float profileIconSize = PROFILE_ICON_SIZE_DP * getDensity();
			Drawable paintedIcon = iconsCache.getPaintedIcon(iconRes, color);
			if (paintedIcon != null) {
				Bitmap profileIconBitmap = AndroidUtils.createScaledBitmap(
						paintedIcon, (int) profileIconSize, (int) profileIconSize);
				canvas.drawBitmap(profileIconBitmap, iconCenter - profileIconSize / 2,
						iconCenter - profileIconSize / 2, profileIconBorderPaint);
			}
			profileIconsBitmapCache.put(key, framedProfileIconBitmap);
		}
		return framedProfileIconBitmap;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_split_interval;
	}

	@DrawableRes
	public int getStraightLineIconRes() {
		return STRAIGHT_LINE_ICON_RES;
	}

	@ColorInt
	public int getStraightLineColor() {
		return ContextCompat.getColor(getCtx(), isNightMode() ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	public static float getProfileIconSizePx(float density) {
		return PROFILE_ICON_FRAME_SIZE_DP * density;
	}

	public static float getMinProfileIconMarginPx(float density) {
		return MIN_PROFILE_ICON_MARGIN_DP * density;
	}
}