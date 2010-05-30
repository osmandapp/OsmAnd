package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.PointF;

public interface OsmandMapLayer {
	
	
	public void initLayer(OsmandMapTileView view);
	
	public void onDraw(Canvas canvas);
	
	public void destroyLayer();
	
	public boolean onTouchEvent(PointF point);
	
	public boolean onLongPressEvent(PointF point);
	
	public boolean drawInScreenPixels();

}
