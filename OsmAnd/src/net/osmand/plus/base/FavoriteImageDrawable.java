package net.osmand.plus.base;

import java.util.TreeMap;

import net.osmand.plus.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class FavoriteImageDrawable extends Drawable {

	private int color;
	Paint paintInnerCircle;
	private Resources resources;
	private Paint paintOuter;
	private Drawable drawable;
	private float density;

	public FavoriteImageDrawable(Context ctx, int color, float d) {
		this.resources = ctx.getResources();
		this.color = color;
		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		this.density = d;
		if (this.density == 0) {
			DisplayMetrics dm = new DisplayMetrics();
			mgr.getDefaultDisplay().getMetrics(dm);
			density = dm.density;
		}
		drawable = getResources().getDrawable(R.drawable.ic_action_fav_dark);
		paintOuter = new Paint();
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintOuter.setColor(color == 0 || color == Color.BLACK ? 0x88555555 : color);
		paintInnerCircle.setColor(color == 0 || color == Color.BLACK ? getResources().getColor(R.color.color_favorite)
				: color);
		paintInnerCircle.setAntiAlias(true);
	}

	@Override
	public int getIntrinsicHeight() {
		return (int) (24 * density);
//		return (int) (drawable.getIntrinsicHeight() + 8 * density);
	}

	@Override
	public int getIntrinsicWidth() {
		return (int) (24 * density);
//		return (int) (drawable.getIntrinsicWidth() + 8 * density);
	}

	public int getColor() {
		return color;
	}

	public Resources getResources() {
		return resources;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		Rect bs = new Rect(bounds);
		bs.inset((int) (4 * density), (int) (4 * density));
		// int min = Math.min(bounds.width(), bounds.height());
		// bs.inset((int)(bs.width() - min + 3 * density) / 2,
		// (int) (bs.height() - min + 3 * density) / 2);
		drawable.setBounds(bs);
	}

	@Override
	public void draw(Canvas canvas) {
		// int max = Math.max(drawable.getMinimumHeight(), drawable.getMinimumWidth());
		Rect bs = getBounds();
		int min = Math.min(bs.width(), bs.height());
		int r = (int) (min / 2);
		int rs = (int) (min / 2 - 1);
		canvas.drawCircle(min / 2, min / 2, r, paintOuter);
		canvas.drawCircle(min / 2, min / 2, rs, paintInnerCircle);
		drawable.draw(canvas);
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
		paintInnerCircle.setAlpha(alpha);

	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintInnerCircle.setColorFilter(cf);
	}

	private static TreeMap<Integer, FavoriteImageDrawable> cache = new TreeMap<Integer, FavoriteImageDrawable>();

	public static FavoriteImageDrawable getOrCreate(Context a, int color, float density) {
		color = color | 0xff000000;
		int hash = (color << 2) + (int) (density * 6);
		FavoriteImageDrawable drawable = cache.get(hash);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(a, color, density);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(hash, drawable);
		}
		return drawable;
	}

}
