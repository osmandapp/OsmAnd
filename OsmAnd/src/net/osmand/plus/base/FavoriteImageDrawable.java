package net.osmand.plus.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;

import net.osmand.GPXUtilities;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.TreeMap;

public class FavoriteImageDrawable extends Drawable {

	private boolean withShadow;
	private boolean synced;
	private boolean history;
	private Bitmap favIcon;
	private Bitmap favBackground;
	private Bitmap syncedStroke;
	private Bitmap syncedColor;
	private Bitmap syncedShadow;
	private Bitmap syncedIcon;
	private Drawable listDrawable;
	private Paint paintIcon = new Paint();
	private Paint paintBackground = new Paint();
	private Paint paintOuter = new Paint();
	private Paint paintInnerCircle = new Paint();
	private ColorFilter colorFilter;
	private ColorFilter grayFilter;
	private Drawable personalPointBitmap;

	private FavoriteImageDrawable(Context ctx, int color, boolean withShadow, boolean synced, FavouritePoint point) {
		this.withShadow = withShadow;
		this.synced = synced;
		Resources res = ctx.getResources();
		int overlayIconId = point != null ? point.getOverlayIconId() : 0;
		if (overlayIconId != 0) {
			personalPointBitmap = UiUtilities.tintDrawable(ResourcesCompat.getDrawable(res, overlayIconId, null),
					ContextCompat.getColor(ctx, R.color.icon_color_default_light));
		}
		int col = color == 0 || color == Color.BLACK ? res.getColor(R.color.color_favorite) : color;
		favIcon = BitmapFactory.decodeResource(res, R.drawable.map_favorite);
		favBackground = BitmapFactory.decodeResource(res, R.drawable.map_white_favorite_shield);
		syncedStroke = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_stroke);
		syncedColor = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_color);
		syncedShadow = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_shadow);
		syncedIcon = BitmapFactory.decodeResource(res, R.drawable.map_marker_point_14dp);
		listDrawable = ResourcesCompat.getDrawable(res, R.drawable.ic_action_fav_dark, null).mutate();
		initSimplePaint(paintOuter, color == 0 || color == Color.BLACK ? 0x88555555 : color);
		initSimplePaint(paintInnerCircle, col);
		colorFilter = new PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY);
		grayFilter = new PorterDuffColorFilter(res.getColor(R.color.color_favorite_gray), PorterDuff.Mode.MULTIPLY);
	}

	private void initSimplePaint(Paint paint, int color) {
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setColor(color);
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		if (!withShadow && !synced) {
			Rect bs = new Rect(bounds);
			//bs.inset((int) (4 * density), (int) (4 * density));
			bs.inset(bs.width() / 4, bs.height() / 4);
			listDrawable.setBounds(bs);
			if (personalPointBitmap != null) {
				personalPointBitmap.setBounds(bounds);
			}
		}
	}

	@Override
	public int getIntrinsicHeight() {
		return synced ? syncedShadow.getHeight() : favBackground.getHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return synced ? syncedShadow.getWidth() : favBackground.getWidth();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		Rect bs = getBounds();
		if (synced) {
			canvas.drawBitmap(syncedShadow, bs.exactCenterX() - syncedShadow.getWidth() / 2f, bs.exactCenterY() - syncedShadow.getHeight() / 2f, paintBackground);
			canvas.drawBitmap(syncedColor, bs.exactCenterX() - syncedColor.getWidth() / 2f, bs.exactCenterY() - syncedColor.getHeight() / 2f, paintBackground);
			canvas.drawBitmap(syncedStroke, bs.exactCenterX() - syncedStroke.getWidth() / 2f, bs.exactCenterY() - syncedStroke.getHeight() / 2f, paintBackground);
			canvas.drawBitmap(syncedIcon, bs.exactCenterX() - syncedIcon.getWidth() / 2f, bs.exactCenterY() - syncedIcon.getHeight() / 2f, paintIcon);
		} else if (withShadow) {
			canvas.drawBitmap(favBackground, bs.exactCenterX() - favBackground.getWidth() / 2f, bs.exactCenterY() - favBackground.getHeight() / 2f, paintBackground);
			canvas.drawBitmap(favIcon, bs.exactCenterX() - favIcon.getWidth() / 2f, bs.exactCenterY() - favIcon.getHeight() / 2f, paintIcon);
		} else if (personalPointBitmap != null) {
			personalPointBitmap.draw(canvas);
		} else {
			int min = Math.min(bs.width(), bs.height());
			int r = (min * 4 / 10);
			int rs = (r - 1);
			canvas.drawCircle(min / 2, min / 2, r, paintOuter);
			canvas.drawCircle(min / 2, min / 2, rs, paintInnerCircle);
			listDrawable.draw(canvas);
		}
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

	private static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, boolean synced, FavouritePoint point) {
		String pointName = "";
		if (point != null && point.isSpecialPoint()) {
			pointName = point.getName();
		}
		color = color | 0xff000000;
		int hash = (color << 4) + ((withShadow ? 1 : 0) << 2) + ((synced ? 3 : 0) << 2);
		String uniqueId = hash + pointName;
		FavoriteImageDrawable drawable = cache.get(uniqueId);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(a, color, withShadow, synced, point);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(uniqueId, drawable);
		}
		return drawable;
	}

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, FavouritePoint point) {
		return getOrCreate(a, color, withShadow, false, point);
	}

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, withShadow, false, null);
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, FavouritePoint point) {
		return getOrCreate(a, color, false, true, point);
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, false, true, null);
	}
}
