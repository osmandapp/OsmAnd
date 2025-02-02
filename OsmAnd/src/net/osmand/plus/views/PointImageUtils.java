package net.osmand.plus.views;

import static net.osmand.data.BackgroundType.CIRCLE;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.TreeMap;

public class PointImageUtils {

	private static final TreeMap<String, PointImageDrawable> DRAWABLE_CACHE = new TreeMap<>();

	private static final Paint BITMAP_PAINT = createBitmapPaint();

	private static final int SMALL_ICON_BORDER_DP = 1;
	private static final int BIG_ICON_BORDER_DP = 2;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;
	private static final int POINT_OUTER_COLOR = 0xffffffff;

	private static Paint createBitmapPaint() {
		Paint bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
		return bitmapPaint;
	}

	private static Bitmap circleBitmap;

	private static Bitmap getCircle(@NonNull Context ctx) {
		if (circleBitmap == null) {
			circleBitmap = RenderingIcons.getBitmapFromVectorDrawable(ctx, R.drawable.bg_point_circle);
		}
		return circleBitmap;
	}

	@NonNull
	private static PointImageDrawable getOrCreate(@NonNull Context context, @NonNull PointImageInfo info) {
		String uniqueId = context.getResources().getResourceEntryName(info.iconId);
		uniqueId += info.type.name();
		int color = info.color | 0xff000000;
		int hash = (color << 4) + ((info.withShadow ? 1 : 0) << 2) + ((info.synced ? 3 : 0) << 2);
		uniqueId = hash + uniqueId;
		PointImageDrawable drawable = DRAWABLE_CACHE.get(uniqueId);
		if (drawable == null) {
			drawable = new PointImageDrawable(context, info);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			DRAWABLE_CACHE.put(uniqueId, drawable);
		}
		return drawable;
	}

	@NonNull
	public static PointImageDrawable getOrCreateSyncedIcon(@NonNull Context context, @ColorInt int color, @Nullable FavouritePoint point) {
		return getFromPoint(context, color, true, true, point);
	}

	@NonNull
	public static PointImageDrawable getOrCreateSyncedIcon(@NonNull Context context, @ColorInt int color, @Nullable WptPt point) {
		return getFromPoint(context, color, true, true, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, @Nullable WptPt point) {
		return getFromPoint(context, color, withShadow, false, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, boolean synced, @Nullable WptPt point) {
		if (point != null) {
			int overlayIconId = context.getResources().getIdentifier("mx_" + point.getIconNameOrDefault(),
					"drawable", context.getPackageName());
			return getOrCreate(context, color, withShadow, synced, overlayIconId,
					BackgroundType.getByTypeName(point.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		} else {
			return getOrCreate(context, color, withShadow);
		}
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, @Nullable FavouritePoint point) {
		return getFromPoint(context, color, withShadow, false, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, boolean synced,
	                                              @Nullable FavouritePoint point) {
		if (point != null) {
			return getOrCreate(context, color, withShadow, synced, point.getOverlayIconId(context),
					point.getBackgroundType());
		} else {
			return getOrCreate(context, color, withShadow);
		}
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color, boolean withShadow) {
		return getOrCreate(context, color, withShadow, DEFAULT_UI_ICON_ID);
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color,
	                                             boolean withShadow, @DrawableRes int iconId) {
		return getOrCreate(context, color, withShadow, false, iconId, CIRCLE);
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color,
	                                             boolean withShadow, boolean synced,
	                                             @DrawableRes int iconId, @NonNull BackgroundType type) {
		iconId = iconId == 0 ? DEFAULT_UI_ICON_ID : iconId;
		return getOrCreate(context, new PointImageInfo(type, color, iconId, synced, withShadow));
	}

	public static void clearCache() {
		DRAWABLE_CACHE.clear();
	}

	public static class PointImageInfo {

		@ColorInt
		protected final int color;
		@DrawableRes
		protected final int iconId;
		protected final boolean synced;
		protected final boolean withShadow;
		protected final BackgroundType type;

		private PointImageInfo(@NonNull BackgroundType type, @ColorInt int color,
		                       @DrawableRes int iconId, boolean synced, boolean withShadow) {
			this.iconId = iconId;
			this.color = color;
			this.synced = synced;
			this.withShadow = withShadow;
			this.type = type;
		}
	}

	public static Bitmap createSmallPointBitmap(@NonNull Context ctx) {
		int borderWidth = AndroidUtils.dpToPx(ctx, SMALL_ICON_BORDER_DP);
		Bitmap circle = getCircle(ctx);
		int smallIconSize = AndroidUtils.dpToPx(ctx, SMALL_ICON_SIZE_DP);
		Bitmap bitmapResult = Bitmap.createBitmap(smallIconSize, smallIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		BITMAP_PAINT.setColorFilter(new PorterDuffColorFilter(POINT_OUTER_COLOR, PorterDuff.Mode.SRC_IN));
		Rect srcRect = new Rect(0, 0, circle.getWidth(), circle.getHeight());
		RectF dstRect = new RectF(0f, 0f, (float) smallIconSize, (float) smallIconSize);
		canvas.drawBitmap(circle, srcRect, dstRect, BITMAP_PAINT);
		BITMAP_PAINT.setColorFilter(new PorterDuffColorFilter(
				ColorUtilities.getColor(ctx, R.color.poi_background), PorterDuff.Mode.SRC_IN));
		dstRect = new RectF(
				(float) borderWidth,
				(float) borderWidth,
				(float) (smallIconSize - borderWidth * 2),
				(float) (smallIconSize - borderWidth * 2));
		canvas.drawBitmap(circle, srcRect, dstRect, BITMAP_PAINT);
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, smallIconSize, smallIconSize, false);
		return bitmapResult;
	}

	public static Bitmap createBigBitmap(@NonNull OsmandApplication app, Bitmap loadedBitmap, boolean isSelected) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int borderWidth = AndroidUtils.dpToPxAuto(app, BIG_ICON_BORDER_DP);
		Bitmap circle = getCircle(app);
		int bigIconSize = AndroidUtils.dpToPxAuto(app, BIG_ICON_SIZE_DP);
		Bitmap bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		BITMAP_PAINT.setColorFilter(new PorterDuffColorFilter(isSelected ? app.getColor(ColorUtilities.getActiveColorId(nightMode)) : POINT_OUTER_COLOR, PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(circle, 0f, 0f, BITMAP_PAINT);
		int cx = circle.getWidth() / 2;
		int cy = circle.getHeight() / 2;
		int radius = (Math.min(cx, cy) - borderWidth * 2);
		canvas.save();
		canvas.clipRect(0, 0, circle.getWidth(), circle.getHeight());
		Path circularPath = new Path();
		circularPath.addCircle((float) cx, (float) cy, (float) radius, Path.Direction.CW);
		canvas.clipPath(circularPath);
		Rect srcRect = new Rect(0, 0, loadedBitmap.getWidth(), loadedBitmap.getHeight());
		RectF dstRect = new RectF(0f, 0f, (float) circle.getWidth(), (float) circle.getHeight());
		BITMAP_PAINT.setColorFilter(null);
		canvas.drawBitmap(loadedBitmap, srcRect, dstRect, BITMAP_PAINT);
		canvas.restore();
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, bigIconSize, bigIconSize, false);
		return bitmapResult;
	}

}