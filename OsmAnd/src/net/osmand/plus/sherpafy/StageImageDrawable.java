package net.osmand.plus.sherpafy;

import net.osmand.plus.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class StageImageDrawable extends Drawable {

	Paint paintInnerCircle;
	private Resources resources;
	private Paint paintOuter;
	private Drawable drawable;
	private float density;

	private TextPaint textPaint;
	private String text;

	private Paint paintBmp;

	private int color;
	public static final int STAGE_COLOR = 0xff2897d4;
	public static final int INFO_COLOR = 0xffadc90e;
	public static final int MENU_COLOR = 0xffb9b9b9;
	public static final int MENU_TCOLOR = 0xff8f8f8f;
	

	public StageImageDrawable(Context ctx, int color, String text, int drawableRes) {
		this.resources = ctx.getResources();
		this.color = color;
		WindowManager mgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		density = dm.density;

		if (text != null) {
			this.text = text;
			textPaint = new TextPaint();
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			textPaint.setTextAlign(Align.CENTER);
			textPaint.setColor(ctx.getResources().getColor(R.color.color_white));
		} else {
			drawable = getResources().getDrawable(drawableRes);
			paintBmp = new Paint();
			paintBmp.setAntiAlias(true);
			paintBmp.setFilterBitmap(true);
			paintBmp.setDither(true);
		}
		paintOuter = new Paint();
		paintOuter.setColor(0x88555555);
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);

		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle.setColor(color);
		paintInnerCircle.setAntiAlias(true);
	}

//	@Override
//	public int getIntrinsicHeight() {
//		return (int) (drawable.getIntrinsicHeight() + 8 * density);
//	}
//
//	@Override
//	public int getIntrinsicWidth() {
//		return (int) (drawable.getIntrinsicWidth() + 8 * density);
//	}

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
		if(textPaint != null) {
			textPaint.setTextSize(bs.height() * 5 / 8);
		}
		if(drawable != null) {
			drawable.setBounds(bs);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		// int max = Math.max(drawable.getMinimumHeight(), drawable.getMinimumWidth());
		Rect bs = getBounds();
		int cx = bs.width() / 2;
		int cy = bs.height() / 2;
		int rx = (int) (Math.min(bs.width(), bs.height()) - 8 * density) / 2;
		canvas.drawCircle(cx, cy, rx, paintInnerCircle);
		if (drawable != null) {
			drawable.draw(canvas);
		} else if(text != null ){
			canvas.drawText(text, cx, cy + rx / 2, textPaint);
		}
	}

	public int getColor() {
		return color;
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

}
