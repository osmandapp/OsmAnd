package net.osmand.plus.base;

import java.util.TreeMap;

import net.osmand.plus.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
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
	private Bitmap bmp;
	private Paint paintBmp;
	private RectF bmpDest;
	

	public FavoriteImageDrawable(Context ctx, int color) {
		this.resources = ctx.getResources();
		this.color = color;
		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		density = dm.density;
		drawable = getResources().getDrawable(R.drawable.ic_action_fav_dark);
		bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_fav_light);
		bmpDest = new RectF(); 
		paintOuter = new Paint();
		paintOuter.setColor(0x88555555);
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);
		paintBmp = new Paint();
		paintBmp.setAntiAlias(true);
		paintBmp.setFilterBitmap(true);
		paintBmp.setDither(true);
		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle.setColor(color == 0 || color == Color.BLACK ? getResources().getColor(R.color.color_favorite) : color);
		paintInnerCircle.setAntiAlias(true);
	}
	
	@Override
	public int getIntrinsicHeight() {
		return (int) (drawable.getIntrinsicHeight() + 8 * density);
	}
	
	@Override
	public int getIntrinsicWidth() {
		return (int) (drawable.getIntrinsicWidth() + 8 * density);
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
		bs.inset((int)(4 * density), (int) (4 * density));
//		int min = Math.min(bounds.width(), bounds.height());
//		bs.inset((int)(bs.width() - min + 3 * density) / 2, 
//				(int) (bs.height() - min + 3 * density) / 2);
		drawable.setBounds(bs);
	}

	@Override
	public void draw(Canvas canvas) {
		// int max = Math.max(drawable.getMinimumHeight(), drawable.getMinimumWidth());
		Rect bs = getBounds();
		int min = Math.min(bs.width(), bs.height());
		int r = (int) (min / 2);
		int rs = (int) (min / 2 - 2 * density);
		canvas.drawCircle(min / 2 , min / 2 + density, r, paintOuter);
		canvas.drawCircle(min / 2 , min / 2 + density, rs, paintInnerCircle);
		drawable.draw(canvas);
	}
	
	public void drawBitmapInCenter(Canvas canvas, int x, int y, float density) {
		float bmpRad = 10 * density;
		bmpDest.set(x - bmpRad, y - bmpRad, x + bmpRad, y + bmpRad);
		canvas.drawCircle(x, density + y, bmpRad + 3 * density, paintOuter);
		canvas.drawCircle(x, density + y, bmpRad + 2 * density, paintInnerCircle);
		canvas.drawBitmap(bmp, null, bmpDest, paintBmp);
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

	public static FavoriteImageDrawable getOrCreate(Context a, int color) {
		color = color | 0xff000000;
		FavoriteImageDrawable drawable = cache.get(color);
		if(drawable == null) {
			drawable = new FavoriteImageDrawable(a, color);
			cache.put(color, drawable);
		}
		return drawable;
	}

	
}
