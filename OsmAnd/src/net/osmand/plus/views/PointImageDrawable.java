package net.osmand.plus.views;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.data.FavouritePoint;
import net.osmand.data.BackgroundType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.TreeMap;

public class PointImageDrawable extends Drawable {

	private final int dp_12_px;
	private final boolean withShadow;
	private boolean history;
	private final Drawable mapIcon;
	private Bitmap mapIconBitmap;
	private final Bitmap mapIconBackgroundTop;
	private final Bitmap mapIconBackgroundCenter;
	private final Bitmap mapIconBackgroundBottom;
	private final Bitmap mapIconBackgroundTopSmall;
	private final Bitmap mapIconBackgroundCenterSmall;
	private final Bitmap mapIconBackgroundBottomSmall;
	private final Drawable uiListIcon;
	private final Drawable uiBackgroundIcon;
	private final Paint paintIcon = new Paint();
	private final Paint paintForeground = new Paint();
	private final Paint paintBackground = new Paint();
	private final ColorFilter colorFilter;
	private final ColorFilter grayFilter;
	private float scale = 1.0f;
	private int mapIconSize;
	private int backSize;

	public static final int DEFAULT_SIZE_ON_MAP_DP = 16;
	public static final int ICON_SIZE_VECTOR_PX = 12;

