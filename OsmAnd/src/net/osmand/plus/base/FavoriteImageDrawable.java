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

	private FavoriteImageDrawable(Context ctx, int color, boolean withShadow, boolean synced, FavouritePoint point) {
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
		syncedStroke = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_stroke);
		syncedColor = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_color);
		syncedShadow = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_shadow);
		syncedIcon = BitmapFactory.decodeResource(res, R.drawable.map_marker_point_14dp);
		colorFilter = new PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN);
		grayFilter = new PorterDuffColorFilter(res.getColor(R.color.color_favorite_gray), PorterDuff.Mode.MULTIPLY);
	}

	private int getMapIconId(Context ctx, int iconId) {
		String iconName = ctx.getResources().getResourceEntryName(iconId);
		return ctx.getResources().getIdentifier(iconName
				.replaceFirst("mx_", "mm_"), "drawable", ctx.getPackageName());
	}

	private int getMapBackgroundIconId(Context ctx, FavouritePoint point, String layer) {
		if (point != null) {
			int iconId = point.getBackgroundType().getIconId();
			String iconName = ctx.getResources().getResourceEntryName(iconId);
			return ctx.getResources().getIdentifier("map_" + iconName + "_" + layer
					, "drawable", ctx.getPackageName());
		}
		return R.drawable.map_white_favorite_shield;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		Rect bs = new Rect(bounds);
		if (!withShadow && !synced) {
			uiBackgroundIcon.setBounds(0, 0, uiBackgroundIcon.getIntrinsicWidth(), uiBackgroundIcon.getIntrinsicHeight());
			int offsetX = bounds.centerX() - uiListIcon.getIntrinsicWidth() / 2;
			int offsetY = bounds.centerY() - uiListIcon.getIntrinsicHeight() / 2;
			uiListIcon.setBounds(offsetX, offsetY, uiListIcon.getIntrinsicWidth() + offsetX,
					uiListIcon.getIntrinsicHeight() + offsetY);
		} else if (withShadow) {
			bs.inset(bs.width() / 3, bs.height() / 3);
			favIcon.setBounds(bs);
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
		canvas.drawBitmap(bitmap, bs.exactCenterX() - bitmap.getWidth() / 2f,
				bs.exactCenterY() - bitmap.getHeight() / 2f, paintBackground);
	}

	public void drawBitmapInCenter(Canvas canvas, float x, float y, boolean history) {
		this.history = history;
		float dx = x - getIntrinsicWidth() / 2f;
		float dy = y - getIntrinsicHeight() / 2f;
		canvas.translate(dx, dy);
		draw(canvas);
		canvas.translate(-dx, -dy);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {
		paintBackground.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintIcon.setColorFilter(cf);
	}

	private static TreeMap<String, FavoriteImageDrawable> cache = new TreeMap<>();

	private static FavoriteImageDrawable getOrCreate(Context ctx, int color, boolean withShadow, boolean synced, FavouritePoint point) {
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

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, FavouritePoint point) {
		return getOrCreate(a, color, withShadow, false, point);
	}

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, withShadow, false, getFavouriteFromWpt(a, pt));
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, FavouritePoint point) {
		return getOrCreate(a, color, false, true, point);
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, false, true, getFavouriteFromWpt(a, pt));
	}

	private static FavouritePoint getFavouriteFromWpt(Context a, GPXUtilities.WptPt pt) {
		FavouritePoint point = null;
		if (pt != null) {
			point = new FavouritePoint(pt.getLatitude(), pt.getLongitude(), pt.name, pt.category);
			point.setIconIdFromName(a, pt.getIconNameOrDefault());
			point.setBackgroundType(BackgroundType.getByTypeName(pt.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		}
		return point;
	}
}
