package net.osmand.plus.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import net.osmand.plus.R;

import java.util.TreeMap;

public class FavoriteImageDrawable extends Drawable {

	private int color;
	private Paint paintIcon;
	private Paint paintBackground;
	private Bitmap favIcon;
	private Bitmap favBackground;
	private Resources resources;

	public FavoriteImageDrawable(Context ctx, int color) {
		this.resources = ctx.getResources();
		this.color = color;
		paintIcon = new Paint();
		int col = color == 0 || color == Color.BLACK ? getResources().getColor(R.color.color_favorite) : color;
		paintIcon.setColorFilter(new PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN));
		paintBackground = new Paint();
		favIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.map_favorite);
		favBackground = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.map_white_favorite_shield);
	}

	@Override
	public int getIntrinsicHeight() {
		return favBackground.getHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return favBackground.getWidth();
	}

	public int getColor() {
		return color;
	}

	public Resources getResources() {
		return resources;
	}

	@Override
	public void draw(Canvas canvas) {
		Rect bs = getBounds();
		canvas.drawBitmap(favBackground, bs.exactCenterX() - favBackground.getWidth() / 2f, bs.exactCenterY() - favBackground.getHeight() / 2f, paintBackground);
		canvas.drawBitmap(favIcon, bs.exactCenterX() - favIcon.getWidth() / 2f, bs.exactCenterY() - favIcon.getHeight() / 2f, paintIcon);
	}

	public void drawBitmapInCenter(Canvas canvas, int x, int y) {
		int dx = x - getIntrinsicWidth() / 2;
		int dy = y - getIntrinsicHeight() / 2;
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

	private static TreeMap<Integer, FavoriteImageDrawable> cache = new TreeMap<>();

	public static FavoriteImageDrawable getOrCreate(Context a, int color, float density) {
		color = color | 0xff000000;
		int hash = (color << 2) + (int) (density * 6);
		FavoriteImageDrawable drawable = cache.get(hash);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(a, color);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(hash, drawable);
		}
		return drawable;
	}
}