	private PointImageDrawable(PointInfo pointInfo) {
		paintForeground.setAntiAlias(true);
		paintBackground.setAntiAlias(true);
		withShadow = pointInfo.withShadow;
		Context ctx = pointInfo.ctx;
		UiUtilities uiUtilities = ((OsmandApplication) ctx.getApplicationContext()).getUIUtilities();
		int overlayIconId = pointInfo.overlayIconId;
		mapIcon = uiUtilities.getIcon(pointInfo.synced
						? R.drawable.ic_action_flag
						: getMapIconId(ctx, overlayIconId),
				R.color.color_white);
		int col = pointInfo.color == 0 ? ContextCompat.getColor(ctx, R.color.color_favorite) : pointInfo.color;
		uiListIcon = uiUtilities.getIcon(overlayIconId, R.color.color_white);
		BackgroundType backgroundType = pointInfo.backgroundType;
		int uiBackgroundIconId = backgroundType.getIconId();
		uiBackgroundIcon = uiUtilities.getPaintedIcon(uiBackgroundIconId, col);
		mapIconBackgroundTop = backgroundType.getMapBackgroundIconId(ctx, "top", false);
		mapIconBackgroundCenter = backgroundType.getMapBackgroundIconId(ctx, "center", false);
		mapIconBackgroundBottom = backgroundType.getMapBackgroundIconId(ctx, "bottom", false);
		mapIconBackgroundTopSmall = backgroundType.getMapBackgroundIconId(ctx, "top", true);
		mapIconBackgroundCenterSmall = backgroundType.getMapBackgroundIconId(ctx, "center", true);
		mapIconBackgroundBottomSmall = backgroundType.getMapBackgroundIconId(ctx, "bottom", true);
		colorFilter = new PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN);
		grayFilter = new PorterDuffColorFilter(ContextCompat.getColor(ctx, R.color.color_favorite_gray), PorterDuff.Mode.SRC_IN);
		dp_12_px = AndroidUtils.dpToPx(pointInfo.ctx, 12);
	}

	private int getMapIconId(Context ctx, int iconId) {
		String iconName = ctx.getResources().getResourceEntryName(iconId);
		return ctx.getResources().getIdentifier(iconName
				.replaceFirst("mx_", "mm_"), "drawable", ctx.getPackageName());
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		if (!withShadow) {
			uiBackgroundIcon.setBounds(0, 0,
					uiBackgroundIcon.getIntrinsicWidth(), uiBackgroundIcon.getIntrinsicHeight());
			int offsetX = bounds.centerX() - uiListIcon.getIntrinsicWidth() / 2;
			int offsetY = bounds.centerY() - uiListIcon.getIntrinsicHeight() / 2;
			uiListIcon.setBounds(offsetX, offsetY, uiListIcon.getIntrinsicWidth() + offsetX,
					uiListIcon.getIntrinsicHeight() + offsetY);
		}
	}

	@Override
	public int getIntrinsicHeight() {
		if (withShadow) {
			return mapIconBackgroundCenter.getHeight();
		}
		return uiBackgroundIcon.getIntrinsicHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		if (withShadow) {
			return mapIconBackgroundCenter.getWidth();
		}
		return uiBackgroundIcon.getIntrinsicWidth();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		Rect bs = getBounds();
		if (withShadow) {
			drawBitmap(canvas, bs, mapIconBackgroundBottom, paintForeground);
			drawBitmap(canvas, bs, mapIconBackgroundCenter, paintBackground);
			drawBitmap(canvas, bs, mapIconBackgroundTop, paintForeground);
			int offsetX = bs.centerX() - mapIconSize / 2;
			int offsetY = bs.centerY() - mapIconSize / 2;
			Rect mapIconBounds = new Rect(offsetX, offsetY, (offsetX + mapIconSize),
					offsetY + mapIconSize);
			drawBitmap(canvas, mapIconBounds, mapIconBitmap, paintForeground);
		} else {
			uiBackgroundIcon.draw(canvas);
			uiListIcon.draw(canvas);
		}
	}

	public void drawBitmap(@NonNull Canvas canvas, Rect bs, Bitmap bitmap, Paint paintBackground) {
		canvas.drawBitmap(bitmap, null, bs, paintBackground);
	}

	public void drawPoint(Canvas canvas, float x, float y, float scale, boolean history) {
		setScale(scale);
		this.history = history;
		Rect rect = new Rect(0, 0, backSize, backSize);
		rect.offset((int) x - backSize / 2, (int) y - backSize / 2);
		setBounds(rect);
		draw(canvas);
	}

	private void setScale(float scale) {
		if (scale != this.scale || this.mapIconSize == 0) {
			this.scale = scale;
			int pixels = (int) (dp_12_px * DEFAULT_SIZE_ON_MAP_DP / 12.0);
			this.mapIconSize = Math.round((scale * pixels / ICON_SIZE_VECTOR_PX * ICON_SIZE_VECTOR_PX));
			this.backSize = (int) (scale * getIntrinsicWidth());
			mapIconBitmap = getBitmapFromVectorDrawable(mapIcon);
		}
	}

	public Bitmap getBitmapFromVectorDrawable(Drawable drawable) {
		Bitmap bitmap = Bitmap.createBitmap(mapIconSize, mapIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public void drawSmallPoint(Canvas canvas, float x, float y, float scale) {
		setScale(scale);
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		int scaledWidth = mapIconBackgroundBottomSmall.getWidth();
		int scaledHeight = mapIconBackgroundBottomSmall.getHeight();
		if (scale != 1.0f) {
			scaledWidth *= scale;
			scaledHeight *= scale;
		}
		Rect destRect = new Rect(0, 0, scaledWidth, scaledHeight);
		destRect.offset((int) x - scaledWidth / 2, (int) y - scaledHeight / 2);
		canvas.drawBitmap(mapIconBackgroundBottomSmall, null, destRect, paintForeground);
		canvas.drawBitmap(mapIconBackgroundCenterSmall, null, destRect, paintBackground);
		canvas.drawBitmap(mapIconBackgroundTopSmall, null, destRect, paintForeground);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	public void setAlpha(float alpha) {
		setAlpha((int) (255 * alpha));
	}

	@Override
	public void setAlpha(int alpha) {
		paintBackground.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintIcon.setColorFilter(cf);
	}

	private static final TreeMap<String, PointImageDrawable> cache = new TreeMap<>();

	private static PointImageDrawable getOrCreate(@NonNull PointInfo pointInfo) {

		String uniqueId = pointInfo.ctx.getResources().getResourceEntryName(pointInfo.overlayIconId);
		uniqueId += pointInfo.backgroundType.name();
		int color = pointInfo.color | 0xff000000;
		int hash = (color << 4) + ((pointInfo.withShadow ? 1 : 0) << 2) + ((pointInfo.synced ? 3 : 0) << 2);
		uniqueId = hash + uniqueId;
		PointImageDrawable drawable = cache.get(uniqueId);
		if (drawable == null) {
			drawable = new PointImageDrawable(pointInfo);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(uniqueId, drawable);
		}
		return drawable;
	}

	public static PointImageDrawable getOrCreateSyncedIcon(Context ctx, @ColorInt int color, FavouritePoint point) {
		return getFromFavorite(ctx, color, true, true, point);
	}

	public static PointImageDrawable getOrCreateSyncedIcon(Context ctx, @ColorInt int color, GPXUtilities.WptPt wpt) {
		return getFromWpt(ctx, color, true, true, wpt);
	}

	public static PointImageDrawable getFromWpt(Context ctx, @ColorInt int color, boolean withShadow,
	                                            GPXUtilities.WptPt wpt) {
		return getFromWpt(ctx, color, withShadow, false, wpt);
	}

	public static PointImageDrawable getFromWpt(Context ctx, @ColorInt int color, boolean withShadow, boolean synced,
	                                            GPXUtilities.WptPt wpt) {
		if (wpt != null) {
			int overlayIconId = ctx.getResources().getIdentifier("mx_" + wpt.getIconNameOrDefault(),
					"drawable", ctx.getPackageName());
			return getOrCreate(ctx, color, withShadow, synced, overlayIconId,
					BackgroundType.getByTypeName(wpt.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		} else {
			return getOrCreate(ctx, color, withShadow);
		}
	}

	public static PointImageDrawable getFromFavorite(Context ctx, @ColorInt int color, boolean withShadow,
	                                                 FavouritePoint favoritePoint) {
		return getFromFavorite(ctx, color, withShadow, false, favoritePoint);
	}

	public static PointImageDrawable getFromFavorite(Context ctx, @ColorInt int color, boolean withShadow,
	                                                 boolean synced, FavouritePoint favoritePoint) {
		if (favoritePoint != null) {
			return getOrCreate(ctx, color, withShadow, synced, favoritePoint.getOverlayIconId(ctx),
					favoritePoint.getBackgroundType());
		} else {
			return getOrCreate(ctx, color, withShadow);
		}
	}

	public static PointImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow) {
		return getOrCreate(ctx, color, withShadow, DEFAULT_UI_ICON_ID);
	}

	public static PointImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow, int overlayIconId) {
		return getOrCreate(ctx, color, withShadow, false, overlayIconId, BackgroundType.CIRCLE);
	}

	public static PointImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow, boolean synced,
	                                             int overlayIconId, @NonNull BackgroundType backgroundType) {
		overlayIconId = overlayIconId == 0 ? DEFAULT_UI_ICON_ID : overlayIconId;
		PointInfo pointInfo = new PointInfo(ctx, color, withShadow, overlayIconId, backgroundType);
		pointInfo.synced = synced;
		return getOrCreate(pointInfo);
	}

	private static class PointInfo {
		Context ctx;
		@ColorInt
		int color;
		boolean withShadow;
		boolean synced;
		@DrawableRes
		int overlayIconId;
		BackgroundType backgroundType;

		private PointInfo(Context ctx, int color, boolean withShadow, int overlayIconId, BackgroundType backgroundType) {
			this.ctx = ctx;
			this.color = color;
			this.withShadow = withShadow;
			this.overlayIconId = overlayIconId;
			this.backgroundType = backgroundType;
		}
	}

	@Nullable
	public Bitmap getBigMergedBitmap(float textScale, boolean history) {
		setScale(textScale);
		int width = getBigMergedBitmapWidth();
		int height = getBigMergedBitmapHeight();
		if (width == 0 || height == 0) {
			return null;
		}
		Bitmap bitmapResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		//get ready bitmap into bitmapResult
		drawPoint(canvas, width / 2.0f, height / 2.0f, scale, history);
		return bitmapResult;
	}

	private int getBigMergedBitmapWidth() {
		int width = 0;
		if (withShadow) {
			width = Math.max(width, mapIconBackgroundBottom.getWidth());
			width = Math.max(width, mapIconBackgroundCenter.getWidth());
			width = Math.max(width, mapIconBackgroundTop.getWidth());
			width = Math.max(width, mapIconBitmap.getWidth());
		} else {
			Bitmap background = getBitmapFromVectorDrawable(uiBackgroundIcon);
			Bitmap listIcon = getBitmapFromVectorDrawable(uiListIcon);
			width = Math.max(width, background.getWidth());
			width = Math.max(width, listIcon.getWidth());
		}
		return scale != 1.0f ? (int) (width * scale) : width;
	}

	private int getBigMergedBitmapHeight() {
		int height = 0;
		if (withShadow) {
			height = Math.max(height, mapIconBackgroundBottom.getHeight());
			height = Math.max(height, mapIconBackgroundCenter.getHeight());
			height = Math.max(height, mapIconBackgroundTop.getHeight());
			height = Math.max(height, mapIconBitmap.getHeight());
		} else {
			Bitmap background = getBitmapFromVectorDrawable(uiBackgroundIcon);
			Bitmap listIcon = getBitmapFromVectorDrawable(uiListIcon);
			height = Math.max(height, background.getHeight());
			height = Math.max(height, listIcon.getHeight());
		}
		return scale != 1.0f ? (int) (height * scale) : height;
	}

	@Nullable
	public Bitmap getSmallMergedBitmap(float textScale) {
		setScale(textScale);
		int width = getSmallMergedBitmapWidth();
		int height = getSmallMergedBitmapHeight();
		if (width == 0 || height == 0) {
			return null;
		}
		Bitmap bitmapResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		//get ready bitmap into bitmapResult
		drawSmallPoint(canvas, width / 2.0f, height / 2.0f, textScale);
		return bitmapResult;
	}

	private int getSmallMergedBitmapWidth() {
		int width = 0;
		width = Math.max(width, mapIconBackgroundBottomSmall.getWidth());
		width = Math.max(width, mapIconBackgroundCenterSmall.getWidth());
		width = Math.max(width, mapIconBackgroundTopSmall.getWidth());
		return scale != 1.0f ? (int) (width * scale) : width;
	}

	private int getSmallMergedBitmapHeight() {
		int height = 0;
		height = Math.max(height, mapIconBackgroundBottomSmall.getHeight());
		height = Math.max(height, mapIconBackgroundCenterSmall.getHeight());
		height = Math.max(height, mapIconBackgroundTopSmall.getHeight());
		return scale != 1.0f ? (int) (height * scale) : height;
	}
}
