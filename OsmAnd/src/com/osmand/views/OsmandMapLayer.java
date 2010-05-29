package com.osmand.views;

import android.graphics.Canvas;
import android.view.MotionEvent;

public interface OsmandMapLayer {
	
	
	public void initLayer(OsmandMapTileView view);
	
	public void onDraw(Canvas canvas);
	
	public void destroyLayer();
	
	public boolean onTouchEvent(MotionEvent event);
	
	public boolean drawInScreenPixels();

}
