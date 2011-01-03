package net.osmand.views;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;

public interface OsmandMapLayer {
	
	
	public void initLayer(OsmandMapTileView view);
	
	public void onDraw(Canvas canvas, RectF latlonRect, boolean nightMode);
	
	public void destroyLayer();
	
	public boolean onTouchEvent(PointF point);
	
	public boolean onLongPressEvent(PointF point);
	
	/**
	 * This method returns whether canvas should be rotated as 
	 * map rotated before {@link #onDraw(Canvas)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public boolean drawInScreenPixels();

}
