package net.osmand.plus.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.data.FavouritePoint;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.TreeMap;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

public class FavoriteImageDrawable extends Drawable {

	private boolean withShadow;
	private boolean synced;
	private boolean history;
	private Drawable favIcon;
	private Bitmap favBackgroundTop;
	private Bitmap favBackgroundCenter;
	private Bitmap favBackgroundBottom;
	private Bitmap favBackgroundTopSmall;
	private Bitmap favBackgroundCenterSmall;
	private Bitmap favBackgroundBottomSmall;
	private Bitmap syncedStroke;
	private Bitmap syncedColor;
	private Bitmap syncedShadow;
	private Bitmap syncedIcon;
	private Drawable uiListIcon;
	private Drawable uiBackgroundIcon;
	private Paint paintIcon = new Paint();
	private Paint paintBackground = new Paint();
	private ColorFilter colorFilter;
	private ColorFilter grayFilter;
	private float scale = 1.0f;

	private FavoriteImageDrawable(Context ctx, @ColorInt int color, boolean withShadow, boolean synced,
	                              FavouritePoint point) {
		this.withShadow = withShadow;
		this.synced = synced;
		Resources res = ctx.getResources();
		UiUtilities uiUtilities = ((OsmandApplication) ctx.getApplicationContext()).getUIUtilities();
		int overlayIconId = point != null ? point.getOverlayIconId(ctx) : 0;
		int uiIconId;
		if (overlayIconId != 0) {
			favIcon = uiUtilities.getIcon(getMapIconId(ctx, overlayIconId), R.color.color_white);
			uiIconId = overlayIconId;
		} else {
			favIcon = res.getDrawable(R.drawable.mm_special_star);
			uiIconId = DEFAULT_UI_ICON_ID;
		}
		int col = color == 0 ? res.getColor(R.color.color_favorite) : color;
		uiListIcon = uiUtilities.getIcon(uiIconId, R.color.color_white);
		int uiBackgroundIconId = point != null ? point.getBackgroundType().getIconId() : R.drawable.bg_point_circle;
		uiBackgroundIcon = uiUtilities.getPaintedIcon(uiBackgroundIconId, col);
		int mapBackgroundIconIdTop = getMapBackgroundIconId(ctx, point, "top");
		int mapBackgroundIconIdCenter = getMapBackgroundIconId(ctx, point, "center");
		int mapBackgroundIconIdBottom = getMapBackgroundIconId(ctx, point, "bottom");
		favBackgroundTop = BitmapFactory.decodeResource(res, mapBackgroundIconIdTop);
		favBackgroundCenter = BitmapFactory.decodeResource(res, mapBackgroundIconIdCenter);
		favBackgroundBottom = BitmapFactory.decodeResource(res, mapBackgroundIconIdBottom);
		int mapBackgroundIconIdTopSmall = getMapBackgroundIconIdSmall(ctx, point, "top");
		int mapBackgroundIconIdCenterSmall = getMapBackgroundIconIdSmall(ctx, point, "center");
		int mapBackgroundIconIdBottomSmall = getMapBackgroundIconIdSmall(ctx, point, "bottom");
		favBackgroundTopSmall = BitmapFactory.decodeResource(res, mapBackgroundIconIdTopSmall);
		favBackgroundCenterSmall = BitmapFactory.decodeResource(res, mapBackgroundIconIdCenterSmall);
		favBackgroundBottomSmall = BitmapFactory.decodeResource(res, mapBackgroundIconIdBottomSmall);
		syncedStroke = BitmapFactory.decodeResource(res, R.drawable.ic_shield_marker_point_stroke);
		syncedColor = BitmapFactory.decodeResource(res, R.drawable.ic_shield_marker_point_color);
		syncedShadow = BitmapFactory.decodeResource(res, R.drawable.ic_shield_marker_point_shadow);
		syncedIcon = BitmapFactory.decodeResource(res, R.drawable.ic_marker_point_14dp);
		colorFilter = new PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN);
		grayFilter = new PorterDuffColorFilter(res.getColor(R.color.color_favorite_gray), PorterDuff.Mode.MULTIPLY);
	}

	private int getMapIconId(Context ctx, int iconId) {
		String iconName = ctx.getResources().getResourceEntryName(iconId);
		return ctx.getResources().getIdentifier(iconName
				.replaceFirst("mx_", "mm_"), "drawable", ctx.getPackageName());
	}

	private int getMapBackgroundIconIdSmall(Context ctx, FavouritePoint point, String layer) {
		if (point != null) {
			int iconId = point.getBackgroundType().getIconId();
			String iconName = ctx.getResources().getResourceEntryName(iconId);
			return ctx.getResources().getIdentifier("ic_" + iconName + "_" + layer + "_small",
					"drawable", ctx.getPackageName());
		}
		return R.drawable.ic_white_shield_small;
	}

	private int getMapBackgroundIconId(Context ctx, FavouritePoint point, String layer) {
		if (point != null) {
			int iconId = point.getBackgroundType().getIconId();
			String iconName = ctx.getResources().getResourceEntryName(iconId);
			return ctx.getResources().getIdentifier("ic_" + iconName + "_" + layer
					, "drawable", ctx.getPackageName());
		}
		return R.drawable.ic_white_favorite_shield;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		if (!withShadow && !synced) {
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
		if (synced) {
			return syncedShadow.getHeight();
		} else if (withShadow) {
			return favBackgroundCenter.getHeight();
		}
		return uiBackgroundIcon.getIntrinsicHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		if (synced) {
			return syncedShadow.getWidth();
		} else if (withShadow) {
			return favBackgroundCenter.getWidth();
		}
		return uiBackgroundIcon.getIntrinsicWidth();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		Rect bs = getBounds();
		if (synced) {
			drawBitmap(canvas, bs, syncedShadow, paintBackground);
			drawBitmap(canvas, bs, syncedColor, paintBackground);
			drawBitmap(canvas, bs, syncedStroke, paintBackground);
			drawBitmap(canvas, bs, syncedIcon, paintIcon);
		} else if (withShadow) {
			drawBitmap(canvas, bs, favBackgroundBottom, null);
			drawBitmap(canvas, bs, favBackgroundCenter, paintBackground);
			drawBitmap(canvas, bs, favBackgroundTop, null);
			favIcon.draw(canvas);
		} else {
			uiBackgroundIcon.draw(canvas);
			uiListIcon.draw(canvas);
		}
	}

	public void drawBitmap(@NonNull Canvas canvas, Rect bs, Bitmap bitmap, Paint paintBackground) {
		canvas.drawBitmap(bitmap, null, bs, paintBackground);
	}

	private void drawInCenter(Canvas canvas, Rect destRect, boolean history) {
		this.history = history;
		setBounds(destRect);
		int offsetX = destRect.centerX() - (int) (favIcon.getIntrinsicWidth() / 2 * scale);
		int offsetY = destRect.centerY() - (int) (favIcon.getIntrinsicHeight() / 2 * scale);
		favIcon.setBounds(offsetX, offsetY, (int) (offsetX + favIcon.getIntrinsicWidth() * scale),
				offsetY + (int) (favIcon.getIntrinsicHeight() * scale));
		draw(canvas);
	}

	public void drawPoint(Canvas canvas, float x, float y, float scale, boolean history) {
		this.scale = scale;
		int scaledWidth = getIntrinsicWidth();
		int scaledHeight = getIntrinsicHeight();
		if (scale != 1.0f) {
			scaledWidth *= scale;
			scaledHeight *= scale;
		}
		Rect rect = new Rect(0, 0, scaledWidth, scaledHeight);
		rect.offset((int) x - scaledWidth / 2, (int) y - scaledHeight / 2);
		drawInCenter(canvas, rect, history);
	}

	public void drawSmallPoint(Canvas canvas, float x, float y, float scale) {
		this.scale = scale;
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		int scaledWidth = favBackgroundBottomSmall.getWidth();
		int scaledHeight = favBackgroundBottomSmall.getHeight();
		if (scale != 1.0f) {
			scaledWidth *= scale;
			scaledHeight *= scale;
		}
		Rect destRect = new Rect(0, 0, scaledWidth, scaledHeight);
		destRect.offset((int) x - scaledWidth / 2, (int) y - scaledHeight / 2);
		canvas.drawBitmap(favBackgroundBottomSmall, null, destRect, null);
		canvas.drawBitmap(favBackgroundCenterSmall, null, destRect, paintBackground);
		canvas.drawBitmap(favBackgroundTopSmall, null, destRect, null);
	}

	@Override
	public int getOpacity() {
		return 0;
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

	public void setScale(float scale) {
		this.scale = scale;
	}

	private static TreeMap<String, FavoriteImageDrawable> cache = new TreeMap<>();

	private static FavoriteImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow,
	                                                 boolean synced, FavouritePoint point) {
		String uniqueId = "";
		if (point != null) {
			uniqueId = point.getIconEntryName(ctx);
			uniqueId += point.getBackgroundType().name();
		}
		color = color | 0xff000000;
		int hash = (color << 4) + ((withShadow ? 1 : 0) << 2) + ((synced ? 3 : 0) << 2);
		uniqueId = hash + uniqueId;
		FavoriteImageDrawable drawable = cache.get(uniqueId);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(ctx, color, withShadow, synced, point);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(uniqueId, drawable);
		}
		return drawable;
	}

	public static FavoriteImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow,
	                                                FavouritePoint point) {
		return getOrCreate(ctx, color, withShadow, false, point);
	}

	public static FavoriteImageDrawable getOrCreate(Context ctx, @ColorInt int color, boolean withShadow,
	                                                GPXUtilities.WptPt pt) {
		return getOrCreate(ctx, color, withShadow, false, getFavouriteFromWpt(ctx, pt));
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context ctx, @ColorInt int color, FavouritePoint point) {
		return getOrCreate(ctx, color, false, true, point);
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context ctx, @ColorInt int color, GPXUtilities.WptPt pt) {
		return getOrCreate(ctx, color, false, true, getFavouriteFromWpt(ctx, pt));
	}

	private static FavouritePoint getFavouriteFromWpt(Context ctx, GPXUtilities.WptPt pt) {
		FavouritePoint point = null;
		if (pt != null) {
			point = new FavouritePoint(pt.getLatitude(), pt.getLongitude(), pt.name, pt.category);
			point.setIconIdFromName(ctx, pt.getIconNameOrDefault());
			point.setBackgroundType(BackgroundType.getByTypeName(pt.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		}
		return point;
	}
}
