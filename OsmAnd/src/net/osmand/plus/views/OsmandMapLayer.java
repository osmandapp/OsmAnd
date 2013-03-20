package net.osmand.plus.views;

import java.util.Map;

import net.osmand.plus.ContextMenuAdapter;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

public abstract class OsmandMapLayer {
	
	
	public abstract void initLayer(OsmandMapTileView view);
	
	public abstract void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings);
	
	public abstract void destroyLayer();
	
	public void onRetainNonConfigurationInstance(Map<String, Object> map) {}
	
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {}
	
	public boolean onSingleTap(PointF point) {
		return false;
	}
	
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

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
	
	public boolean onTouchEvent(MotionEvent event) { return false;}
	
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
