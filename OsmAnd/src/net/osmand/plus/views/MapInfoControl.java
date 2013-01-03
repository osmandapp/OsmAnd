package net.osmand.plus.views;

import net.osmand.plus.ClientContext;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class MapInfoControl extends View implements MapControlUpdateable {
	int width = 0;
	int height = 0;
	Rect padding = new Rect();
	int shadowColor = Color.WHITE;

	public MapInfoControl(Context ctx) {
		super(ctx);
	}
	
	@Override
	public void setBackgroundDrawable(Drawable d) {
		d.getPadding(padding);
		super.setBackgroundDrawable(d);
	}

	
	public void setWDimensions(int w, int h){
		setMeasuredDimension(w + padding.left + padding.right, h + padding.top + padding.bottom);
	}
	
	@Override
	protected final void onLayout(boolean changed, int left, int top, int right, int bottom) {
		onWLayout(right - left - padding.left - padding.right, bottom - top - padding.bottom - padding.top);
	}
	
	public int getWHeight(){
		return getBottom() - getTop() - padding.top - padding.bottom;
	}
	
	public int getWWidth(){
		return getRight() - getLeft() - padding.left - padding.right; 
	}
	
	// To override
	protected void onWLayout(int w, int h) {

	}
	
	protected void drawShadowText(Canvas cv, String text, float centerX, float centerY, Paint textPaint) {
		ShadowText.draw(text, cv, centerX, centerY, textPaint, shadowColor);
	}
	
	public void setShadowColor(int shadowColor) {
		this.shadowColor = shadowColor;
	}
	
	public int getShadowColor() {
		return shadowColor;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.translate(padding.left, padding.top);
		canvas.clipRect(0, 0, getWWidth(),getWHeight());
	}
	
	@Override
	public boolean updateInfo() { return false; }
	
	protected boolean updateVisibility(boolean visible) {
		if (visible != (getVisibility() == View.VISIBLE)) {
			if (visible) {
				setVisibility(View.VISIBLE);
			} else {
				setVisibility(View.GONE);
			}
			requestLayout();
			invalidate();
			return true;
		}
		return false;
	}
	
	public ClientContext getClientContext(){
		return (ClientContext) getContext().getApplicationContext();
	}
	

}