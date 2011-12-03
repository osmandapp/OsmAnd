package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class MapInfoControl extends View {
	int width = 0;
	int height = 0;
	Rect padding = new Rect();

	public MapInfoControl(Context ctx, int background) {
		super(ctx);
		Drawable drawable = ctx.getResources().getDrawable(background).mutate();
		drawable.getPadding(padding);
		setBackgroundDrawable(drawable);
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
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.translate(padding.left, padding.top);
		canvas.clipRect(0, 0, getWWidth(),getWHeight());
	}
	
	public boolean updateInfo() { return false; }
	
	protected boolean updateVisibility(boolean visible) {
		if (visible != (getVisibility() == View.VISIBLE)) {
			if (visible) {
				setVisibility(View.VISIBLE);
			} else {
				setVisibility(View.GONE);
			}
			requestLayout();
			return true;
		}
		return false;
	}
	

}