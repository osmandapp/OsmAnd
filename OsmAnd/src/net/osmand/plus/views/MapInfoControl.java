package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public abstract class MapInfoControl extends View {
	int width = 0;
	int height = 0;
	Drawable background;

	public MapInfoControl(Context ctx, int background) {
		super(ctx);
		this.background = ctx.getResources().getDrawable(background).mutate();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		background.setState(getDrawableState());
		invalidate();
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
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		background.setBounds(0, 0, right - left, bottom - top);
	}

	@Override
	protected void onDraw(Canvas cv) {
		background.draw(cv);
	}
}