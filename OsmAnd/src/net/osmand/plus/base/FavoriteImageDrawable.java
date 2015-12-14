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

import net.osmand.plus.R;

import java.util.TreeMap;

public class FavoriteImageDrawable extends Drawable {

	private int color;
	private Paint paintIcon;
	private Paint paintBackground;
	private Bitmap favIcon;
	private Bitmap favBackground;
	private Resources resources;
	private boolean withShadow;
	private Paint paintOuter;
	private Paint paintInnerCircle;
	private Drawable listDrawable;

	public FavoriteImageDrawable(Context ctx, int color, boolean withShadow) {
		this.withShadow = withShadow;
		this.resources = ctx.getResources();
		this.color = color;
		paintBackground = new Paint();
		int col = color == 0 || color == Color.BLACK ? getResources().getColor(R.color.color_favorite) : color;
		paintBackground.setColorFilter(new PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY));
		paintIcon = new Paint();
		favIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.map_favorite);
		favBackground = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.map_white_favorite_shield);
		listDrawable = getResources().getDrawable(R.drawable.ic_action_fav_dark).mutate();

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
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		
		if (!withShadow) {
			Rect bs = new Rect(bounds);
			 //bs.inset((int) (4 * density), (int) (4 * density));
			bs.inset(bs.width() / 4, bs.height() / 4);
			listDrawable.setBounds(bs);
		}
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
		if(withShadow) {
			canvas.drawBitmap(favBackground, bs.exactCenterX() - favBackground.getWidth() / 2f, bs.exactCenterY() - favBackground.getHeight() / 2f, paintBackground);
			canvas.drawBitmap(favIcon, bs.exactCenterX() - favIcon.getWidth() / 2f, bs.exactCenterY() - favIcon.getHeight() / 2f, paintIcon);
		} else {
			int min = Math.min(bs.width(), bs.height());
			int r = (min * 4 / 10);
			int rs = (r - 1);
			canvas.drawCircle(min / 2, min / 2, r, paintOuter);
			canvas.drawCircle(min / 2, min / 2, rs, paintInnerCircle);
			listDrawable.draw(canvas);
		}
	}

	public void drawBitmapInCenter(Canvas canvas, int x, int y) {
		int dx = x - getIntrinsicWidth() / 2;
		int dy = y - getIntrinsicHeight() / 2;
		canvas.translate(dx, dy);
		draw(canvas);
		canvas.translate(-dx, -dy);
	}

	public void drawBitmapInCenter(Canvas canvas, float x, float y) {
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

	private static TreeMap<Integer, FavoriteImageDrawable> cache = new TreeMap<>();

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow) {
		color = color | 0xff000000;
		int hash = (color << 2) + (withShadow ? 1 : 0);
		FavoriteImageDrawable drawable = cache.get(hash);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(a, color, withShadow);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(hash, drawable);
		}
		return drawable;
	}
}
