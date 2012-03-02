package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

public abstract class OsmandMapLayer {
	
	
	public abstract void initLayer(OsmandMapTileView view);
	
	public abstract void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings);
	
	public abstract void destroyLayer();
	
	public boolean onSingleTap(PointF point) {
		return false;
	}
	
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public boolean onTouchEvent(MotionEvent event) { return false;}

	/**
	 * Provide a method for layers to consume scrolling event, stopping map from being dragged
	 * Any layer should return true to consume event.
	 */
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	/**
	 * Provide a method for layers to consume fling event, stopping map from being moved
	 * Any layer should return true to consume event.
	 */
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}
	
	/**
	 * This method returns whether canvas should be rotated as 
	 * map rotated before {@link #onDraw(Canvas)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public abstract boolean drawInScreenPixels();
	
	public static class DrawSettings {
		private final boolean nightMode;
		private final boolean force;

		public DrawSettings(boolean nightMode) {
			this(nightMode,false);
		}

		public DrawSettings(boolean nightMode, boolean force) {
			this.nightMode = nightMode;
			this.force = force;
		}
		
		public boolean isForce() {
			return force;
		}
		
		public boolean isNightMode() {
			return nightMode;
		}
	}

}
